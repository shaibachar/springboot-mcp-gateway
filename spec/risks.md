# Project Risks

## Purpose
Flag the main delivery and operational risks with quick mitigations for the MCP gateway.

## Top risks and mitigations
- **Discovery cache drift**: Cached tool lists can go stale if upstream contracts change.
  - Mitigate with the 5-minute TTL plus `/mcp/tools/refresh` to force recompute.
- **Name normalization collisions**: Loose matching may resolve an unintended tool.
  - Keep tests around near-miss names and log the resolved target with the `requestId` for traceability.
- **Schema mismatch**: GraphQL or REST schema metadata may diverge from runtime behavior.
  - Validate schemas during refresh and fail fast with `serialization_error` if incompatible.
- **Error contract regressions**: Missing `requestId` or unstable `errorCode` values break clients.
  - Add contract tests in CI to exercise success, validation failure, and unknown tool paths.
- **Cache eviction latency under load**: Heavy refresh traffic could temporarily stall tool resolution.
  - Keep refresh idempotent and short-lived; monitor refresh latency and adjust cache size or TTL if needed.
- **Memory leaks in long-running applications**: Without proper cleanup, caches could retain stale references.
  - Both discovery services implement `@PreDestroy` cleanup methods that clear caches when the Spring bean is destroyed, ensuring resources are released in container environments.
