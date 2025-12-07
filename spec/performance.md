# Performance Expectations

## Purpose
Define practical performance behaviors for the MCP gateway so responses stay fast and predictable.

## Targets
- **Tool discovery**: Warm cache responses under ~100 ms; cold refresh completes within a few seconds.
- **Tool execution**: Add minimal overhead beyond backend latency; avoid extra serialization passes.
- **Memory**: Favor bounded in-memory caches for discovery results and avoid unbounded collections per request.

## Practices
- Keep discovery caches small, TTL-based, and cleared by `/mcp/tools/refresh` before recompute.
- Normalize names once per request and reuse the normalized value across resolution and logging.
- Stream responses where possible to reduce payload buffering.
- Add lightweight metrics for cache hits/misses, refresh duration, and execution latency.
