package com.shaibachar.springbootmcplib.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.Map;

/**
 * Represents an MCP tool that corresponds to a REST endpoint.
 * This model follows the Model Context Protocol specification for tool definitions.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class McpTool {

    /**
     * The unique name/identifier of the tool.
     * Typically derived from the controller method name and path.
     */
    private String name;

    /**
     * A human-readable description of what the tool does.
     */
    private String description;

    /**
     * JSON Schema describing the input parameters for the tool.
     */
    private Map<String, Object> inputSchema;

    /**
     * Default constructor for JSON deserialization.
     */
    public McpTool() {
    }

    /**
     * Constructor with all fields.
     *
     * @param name The unique name of the tool
     * @param description The description of the tool
     * @param inputSchema The JSON Schema for input parameters
     */
    public McpTool(String name, String description, Map<String, Object> inputSchema) {
        this.name = name;
        this.description = description;
        this.inputSchema = inputSchema;
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
     * Gets the tool description.
     *
     * @return the tool description
     */
    public String getDescription() {
        return description;
    }

    /**
     * Sets the tool description.
     *
     * @param description the tool description
     */
    public void setDescription(String description) {
        this.description = description;
    }

    /**
     * Gets the input schema.
     *
     * @return the input schema as a Map
     */
    public Map<String, Object> getInputSchema() {
        return inputSchema;
    }

    /**
     * Sets the input schema.
     *
     * @param inputSchema the input schema as a Map
     */
    public void setInputSchema(Map<String, Object> inputSchema) {
        this.inputSchema = inputSchema;
    }

    @Override
    public String toString() {
        return "McpTool{" +
                "name='" + name + '\'' +
                ", description='" + description + '\'' +
                ", inputSchema=" + inputSchema +
                '}';
    }
}
