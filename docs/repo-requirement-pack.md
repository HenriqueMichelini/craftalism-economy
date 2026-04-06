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
  - Consume issuance-side truth and use token acquisition/configuration consistently with issuer expectations
- `ci-cd`
  - Meet plugin quality/release gate expectations
- `testing`
  - Meet plugin-side testing expectations
- `documentation`
  - Keep README and operational docs aligned with real behavior and consumed contracts
- `security-access-control`
  - Use protected surfaces correctly and avoid misrepresenting access assumptions in docs

## Current Phase Objective
This phase is limited to:
- verifying plugin conformance to consumed contracts
- correcting plugin-local behavior where it clearly violates shared contracts
- correcting documentation drift directly related to actual plugin behavior
- correcting CI/CD or testing gaps only where they materially weaken trust in this repo’s contract consumption

This phase is not for re-implementing server ownership inside the plugin.

## Required This Phase
- Verify each consumed contract and classify it as:
  - already compliant
  - partially compliant
  - missing
  - incorrectly implemented
- Implement only confirmed plugin-local gaps in consumed-contract conformance
- Verify `/pay` against the canonical transfer contract
- Verify any fallback behavior is explicit, controlled, and documented if it exists
- Fix documentation only where it directly contradicts actual plugin behavior or consumed contracts
- Fix CI/CD or testing only where:
  - required standards are clearly violated, and
  - the gap materially weakens confidence in plugin behavior

## Not Required This Phase
- API-side transaction ownership
- API route ownership
- API idempotency persistence
- API incident persistence model
- Broad plugin architecture rewrites
- New features unrelated to consumed-contract alignment

## Local Requirements
- Keep async command behavior clear and safe
- Keep fallback behavior explicit and easy to remove later if temporary
- Keep token caching/usage correct from a consumer perspective
- Preserve player-facing UX quality without leaking low-level server details
- Keep local event/listener behavior correct and maintainable

## Governance Requirements
- Comply with shared `ci-cd`, `testing`, `documentation`, and `security-access-control` standards
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
- Are access/auth assumptions aligned with the shared security/access-control standard?
- Are tests and release workflows sufficient to avoid shipping unverified plugin builds?

## Success Criteria
- Plugin behavior is a clean contract consumer
- `/pay` and related flows match the shared API contract
- Fallback behavior, if present, is explicit and controlled
- Docs match implementation where contract consumption applies
- CI/CD and tests meet minimum required confidence for this phase