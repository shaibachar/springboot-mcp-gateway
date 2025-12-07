# Discovery Caching and Refresh

## Purpose
Avoid heavy reflection scans on every discovery call while allowing manual invalidation.

## Contract
- `EndpointDiscoveryService` and `GraphQLDiscoveryService` cache results for 5 minutes (timestamp TTL).
- `/mcp/tools/refresh` clears discovery caches and tool mappings, then rebuilds them.
- `McpToolMappingService` returns copies so callers cannot mutate cached lists.

## Acceptance criteria
- Two `/mcp/tools` calls within 5 minutes reuse cached data (logs show cache hits).
- After `/mcp/tools/refresh`, the next `/mcp/tools` call triggers a rescan.
- No stale endpoints survive beyond the TTL or a refresh.
