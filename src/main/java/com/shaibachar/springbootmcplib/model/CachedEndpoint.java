package com.shaibachar.springbootmcplib.model;

import com.shaibachar.springbootmcplib.util.TimeProvider;

/**
 * Wrapper class for cached endpoint metadata with TTL tracking.
 * @param <T> the type of endpoint metadata (EndpointMetadata or GraphQLEndpointMetadata)
 */
public class CachedEndpoint<T> {
    private final T endpoint;
    private final long cachedAtTimestamp;
    private static TimeProvider timeProvider = new TimeProvider();

    /**
     * Constructs a new cached endpoint.
     *
     * @param endpoint the endpoint metadata
     * @param cachedAtTimestamp the timestamp when this endpoint was cached
     */
    public CachedEndpoint(T endpoint, long cachedAtTimestamp) {
        this.endpoint = endpoint;
        this.cachedAtTimestamp = cachedAtTimestamp;
    }

    /**
     * Sets the time provider for testing purposes.
     * @param provider the time provider
     */
    public static void setTimeProvider(TimeProvider provider) {
        timeProvider = provider;
    }

    /**
     * Gets the endpoint metadata.
     *
     * @return the endpoint
     */
    public T getEndpoint() {
        return endpoint;
    }

    /**
     * Gets the timestamp when this endpoint was cached.
     *
     * @return the cached timestamp in milliseconds
     */
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
        long now = timeProvider.getCurrentTimeMillis();
        return (now - cachedAtTimestamp) >= ttlMillis;
    }
}
