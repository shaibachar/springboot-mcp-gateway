package com.shaibachar.springbootmcplib.service;

import com.shaibachar.springbootmcplib.config.McpProperties;
import com.shaibachar.springbootmcplib.model.EndpointMetadata;
import com.shaibachar.springbootmcplib.util.TimeProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for EndpointDiscoveryService.
 */
class EndpointDiscoveryServiceTest {

    @Mock
    private RequestMappingHandlerMapping handlerMapping;

    @Mock
    private TimeProvider timeProvider;

    private EndpointDiscoveryService discoveryService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        McpProperties properties = new McpProperties();
        properties.getCache().setTtlMillis(5 * 60 * 1000); // 5 minutes
        when(timeProvider.getCurrentTimeMillis()).thenReturn(System.currentTimeMillis());
        discoveryService = new EndpointDiscoveryService(handlerMapping, properties, timeProvider);
    }

    @Test
    void testDiscoverEndpointsWithEmptyMapping() {
        when(handlerMapping.getHandlerMethods()).thenReturn(new java.util.HashMap<>());

        List<EndpointMetadata> endpoints = discoveryService.discoverEndpoints();

        assertNotNull(endpoints);
        assertEquals(0, endpoints.size());
    }

    @Test
    void testServiceInitialization() {
        assertNotNull(discoveryService);
    }
}
