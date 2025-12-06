package com.shaibachar.springbootmcplib.controller;

import com.shaibachar.springbootmcplib.model.*;
import com.shaibachar.springbootmcplib.service.McpToolExecutionService;
import com.shaibachar.springbootmcplib.service.McpToolMappingService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * REST controller that exposes MCP (Model Context Protocol) endpoints.
 * Provides endpoints for listing available tools and executing them.
 */
@RestController
@RequestMapping("/mcp")
public class McpController {

    private static final Logger logger = LoggerFactory.getLogger(McpController.class);

    private final McpToolMappingService toolMappingService;
    private final McpToolExecutionService toolExecutionService;

    /**
     * Constructor with dependency injection.
     *
     * @param toolMappingService the tool mapping service
     * @param toolExecutionService the tool execution service
     */
    public McpController(McpToolMappingService toolMappingService,
                        McpToolExecutionService toolExecutionService) {
        this.toolMappingService = toolMappingService;
        this.toolExecutionService = toolExecutionService;
        logger.debug("McpController initialized");
    }

    /**
     * Lists all available MCP tools.
     * This endpoint returns metadata about all REST endpoints that have been
     * discovered and mapped to MCP tools.
     *
     * @return response containing the list of tools
     */
    @GetMapping("/tools")
    public ResponseEntity<McpToolsResponse> listTools() {
        logger.debug("Received request to list MCP tools");

        try {
            List<McpTool> tools = toolMappingService.getAllTools();
            logger.debug("Returning {} tools", tools.size());

            McpToolsResponse response = new McpToolsResponse(tools);
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            logger.error("Error listing tools", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Executes a specific MCP tool.
     * This endpoint accepts a tool execution request and invokes the corresponding
     * REST endpoint with the provided arguments.
     *
     * @param request the tool execution request
     * @return response containing the execution result
     */
    @PostMapping("/tools/execute")
    public ResponseEntity<McpToolExecutionResponse> executeTool(@Valid @RequestBody McpToolExecutionRequest request,
                                                                BindingResult bindingResult) {
        String requestId = UUID.randomUUID().toString();
        logger.debug("Received request to execute tool: {} with correlation {}", request.getName(), requestId);

        try {
            if (bindingResult.hasErrors()) {
                logger.error("Validation failed for tool {}: {}", request.getName(), bindingResult.getAllErrors());
                McpToolExecutionResponse errorResponse = createErrorResponse(
                        "validation_error",
                        requestId,
                        "Tool name is required",
                        bindingResult.getAllErrors()
                );
                return ResponseEntity.badRequest().body(errorResponse);
            }

            McpToolExecutionResponse response = toolExecutionService.executeTool(
                    request.getName(),
                    request.getArguments(),
                    requestId
            );

            logger.debug("Tool execution completed: {}", request.getName());

            if (Boolean.TRUE.equals(response.getIsError())) {
                return ResponseEntity.badRequest().body(response);
            }

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            logger.error("Error executing tool: " + request.getName(), e);
            McpToolExecutionResponse errorResponse = createErrorResponse(
                    "internal_error",
                    requestId,
                    "Internal error occurred",
                    null
            );
            return ResponseEntity.internalServerError().body(errorResponse);
        }
    }

    /**
     * Refreshes the tool cache.
     * This endpoint can be called to force a refresh of the discovered endpoints
     * and their corresponding MCP tools.
     *
     * @return response indicating success
     */
    @PostMapping("/tools/refresh")
    public ResponseEntity<String> refreshTools() {
        logger.debug("Received request to refresh MCP tools");

        try {
            toolMappingService.refreshTools();
            logger.debug("Tools refreshed successfully");
            return ResponseEntity.ok("Tools refreshed successfully");

        } catch (Exception e) {
            logger.error("Error refreshing tools", e);
            return ResponseEntity.internalServerError().body("Error refreshing tools");
        }
    }

    /**
     * Creates an error response.
     *
     * @param errorMessage the error message
     * @return the error response
     */
    private McpToolExecutionResponse createErrorResponse(String errorCode, String requestId, String errorMessage, Object details) {
        McpToolExecutionResponse.ContentItem content =
                new McpToolExecutionResponse.ContentItem("text", errorMessage);
        return new McpToolExecutionResponse(java.util.Collections.singletonList(content), errorCode, requestId, details);
    }
}
