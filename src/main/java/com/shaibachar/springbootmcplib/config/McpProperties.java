package com.shaibachar.springbootmcplib.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuration properties for Spring Boot MCP Library.
 */
@ConfigurationProperties(prefix = "mcp")
public class McpProperties {

    /**
     * Cache configuration properties.
     */
    private Cache cache = new Cache();

    /**
     * Gets the cache configuration.
     *
     * @return the cache configuration
     */
    public Cache getCache() {
        return cache;
    }

    /**
     * Sets the cache configuration.
     *
     * @param cache the cache configuration
     */
    public void setCache(Cache cache) {
        this.cache = cache;
    }

    /**
     * Cache-related properties.
     */
    public static class Cache {
        /**
         * Time-to-live for endpoint cache entries in milliseconds.
         * Default is 5 minutes (300000 milliseconds).
         */
        private long ttlMillis = 5 * 60 * 1000;

        /**
         * Gets the cache TTL in milliseconds.
         *
         * @return the TTL in milliseconds
         */
        public long getTtlMillis() {
            return ttlMillis;
        }

        /**
         * Sets the cache TTL in milliseconds.
         *
         * @param ttlMillis the TTL in milliseconds
         */
        public void setTtlMillis(long ttlMillis) {
            this.ttlMillis = ttlMillis;
        }
    }
}
