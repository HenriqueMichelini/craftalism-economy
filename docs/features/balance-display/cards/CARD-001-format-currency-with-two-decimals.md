# CARD-001: Format Currency Output With Two Decimals

## Status

completed

## Objective

Ensure balance-related player messages and local log messages display currency amounts with exactly two decimal places while preserving four-decimal internal precision.

## Context

The repository has no `docs/index.md`, `docs/context-policy.md`, or feature contract for balance display. Local evidence shows command messages use `CurrencyFormatter.formatCurrency`, while `CurrencyFormatter.toDisplayValue` and `fromDisplayValue` preserve the four-decimal stored scale.

## Required Reading

- `../../../../AGENTS.md`
- `../../../repo-contract-map.md`
- `../../../repo-requirement-pack.md`
- `../../../../README.md`

## Expected Behavior

Currency display output rounds to exactly two fraction digits for player-facing command messages and log output that uses the shared currency formatter. Stored values and conversion precision remain based on `DECIMAL_SCALE = 10000`.

## Acceptance Criteria

- [x] `formatCurrency(long)` displays exactly two decimal places, rounded half-up from the four-decimal internal amount.
- [x] `formatCurrency(BigDecimal)` displays exactly two decimal places.
- [x] `toDisplayValue`, `fromDisplayValue`, and `DECIMAL_SCALE` continue to preserve four-decimal precision.
- [x] Existing command paths continue to use the shared formatter instead of duplicating formatting logic.

## Expected Files to Change

```text
java/src/main/java/io/github/HenriqueMichelini/craftalism/economy/domain/service/currency/CurrencyFormatter.java
java/src/test/java/io/github/HenriqueMichelini/craftalism/economy/domain/service/currency/CurrencyFormatterTest.java
```

## Constraints

- Do not change API DTOs, persisted amount scale, or backend contracts.
- Do not change command parsing or accepted input formats.
- Do not modify unrelated features.
- Do not introduce architectural changes unless explicitly required.

## Validation Commands

```bash
cd java
./gradlew test --tests io.github.HenriqueMichelini.craftalism.economy.domain.service.currency.CurrencyFormatterTest
./gradlew test
```

If the full test suite is not available in the current environment, run the targeted formatter test and report the skipped broader validation.

## Out of Scope

- Changing backend amount precision or shared API contracts.
- Changing command input validation.
- Reworking message templates in `logs.yml`.
- Broad documentation restructuring.

## Completion Notes

Implemented in `CurrencyFormatter` by capping display formatting at exactly two fraction digits. Formatter tests now verify two-decimal display rounding while retaining four-decimal conversion precision. Validation passed with the targeted formatter test and the full Gradle test suite.
