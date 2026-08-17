# Craftalism Economy

Craftalism Economy is a Paper Minecraft plugin that delegates player and balance data to the Craftalism API. Economy commands use asynchronous, authenticated HTTP requests so network work does not block the server thread; player-facing responses are scheduled back onto the server thread.

## Features

- `/balance`, `/pay`, `/setbalance`, and `/baltop` commands.
- OAuth 2.0 `client_credentials` authentication with token caching and request coalescing.
- Caffeine caches for player and balance records (10,000 entries, 30-minute expiry after write).
- Player and balance provisioning when a player joins.
- Atomic payments through the balance transfer endpoint, with an optional legacy withdraw/deposit fallback.
- Configurable currency formatting and YAML-based player messages.

## Architecture

The Java sources are organized into four main packages:

| Package | Responsibility |
|---|---|
| `presentation` | Bukkit command executors, event listeners, and command input validation. |
| `application` | Command and lifecycle orchestration, result mapping, and cache coordination. |
| `domain` | Economy models, amount rules, currency formatting, and message helpers. Some helpers depend on the Bukkit API. |
| `infra` | HTTP and OAuth clients, API DTOs and services, Caffeine repositories, configuration loading, and bootstrap wiring. |

`CraftalismEconomy` creates a `BootContainer` during plugin startup. The container loads configuration and messages, constructs API and application services, and registers commands and join/quit listeners.

## Technology

| Category | Technology |
|---|---|
| Java | 21 |
| Minecraft API | Paper API `1.21.4-R0.1-SNAPSHOT` |
| Build | Gradle Wrapper 8.5 |
| HTTP | Java `HttpClient` with HTTP/2 enabled |
| JSON | Gson 2.13.2 |
| Cache | Caffeine 3.1.8 |
| Tests | JUnit 5, Mockito 5.19.0, and MockBukkit 4.0.0 |

## Prerequisites

- JDK 21.
- A Paper 1.21.4 server.
- Reachable Craftalism API and OAuth authorization server instances.
- OAuth client credentials with the scopes required by the configured backend (the bundled default is `api:read api:write`).

## Build and Test

Run Gradle from the `java/` directory:

```bash
cd java
./gradlew build
```

The build runs the test suite and creates the shaded plugin JAR at `java/build/libs/craftalism-economy-0.1.8.jar`.

To run only the tests:

```bash
cd java
./gradlew test
```

The test suite covers presentation, application, domain, configuration, OAuth, and API-service behavior with mocks and MockBukkit. It does not run against live Craftalism API or authorization server instances.

## Installation

1. Build the plugin.
2. Copy `java/build/libs/craftalism-economy-0.1.8.jar` into the Paper server's `plugins/` directory.
3. Start the server once. The plugin creates its files under `plugins/CraftalismEconomy/`.
4. Configure the API URL, authorization server, and OAuth client credentials in `connection-config.yml` or through the supported environment variables.
5. Restart the server. The plugin does not provide a configuration reload command.

The plugin can start with an empty client secret, but authenticated API operations will fail until valid credentials are supplied.

## Configuration

The plugin creates `config.yml`, `connection-config.yml`, and `logs.yml` in its data directory on first startup.

### `config.yml`

| Key | Default | Description |
|---|---:|---|
| `default-balance` | `100000000` | Initial balance in scaled integer units. Amounts use a scale of 10,000, so the default displays as `10,000.00` with `en-US` formatting. |
| `locale` | `en-US` | Java locale used for grouping and decimal separators. Invalid locales fall back to `en-US`. |
| `currency-symbol` | `$` | Text prepended to formatted amounts. |
| `null-representation` | `—` | Fallback text used when currency formatting fails. |

Command amounts accept plain non-negative decimal values with at most two fractional digits. `/pay` requires a value greater than zero; `/setbalance` also accepts zero.

### `connection-config.yml`

| Key | Default | Description |
|---|---|---|
| `url` | `http://localhost:8080` | Craftalism API base URL. |
| `auth-server-url` | `http://localhost:9000` | Authorization server base URL. |
| `token-path` | `/oauth2/token` | Token path appended to `auth-server-url`, or an absolute token endpoint URL. |
| `client-id` | `minecraft-server` | OAuth client ID. |
| `client-secret` | empty | OAuth client secret. Prefer an environment variable outside local development. |
| `scopes` | `api:read api:write` | Space-separated OAuth scopes. |
| `http-connect-timeout-seconds` | `5` | HTTP connection timeout. Non-positive values fall back to `5`. |
| `http-request-timeout-seconds` | `10` | Per-request timeout. Non-positive values fall back to `10`. |
| `pay-legacy-fallback-enabled` | `false` | If enabled, use withdraw/deposit when the transfer endpoint responds with `404`, `405`, or `501`. |

