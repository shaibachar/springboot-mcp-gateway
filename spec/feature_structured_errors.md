# Structured Error Payloads

## Purpose
Give clients stable error codes, correlation IDs, and optional structured details while keeping legacy `content` intact.

## Contract
- Response fields: `requestId` (always), `errorCode` (on errors), `details` (optional object).
- Error codes: `validation_error`, `tool_not_found`, `execution_error`, `serialization_error`, `internal_error`.
- Controllers issue a UUID `requestId` and echo it on both success and failure.
- Validation errors surface Spring binding results inside `details`.

## Acceptance criteria
- Blank `name` on `/mcp/tools/execute` → HTTP 400, `errorCode=validation_error`, non-empty `requestId`.
- Unknown tool name → `errorCode=tool_not_found` with the request ID.
- Successful executions echo the same controller-generated `requestId`.
