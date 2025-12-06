package com.shaibachar.springbootmcplib.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.shaibachar.springbootmcplib.controller.McpController;
import com.shaibachar.springbootmcplib.service.EndpointDiscoveryService;
import com.shaibachar.springbootmcplib.service.McpToolExecutionService;
import com.shaibachar.springbootmcplib.service.McpToolMappingService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

/**
 * Auto-configuration class for Spring Boot MCP Library.
 * This configuration is automatically activated when the library is added as a dependency
 * to a Spring Boot application.
 */
@Configuration
@ConditionalOnWebApplication
public class McpAutoConfiguration {

    private static final Logger logger = LoggerFactory.getLogger(McpAutoConfiguration.class);

    /**
     * Constructor that logs the initialization of the auto-configuration.
     */
    public McpAutoConfiguration() {
        logger.debug("Initializing Spring Boot MCP Library auto-configuration");
    }

    /**
     * Creates the endpoint discovery service bean.
     *
     * @param handlerMapping the Spring request mapping handler
     * @return the endpoint discovery service
     */
    @Bean
    @ConditionalOnMissingBean
    public EndpointDiscoveryService endpointDiscoveryService(RequestMappingHandlerMapping handlerMapping) {
        logger.debug("Creating EndpointDiscoveryService bean");
        return new EndpointDiscoveryService(handlerMapping);
    }

    /**
     * Creates the MCP tool mapping service bean.
     *
     * @param discoveryService the endpoint discovery service
     * @return the tool mapping service
     */
    @Bean
    @ConditionalOnMissingBean
    public McpToolMappingService mcpToolMappingService(EndpointDiscoveryService discoveryService) {
        logger.debug("Creating McpToolMappingService bean");
        return new McpToolMappingService(discoveryService);
    }

    /**
     * Creates the MCP tool execution service bean.
     *
     * @param discoveryService the endpoint discovery service
     * @param applicationContext the Spring application context
     * @param objectMapper the Jackson object mapper
     * @return the tool execution service
     */
    @Bean
    @ConditionalOnMissingBean
    public McpToolExecutionService mcpToolExecutionService(EndpointDiscoveryService discoveryService,
                                                           ApplicationContext applicationContext,
                                                           ObjectMapper objectMapper) {
        logger.debug("Creating McpToolExecutionService bean");
        return new McpToolExecutionService(discoveryService, applicationContext, objectMapper);
    }

    /**
     * Creates the MCP controller bean.
     *
     * @param toolMappingService the tool mapping service
     * @param toolExecutionService the tool execution service
     * @return the MCP controller
     */
    @Bean
    @ConditionalOnMissingBean
    public McpController mcpController(McpToolMappingService toolMappingService,
                                      McpToolExecutionService toolExecutionService) {
        logger.debug("Creating McpController bean");
        return new McpController(toolMappingService, toolExecutionService);
    }
}
