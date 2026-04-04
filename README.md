# Craftalism Economy

> Paper/Spigot Minecraft plugin that delegates all economy data to the Craftalism API, keeping balance state centralized and commands responsive via fully asynchronous execution.

---

## Overview

Instead of storing balances inside the Minecraft server, this plugin acts as an asynchronous HTTP client to the Craftalism backend. Players interact with economy commands in-game; the plugin translates those commands into authenticated API calls, caches results locally, and maps API responses back to player-facing messages.

**Key capabilities:**

- `/balance`, `/pay`, `/setbalance`, and `/baltop` economy commands.
- Fully asynchronous command execution via `CompletableFuture` to avoid blocking the main server thread.
- OAuth2 `client_credentials` authentication with automatic token caching.
- Caffeine-based in-memory cache for players and balances to reduce API round-trips.
- Atomic `/pay` execution through a single API transfer request.
- Automatic player and balance provisioning on player join.
- Config-driven currency formatting (locale, symbol, fallback representation).
- YAML-driven player-facing message templates.

---

## Architecture

The codebase follows a strict four-layer architecture. Dependencies point inward: presentation and infrastructure depend on application and domain; domain has no outward dependencies.

### Presentation layer (`presentation/`)

Bukkit command executors and event listeners. Validates input, calls application services, and maps results to player messages.

| Component | Responsibility |
|---|---|
| `PayCommand` | Handles `/pay <player> <amount>`. |
| `BalanceCommand` | Handles `/balance [player]`. |
| `SetBalanceCommand` | Handles `/setbalance <player> <amount>`. |
| `BaltopCommand` | Handles `/baltop`. |
| `OnJoin` | Provisions player and balance in the API and cache on login. |
| `PlayerNameCheck` | Input validation for player name arguments. |

### Application layer (`application/`)

Use-case orchestration. Each service coordinates domain and infrastructure calls and returns explicit status enums or result DTOs. No low-level exceptions reach the presentation layer.

### Domain layer (`domain/`)

Core models (`Player`, `Balance`, `Transaction`) and business rules (`FundsTransfer`, `AmountCheck`, `CurrencyFormatter`, `CurrencyParser`). No framework dependencies.

### Infrastructure layer (`infra/`)

External integrations: HTTP client, OAuth2 token service, API service classes (`PlayerApiService`, `BalanceApiService`, `TransactionApiService`), Caffeine cache repositories, config loading, and the plugin bootstrap container.

---

## Tech Stack

| Category | Technology |
|---|---|
| Language | Java 21 |
| Minecraft API | Paper API 1.21.4-R0.1-SNAPSHOT |
| HTTP Client | Java `HttpClient` |
| Serialization | Gson |
| Cache | Caffeine |
| Build Tool | Gradle Wrapper |
| Testing | JUnit 5, Mockito, MockBukkit |

---

## Prerequisites

- Java 21+
- A running Paper-compatible Minecraft server (1.21.4+)
- A running instance of the Craftalism API and Craftalism Authorization Server

---

## Configuration

Configuration files are bundled in the JAR and written to the plugin data folder on first run.

### `config.yml`

| Key | Default | Description |
|---|---|---|
| `default-balance` | `100000000` | Initial balance for new players (internal scaled integer). |
| `locale` | `en-US` | Locale used for currency formatting. |
| `currency-symbol` | `$` | Symbol prepended to formatted amounts. |
| `null-representation` | — | String shown when a value cannot be formatted. |

### `connection-config.yml`

| Key | Description |
|---|---|
| `api-base-url` | Base URL of the Craftalism API. |
| `auth-issuer-uri` | OAuth2 issuer URI (Authorization Server). |
| `auth-token-path` | Token endpoint path relative to the issuer URI. |
| `client-id` | OAuth2 client ID. |
| `client-secret` | OAuth2 client secret. **Use environment variable override in production.** |
| `client-scopes` | Space-separated list of requested scopes. |
| HTTP timeouts | Connection and read timeout values. |

### `logs.yml`

All player-facing message templates and command output prefixes.

### Environment variable overrides

`ConfigLoader` reads the following variables and applies them over `connection-config.yml` values at startup.

