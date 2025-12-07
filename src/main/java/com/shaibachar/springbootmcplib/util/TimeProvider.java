package com.shaibachar.springbootmcplib.util;

/**
 * Utility class for providing current time.
 * This abstraction allows for easier testing by enabling time mocking.
 */
public class TimeProvider {

    /**
     * Returns the current time in milliseconds since epoch.
     * This is a wrapper around System.currentTimeMillis() to allow for testing.
     *
     * @return the current time in milliseconds
     */
    public long getCurrentTimeMillis() {
        return System.currentTimeMillis();
    }
}
