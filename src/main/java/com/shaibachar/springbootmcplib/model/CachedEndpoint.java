package com.shaibachar.springbootmcplib.model;

/**
 * Wrapper class for cached endpoint metadata with TTL tracking.
 * @param <T> the type of endpoint metadata (EndpointMetadata or GraphQLEndpointMetadata)
 */
public class CachedEndpoint<T> {
    private final T endpoint;
    private final long cachedAtTimestamp;

    public CachedEndpoint(T endpoint, long cachedAtTimestamp) {
        this.endpoint = endpoint;
        this.cachedAtTimestamp = cachedAtTimestamp;
    }

    public T getEndpoint() {
        return endpoint;
    }

    public long getCachedAtTimestamp() {
        return cachedAtTimestamp;
    }

    /**
     * Checks if this cached endpoint has expired based on the given TTL.
     *
     * @param ttlMillis the TTL in milliseconds
     * @return true if the cache entry has expired
     */
    public boolean isExpired(long ttlMillis) {
        long now = System.currentTimeMillis();
        return (now - cachedAtTimestamp) >= ttlMillis;
    }
}