| Variable | Overrides |
|---|---|
| `CRAFTALISM_API_URL` | `api-base-url` |
| `AUTH_ISSUER_URI` | `auth-issuer-uri` |
| `AUTH_TOKEN_PATH` | `auth-token-path` |
| `MINECRAFT_CLIENT_ID` | `client-id` |
| `MINECRAFT_CLIENT_SECRET` or `CRAFTALISM_API_KEY` | `client-secret` |
| `MINECRAFT_CLIENT_SCOPES` | `client-scopes` |

> **Important:** Set `MINECRAFT_CLIENT_SECRET` (or `CRAFTALISM_API_KEY`) as an environment variable in production. Do not hardcode the client secret in `connection-config.yml`.

---

## Running Locally

### Build

```bash
cd java
./gradlew clean build
```

The plugin JAR is output to `java/build/libs/`.

### Install

1. Copy the generated JAR into your Paper server `plugins/` directory.
2. Start the server once to generate the config files in `plugins/craftalism-economy/`.
3. Edit `connection-config.yml` with the correct API and auth server URLs and credentials.
4. Restart the server.

For local development, start the Craftalism API and Authorization Server first, then point `connection-config.yml` at them before launching the Minecraft server.

---

## Commands and Permissions

| Command | Permission | Default | Description |
|---|---|---|---|
| `/pay <player> <amount>` | `craftalism.pay` | true | Transfer funds to another player. |
| `/balance [player]` (self) | `craftalism.balance.self` | true | Check your own balance. |
| `/balance <player>` (other) | `craftalism.balance.other` | op | Check another player's balance. |
| `/setbalance <player> <amount>` | `craftalism.setbalance` | op | Set a player's balance directly. |
| `/baltop` | `craftalism.baltop` | true | List the top balances. |

---

## API Reference

The plugin calls the following Craftalism API endpoints. All requests carry a Bearer token obtained via OAuth2 `client_credentials`.

| Method | Path | Description |
|---|---|---|
| `GET` | `/api/players/{uuid}` | Look up a player by UUID. |
| `GET` | `/api/players/name/{name}` | Look up a player by display name. |
| `POST` | `/api/players` | Register a new player. |
| `GET` | `/api/balances/{uuid}` | Get a player's balance. |
| `POST` | `/api/balances` | Create a balance record. |
| `PUT` | `/api/balances/{uuid}/set` | Set a player's balance. |
| `POST` | `/api/balances/{uuid}/deposit` | Deposit funds. |
| `POST` | `/api/balances/{uuid}/withdraw` | Withdraw funds. |
| `POST` | `/api/transfers` | Execute an atomic player-to-player transfer. |
| `GET` | `/api/balances/top` | Get top balances. |

### `/pay` flow

1. Validate sender permissions and command arguments.
2. Resolve the sender from cache or API; resolve the receiver by name from the API.
3. Validate amount and reject self-payment.
4. Verify the sender has enough balance.
5. Submit a single atomic transfer request to the API.
6. Map the result status to player-facing messages.

---

## Testing

```bash
cd java
./gradlew test
```

Unit tests cover the presentation, application, domain, and infrastructure layers using JUnit 5, Mockito, and MockBukkit.

---

## Project Structure

```text
java/
├── build.gradle
├── gradlew
└── src/
    ├── main/java/io/github/HenriqueMichelini/craftalism/economy/
    │   ├── presentation/
    │   ├── application/
    │   ├── domain/
    │   └── infra/
    ├── main/resources/
    │   ├── plugin.yml
    │   ├── config.yml
    │   ├── connection-config.yml
    │   └── logs.yml
    └── test/java/
```

---

## Known Limitations

- The `OnQuit` listener class exists but is not registered in `EventRegistrar`; player quit events are not handled.
- `BootContainer#shutdown()` is a placeholder and does not perform any cleanup.
- `/setbalance` input validation only accepts numeric digit strings before scaling; it does not accept decimal input.
- No integration or end-to-end tests run against a real API instance.

---

## Roadmap

- Register `OnQuit` in `EventRegistrar` and implement cache eviction on player disconnect.
- Implement `BootContainer#shutdown()` to clean up HTTP client and cache resources.
- Fix `/setbalance` to accept decimal input consistently with `/pay`.
- Add integration tests against a live Craftalism API instance.

---

## License

MIT. See [`LICENSE`](./LICENSE) for details.
