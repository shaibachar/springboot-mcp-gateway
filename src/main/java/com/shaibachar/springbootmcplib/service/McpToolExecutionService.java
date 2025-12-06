package com.shaibachar.springbootmcplib.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.shaibachar.springbootmcplib.model.EndpointMetadata;
import com.shaibachar.springbootmcplib.model.GraphQLEndpointMetadata;
import com.shaibachar.springbootmcplib.model.McpToolExecutionResponse;
import com.shaibachar.springbootmcplib.util.EndpointUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.*;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.*;

/**
 * Service responsible for executing MCP tools by invoking the corresponding REST or GraphQL endpoints.
 * Handles parameter mapping and method invocation.
 */
public class McpToolExecutionService {

    private static final Logger logger = LoggerFactory.getLogger(McpToolExecutionService.class);

    private final EndpointDiscoveryService discoveryService;
    private final GraphQLDiscoveryService graphQLDiscoveryService;
    private final ApplicationContext applicationContext;
    private final ObjectMapper objectMapper;

    /**
     * Constructor with dependency injection.
     *
     * @param discoveryService the endpoint discovery service
     * @param graphQLDiscoveryService the GraphQL discovery service
     * @param applicationContext the Spring application context
     * @param objectMapper the Jackson object mapper
     */
    public McpToolExecutionService(EndpointDiscoveryService discoveryService,
                                   GraphQLDiscoveryService graphQLDiscoveryService,
                                   ApplicationContext applicationContext,
                                   ObjectMapper objectMapper) {
        this.discoveryService = discoveryService;
        this.graphQLDiscoveryService = graphQLDiscoveryService;
        this.applicationContext = applicationContext;
        this.objectMapper = objectMapper;
        logger.debug("McpToolExecutionService initialized");
    }

    /**
     * Executes a tool by name with the given arguments.
     *
     * @param toolName the name of the tool to execute
     * @param arguments the input arguments
     * @return the execution response
     */
    public McpToolExecutionResponse executeTool(String toolName, Map<String, Object> arguments) {
        logger.debug("Executing tool: {} with arguments: {}", toolName, arguments);

        try {
            // Check if it's a GraphQL tool
            if (toolName.startsWith("graphql_")) {
                GraphQLEndpointMetadata graphqlEndpoint = findGraphQLEndpointForTool(toolName);
                if (graphqlEndpoint != null) {
                    Object result = invokeGraphQLEndpoint(graphqlEndpoint, arguments);
                    return createSuccessResponse(result);
                }
            }
            
            // Try REST endpoint
            EndpointMetadata endpoint = findEndpointForTool(toolName);
            if (endpoint == null) {
                logger.error("Tool not found: {}", toolName);
                return createErrorResponse("Tool not found: " + toolName);
            }

            // Invoke the endpoint
            Object result = invokeEndpoint(endpoint, arguments);

            // Convert result to response
            return createSuccessResponse(result);

        } catch (Exception e) {
            logger.error("Error executing tool: " + toolName, e);
            return createErrorResponse("Error executing tool: " + e.getMessage());
        }
    }

    /**
     * Finds the endpoint metadata for a given tool name.
     *
     * @param toolName the tool name
     * @return the endpoint metadata or null
     */
    private EndpointMetadata findEndpointForTool(String toolName) {
        List<EndpointMetadata> endpoints = discoveryService.discoverEndpoints();

        for (EndpointMetadata endpoint : endpoints) {
            String generatedName = EndpointUtils.generateToolName(endpoint);
            if (generatedName.equals(toolName)) {
                logger.debug("Found endpoint for tool {}: {}", toolName, endpoint);
                return endpoint;
            }
        }

        return null;
    }

    /**
     * Finds the GraphQL endpoint metadata for a given tool name.
     *
     * @param toolName the tool name
     * @return the GraphQL endpoint metadata or null
     */
    private GraphQLEndpointMetadata findGraphQLEndpointForTool(String toolName) {
        List<GraphQLEndpointMetadata> endpoints = graphQLDiscoveryService.discoverGraphQLEndpoints();

        for (GraphQLEndpointMetadata endpoint : endpoints) {
            String generatedName = generateGraphQLToolName(endpoint);
            if (generatedName.equals(toolName)) {
                logger.debug("Found GraphQL endpoint for tool {}: {}", toolName, endpoint);
                return endpoint;
            }
        }

        return null;
    }

    /**
     * Generates the tool name for a GraphQL endpoint (must match the one in McpToolMappingService).
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
     * Invokes the GraphQL endpoint with the given arguments.
     *
     * @param endpoint the GraphQL endpoint metadata
     * @param arguments the input arguments
     * @return the result of the invocation
     * @throws Exception if invocation fails
     */
    private Object invokeGraphQLEndpoint(GraphQLEndpointMetadata endpoint, Map<String, Object> arguments) throws Exception {
        logger.debug("Invoking GraphQL endpoint: {}", endpoint);

        // Get the controller bean
        Object controller = applicationContext.getBean(endpoint.getControllerClass());
        Method method = endpoint.getHandlerMethod();

        // Prepare method arguments
        Object[] methodArgs = prepareGraphQLMethodArguments(endpoint, arguments);

        logger.debug("Invoking GraphQL method {} with {} arguments", method.getName(), methodArgs.length);

        // Invoke the method
        return method.invoke(controller, methodArgs);
    }

