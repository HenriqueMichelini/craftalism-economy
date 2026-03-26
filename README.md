# Craftalism Economy

Craftalism Economy is a **Paper/Spigot Minecraft plugin** that delegates economy data to an external HTTP API.

Instead of storing balances directly inside the Minecraft server, the plugin acts as an asynchronous client for a backend service (players, balances, transactions). This keeps command handling responsive while centralizing economy state in one API.

---

## What this project does

This plugin provides economy gameplay commands and sync behavior:

- `/balance` — check your own or another player's balance
- `/pay` — transfer funds between players with rollback protection
- `/setbalance` — set a player's balance (permission-gated)
- `/baltop` — list top balances
- Automatic player + balance bootstrap on player join

All business flows are implemented asynchronously with `CompletableFuture` and call an OAuth2-protected external API.

---

## Key features (implemented)

- **Asynchronous command execution** to avoid blocking the main server thread
- **Layered architecture** (presentation → application → domain → infrastructure)
- **Typed command outcomes** (status enums and execution result DTOs)
- **External API integration** for players/balances/transactions
- **OAuth2 client-credentials authentication** with token caching
- **Rollback logic for `/pay` transfers** when deposit fails after withdrawal
- **Caffeine-based in-memory cache** for players and balances
- **Config-driven currency formatting** (locale, symbol, fallback)
- **YAML-driven player-facing message templates** (`logs.yml`)
- **Unit test coverage** across presentation/application/domain/infra modules

---

## Tech stack

- **Language**: Java 21
- **Build tool**: Gradle (wrapper included)
- **Minecraft API**: Paper API `1.21.4-R0.1-SNAPSHOT`
- **Libraries**:
  - Gson (JSON)
  - Apache HttpClient dependency present (plugin runtime client currently uses Java `HttpClient`)
  - Caffeine (cache)
- **Testing**:
  - JUnit 5
  - Mockito
  - MockBukkit

---

## Architecture overview

The codebase follows a layered structure:

### 1) Presentation layer (`presentation`)
- Bukkit command executors:
  - `PayCommand`
  - `BalanceCommand`
  - `SetBalanceCommand`
  - `BaltopCommand`
- Event listener:
  - `OnJoin` (loads/creates player + balance in API/cache)
- Input validation:
  - `PlayerNameCheck`

### 2) Application layer (`application`)
Use-case orchestration and status mapping:

- `PayCommandApplicationService`
- `BalanceCommandApplicationService`
- `SetBalanceCommandApplicationService`
- `BaltopCommandApplicationService`
- `PlayerApplicationService`
- `BalanceApplicationService`
- `TransactionApplicationService`

These classes coordinate domain logic and infra services, returning explicit DTO/status results instead of leaking low-level exceptions into command handlers.

### 3) Domain layer (`domain`)
Core models and rules:

- Models: `Player`, `Balance`, `Transaction`
- Currency services: `CurrencyFormatter`, `CurrencyParser`
- Validation/services: `FundsTransfer`, `AmountCheck`
- Messaging abstractions via log/message helper classes

### 4) Infrastructure layer (`infra`)
External integrations and wiring:

- API services: `PlayerApiService`, `BalanceApiService`, `TransactionApiService`
- HTTP client + OAuth2 token service
- Config loading (`ConfigLoader`, `ConnectionConfig`)
- Cache repositories (`PlayerCacheRepository`, `BalanceCacheRepository`)
- Bootstrap/container wiring (`BootContainer`)

---

## Runtime flow (high-level)

### `/pay` flow (implemented)
1. Validate sender permissions and arguments.
2. Resolve payer from cache/API and receiver from API.
3. Validate amount/self-payment.
4. Withdraw from payer.
5. Deposit to receiver.
6. If deposit fails, attempt rollback deposit back to payer.
7. Register transaction as best-effort (failure does not revert successful transfer).
8. Map result status to player messages.

### Join flow (implemented)
On player join, plugin ensures player exists in API and then ensures balance exists (or creates default balance), caching results.

---

## Configuration

Configuration files are bundled under `java/src/main/resources/` and copied to plugin data folder at runtime:

- `config.yml`
  - `default-balance` (internal scaled value; default `100000000`)
  - `locale` (default `en-US`)
  - `currency-symbol` (default `$`)
  - `null-representation`

- `connection-config.yml`
  - API base URL
  - OAuth issuer/token path
  - OAuth client id/secret/scopes
  - HTTP timeouts

- `logs.yml`
  - Prefix and localized/message-template strings for all commands and system messages

### Environment variable overrides

`ConfigLoader` supports environment overrides for connection/auth values:

- `CRAFTALISM_API_URL`
- `AUTH_ISSUER_URI`
- `AUTH_TOKEN_PATH`
- `MINECRAFT_CLIENT_ID`
- `MINECRAFT_CLIENT_SECRET`
- `CRAFTALISM_API_KEY` (fallback for client secret)
- `MINECRAFT_CLIENT_SCOPES`

> In production, prefer environment variables for secrets instead of hardcoding `client-secret` in YAML.

---

## Commands and permissions

### Commands

- `/pay <player> <amount>`
- `/balance` or `/balance <player>`
- `/setbalance <player> <amount>`
- `/baltop`

### Permissions

- `craftalism.pay` (default: true)
- `craftalism.balance.self` (default: true)
- `craftalism.balance.other` (default: op)
- `craftalism.setbalance` (default: op)
- `craftalism.baltop` (default: true)

---

## External API contract expected by plugin

The plugin calls these endpoints:

- `GET /api/players/{uuid}`
- `GET /api/players/name/{name}`
- `POST /api/players`

- `GET /api/balances/{uuid}`
- `POST /api/balances`
- `PUT /api/balances/{uuid}/set`
- `POST /api/balances/{uuid}/deposit?amount={amount}`
- `POST /api/balances/{uuid}/withdraw?amount={amount}`
- `GET /api/balances/top?limit={n}`

- `POST /api/transactions`

OAuth2 token retrieval is done via configured auth server + token path using client credentials flow.

---

## Build and test

From repository root:

```bash
cd java
./gradlew clean build
```

Run tests:

```bash
cd java
./gradlew test
```

---

## Local development setup (plugin + API)

1. Start the external economy API and OAuth2 issuer configured in `connection-config.yml`.
2. Build the plugin JAR with Gradle.
3. Copy generated JAR into your Paper server `plugins/` folder.
4. Start server once to generate config files (if needed).
5. Edit plugin configs in the server plugin data folder.
6. Restart server and test commands in-game.

---

## Repository structure

```text
.
├── README.md
├── LICENSE
└── java/
    ├── build.gradle
    ├── gradlew
    ├── src/main/java/io/github/HenriqueMichelini/craftalism/economy/
    │   ├── presentation/
    │   ├── application/
    │   ├── domain/
    │   └── infra/
    ├── src/main/resources/
    │   ├── plugin.yml
    │   ├── config.yml
    │   ├── connection-config.yml
    │   └── logs.yml
    └── src/test/java/
```

---

## Current limitations / improvement opportunities

- `OnQuit` listener exists but is not registered in `EventRegistrar`.
- `BootContainer#shutdown()` is currently a placeholder.
- Integration/E2E tests against a real API are not included.
- Some command input validation rules are strict/uneven (`/setbalance` currently accepts numeric digits only before scaling).

---

## License

MIT License.