The legacy payment fallback is not atomic. If the receiver deposit fails, the plugin attempts to restore the withdrawn amount to the sender, but that compensating request can also fail. Keep the fallback disabled unless it is required for a mixed-version API deployment.

### Environment variables

The following variables override their corresponding connection settings at startup:

| Variable | Overrides |
|---|---|
| `CRAFTALISM_API_URL` | `url` |
| `AUTH_ISSUER_URI` | `auth-server-url` |
| `AUTH_TOKEN_PATH` | `token-path` |
| `MINECRAFT_CLIENT_ID` | `client-id` |
| `MINECRAFT_CLIENT_SECRET` | `client-secret` |
| `CRAFTALISM_API_KEY` | `client-secret` when `MINECRAFT_CLIENT_SECRET` is unset or blank |
| `MINECRAFT_CLIENT_SCOPES` | `scopes` |

Set `MINECRAFT_CLIENT_SECRET` (or the lower-precedence `CRAFTALISM_API_KEY`) instead of committing a production secret to `connection-config.yml`. Timeout and legacy fallback settings do not have environment-variable overrides.

### `logs.yml`

`logs.yml` contains the global message prefix and all player-facing command templates. Bukkit `&` color codes and placeholders such as `{player}`, `{target}`, `{amount}`, `{balance}`, and `{rank}` are resolved when messages are sent.

## Commands and Permissions

| Command | Permission | Default | Description |
|---|---|---|---|
| `/pay <player> <amount>` | `craftalism.pay` | Everyone | Transfer a positive amount to another player. Player-only. |
| `/balance` | `craftalism.balance.self` | Everyone | Show the executing player's balance. Player-only. |
| `/balance <player>` | `craftalism.balance.other` | Operators | Show another player's balance. Player-only. |
| `/setbalance <player> <amount>` | `craftalism.setbalance` | Operators | Set a player's balance. May be run by a player or the console. |
| `/baltop` | `craftalism.baltop` | Everyone | Show up to 10 balances returned by the leaderboard endpoint. Player-only. |

## Backend API Usage

Every API request includes a bearer token obtained with the OAuth `client_credentials` grant.

| Method | Path | Use |
|---|---|---|
| `GET` | `/api/players/{uuid}` | Find a player by UUID. |
| `GET` | `/api/players/name/{name}` | Find a player by URL-encoded name. |
| `POST` | `/api/players` | Create a player record. |
| `GET` | `/api/balances/{uuid}` | Get a balance. |
| `POST` | `/api/balances` | Create a balance. |
| `PUT` | `/api/balances/{uuid}/set` | Set a balance. |
| `POST` | `/api/balances/transfer` | Transfer funds atomically. The plugin sends an `Idempotency-Key` header. |
| `POST` | `/api/balances/{uuid}/withdraw?amount={amount}` | Legacy payment withdrawal. |
| `POST` | `/api/balances/{uuid}/deposit?amount={amount}` | Legacy payment deposit or compensation. |
| `GET` | `/api/balances/top?limit=10` | Retrieve leaderboard balances. |

For `/pay`, the plugin resolves the payer and receiver, validates the command input and self-payment rule, and submits the transfer request. The backend is responsible for validating available funds and performing the atomic balance update. Transfer failures are mapped to player-facing statuses such as insufficient funds, duplicate request, invalid request, unavailable endpoint, or temporary failure.

## Project Structure

```text
.
├── docs/                         # Requirements, contracts, backlog, and feature cards
├── java/
│   ├── build.gradle
│   ├── gradlew
│   └── src/
│       ├── main/
│       │   ├── java/io/github/HenriqueMichelini/craftalism/economy/
│       │   │   ├── application/
│       │   │   ├── domain/
│       │   │   ├── infra/
│       │   │   └── presentation/
│       │   └── resources/
│       │       ├── config.yml
│       │       ├── connection-config.yml
│       │       ├── logs.yml
│       │       └── plugin.yml
│       └── test/java/
├── AGENTS.md
└── LICENSE
```

## License

This project is licensed under the MIT License. See [`LICENSE`](./LICENSE).
