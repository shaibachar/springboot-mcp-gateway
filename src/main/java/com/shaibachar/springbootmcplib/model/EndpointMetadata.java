package com.shaibachar.springbootmcplib.model;

import org.springframework.web.bind.annotation.RequestMethod;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.Arrays;
import java.util.Objects;

/**
 * Metadata about a REST endpoint discovered from Spring controllers.
 * Used internally to map endpoints to MCP tools.
 */
public class EndpointMetadata {

    /**
     * The URL path pattern for the endpoint.
     */
    private final String path;

    /**
     * The HTTP method (GET, POST, etc.).
     */
    private final RequestMethod httpMethod;

    /**
     * The controller class containing this endpoint.
     */
    private final Class<?> controllerClass;

    /**
     * The handler method for this endpoint.
     */
    private final Method handlerMethod;

    /**
     * The parameters of the handler method.
     */
    private final Parameter[] parameters;

    /**
     * The base path from the controller class (if any).
     */
    private final String basePath;

    /**
     * Constructor for EndpointMetadata.
     *
     * @param path the endpoint path
     * @param httpMethod the HTTP method
     * @param controllerClass the controller class
     * @param handlerMethod the handler method
     * @param basePath the base path from controller
     */
    public EndpointMetadata(String path, RequestMethod httpMethod, Class<?> controllerClass, 
                           Method handlerMethod, String basePath) {
        this.path = path;
        this.httpMethod = httpMethod;
        this.controllerClass = controllerClass;
        this.handlerMethod = handlerMethod;
        this.parameters = handlerMethod.getParameters();
        this.basePath = basePath;
    }

    /**
     * Gets the full path including base path.
     *
     * @return the full path
     */
    public String getFullPath() {
        String base = basePath != null && !basePath.isEmpty() ? basePath : "";
        String endpoint = path != null && !path.isEmpty() ? path : "";
        
        // Normalize paths
        if (base.endsWith("/") && endpoint.startsWith("/")) {
            return base + endpoint.substring(1);
        } else if (!base.endsWith("/") && !endpoint.startsWith("/") && !base.isEmpty() && !endpoint.isEmpty()) {
            return base + "/" + endpoint;
        } else {
            return base + endpoint;
        }
    }

    /**
     * Gets the endpoint path.
     *
     * @return the path
     */
    public String getPath() {
        return path;
    }

    /**
     * Gets the HTTP method.
     *
     * @return the HTTP method
     */
    public RequestMethod getHttpMethod() {
        return httpMethod;
    }

    /**
     * Gets the controller class.
     *
     * @return the controller class
     */
    public Class<?> getControllerClass() {
        return controllerClass;
    }

    /**
     * Gets the handler method.
     *
     * @return the handler method
     */
    public Method getHandlerMethod() {
        return handlerMethod;
    }

    /**
     * Gets the method parameters.
     *
     * @return the parameters array
     */
    public Parameter[] getParameters() {
        return parameters;
    }

    /**
     * Gets the base path.
     *
     * @return the base path
     */
    public String getBasePath() {
        return basePath;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        EndpointMetadata that = (EndpointMetadata) o;
        return Objects.equals(path, that.path) &&
                httpMethod == that.httpMethod &&
                Objects.equals(controllerClass, that.controllerClass) &&
                Objects.equals(handlerMethod, that.handlerMethod);
    }

    @Override
    public int hashCode() {
        return Objects.hash(path, httpMethod, controllerClass, handlerMethod);
    }

    @Override
    public String toString() {
        return "EndpointMetadata{" +
                "path='" + path + '\'' +
                ", httpMethod=" + httpMethod +
                ", controllerClass=" + controllerClass.getSimpleName() +
                ", handlerMethod=" + handlerMethod.getName() +
                ", basePath='" + basePath + '\'' +
                '}';
    }
}
