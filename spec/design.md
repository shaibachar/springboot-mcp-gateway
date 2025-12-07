# MCP Gateway Design Snapshot

## What the system must deliver
- Structured, traceable error payloads for every MCP execution.
- Resilient tool resolution (name normalization and partial matches).
- Cached discovery for REST and GraphQL tools with a refresh hook.
- Rich tool metadata so clients understand argument shape and nullability.

## Error contract
- `McpToolExecutionResponse` always includes a `requestId`; errors add `errorCode` and optional `details`.
- Stable error codes: `validation_error`, `tool_not_found`, `execution_error`, `serialization_error`, `internal_error`.
- Controllers generate the `requestId` per execution and pass it through responses.
- Validation exposes Spring binding errors inside `details`.

## Discovery and refresh
- REST and GraphQL discovery results are cached in-memory for 5 minutes.
- `/mcp/tools/refresh` clears discovery caches and tool mappings before recomputing.
- **Library endpoint filtering**: Discovery automatically excludes the MCP library's own controller endpoints (McpController) to prevent self-reference and infinite recursion. Only application endpoints are exposed as MCP tools.

## Tool resolution rules
- Normalize names to lowercase alphanumerics and allow containment matches so near-miss names still resolve.
- REST and GraphQL share the same normalization strategy.

## Metadata expectations
- GraphQL argument schemas publish `graphqlType`, `javaType`, `nullable`, plus existing JSON schema `type`.
- REST schemas include `javaType` for parity.

## Compatibility
- Successful responses keep the legacy `content` structure; `requestId` is additive.
- Error responses stay backward compatible while adding structured fields.
