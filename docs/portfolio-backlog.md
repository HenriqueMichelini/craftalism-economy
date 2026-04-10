# Craftalism Economy Portfolio Backlog

Date: 2026-04-10

## Purpose

This backlog focuses on making the Minecraft plugin a stronger distributed
client and a more convincing portfolio example of async integration work.

Source:

- [portfolio-evolution-roadmap.md](/home/henriquemichelini/IdeaProjects/craftalism/docs/portfolio-evolution-roadmap.md)
- [repo-requirement-pack.md](/home/henriquemichelini/IdeaProjects/craftalism-economy/docs/repo-requirement-pack.md)

## Now

### High priority

- Add live-stack integration tests against real auth and API services.
- Add tests for token refresh, timeout handling, degraded transfer fallback, and
  cache correctness.
- Add startup validation for API URL, issuer URL, token path, scopes, and
  required secrets.
- Improve player-facing and operator-facing diagnostics for auth failures,
  backend unavailability, and degraded-mode behavior.

### Medium priority

- Tighten release confidence so plugin artifacts are not treated as trustworthy
  without real test execution.
- Clarify docs around fallback behavior and when it is considered non-canonical.

## Next

### High priority

- Add correlation IDs or request tracing hooks to connect in-game failures with
  backend logs.
- Improve command ergonomics with better help text, tab completion, and admin
  diagnostics.
- Add bounded retry and resilience controls that remain explicit and easy to
  reason about.

### Medium priority

- Add clearer separation between player-friendly messages and operator-debug
  detail.
- Add a documented demo-server setup for showing the plugin in a real integrated
  flow.

## Later

- Add carefully scoped admin observability commands if they improve operations
  without duplicating backend ownership.
- Add stronger profiling around async execution and cache behavior if needed.

## Done When

- The plugin is a disciplined consumer of backend contracts.
- Failure modes are visible and understandable in both gameplay and logs.
- The repo demonstrates strong async client engineering rather than only command
  handling.
