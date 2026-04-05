# Repo Contract Map: craftalism-economy

## Repository Role
`craftalism-economy` is the Minecraft plugin client for the ecosystem. It consumes API and auth contracts and translates them into player-facing command behavior. It is responsible for plugin-side orchestration, resilience, UX mapping, and optional backward-compatibility behavior where explicitly supported.

## Owned Contracts
- No core backend contracts
- Owns plugin-local behavior for:
  - command UX
  - fallback strategy
  - async orchestration
  - lifecycle cleanup
  - local listener/cache correctness

## Consumed Contracts
- `transfer-flow`
  - Must consume the canonical transfer endpoint and semantics correctly
- `transaction-routes`
  - Must use canonical transaction routes if transaction details are consumed
- `error-semantics`
  - Must map known API failures appropriately in plugin UX
- `idempotency`
  - Must participate in retry-safe transfer behavior according to API contract
- `incident-recording`
  - Must emit or report incidents only according to the shared contract when required
- `auth-issuer`
  - Must obtain/use tokens and configuration consistently with issuer expectations
- `ci-cd`
  - Must comply with plugin quality/release gates
- `testing`
  - Must meet plugin-side testing expectations
- `documentation`
  - Must keep README and operational docs aligned with actual behavior and consumed contracts

## Local-Only Responsibilities
- `/pay` behavior and UX
- Optional fallback to legacy flow when intentionally supported
- Async command execution and exception handling
- Token usage/caching from a plugin-consumer perspective
- Lifecycle shutdown cleanup
- Event listener registration/correctness

## Out of Scope
- API DB transaction ownership
- API route ownership
- API idempotency persistence
- API incident persistence model
- Auth-server security-chain internals
- Deployment compose ownership

## Compliance Questions
- Does the plugin conform to API/auth contracts without re-implementing server responsibilities?
- Is `/pay` aligned with canonical transfer behavior?
- Is fallback behavior explicit, safe, and documented if present?
- Are release workflows and tests strong enough to avoid shipping unverified plugin builds?

## Success Signal
This repo is compliant when it behaves as a clean contract consumer: reliable, well-documented, UX-aware, and aligned with API/auth behavior without redefining backend ownership.
