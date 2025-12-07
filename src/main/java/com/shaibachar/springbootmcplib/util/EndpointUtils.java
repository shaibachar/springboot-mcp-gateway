package com.shaibachar.springbootmcplib.util;

import com.shaibachar.springbootmcplib.model.EndpointMetadata;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.*;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;

/**
 * Utility class for endpoint and parameter operations.
 * Provides shared methods for working with REST endpoint metadata and parameters.
 */
public final class EndpointUtils {

    private static final Logger logger = LoggerFactory.getLogger(EndpointUtils.class);

    private EndpointUtils() {
        // Prevent instantiation
    }

    /**
     * Generates a unique tool name from the endpoint metadata.
     * Format: httpMethod_path_methodName
     *
     * @param endpoint the endpoint metadata
     * @return the tool name
     */
    public static String generateToolName(EndpointMetadata endpoint) {
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
     * Checks if a parameter is a special Spring framework parameter.
     * Special parameters should be skipped during tool schema generation.
     *
     * @param param the parameter
     * @return true if it's a special parameter
     */
    public static boolean isSpecialParameter(Parameter param) {
        Class<?> type = param.getType();
        String typeName = type.getName();

        // Skip Spring-specific types
        return typeName.startsWith("org.springframework.web.context") ||
               typeName.startsWith("org.springframework.http.HttpServletRequest") ||
               typeName.startsWith("org.springframework.http.HttpServletResponse") ||
               typeName.startsWith("javax.servlet") ||
               typeName.startsWith("jakarta.servlet");
    }

    /**
     * Gets the parameter name from annotations or reflection.
     * Checks for @PathVariable, @RequestParam, and @RequestBody annotations.
     *
     * @param param the parameter
     * @return the parameter name
     */
    public static String getParameterName(Parameter param) {
        // Check for @PathVariable
        PathVariable pathVariable = param.getAnnotation(PathVariable.class);
        if (pathVariable != null && !pathVariable.value().isEmpty()) {
            return pathVariable.value();
        }

        // Check for @RequestParam
        RequestParam requestParam = param.getAnnotation(RequestParam.class);
        if (requestParam != null && !requestParam.value().isEmpty()) {
            return requestParam.value();
        }

        // Check for @RequestBody
        RequestBody requestBody = param.getAnnotation(RequestBody.class);
        if (requestBody != null) {
            return "body";
        }

        // Fallback to parameter name from reflection
        return param.getName();
    }

    /**
     * Gets the parameter name from GraphQL @Argument annotation or reflection.
     *
     * @param param the parameter
     * @return the parameter name
     */
    @SuppressWarnings("unchecked") // Reflection-based annotation access requires unchecked cast
    public static String getGraphQLParameterName(Parameter param) {
        try {
            // Check for @Argument annotation
            Class<?> argumentClass = Class.forName("org.springframework.graphql.data.method.annotation.Argument");
            Class<? extends java.lang.annotation.Annotation> annotationClass = (Class<? extends java.lang.annotation.Annotation>) argumentClass;
            Object argumentAnnotation = param.getAnnotation(annotationClass);
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
     * Checks if a parameter is a special GraphQL parameter.
     *
     * @param param the parameter
     * @return true if it's a special parameter
     */
    public static boolean isGraphQLSpecialParameter(Parameter param) {
        Class<?> type = param.getType();
        String typeName = type.getName();

        return typeName.startsWith("graphql.schema.DataFetchingEnvironment") ||
               typeName.startsWith("org.springframework.graphql.data.method.annotation.support");
    }
}
