# Feature: Discovery Caching and Refresh

## Overview
Endpoint and GraphQL discovery now use a 5-minute in-memory cache to avoid repeated reflection scans while exposing a refresh hook to invalidate stale mappings.

## Key Changes
- `EndpointDiscoveryService` and `GraphQLDiscoveryService` keep cached discovery results with timestamp-based TTL.
- `/mcp/tools/refresh` clears discovery caches and tool mappings before recomputing.
- `McpToolMappingService` still returns defensive copies to avoid external mutation of cached lists.

## Acceptance Criteria
- Sequential `/mcp/tools` calls within 5 minutes reuse cached discovery data (log statements show cache hits).
- Invoking `/mcp/tools/refresh` forces subsequent `/mcp/tools` calls to rescan endpoints.
- No stale endpoints persist beyond the TTL or after a refresh call.
