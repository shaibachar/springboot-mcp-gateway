package com.shaibachar.springbootmcplib.model;

/**
 * Stable machine-readable error codes used across MCP responses.
 */
public enum McpErrorCode {
    VALIDATION_ERROR("validation_error"),
    TOOL_NOT_FOUND("tool_not_found"),
    EXECUTION_ERROR("execution_error"),
    SERIALIZATION_ERROR("serialization_error"),
    INTERNAL_ERROR("internal_error");

    private final String code;

    McpErrorCode(String code) {
        this.code = code;
    }

    /**
     * Gets the wire-safe error code value.
     *
     * @return the error code string
     */
    public String getCode() {
        return code;
    }
}
