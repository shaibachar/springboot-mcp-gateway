package com.shaibachar.springbootmcplib.model;

/**
 * Stable machine-readable error codes used across MCP responses.
 */
public enum McpErrorCode {
    /** Validation error code for invalid input */
    VALIDATION_ERROR("validation_error"),
    /** Tool not found error code */
    TOOL_NOT_FOUND("tool_not_found"),
    /** Execution error code for runtime failures */
    EXECUTION_ERROR("execution_error"),
    /** Serialization error code for JSON processing failures */
    SERIALIZATION_ERROR("serialization_error"),
    /** Internal error code for unexpected failures */
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
