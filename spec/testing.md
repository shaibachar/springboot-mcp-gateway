# Testing Plan

## Automated
- Run `mvn test` to execute the existing unit test suite.

## Manual
- Call `/mcp/tools` twice within five minutes and confirm logs indicate cache hits on the second call.
- POST `/mcp/tools/execute` with an empty `name` to observe `validation_error`, `requestId`, and binding errors.
- POST `/mcp/tools/execute` with a near-miss tool name (e.g., missing underscores) to verify partial matching resolves the tool when normalization matches.
- Inspect a GraphQL tool in `/mcp/tools` response and confirm `graphqlType`, `javaType`, and `nullable` fields appear per argument.
