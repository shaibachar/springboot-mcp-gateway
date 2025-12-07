# Structured Error Payloads

## Purpose
Give clients stable error codes, correlation IDs, and optional structured details while keeping legacy `content` intact.

## Contract
- Response fields: `requestId` (always), `errorCode` (on errors), `details` (optional object).
- Error codes: `validation_error`, `tool_not_found`, `execution_error`, `serialization_error`, `internal_error` provided via a single enum/constant source so they stay stable across controllers and services.
- Controllers issue a UUID `requestId` and echo it on both success and failure, even for discovery endpoints like `/mcp/tools` and `/mcp/tools/refresh`.
- Validation errors surface Spring binding results inside `details` as structured field/global error entries.

## Acceptance criteria
- Blank `name` on `/mcp/tools/execute` → HTTP 400, `errorCode=validation_error`, non-empty `requestId`, and binding `details` listing field/global errors.
- Unknown tool name → `errorCode=tool_not_found` with the request ID.
- Successful executions and discovery/refresh responses echo the same controller-generated `requestId`.
- `/mcp/tools` and `/mcp/tools/refresh` respond with the standard MCP envelope instead of plain strings so logs and clients can rely on stable tracing.
