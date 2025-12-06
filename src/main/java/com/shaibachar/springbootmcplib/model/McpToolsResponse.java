package com.shaibachar.springbootmcplib.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.List;

/**
 * Response model for the list tools MCP endpoint.
 * Contains the list of all available MCP tools.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class McpToolsResponse {

    /**
     * List of available MCP tools.
     */
    private List<McpTool> tools;

    /**
     * Default constructor for JSON deserialization.
     */
    public McpToolsResponse() {
    }

    /**
     * Constructor with tools list.
     *
     * @param tools the list of tools
     */
    public McpToolsResponse(List<McpTool> tools) {
        this.tools = tools;
    }

    /**
     * Gets the list of tools.
     *
     * @return the list of tools
     */
    public List<McpTool> getTools() {
        return tools;
    }

    /**
     * Sets the list of tools.
     *
     * @param tools the list of tools
     */
    public void setTools(List<McpTool> tools) {
        this.tools = tools;
    }

    @Override
    public String toString() {
        return "McpToolsResponse{" +
                "tools=" + tools +
                '}';
    }
}
