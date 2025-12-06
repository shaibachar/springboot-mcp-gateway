package com.shaibachar.springbootmcplib.service;

import com.shaibachar.springbootmcplib.model.EndpointMetadata;
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

    private EndpointDiscoveryService discoveryService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        discoveryService = new EndpointDiscoveryService(handlerMapping);
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
