package com.shaibachar.springbootmcplib.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.shaibachar.springbootmcplib.model.EndpointMetadata;
import com.shaibachar.springbootmcplib.model.McpToolExecutionResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.*;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.*;

/**
 * Service responsible for executing MCP tools by invoking the corresponding REST endpoints.
 * Handles parameter mapping and method invocation.
 */
@Service
public class McpToolExecutionService {

    private static final Logger logger = LoggerFactory.getLogger(McpToolExecutionService.class);

    private final EndpointDiscoveryService discoveryService;
    private final ApplicationContext applicationContext;
    private final ObjectMapper objectMapper;

    /**
     * Constructor with dependency injection.
     *
     * @param discoveryService the endpoint discovery service
     * @param applicationContext the Spring application context
     * @param objectMapper the Jackson object mapper
     */
    public McpToolExecutionService(EndpointDiscoveryService discoveryService,
                                   ApplicationContext applicationContext,
                                   ObjectMapper objectMapper) {
        this.discoveryService = discoveryService;
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
            // Find the endpoint for this tool
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
            String generatedName = generateToolName(endpoint);
            if (generatedName.equals(toolName)) {
                logger.debug("Found endpoint for tool {}: {}", toolName, endpoint);
                return endpoint;
            }
        }

        return null;
    }

    /**
     * Generates the tool name from endpoint (must match McpToolMappingService).
     *
     * @param endpoint the endpoint metadata
     * @return the tool name
     */
    private String generateToolName(EndpointMetadata endpoint) {
        String method = endpoint.getHttpMethod().name().toLowerCase();
        String path = endpoint.getFullPath()
                .replaceAll("[^a-zA-Z0-9/]", "_")
                .replaceAll("/", "_")
                .replaceAll("_+", "_")
                .replaceAll("^_|_$", "");
        String methodName = endpoint.getHandlerMethod().getName();

        return method + "_" + path + "_" + methodName;
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

        // Invoke the method
        method.setAccessible(true);
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
            if (isSpecialParameter(param)) {
                methodArgs[i] = null;
                continue;
            }

            String paramName = getParameterName(param);
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
     * Gets the parameter name from annotations.
     *
     * @param param the parameter
     * @return the parameter name
     */
    private String getParameterName(Parameter param) {
        PathVariable pathVariable = param.getAnnotation(PathVariable.class);
        if (pathVariable != null && !pathVariable.value().isEmpty()) {
            return pathVariable.value();
        }

        RequestParam requestParam = param.getAnnotation(RequestParam.class);
        if (requestParam != null && !requestParam.value().isEmpty()) {
            return requestParam.value();
        }

        RequestBody requestBody = param.getAnnotation(RequestBody.class);
        if (requestBody != null) {
            return "body";
        }

        return param.getName();
    }

    /**
     * Checks if a parameter is a special Spring parameter.
     *
     * @param param the parameter
     * @return true if special
     */
    private boolean isSpecialParameter(Parameter param) {
        Class<?> type = param.getType();
        String typeName = type.getName();

        return typeName.startsWith("org.springframework.web.context") ||
               typeName.startsWith("org.springframework.http.HttpServletRequest") ||
               typeName.startsWith("org.springframework.http.HttpServletResponse") ||
               typeName.startsWith("javax.servlet") ||
               typeName.startsWith("jakarta.servlet");
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
            return createErrorResponse("Error serializing result: " + e.getMessage());
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