    /**
     * Prepares the method arguments for GraphQL endpoint from the input arguments map.
     *
     * @param endpoint the GraphQL endpoint metadata
     * @param arguments the input arguments
     * @return array of method arguments
     * @throws Exception if argument preparation fails
     */
    private Object[] prepareGraphQLMethodArguments(GraphQLEndpointMetadata endpoint, Map<String, Object> arguments) throws Exception {
        Parameter[] parameters = endpoint.getParameters();
        Object[] methodArgs = new Object[parameters.length];

        for (int i = 0; i < parameters.length; i++) {
            Parameter param = parameters[i];

            // Skip special GraphQL parameters
            if (isGraphQLSpecialParameter(param)) {
                methodArgs[i] = null;
                continue;
            }

            String paramName = getGraphQLParameterName(param);
            Object value = arguments != null ? arguments.get(paramName) : null;

            // Convert value to parameter type
            methodArgs[i] = convertValue(value, param.getType());

            logger.debug("Prepared GraphQL argument {}: {} = {}", i, paramName, methodArgs[i]);
        }

        return methodArgs;
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
     * Invokes the endpoint with the given arguments.
     *
     * @param endpoint the endpoint metadata
     * @param arguments the input arguments
     * @return the result of the invocation
     * @throws Exception if invocation fails
     */
    private Object invokeEndpoint(EndpointMetadata endpoint, Map<String, Object> arguments) throws Exception {
        logger.debug("Invoking endpoint: {}", endpoint);

        // Get the controller bean
        Object controller = applicationContext.getBean(endpoint.getControllerClass());
        Method method = endpoint.getHandlerMethod();

        // Prepare method arguments
        Object[] methodArgs = prepareMethodArguments(endpoint, arguments);

        logger.debug("Invoking method {} with {} arguments", method.getName(), methodArgs.length);

        // Invoke the method (controller methods are public, so no accessibility changes needed)
        return method.invoke(controller, methodArgs);
    }

    /**
     * Prepares the method arguments from the input arguments map.
     *
     * @param endpoint the endpoint metadata
     * @param arguments the input arguments
     * @return array of method arguments
     * @throws Exception if argument preparation fails
     */
    private Object[] prepareMethodArguments(EndpointMetadata endpoint, Map<String, Object> arguments) throws Exception {
        Parameter[] parameters = endpoint.getParameters();
        Object[] methodArgs = new Object[parameters.length];

        for (int i = 0; i < parameters.length; i++) {
            Parameter param = parameters[i];

            // Skip special Spring parameters
            if (EndpointUtils.isSpecialParameter(param)) {
                methodArgs[i] = null;
                continue;
            }

            String paramName = EndpointUtils.getParameterName(param);
            Object value = arguments != null ? arguments.get(paramName) : null;

            // Convert value to parameter type
            methodArgs[i] = convertValue(value, param.getType());

            logger.debug("Prepared argument {}: {} = {}", i, paramName, methodArgs[i]);
        }

        return methodArgs;
    }

    /**
     * Converts a value to the target type.
     *
     * @param value the value to convert
     * @param targetType the target type
     * @return the converted value
     * @throws Exception if conversion fails
     */
    private Object convertValue(Object value, Class<?> targetType) throws Exception {
        if (value == null) {
            return null;
        }

        // If types already match
        if (targetType.isInstance(value)) {
            return value;
        }

        // Use Jackson for complex type conversion
        return objectMapper.convertValue(value, targetType);
    }

    /**
     * Creates a success response from the result.
     *
     * @param result the result object
     * @return the execution response
     */
    private McpToolExecutionResponse createSuccessResponse(Object result) {
        try {
            String resultText = objectMapper.writeValueAsString(result);
            McpToolExecutionResponse.ContentItem content = 
                new McpToolExecutionResponse.ContentItem("text", resultText);
            
            return new McpToolExecutionResponse(Collections.singletonList(content), false);
        } catch (Exception e) {
            logger.error("Error serializing result", e);
            return createErrorResponse("Error processing result");
        }
    }

    /**
     * Creates an error response.
     *
     * @param errorMessage the error message
     * @return the execution response
     */
    private McpToolExecutionResponse createErrorResponse(String errorMessage) {
        McpToolExecutionResponse.ContentItem content = 
            new McpToolExecutionResponse.ContentItem("text", errorMessage);
        
        return new McpToolExecutionResponse(Collections.singletonList(content), true);
    }
}
