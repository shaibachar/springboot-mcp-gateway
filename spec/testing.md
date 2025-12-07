# Testing Checklist

## Automated
- `mvn test`

## Test Infrastructure
- **TimeProvider abstraction**: The library uses a `TimeProvider` utility class to abstract system time calls (`System.currentTimeMillis()`). This enables deterministic testing of cache TTL expiration without waiting for real time to pass. Tests can inject a mock `TimeProvider` to simulate time advancement and verify cache expiration behavior.
- **CachedEndpoint wrapper**: Per-endpoint cache entries use a generic `CachedEndpoint<T>` wrapper that stores the endpoint metadata along with its cached timestamp. This allows independent TTL tracking per endpoint and enables testing of partial cache expiration scenarios.

## Manual spot checks
- Call `/mcp/tools` twice within five minutes → second call logs a cache hit.
- Wait past the TTL or force `/mcp/tools/refresh` → subsequent `/mcp/tools` call triggers a rescan and new requestId in the envelope.
- POST `/mcp/tools/execute` with empty `name` → HTTP 400, `validation_error`, `requestId`, binding errors.
- POST `/mcp/tools/execute` with a near-miss tool name (e.g., missing underscores) → normalization still resolves the tool.
- Inspect a GraphQL tool in `/mcp/tools` → arguments list `graphqlType`, `javaType`, and correct `nullable` flags.
