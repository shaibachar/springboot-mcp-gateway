package com.shaibachar.springbootmcplib.service;

import com.shaibachar.springbootmcplib.config.McpProperties;
import com.shaibachar.springbootmcplib.model.CachedEndpoint;
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
import java.util.concurrent.ConcurrentHashMap;

/**
 * Service responsible for discovering GraphQL endpoints from Spring GraphQL controllers.
 * Scans for classes annotated with @Controller and methods annotated with GraphQL annotations.
 */
public class GraphQLDiscoveryService {

    private static final Logger logger = LoggerFactory.getLogger(GraphQLDiscoveryService.class);

    private final ApplicationContext applicationContext;
    private final McpProperties properties;
    private final boolean graphqlAvailable;
    private final Map<String, CachedEndpoint<GraphQLEndpointMetadata>> cachedEndpointsMap = new ConcurrentHashMap<>();

    /**
     * Constructor with dependency injection.
     *
     * @param applicationContext the Spring application context
     * @param properties the MCP properties
     */
    @Autowired
    public GraphQLDiscoveryService(ApplicationContext applicationContext, McpProperties properties) {
        this.applicationContext = applicationContext;
        this.properties = properties;
        this.graphqlAvailable = isGraphQLAvailable();
        if (graphqlAvailable) {
            logger.debug("GraphQL support is available with TTL: {} ms", properties.getCache().getTtlMillis());
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
     * Uses per-endpoint caching with individual TTL tracking.
     * Only re-discovers endpoints whose cache has expired.
     *
     * @return list of GraphQL endpoint metadata
     */
    public List<GraphQLEndpointMetadata> discoverGraphQLEndpoints() {
        logger.debug("Starting GraphQL endpoint discovery with per-endpoint TTL caching");
        long ttlMillis = properties.getCache().getTtlMillis();
        long now = System.currentTimeMillis();

        if (!graphqlAvailable) {
            logger.debug("GraphQL not available, skipping discovery");
            return new ArrayList<>();
        }

        try {
            // Get all beans with @Controller annotation
            Map<String, Object> controllers = applicationContext.getBeansWithAnnotation(
                    org.springframework.stereotype.Controller.class);

            // Collect all current GraphQL endpoints
            Map<String, GraphQLEndpointMetadata> currentEndpoints = new java.util.HashMap<>();
            
            for (Map.Entry<String, Object> entry : controllers.entrySet()) {
                Object controller = entry.getValue();
                Class<?> controllerClass = controller.getClass();

                // Skip library's own controllers
                if (isLibraryController(controllerClass)) {
                    continue;
                }

                // Discover GraphQL methods and collect them
                discoverGraphQLMethodsInController(controllerClass, currentEndpoints, now);
            }

            // Remove stale endpoints from cache (no longer exist)
            cachedEndpointsMap.keySet().retainAll(currentEndpoints.keySet());

            List<GraphQLEndpointMetadata> endpoints = new ArrayList<>();
            int cachedCount = 0;
            int refreshedCount = 0;

            // Process each endpoint
            for (Map.Entry<String, GraphQLEndpointMetadata> entry : currentEndpoints.entrySet()) {
                String endpointKey = entry.getKey();
                GraphQLEndpointMetadata metadata = entry.getValue();

                CachedEndpoint<GraphQLEndpointMetadata> cached = cachedEndpointsMap.get(endpointKey);

                if (cached != null && !cached.isExpired(ttlMillis)) {
                    // Use cached endpoint
                    endpoints.add(cached.getEndpoint());
                    cachedCount++;
                    logger.debug("Using cached GraphQL endpoint: {}", endpointKey);
                } else {
                    // Use newly discovered endpoint
                    endpoints.add(metadata);
                    cachedEndpointsMap.put(endpointKey, new CachedEndpoint<>(metadata, now));
                    refreshedCount++;
                    logger.debug("Refreshed GraphQL endpoint: {} - {}", 
                        metadata.getOperationType(), metadata.getFieldName());
                }
            }

            logger.debug("GraphQL endpoint discovery completed. Total: {}, Cached: {}, Refreshed: {}", 
                endpoints.size(), cachedCount, refreshedCount);
            
            return endpoints;
        } catch (Exception e) {
            logger.error("Error during GraphQL endpoint discovery", e);
            return new ArrayList<>();
        }
    }

    /**
     * Clears cached discovery results.
     */
    public void clearCache() {
        cachedEndpointsMap.clear();
        logger.debug("Cleared GraphQL endpoint cache");
    }

    /**
     * Discovers GraphQL methods in a controller class.
     *
     * @param controllerClass the controller class
     * @param endpointsMap the map to add discovered endpoints to (key is endpoint identifier)
     * @param timestamp the current timestamp for caching
     */
    @SuppressWarnings("unchecked") // Reflection-based annotation access requires unchecked cast
    private void discoverGraphQLMethodsInController(Class<?> controllerClass, 
                                                     Map<String, GraphQLEndpointMetadata> endpointsMap,
                                                     long timestamp) {
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
                    String endpointKey = generateEndpointKey(metadata);
                    endpointsMap.put(endpointKey, metadata);
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
                    String endpointKey = generateEndpointKey(metadata);
                    endpointsMap.put(endpointKey, metadata);
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

    /**
     * Generates a unique key for a GraphQL endpoint to track in the cache.
     *
     * @param metadata the GraphQL endpoint metadata
     * @return the endpoint key
     */
    private String generateEndpointKey(GraphQLEndpointMetadata metadata) {
        return metadata.getOperationType().name() + ":" + 
               metadata.getFieldName() + ":" + 
               metadata.getControllerClass().getName() + ":" + 
               metadata.getHandlerMethod().getName();
    }
}
