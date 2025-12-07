package com.shaibachar.springbootmcplib.service;

import com.shaibachar.springbootmcplib.config.McpProperties;
import com.shaibachar.springbootmcplib.model.CachedEndpoint;
import com.shaibachar.springbootmcplib.model.GraphQLEndpointMetadata;
import com.shaibachar.springbootmcplib.util.TimeProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import jakarta.annotation.PreDestroy;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Service responsible for discovering GraphQL endpoints from Spring GraphQL controllers.
 * Scans for classes annotated with @Controller and methods annotated with GraphQL annotations.
 * 
 * Thread Safety: This service is thread-safe. Discovery results are cached in a ConcurrentHashMap
 * with per-endpoint TTL tracking. The cache is bounded by the number of GraphQL endpoints and uses
 * TTL-based eviction to prevent memory leaks.
 */
public class GraphQLDiscoveryService {

    private static final Logger logger = LoggerFactory.getLogger(GraphQLDiscoveryService.class);

    private final ApplicationContext applicationContext;
    private final McpProperties properties;
    private final TimeProvider timeProvider;
    private final boolean graphqlAvailable;
    private final Map<String, CachedEndpoint<GraphQLEndpointMetadata>> cachedEndpointsMap = new ConcurrentHashMap<>();

    /**
     * Constructor with dependency injection.
     *
     * @param applicationContext the Spring application context
     * @param properties the MCP properties
     * @param timeProvider the time provider
     */
    @Autowired
    public GraphQLDiscoveryService(ApplicationContext applicationContext, McpProperties properties, TimeProvider timeProvider) {
        this.applicationContext = applicationContext;
        this.properties = properties;
        this.timeProvider = timeProvider;
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
        
        if (!graphqlAvailable) {
            logger.debug("GraphQL not available, skipping discovery");
            return new ArrayList<>();
        }

        try {
            long ttlMillis = properties.getCache().getTtlMillis();
            long now = timeProvider.getCurrentTimeMillis();
            
            // Collect current endpoints from all controllers
            Map<String, GraphQLEndpointMetadata> currentEndpoints = collectCurrentEndpoints();
            
            // Process endpoints with cache-or-refresh logic
            List<GraphQLEndpointMetadata> endpoints = processEndpointsWithCache(currentEndpoints, ttlMillis, now);
            
            return endpoints;
        } catch (Exception e) {
            logger.error("Error during GraphQL endpoint discovery", e);
            return new ArrayList<>();
        }
    }

    /**
     * Collects all current GraphQL endpoints from application controllers.
     * Discovers endpoints from all @Controller beans and filters out library controllers.
     *
     * @return map of endpoint keys to their metadata
     */
    private Map<String, GraphQLEndpointMetadata> collectCurrentEndpoints() {
        Map<String, Object> controllers = applicationContext.getBeansWithAnnotation(
                org.springframework.stereotype.Controller.class);

        Map<String, GraphQLEndpointMetadata> currentEndpoints = new java.util.HashMap<>();
        long now = timeProvider.getCurrentTimeMillis();
        
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
        
        return currentEndpoints;
    }

    /**
     * Processes endpoints using cache-or-refresh logic.
     * Returns cached endpoints if still valid, otherwise uses newly discovered ones.
     *
     * @param currentEndpoints map of discovered endpoint keys to metadata
     * @param ttlMillis cache TTL in milliseconds
     * @param now current timestamp
     * @return list of endpoint metadata (cached or refreshed)
     */
    private List<GraphQLEndpointMetadata> processEndpointsWithCache(
            Map<String, GraphQLEndpointMetadata> currentEndpoints,
            long ttlMillis,
            long now) {
        
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
    }

    /**
     * Clears cached discovery results.
     */
    public void clearCache() {
        cachedEndpointsMap.clear();
        logger.debug("Cleared GraphQL endpoint cache");
    }

    /**
     * Cleanup method called when the bean is destroyed.
     * Ensures resources are released to prevent memory leaks.
     */
    @PreDestroy
    public void destroy() {
        logger.debug("Shutting down GraphQLDiscoveryService");
        clearCache();
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
                Class<? extends java.lang.annotation.Annotation> queryAnnotationClass = (Class<? extends java.lang.annotation.Annotation>) queryMappingClass;
                if (method.isAnnotationPresent(queryAnnotationClass)) {
                    Object queryMapping = method.getAnnotation(queryAnnotationClass);
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
                Class<? extends java.lang.annotation.Annotation> mutationAnnotationClass = (Class<? extends java.lang.annotation.Annotation>) mutationMappingClass;
                if (method.isAnnotationPresent(mutationAnnotationClass)) {
                    Object mutationMapping = method.getAnnotation(mutationAnnotationClass);
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
