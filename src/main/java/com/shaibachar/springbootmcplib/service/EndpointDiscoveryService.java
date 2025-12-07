package com.shaibachar.springbootmcplib.service;

import com.shaibachar.springbootmcplib.config.McpProperties;
import com.shaibachar.springbootmcplib.model.CachedEndpoint;
import com.shaibachar.springbootmcplib.model.EndpointMetadata;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.mvc.method.RequestMappingInfo;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

import java.lang.reflect.Method;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Service responsible for discovering REST endpoints from Spring MVC controllers.
 * Scans the application context for all registered request mappings and builds
 * endpoint metadata.
 * Implements ApplicationListener to ensure discovery happens after context is fully initialized.
 */
public class EndpointDiscoveryService implements ApplicationListener<ContextRefreshedEvent> {

    private static final Logger logger = LoggerFactory.getLogger(EndpointDiscoveryService.class);

    private final RequestMappingHandlerMapping handlerMapping;
    private final McpProperties properties;
    private boolean contextRefreshed = false;
    private final Map<String, CachedEndpoint<EndpointMetadata>> cachedEndpointsMap = new ConcurrentHashMap<>();

    /**
     * Constructor with dependency injection.
     *
     * @param handlerMapping Spring's request mapping handler
     * @param properties the MCP properties
     */
    public EndpointDiscoveryService(RequestMappingHandlerMapping handlerMapping, McpProperties properties) {
        this.handlerMapping = handlerMapping;
        this.properties = properties;
        logger.debug("EndpointDiscoveryService initialized with TTL: {} ms", properties.getCache().getTtlMillis());
    }

    @Override
    public void onApplicationEvent(ContextRefreshedEvent event) {
        contextRefreshed = true;
        logger.debug("Application context refreshed, ready to discover endpoints");
    }

    /**
     * Discovers all REST endpoints in the application.
     * Uses per-endpoint caching with individual TTL tracking.
     * Only re-discovers endpoints whose cache has expired.
     *
     * @return list of endpoint metadata
     */
    public List<EndpointMetadata> discoverEndpoints() {
        logger.debug("Starting endpoint discovery with per-endpoint TTL caching");
        long ttlMillis = properties.getCache().getTtlMillis();
        long now = System.currentTimeMillis();
        
        // Collect all current handler methods from Spring
        Map<RequestMappingInfo, HandlerMethod> handlerMethods = handlerMapping.getHandlerMethods();
        logger.debug("Found {} handler methods", handlerMethods.size());
        
        // Build a map of endpoint keys from current handlers
        Map<String, RequestMappingInfo> currentEndpointKeys = new HashMap<>();
        for (Map.Entry<RequestMappingInfo, HandlerMethod> entry : handlerMethods.entrySet()) {
            RequestMappingInfo mappingInfo = entry.getKey();
            HandlerMethod handlerMethod = entry.getValue();
            
            // Skip MCP library's own endpoints
            if (isLibraryEndpoint(handlerMethod)) {
                continue;
            }
            
            String endpointKey = generateEndpointKey(mappingInfo, handlerMethod);
            if (endpointKey != null) {
                currentEndpointKeys.put(endpointKey, mappingInfo);
            }
        }
        
        // Remove stale endpoints from cache (no longer exist in Spring)
        cachedEndpointsMap.keySet().retainAll(currentEndpointKeys.keySet());
        
        List<EndpointMetadata> endpoints = new ArrayList<>();
        int cachedCount = 0;
        int refreshedCount = 0;
        
        // Process each endpoint
        for (Map.Entry<String, RequestMappingInfo> entry : currentEndpointKeys.entrySet()) {
            String endpointKey = entry.getKey();
            RequestMappingInfo mappingInfo = entry.getValue();
            
            CachedEndpoint<EndpointMetadata> cached = cachedEndpointsMap.get(endpointKey);
            
            if (cached != null && !cached.isExpired(ttlMillis)) {
                // Use cached endpoint
                endpoints.add(cached.getEndpoint());
                cachedCount++;
                logger.debug("Using cached endpoint: {}", endpointKey);
            } else {
                // Rediscover this specific endpoint
                HandlerMethod handlerMethod = handlerMethods.get(mappingInfo);
                EndpointMetadata metadata = createEndpointMetadata(mappingInfo, handlerMethod);
                
                if (metadata != null) {
                    endpoints.add(metadata);
                    cachedEndpointsMap.put(endpointKey, new CachedEndpoint<>(metadata, now));
                    refreshedCount++;
                    logger.debug("Refreshed endpoint: {} {} - {}", 
                        metadata.getHttpMethod(), 
                        metadata.getFullPath(), 
                        metadata.getHandlerMethod().getName());
                }
            }
        }
        
        logger.debug("Endpoint discovery completed. Total: {}, Cached: {}, Refreshed: {}", 
            endpoints.size(), cachedCount, refreshedCount);
        
        return endpoints;
    }

