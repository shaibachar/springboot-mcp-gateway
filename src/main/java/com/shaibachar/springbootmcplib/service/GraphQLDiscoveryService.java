package com.shaibachar.springbootmcplib.service;

import com.shaibachar.springbootmcplib.model.GraphQLEndpointMetadata;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.core.annotation.AnnotationUtils;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Service responsible for discovering GraphQL endpoints from Spring GraphQL controllers.
 * Scans for classes annotated with @Controller and methods annotated with GraphQL annotations.
 */
public class GraphQLDiscoveryService {

    private static final Logger logger = LoggerFactory.getLogger(GraphQLDiscoveryService.class);

    private final ApplicationContext applicationContext;
    private final boolean graphqlAvailable;

    /**
     * Constructor with dependency injection.
     *
     * @param applicationContext the Spring application context
     */
    @Autowired
    public GraphQLDiscoveryService(ApplicationContext applicationContext) {
        this.applicationContext = applicationContext;
        this.graphqlAvailable = isGraphQLAvailable();
        if (graphqlAvailable) {
            logger.debug("GraphQL support is available");
        } else {
            logger.debug("GraphQL support is not available");
        }
    }

    /**
     * Checks if Spring for GraphQL is available on the classpath.
     *
     * @return true if GraphQL is available
     */
    private boolean isGraphQLAvailable() {
        try {
            Class.forName("org.springframework.graphql.data.method.annotation.QueryMapping");
            return true;
        } catch (ClassNotFoundException e) {
            return false;
        }
    }

    /**
     * Discovers all GraphQL endpoints in the application.
     *
     * @return list of GraphQL endpoint metadata
     */
    public List<GraphQLEndpointMetadata> discoverGraphQLEndpoints() {
        logger.debug("Starting GraphQL endpoint discovery");
        List<GraphQLEndpointMetadata> endpoints = new ArrayList<>();

        if (!graphqlAvailable) {
            logger.debug("GraphQL not available, skipping discovery");
            return endpoints;
        }

        try {
            // Get all beans with @Controller annotation
            Map<String, Object> controllers = applicationContext.getBeansWithAnnotation(
                    org.springframework.stereotype.Controller.class);

            for (Map.Entry<String, Object> entry : controllers.entrySet()) {
                Object controller = entry.getValue();
                Class<?> controllerClass = controller.getClass();

                // Skip library's own controllers
                if (isLibraryController(controllerClass)) {
                    continue;
                }

                // Scan for GraphQL methods
                discoverGraphQLMethodsInController(controllerClass, endpoints);
            }

            logger.debug("GraphQL endpoint discovery completed. Found {} endpoints", endpoints.size());
        } catch (Exception e) {
            logger.error("Error during GraphQL endpoint discovery", e);
        }

        return endpoints;
    }

    /**
     * Discovers GraphQL methods in a controller class.
     *
     * @param controllerClass the controller class
     * @param endpoints the list to add discovered endpoints to
     */
    @SuppressWarnings("unchecked") // Reflection-based annotation access requires unchecked cast
    private void discoverGraphQLMethodsInController(Class<?> controllerClass, 
                                                     List<GraphQLEndpointMetadata> endpoints) {
        try {
            Class<?> queryMappingClass = Class.forName("org.springframework.graphql.data.method.annotation.QueryMapping");
            Class<?> mutationMappingClass = Class.forName("org.springframework.graphql.data.method.annotation.MutationMapping");

            for (Method method : controllerClass.getDeclaredMethods()) {
                // Check for @QueryMapping using reflection to avoid compile-time dependency
                if (method.isAnnotationPresent((Class) queryMappingClass)) {
                    Object queryMapping = method.getAnnotation((Class) queryMappingClass);
                    String fieldName = extractFieldName(queryMapping, method);
                    GraphQLEndpointMetadata metadata = new GraphQLEndpointMetadata(
                            fieldName,
                            GraphQLEndpointMetadata.OperationType.QUERY,
                            controllerClass,
                            method
                    );
                    endpoints.add(metadata);
                    logger.debug("Discovered GraphQL query: {} - {}", fieldName, method.getName());
                }

                // Check for @MutationMapping using reflection to avoid compile-time dependency
                if (method.isAnnotationPresent((Class) mutationMappingClass)) {
                    Object mutationMapping = method.getAnnotation((Class) mutationMappingClass);
                    String fieldName = extractFieldName(mutationMapping, method);
                    GraphQLEndpointMetadata metadata = new GraphQLEndpointMetadata(
                            fieldName,
                            GraphQLEndpointMetadata.OperationType.MUTATION,
                            controllerClass,
                            method
                    );
                    endpoints.add(metadata);
                    logger.debug("Discovered GraphQL mutation: {} - {}", fieldName, method.getName());
                }
            }
        } catch (Exception e) {
            logger.error("Error discovering GraphQL methods in controller: {}", 
                    controllerClass.getSimpleName(), e);
        }
    }

    /**
     * Extracts the field name from a GraphQL mapping annotation.
     *
     * @param annotation the annotation object
     * @param method the method
     * @return the field name
     */
    private String extractFieldName(Object annotation, Method method) {
        try {
            Method nameMethod = annotation.getClass().getMethod("name");
            String name = (String) nameMethod.invoke(annotation);
            if (name != null && !name.isEmpty()) {
                return name;
            }
            
            Method valueMethod = annotation.getClass().getMethod("value");
            String value = (String) valueMethod.invoke(annotation);
            if (value != null && !value.isEmpty()) {
                return value;
            }
        } catch (Exception e) {
            logger.debug("Could not extract field name from annotation, using method name");
        }
        
        // Default to method name
        return method.getName();
    }

    /**
     * Checks if the controller belongs to the MCP library itself.
     *
     * @param controllerClass the controller class
     * @return true if it's a library controller
     */
    private boolean isLibraryController(Class<?> controllerClass) {
        String packageName = controllerClass.getPackage().getName();
        return packageName.equals("com.shaibachar.springbootmcplib.controller");
    }
}
