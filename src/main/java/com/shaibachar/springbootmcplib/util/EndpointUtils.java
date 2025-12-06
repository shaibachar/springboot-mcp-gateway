package com.shaibachar.springbootmcplib.util;

import com.shaibachar.springbootmcplib.model.EndpointMetadata;
import org.springframework.web.bind.annotation.*;

import java.lang.reflect.Parameter;

/**
 * Utility class for endpoint and parameter operations.
 * Provides shared methods for working with REST endpoint metadata and parameters.
 */
public final class EndpointUtils {

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
}
