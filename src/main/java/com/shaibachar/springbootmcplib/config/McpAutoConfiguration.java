package com.shaibachar.springbootmcplib.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.shaibachar.springbootmcplib.controller.McpController;
import com.shaibachar.springbootmcplib.service.EndpointDiscoveryService;
import com.shaibachar.springbootmcplib.service.GraphQLDiscoveryService;
import com.shaibachar.springbootmcplib.service.McpToolExecutionService;
import com.shaibachar.springbootmcplib.service.McpToolMappingService;
import com.shaibachar.springbootmcplib.util.TimeProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
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
@EnableConfigurationProperties(McpProperties.class)
public class McpAutoConfiguration {

    private static final Logger logger = LoggerFactory.getLogger(McpAutoConfiguration.class);

    /**
     * Constructor that logs the initialization of the auto-configuration.
     */
    public McpAutoConfiguration() {
        logger.debug("Initializing Spring Boot MCP Library auto-configuration");
    }

    /**
     * Creates the time provider bean.
     *
     * @return the time provider
     */
    @Bean
    @ConditionalOnMissingBean
    public TimeProvider timeProvider() {
        logger.debug("Creating TimeProvider bean");
        return new TimeProvider();
    }

    /**
     * Creates the endpoint discovery service bean.
     *
     * @param handlerMapping the Spring request mapping handler
     * @param properties the MCP properties
     * @param timeProvider the time provider
     * @return the endpoint discovery service
     */
    @Bean
    @ConditionalOnMissingBean
    public EndpointDiscoveryService endpointDiscoveryService(RequestMappingHandlerMapping handlerMapping,
                                                             McpProperties properties,
                                                             TimeProvider timeProvider) {
        logger.debug("Creating EndpointDiscoveryService bean");
        return new EndpointDiscoveryService(handlerMapping, properties, timeProvider);
    }

    /**
     * Creates the GraphQL discovery service bean.
     *
     * @param applicationContext the Spring application context
     * @param properties the MCP properties
     * @param timeProvider the time provider
     * @return the GraphQL discovery service
     */
    @Bean
    @ConditionalOnMissingBean
    public GraphQLDiscoveryService graphQLDiscoveryService(ApplicationContext applicationContext,
                                                          McpProperties properties,
                                                          TimeProvider timeProvider) {
        logger.debug("Creating GraphQLDiscoveryService bean");
        return new GraphQLDiscoveryService(applicationContext, properties, timeProvider);
    }

    /**
     * Creates the MCP tool mapping service bean.
     *
     * @param discoveryService the endpoint discovery service
     * @param graphQLDiscoveryService the GraphQL discovery service
     * @return the tool mapping service
     */
    @Bean
    @ConditionalOnMissingBean
    public McpToolMappingService mcpToolMappingService(EndpointDiscoveryService discoveryService,
                                                       GraphQLDiscoveryService graphQLDiscoveryService) {
        logger.debug("Creating McpToolMappingService bean");
        return new McpToolMappingService(discoveryService, graphQLDiscoveryService);
    }

    /**
     * Creates the MCP tool execution service bean.
     *
     * @param discoveryService the endpoint discovery service
     * @param graphQLDiscoveryService the GraphQL discovery service
     * @param applicationContext the Spring application context
     * @param objectMapper the Jackson object mapper
     * @return the tool execution service
     */
    @Bean
    @ConditionalOnMissingBean
    public McpToolExecutionService mcpToolExecutionService(EndpointDiscoveryService discoveryService,
                                                           GraphQLDiscoveryService graphQLDiscoveryService,
                                                           ApplicationContext applicationContext,
                                                           ObjectMapper objectMapper) {
        logger.debug("Creating McpToolExecutionService bean");
        return new McpToolExecutionService(discoveryService, graphQLDiscoveryService, applicationContext, objectMapper);
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
