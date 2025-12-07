# Discovery Caching and Refresh

## Purpose
Avoid heavy reflection scans on every discovery call while allowing manual invalidation.

## Contract
- `EndpointDiscoveryService` and `GraphQLDiscoveryService` cache individual endpoints with configurable TTL.
- Each endpoint has its own timestamp and is recached independently when its TTL expires.
- Cache TTL is configurable via `mcp.cache.ttl-millis` property (default: 5 minutes = 300000 ms).
- `/mcp/tools/refresh` clears all discovery caches and tool mappings, then rebuilds them.
- `McpToolMappingService` returns copies so callers cannot mutate cached lists.

## Implementation Details
- **CachedEndpoint wrapper**: Endpoints are cached using a generic `CachedEndpoint<T>` wrapper class that stores both the endpoint metadata and its cached timestamp. This enables independent TTL tracking per endpoint.
- **Per-endpoint expiration**: The `isExpired(long ttlMillis)` method on `CachedEndpoint` checks if the individual endpoint's cache has expired based on the configured TTL.
- **Stale entry removal**: During discovery, endpoint keys are collected and the cache automatically removes stale entries (endpoints that no longer exist) using `cachedEndpointsMap.keySet().retainAll(currentEndpointKeys.keySet())`.
- **Thread-safe storage**: Cache maps use `ConcurrentHashMap` for thread-safe concurrent access without external synchronization.

## Configuration
The cache TTL can be configured in `application.properties` or `application.yml`:

```properties
# Set cache TTL to 10 minutes (600000 milliseconds)
mcp.cache.ttl-millis=600000
```

```yaml
# Or in YAML format
mcp:
  cache:
    ttl-millis: 600000
```

## Acceptance criteria
- Two `/mcp/tools` calls within the configured TTL reuse cached data (logs show cache hits).
- After an endpoint's TTL expires, only that specific endpoint is rediscovered on the next call.
- After `/mcp/tools/refresh`, the next `/mcp/tools` call triggers a full rescan of all endpoints.
- No stale endpoints survive beyond their individual TTL or a refresh.
- The TTL is configurable via library property `mcp.cache.ttl-millis`.
