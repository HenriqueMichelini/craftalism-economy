# Repo Requirement Pack: craftalism-economy

## Repo Role
`craftalism-economy` is the Minecraft plugin client for the ecosystem. It consumes API and auth contracts and translates them into player-facing command behavior. It is responsible for plugin-side orchestration, resilience, UX mapping, and backward-compatibility behavior only when explicitly supported.

## Owned Contracts
- No core backend contracts
- Own plugin-local behavior for:
  - command UX
  - fallback strategy
  - async orchestration
  - lifecycle cleanup
  - listener/cache correctness

## Consumed Contracts
- `transfer-flow`
  - Consume the canonical transfer endpoint and semantics correctly
- `transaction-routes`
  - Use canonical transaction routes when transaction details are consumed
- `error-semantics`
  - Map API/domain failures appropriately in plugin UX
- `idempotency`
  - Participate correctly in retry-safe transfer behavior according to API contract
- `incident-recording`
  - Emit or report incidents only according to the shared contract when required
- `auth-issuer`
  - Use token acquisition/configuration consistently with issuer expectations
- `ci-cd`
  - Meet plugin quality/release gate expectations
- `testing`
  - Meet plugin-side testing expectations
- `documentation`
  - Keep README and operational docs aligned with real behavior and consumed contracts

## Current Priority Areas
- Verify `/pay` aligns with the canonical transfer contract
- Verify plugin behavior does not re-implement server ownership locally
- Verify legacy fallback behavior is explicit and configurable if supported
- Improve player-facing error mapping where the API contract supports it
- Verify lifecycle cleanup and local listener correctness
- Improve CI/CD quality gates if missing or weak
- Remove release practices that ship untested artifacts
- Align docs with actual endpoint and contract usage

## Local Requirements
- Keep async command behavior clear and safe
- Keep fallback behavior explicit and easy to remove later if temporary
- Keep token caching/usage correct from a consumer perspective
- Preserve player-facing UX quality without leaking low-level server details
- Keep local event/listener behavior correct and maintainable

## Governance Requirements
- Comply with shared `ci-cd`, `testing`, and `documentation` standards
- Treat API/auth contracts as authoritative
- Do not implement server-side ownership concerns in the plugin

## Out of Scope
- API DB transaction ownership
- API route ownership
- API idempotency persistence
- API incident persistence model
- Auth-server security chain internals
- Deployment compose/runtime ownership

## Audit Questions
- Does the plugin conform to consumed contracts without redefining backend ownership?
- Is `/pay` aligned with canonical transfer behavior?
- Is any fallback behavior explicit, safe, and documented?
- Are tests and release workflows strong enough to avoid shipping unverified plugin builds?
- Are docs aligned with real endpoint/contract usage?

## Success Criteria
- Plugin behavior is a clean contract consumer
- `/pay` and related flows match the shared API contract
- Fallback behavior, if present, is explicit and controlled
- Docs match implementation
- CI/CD and tests enforce basic quality expectations
