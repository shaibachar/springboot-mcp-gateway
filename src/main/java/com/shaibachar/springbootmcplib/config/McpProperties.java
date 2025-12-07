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

    public Cache getCache() {
        return cache;
    }

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

        public long getTtlMillis() {
            return ttlMillis;
        }

        public void setTtlMillis(long ttlMillis) {
            this.ttlMillis = ttlMillis;
        }
    }
}
