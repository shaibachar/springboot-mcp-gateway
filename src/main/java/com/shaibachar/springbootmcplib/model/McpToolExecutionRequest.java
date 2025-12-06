package com.shaibachar.springbootmcplib.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.validation.constraints.NotBlank;
import java.util.Map;

/**
 * Request model for executing an MCP tool.
 * Contains the tool name and input parameters.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class McpToolExecutionRequest {

    /**
     * The name of the tool to execute.
     */
    @NotBlank(message = "Tool name is required")
    private String name;

    /**
     * Input parameters for the tool execution.
     */
    private Map<String, Object> arguments;

    /**
     * Default constructor for JSON deserialization.
     */
    public McpToolExecutionRequest() {
    }

    /**
     * Constructor with all fields.
     *
     * @param name the tool name
     * @param arguments the input arguments
     */
    public McpToolExecutionRequest(String name, Map<String, Object> arguments) {
        this.name = name;
        this.arguments = arguments;
    }

    /**
     * Gets the tool name.
     *
     * @return the tool name
     */
    public String getName() {
        return name;
    }

    /**
     * Sets the tool name.
     *
     * @param name the tool name
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * Gets the input arguments.
     *
     * @return the arguments map
     */
    public Map<String, Object> getArguments() {
        return arguments;
    }

    /**
     * Sets the input arguments.
     *
     * @param arguments the arguments map
     */
    public void setArguments(Map<String, Object> arguments) {
        this.arguments = arguments;
    }

    @Override
    public String toString() {
        return "McpToolExecutionRequest{" +
                "name='" + name + '\'' +
                ", arguments=" + arguments +
                '}';
    }
}
