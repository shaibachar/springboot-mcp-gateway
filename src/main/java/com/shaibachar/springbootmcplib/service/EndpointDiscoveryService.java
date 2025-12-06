package com.shaibachar.springbootmcplib.service;

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

/**
 * Service responsible for discovering REST endpoints from Spring MVC controllers.
 * Scans the application context for all registered request mappings and builds
 * endpoint metadata.
 * Implements ApplicationListener to ensure discovery happens after context is fully initialized.
 */
@Component
public class EndpointDiscoveryService implements ApplicationListener<ContextRefreshedEvent> {

    private static final Logger logger = LoggerFactory.getLogger(EndpointDiscoveryService.class);

    private final RequestMappingHandlerMapping handlerMapping;
    private boolean contextRefreshed = false;

    /**
     * Constructor with dependency injection.
     *
     * @param handlerMapping Spring's request mapping handler
     */
    public EndpointDiscoveryService(RequestMappingHandlerMapping handlerMapping) {
        this.handlerMapping = handlerMapping;
        logger.debug("EndpointDiscoveryService initialized");
    }

    @Override
    public void onApplicationEvent(ContextRefreshedEvent event) {
        contextRefreshed = true;
        logger.debug("Application context refreshed, ready to discover endpoints");
    }

    /**
     * Discovers all REST endpoints in the application.
     * Excludes MCP library's own endpoints to avoid recursion.
     *
     * @return list of endpoint metadata
     */
    public List<EndpointMetadata> discoverEndpoints() {
        logger.debug("Starting endpoint discovery");
        List<EndpointMetadata> endpoints = new ArrayList<>();

        Map<RequestMappingInfo, HandlerMethod> handlerMethods = handlerMapping.getHandlerMethods();
        logger.debug("Found {} handler methods", handlerMethods.size());

        for (Map.Entry<RequestMappingInfo, HandlerMethod> entry : handlerMethods.entrySet()) {
            RequestMappingInfo mappingInfo = entry.getKey();
            HandlerMethod handlerMethod = entry.getValue();

            // Skip MCP library's own endpoints
            if (isLibraryEndpoint(handlerMethod)) {
                logger.debug("Skipping library endpoint: {}", handlerMethod.getMethod().getName());
                continue;
            }

            EndpointMetadata metadata = createEndpointMetadata(mappingInfo, handlerMethod);
            if (metadata != null) {
                endpoints.add(metadata);
                logger.debug("Discovered endpoint: {} {} - {}", 
                    metadata.getHttpMethod(), 
                    metadata.getFullPath(), 
                    metadata.getHandlerMethod().getName());
            }
        }

        logger.debug("Endpoint discovery completed. Found {} endpoints", endpoints.size());
        return endpoints;
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
