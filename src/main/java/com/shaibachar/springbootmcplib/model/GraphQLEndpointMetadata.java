package com.shaibachar.springbootmcplib.model;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.Objects;

/**
 * Metadata about a GraphQL endpoint discovered from Spring GraphQL controllers.
 * Used internally to map GraphQL queries and mutations to MCP tools.
 */
public class GraphQLEndpointMetadata {

    /**
     * The type of GraphQL operation (QUERY or MUTATION).
     */
    public enum OperationType {
        QUERY, MUTATION
    }

    /**
     * The name of the GraphQL field.
     */
    private final String fieldName;

    /**
     * The type of operation (QUERY or MUTATION).
     */
    private final OperationType operationType;

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
     * Constructor for GraphQLEndpointMetadata.
     *
     * @param fieldName the GraphQL field name
     * @param operationType the operation type (QUERY or MUTATION)
     * @param controllerClass the controller class
     * @param handlerMethod the handler method
     */
    public GraphQLEndpointMetadata(String fieldName, OperationType operationType,
                                   Class<?> controllerClass, Method handlerMethod) {
        this.fieldName = fieldName;
        this.operationType = operationType;
        this.controllerClass = controllerClass;
        this.handlerMethod = handlerMethod;
        this.parameters = handlerMethod.getParameters();
    }

    /**
     * Gets the field name.
     *
     * @return the field name
     */
    public String getFieldName() {
        return fieldName;
    }

    /**
     * Gets the operation type.
     *
     * @return the operation type
     */
    public OperationType getOperationType() {
        return operationType;
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

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        GraphQLEndpointMetadata that = (GraphQLEndpointMetadata) o;
        return Objects.equals(fieldName, that.fieldName) &&
                operationType == that.operationType &&
                Objects.equals(controllerClass, that.controllerClass) &&
                Objects.equals(handlerMethod, that.handlerMethod);
    }

    @Override
    public int hashCode() {
        return Objects.hash(fieldName, operationType, controllerClass, handlerMethod);
    }

    @Override
    public String toString() {
        return "GraphQLEndpointMetadata{" +
                "fieldName='" + fieldName + '\'' +
                ", operationType=" + operationType +
                ", controllerClass=" + controllerClass.getSimpleName() +
                ", handlerMethod=" + handlerMethod.getName() +
                '}';
    }
}
