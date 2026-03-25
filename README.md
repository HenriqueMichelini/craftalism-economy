# Craftalism Economy

A production-oriented Minecraft economy plugin built with a layered architecture and asynchronous API integration.

## Project goals

- Keep game-thread work minimal and non-blocking.
- Encapsulate business rules in application/domain layers.
- Make failures explicit through typed result DTOs and status enums.
- Keep infrastructure concerns (HTTP, config, cache, external API) isolated from gameplay commands.

## Architecture

The project follows a **layered architecture** with explicit boundaries:

- **Presentation (`presentation`)**
  - Bukkit commands and listeners.
  - Input validation (e.g., player name checks).
  - Maps application result DTOs to player-facing messages.

- **Application (`application`)**
  - Orchestrates use-cases (`PayCommandApplicationService`, `SetBalanceCommandApplicationService`, etc.).
  - Coordinates domain and infrastructure services.
  - Returns typed execution results (`*ExecutionResult`) instead of leaking infra exceptions.

- **Domain (`domain`)**
  - Core entities (`Player`, `Balance`, `Transaction`).
  - Currency and validation services.
  - Log message abstractions.

- **Infrastructure (`infra`)**
  - External API clients/services.
  - Config loading and bootstrap wiring.
  - Cache repositories.

## Request flow example (`/pay`)

1. Command handler validates raw command arguments.
2. `PayCommandApplicationService` resolves payer + receiver.
3. Business validation runs (self-payment, amount, funds).
4. Transfer is executed with rollback protection.
5. Transaction logging runs as best-effort.
6. Result status is translated into localized/logged messages.

## Reliability and failure handling

- External failures are unwrapped and normalized via asynchronous exception handling.
- Transfer path includes rollback behavior if receiver deposit fails.
- Transaction registration is non-blocking best-effort to avoid reverting successful transfers.

## Requirements

- Java 21+
- Gradle Wrapper (included)
- A Paper/Spigot-compatible server for runtime testing

> Note: In some environments, Gradle may fail if the runtime JDK is newer than the wrapper/toolchain support.

## Running locally

```bash
cd java
./gradlew clean build
```

## Testing

```bash
cd java
./gradlew test
```

## Configuration

Main configs are under `java/src/main/resources/`:

- `config.yml`
- `connection-config.yml`
- `logs.yml`

Adjust API endpoints, auth, formatting, and default economy values before production deployment.

## Portfolio/production readiness checklist

- [x] Layered architecture with clear separation of concerns
- [x] Dedicated application services per command/use-case
- [x] Unit tests for domain/application/infra behavior
- [x] Explicit status-driven responses for command flows
- [ ] Integration tests against a real API sandbox
- [ ] Production telemetry/metrics and alerting hooks
