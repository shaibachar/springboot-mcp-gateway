# Feature: Structured Error Payloads

## Overview
MCP tool executions now emit stable error codes, correlation IDs, and optional structured details alongside the legacy `content` list. This improves debuggability for clients and operators.

## Key Changes
- `McpToolExecutionResponse` adds `errorCode`, `requestId`, and `details` fields.
- `McpController` generates a UUID per request and returns it in success and error responses.
- Validation issues surface `validation_error` codes with Spring `BindingResult` objects in `details`.
- Execution failures map to `tool_not_found`, `execution_error`, or `serialization_error` codes.

## Acceptance Criteria
- When `name` is blank, `/mcp/tools/execute` returns HTTP 400 with `errorCode=validation_error` and a non-empty `requestId`.
- When a tool name does not resolve, the response includes `errorCode=tool_not_found`.
- Successful executions include the same `requestId` echoed from the controller.
