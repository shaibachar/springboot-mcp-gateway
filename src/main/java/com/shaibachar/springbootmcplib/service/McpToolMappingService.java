package com.shaibachar.springbootmcplib.service;

import com.shaibachar.springbootmcplib.model.EndpointMetadata;
import com.shaibachar.springbootmcplib.model.GraphQLEndpointMetadata;
import com.shaibachar.springbootmcplib.model.McpTool;
import com.shaibachar.springbootmcplib.util.EndpointUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.*;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.*;

/**
 * Service responsible for mapping REST and GraphQL endpoints to MCP tools.
 * Converts Spring MVC endpoint metadata into MCP tool definitions with JSON schemas.
 */
public class McpToolMappingService {

    private static final Logger logger = LoggerFactory.getLogger(McpToolMappingService.class);

    private final EndpointDiscoveryService discoveryService;
    private final GraphQLDiscoveryService graphQLDiscoveryService;
    private List<McpTool> cachedTools;

    /**
     * Constructor with dependency injection.
     *
     * @param discoveryService the endpoint discovery service
     * @param graphQLDiscoveryService the GraphQL discovery service
     */
    public McpToolMappingService(EndpointDiscoveryService discoveryService,
                                 GraphQLDiscoveryService graphQLDiscoveryService) {
        this.discoveryService = discoveryService;
        this.graphQLDiscoveryService = graphQLDiscoveryService;
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
            List<EndpointMetadata> restEndpoints = discoveryService.discoverEndpoints();
            List<GraphQLEndpointMetadata> graphqlEndpoints = graphQLDiscoveryService.discoverGraphQLEndpoints();
            
            cachedTools = new ArrayList<>();
            cachedTools.addAll(mapEndpointsToTools(restEndpoints));
            cachedTools.addAll(mapGraphQLEndpointsToTools(graphqlEndpoints));
            
            logger.debug("Built {} MCP tools ({} REST, {} GraphQL)", 
                    cachedTools.size(), restEndpoints.size(), graphqlEndpoints.size());
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
                logger.error("Error mapping endpoint: {} {}", 
                    endpoint.getHttpMethod(), endpoint.getFullPath(), e);
            }
        }

        return tools;
    }

    /**
     * Maps GraphQL endpoint metadata to MCP tools.
     *
     * @param endpoints the list of GraphQL endpoint metadata
     * @return list of MCP tools
     */
    private List<McpTool> mapGraphQLEndpointsToTools(List<GraphQLEndpointMetadata> endpoints) {
        List<McpTool> tools = new ArrayList<>();

        for (GraphQLEndpointMetadata endpoint : endpoints) {
            try {
                McpTool tool = createToolFromGraphQLEndpoint(endpoint);
                tools.add(tool);
                logger.debug("Mapped GraphQL endpoint to tool: {}", tool.getName());
            } catch (Exception e) {
                logger.error("Error mapping GraphQL endpoint: {} {}", 
                    endpoint.getOperationType(), endpoint.getFieldName(), e);
            }
        }

        return tools;
    }

    /**
     * Creates an MCP tool from GraphQL endpoint metadata.
     *
     * @param endpoint the GraphQL endpoint metadata
     * @return the MCP tool
     */
    private McpTool createToolFromGraphQLEndpoint(GraphQLEndpointMetadata endpoint) {
        String toolName = generateGraphQLToolName(endpoint);
        String description = generateGraphQLDescription(endpoint);
        Map<String, Object> inputSchema = generateGraphQLInputSchema(endpoint);

        logger.debug("Created GraphQL tool: name={}, field={}, type={}", 
            toolName, endpoint.getFieldName(), endpoint.getOperationType());

        return new McpTool(toolName, description, inputSchema);
    }

    /**
     * Generates a unique tool name from GraphQL endpoint metadata.
     *
     * @param endpoint the GraphQL endpoint metadata
     * @return the tool name
     */
    private String generateGraphQLToolName(GraphQLEndpointMetadata endpoint) {
        String operationType = endpoint.getOperationType().name().toLowerCase();
        String fieldName = endpoint.getFieldName()
                .replaceAll("[^a-zA-Z0-9]", "_")
                .replaceAll("_+", "_")
                .replaceAll("^_|_$", "");
        
        return "graphql_" + operationType + "_" + fieldName;
    }

    /**
     * Generates a description for the GraphQL tool.
     *
     * @param endpoint the GraphQL endpoint metadata
     * @return the description
     */
    private String generateGraphQLDescription(GraphQLEndpointMetadata endpoint) {
        return String.format("Calls GraphQL %s '%s' (Controller: %s, Method: %s)",
                endpoint.getOperationType().name(),
                endpoint.getFieldName(),
                endpoint.getControllerClass().getSimpleName(),
                endpoint.getHandlerMethod().getName());
    }

    /**
     * Generates a JSON schema for the GraphQL tool's input parameters.
     *
     * @param endpoint the GraphQL endpoint metadata
     * @return the input schema as a Map
     */
    private Map<String, Object> generateGraphQLInputSchema(GraphQLEndpointMetadata endpoint) {
        Map<String, Object> schema = new HashMap<>();
        schema.put("type", "object");

        Map<String, Object> properties = new HashMap<>();
        List<String> required = new ArrayList<>();

        Parameter[] parameters = endpoint.getParameters();
        logger.debug("Generating schema for {} GraphQL parameters", parameters.length);

        for (Parameter param : parameters) {
            // Skip special Spring/GraphQL parameters
            if (isGraphQLSpecialParameter(param)) {
                logger.debug("Skipping special GraphQL parameter: {}", param.getType().getSimpleName());
                continue;
            }

            String paramName = getGraphQLParameterName(param);
            Map<String, Object> paramSchema = generateParameterSchema(param);

            properties.put(paramName, paramSchema);

            // Check if parameter is required (not annotated with @Argument with required=false)
            if (isGraphQLRequiredParameter(param)) {
                required.add(paramName);
            }

            logger.debug("Added GraphQL parameter to schema: {} ({})", paramName, param.getType().getSimpleName());
        }

        schema.put("properties", properties);
        if (!required.isEmpty()) {
            schema.put("required", required);
        }

        return schema;
    }

    /**
     * Checks if a parameter is a special GraphQL parameter.
     *
     * @param param the parameter
     * @return true if it's a special parameter
     */
    private boolean isGraphQLSpecialParameter(Parameter param) {
        Class<?> type = param.getType();
        String typeName = type.getName();

        // Skip GraphQL-specific types
        return typeName.startsWith("graphql.schema.DataFetchingEnvironment") ||
               typeName.startsWith("org.springframework.graphql.data.method.annotation.support");
    }

    /**
     * Gets the parameter name from GraphQL annotations or reflection.
     *
     * @param param the parameter
     * @return the parameter name
     */
    private String getGraphQLParameterName(Parameter param) {
        try {
            // Check for @Argument annotation
            Class<?> argumentClass = Class.forName("org.springframework.graphql.data.method.annotation.Argument");
            Object argumentAnnotation = param.getAnnotation((Class) argumentClass);
            if (argumentAnnotation != null) {
                Method nameMethod = argumentClass.getMethod("name");
                String name = (String) nameMethod.invoke(argumentAnnotation);
                if (name != null && !name.isEmpty()) {
                    return name;
                }
                
                Method valueMethod = argumentClass.getMethod("value");
                String value = (String) valueMethod.invoke(argumentAnnotation);
                if (value != null && !value.isEmpty()) {
                    return value;
                }
            }
        } catch (Exception e) {
            logger.debug("Could not extract parameter name from @Argument annotation");
        }

        // Fallback to parameter name from reflection
        return param.getName();
    }

    /**
     * Checks if a GraphQL parameter is required.
     *
     * @param param the parameter
     * @return true if required
     */
    private boolean isGraphQLRequiredParameter(Parameter param) {
        try {
            // Check @Argument required attribute
            Class<?> argumentClass = Class.forName("org.springframework.graphql.data.method.annotation.Argument");
            Object argumentAnnotation = param.getAnnotation((Class) argumentClass);
            if (argumentAnnotation != null) {
                // GraphQL arguments are optional by default in Spring for GraphQL
                return false;
            }
        } catch (Exception e) {
            logger.debug("Could not check GraphQL parameter required status");
        }

        // Default to not required for GraphQL
        return false;
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
