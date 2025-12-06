package com.shaibachar.springbootmcplib.service;

import com.shaibachar.springbootmcplib.model.EndpointMetadata;
import com.shaibachar.springbootmcplib.model.McpTool;
import com.shaibachar.springbootmcplib.util.EndpointUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.*;

import java.lang.reflect.Parameter;
import java.util.*;

/**
 * Service responsible for mapping REST endpoints to MCP tools.
 * Converts Spring MVC endpoint metadata into MCP tool definitions with JSON schemas.
 */
@Service
public class McpToolMappingService {

    private static final Logger logger = LoggerFactory.getLogger(McpToolMappingService.class);

    private final EndpointDiscoveryService discoveryService;
    private List<McpTool> cachedTools;

    /**
     * Constructor with dependency injection.
     *
     * @param discoveryService the endpoint discovery service
     */
    public McpToolMappingService(EndpointDiscoveryService discoveryService) {
        this.discoveryService = discoveryService;
        logger.debug("McpToolMappingService initialized");
    }

    /**
     * Gets all available MCP tools.
     * Results are cached after first call.
     *
     * @return list of MCP tools
     */
    public List<McpTool> getAllTools() {
        if (cachedTools == null) {
            logger.debug("Building MCP tools from discovered endpoints");
            List<EndpointMetadata> endpoints = discoveryService.discoverEndpoints();
            cachedTools = mapEndpointsToTools(endpoints);
            logger.debug("Built {} MCP tools", cachedTools.size());
        }
        return new ArrayList<>(cachedTools);
    }

    /**
     * Refreshes the tool cache by re-discovering endpoints.
     */
    public void refreshTools() {
        logger.debug("Refreshing MCP tools cache");
        cachedTools = null;
        getAllTools();
    }

    /**
     * Finds a tool by name.
     *
     * @param toolName the tool name
     * @return the tool or null if not found
     */
    public McpTool findToolByName(String toolName) {
        logger.debug("Finding tool by name: {}", toolName);
        return getAllTools().stream()
                .filter(tool -> tool.getName().equals(toolName))
                .findFirst()
                .orElse(null);
    }

    /**
     * Maps endpoint metadata to MCP tools.
     *
     * @param endpoints the list of endpoint metadata
     * @return list of MCP tools
     */
    private List<McpTool> mapEndpointsToTools(List<EndpointMetadata> endpoints) {
        List<McpTool> tools = new ArrayList<>();

        for (EndpointMetadata endpoint : endpoints) {
            try {
                McpTool tool = createToolFromEndpoint(endpoint);
                tools.add(tool);
                logger.debug("Mapped endpoint to tool: {}", tool.getName());
            } catch (Exception e) {
                logger.error("Error mapping endpoint to tool: " + endpoint, e);
            }
        }

        return tools;
    }

    /**
     * Creates an MCP tool from endpoint metadata.
     *
     * @param endpoint the endpoint metadata
     * @return the MCP tool
     */
    private McpTool createToolFromEndpoint(EndpointMetadata endpoint) {
        String toolName = EndpointUtils.generateToolName(endpoint);
        String description = generateDescription(endpoint);
        Map<String, Object> inputSchema = generateInputSchema(endpoint);

        logger.debug("Created tool: name={}, path={}, method={}", 
            toolName, endpoint.getFullPath(), endpoint.getHttpMethod());

        return new McpTool(toolName, description, inputSchema);
    }

    /**
     * Generates a description for the tool.
     *
     * @param endpoint the endpoint metadata
     * @return the description
     */
    private String generateDescription(EndpointMetadata endpoint) {
        return String.format("Calls %s %s (Controller: %s, Method: %s)",
                endpoint.getHttpMethod().name(),
                endpoint.getFullPath(),
                endpoint.getControllerClass().getSimpleName(),
                endpoint.getHandlerMethod().getName());
    }

    /**
     * Generates a JSON schema for the tool's input parameters.
     *
     * @param endpoint the endpoint metadata
     * @return the input schema as a Map
     */
    private Map<String, Object> generateInputSchema(EndpointMetadata endpoint) {
        Map<String, Object> schema = new HashMap<>();
        schema.put("type", "object");

        Map<String, Object> properties = new HashMap<>();
        List<String> required = new ArrayList<>();

        Parameter[] parameters = endpoint.getParameters();
        logger.debug("Generating schema for {} parameters", parameters.length);

        for (Parameter param : parameters) {
            // Skip special Spring parameters
            if (EndpointUtils.isSpecialParameter(param)) {
                logger.debug("Skipping special parameter: {}", param.getType().getSimpleName());
                continue;
            }

            String paramName = EndpointUtils.getParameterName(param);
            Map<String, Object> paramSchema = generateParameterSchema(param);

            properties.put(paramName, paramSchema);

            // Add to required if not annotated with @RequestParam(required=false)
            if (isRequiredParameter(param)) {
                required.add(paramName);
            }

            logger.debug("Added parameter to schema: {} ({})", paramName, param.getType().getSimpleName());
        }

        schema.put("properties", properties);
        if (!required.isEmpty()) {
            schema.put("required", required);
        }

        return schema;
    }

    /**
     * Generates schema for a single parameter.
     *
     * @param param the parameter
     * @return the parameter schema
     */
    private Map<String, Object> generateParameterSchema(Parameter param) {
        Map<String, Object> schema = new HashMap<>();
        Class<?> type = param.getType();

        // Map Java types to JSON schema types
        if (type == String.class) {
            schema.put("type", "string");
        } else if (type == Integer.class || type == int.class ||
                   type == Long.class || type == long.class) {
            schema.put("type", "integer");
        } else if (type == Double.class || type == double.class ||
                   type == Float.class || type == float.class) {
            schema.put("type", "number");
        } else if (type == Boolean.class || type == boolean.class) {
            schema.put("type", "boolean");
        } else if (type.isArray() || Collection.class.isAssignableFrom(type)) {
            schema.put("type", "array");
        } else {
            // For complex objects, use object type
            schema.put("type", "object");
        }

        return schema;
    }

    /**
     * Checks if a parameter is required.
     *
     * @param param the parameter
     * @return true if required
     */
    private boolean isRequiredParameter(Parameter param) {
        // Check @RequestParam required attribute
        RequestParam requestParam = param.getAnnotation(RequestParam.class);
        if (requestParam != null) {
            return requestParam.required();
        }

        // Check @PathVariable required attribute
        PathVariable pathVariable = param.getAnnotation(PathVariable.class);
        if (pathVariable != null) {
            return pathVariable.required();
        }

        // @RequestBody is typically required unless wrapped in Optional
        RequestBody requestBody = param.getAnnotation(RequestBody.class);
        if (requestBody != null) {
            return requestBody.required();
        }

        // Default to not required
        return false;
    }
}
