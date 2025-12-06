# MCP Gateway Enhancements Design

## Goals
- Provide structured, traceable error payloads for MCP executions.
- Improve resiliency via tool name tolerance and cached discovery.
- Enrich tool metadata, especially for GraphQL operations, so clients can reason about argument shapes.
- Document validation and refresh flows so operators know how to reset caches.

## Error Contract
- `McpToolExecutionResponse` now includes `errorCode`, `requestId`, and `details` fields.
- Controllers generate a `requestId` per execution and propagate it through the service layer.
- Validation failures return `errorCode=validation_error` with binding errors in `details`.
- Runtime failures use stable codes (`tool_not_found`, `execution_error`, `serialization_error`, `internal_error`) with the same `requestId` clients receive.

## Discovery Caching
- REST and GraphQL discovery services maintain a 5-minute in-memory cache to avoid reflection scans on every call.
- `/mcp/tools/refresh` invalidates caches across REST, GraphQL, and tool mappings before recomputing tools.

## Tool Resolution Resilience
- Tool lookups now normalize names (lowercase, strip non-alphanumerics) and allow partial containment matches, improving compatibility with minor naming drifts.
- Both REST and GraphQL resolution paths share the normalization strategy.

## GraphQL Metadata
- GraphQL input schemas add `graphqlType`, `javaType`, and `nullable` hints per argument.
- Existing required detection is reused; nullable is computed as the inverse.
- JSON schema maps still include the previous `type` hints for compatibility.

## Validation Hooks
- Execution requests enforce `name` via Bean Validation; Spring’s binding errors are surfaced in the structured response.

## Compatibility
- Success responses retain the prior `content` shape, with the addition of `requestId` for traceability. Error responses remain backward-compatible via `content` while layering structured fields.
