# Testing Checklist

## Automated
- `mvn test`

## Manual spot checks
- Call `/mcp/tools` twice within five minutes → second call logs a cache hit.
- Wait past the TTL or force `/mcp/tools/refresh` → subsequent `/mcp/tools` call triggers a rescan and new requestId in the envelope.
- POST `/mcp/tools/execute` with empty `name` → HTTP 400, `validation_error`, `requestId`, binding errors.
- POST `/mcp/tools/execute` with a near-miss tool name (e.g., missing underscores) → normalization still resolves the tool.
- Inspect a GraphQL tool in `/mcp/tools` → arguments list `graphqlType`, `javaType`, and correct `nullable` flags.