    /**
     * Clears cached discovery results.
     */
    public void clearCache() {
        cachedEndpointsMap.clear();
        logger.debug("Cleared endpoint cache");
    }

    /**
     * Generates a unique key for an endpoint to track in the cache.
     *
     * @param mappingInfo the request mapping information
     * @param handlerMethod the handler method
     * @return the endpoint key or null if invalid
     */
    private String generateEndpointKey(RequestMappingInfo mappingInfo, HandlerMethod handlerMethod) {
        Set<String> patterns = mappingInfo.getPatternValues();
        if (patterns.isEmpty()) {
            return null;
        }
        
        Set<org.springframework.web.bind.annotation.RequestMethod> methods = 
            mappingInfo.getMethodsCondition().getMethods();
        if (methods.isEmpty()) {
            return null;
        }
        
        String path = patterns.iterator().next();
        String httpMethod = methods.iterator().next().name();
        String className = handlerMethod.getBeanType().getName();
        String methodName = handlerMethod.getMethod().getName();
        
        return httpMethod + ":" + path + ":" + className + ":" + methodName;
    }

    /**
     * Creates endpoint metadata from Spring's request mapping info and handler method.
     *
     * @param mappingInfo the request mapping information
     * @param handlerMethod the handler method
     * @return endpoint metadata or null if invalid
     */
    private EndpointMetadata createEndpointMetadata(RequestMappingInfo mappingInfo, HandlerMethod handlerMethod) {
        try {
            // Get path patterns
            Set<String> patterns = mappingInfo.getPatternValues();
            if (patterns.isEmpty()) {
                logger.debug("No path patterns found for method: {}", handlerMethod.getMethod().getName());
                return null;
            }

            String path = patterns.iterator().next();

            // Get HTTP method
            Set<org.springframework.web.bind.annotation.RequestMethod> methods = mappingInfo.getMethodsCondition().getMethods();
            if (methods.isEmpty()) {
                logger.debug("No HTTP methods found for path: {}", path);
                return null;
            }

            org.springframework.web.bind.annotation.RequestMethod httpMethod = methods.iterator().next();

            // Get controller class and method
            Class<?> controllerClass = handlerMethod.getBeanType();
            Method method = handlerMethod.getMethod();

            // Get base path from controller
            String basePath = getControllerBasePath(controllerClass);

            return new EndpointMetadata(path, httpMethod, controllerClass, method, basePath);
        } catch (Exception e) {
            logger.error("Error creating endpoint metadata", e);
            return null;
        }
    }

    /**
     * Extracts the base path from controller-level RequestMapping annotation.
     *
     * @param controllerClass the controller class
     * @return the base path or empty string
     */
    private String getControllerBasePath(Class<?> controllerClass) {
        RequestMapping requestMapping = controllerClass.getAnnotation(RequestMapping.class);
        if (requestMapping != null && requestMapping.value().length > 0) {
            return requestMapping.value()[0];
        }
        return "";
    }

    /**
     * Checks if the handler method belongs to the MCP library itself.
     *
     * @param handlerMethod the handler method
     * @return true if it's a library endpoint
     */
    private boolean isLibraryEndpoint(HandlerMethod handlerMethod) {
        String packageName = handlerMethod.getBeanType().getPackage().getName();
        // Only exclude the MCP controller, not all library classes
        return packageName.equals("com.shaibachar.springbootmcplib.controller");
    }
}
