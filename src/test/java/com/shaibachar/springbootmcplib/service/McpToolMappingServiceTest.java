package com.shaibachar.springbootmcplib.service;

import com.shaibachar.springbootmcplib.model.EndpointMetadata;
import com.shaibachar.springbootmcplib.model.McpTool;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.web.bind.annotation.RequestMethod;

import java.lang.reflect.Method;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for McpToolMappingService.
 */
class McpToolMappingServiceTest {

    @Mock
    private EndpointDiscoveryService discoveryService;

    private McpToolMappingService mappingService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        mappingService = new McpToolMappingService(discoveryService);
    }

    @Test
    void testGetAllTools() throws NoSuchMethodException {
        // Create mock endpoint
        Method method = TestController.class.getMethod("testMethod");
        EndpointMetadata endpoint = new EndpointMetadata(
                "/test",
                RequestMethod.GET,
                TestController.class,
                method,
                "/api"
        );

        when(discoveryService.discoverEndpoints()).thenReturn(Collections.singletonList(endpoint));

        List<McpTool> tools = mappingService.getAllTools();

        assertNotNull(tools);
        assertEquals(1, tools.size());

        McpTool tool = tools.get(0);
        assertNotNull(tool.getName());
        assertNotNull(tool.getDescription());
        assertNotNull(tool.getInputSchema());

        assertTrue(tool.getName().contains("get"));
        assertTrue(tool.getDescription().contains("GET"));
    }

    @Test
    void testToolsAreCached() throws NoSuchMethodException {
        Method method = TestController.class.getMethod("testMethod");
        EndpointMetadata endpoint = new EndpointMetadata(
                "/test",
                RequestMethod.GET,
                TestController.class,
                method,
                ""
        );

        when(discoveryService.discoverEndpoints()).thenReturn(Collections.singletonList(endpoint));

        // Call twice
        mappingService.getAllTools();
        mappingService.getAllTools();

        // discoveryService should only be called once (caching)
        verify(discoveryService, times(1)).discoverEndpoints();
    }

    @Test
    void testRefreshTools() throws NoSuchMethodException {
        Method method = TestController.class.getMethod("testMethod");
        EndpointMetadata endpoint = new EndpointMetadata(
                "/test",
                RequestMethod.GET,
                TestController.class,
                method,
                ""
        );

        when(discoveryService.discoverEndpoints()).thenReturn(Collections.singletonList(endpoint));

        mappingService.getAllTools();
        mappingService.refreshTools();

        // After refresh, discoveryService should be called again
        verify(discoveryService, times(2)).discoverEndpoints();
    }

    @Test
    void testFindToolByName() throws NoSuchMethodException {
        Method method = TestController.class.getMethod("testMethod");
        EndpointMetadata endpoint = new EndpointMetadata(
                "/test",
                RequestMethod.GET,
                TestController.class,
                method,
                ""
        );

        when(discoveryService.discoverEndpoints()).thenReturn(Collections.singletonList(endpoint));

        List<McpTool> tools = mappingService.getAllTools();
        String toolName = tools.get(0).getName();

        McpTool found = mappingService.findToolByName(toolName);

        assertNotNull(found);
        assertEquals(toolName, found.getName());
    }

    @Test
    void testFindToolByNameNotFound() {
        when(discoveryService.discoverEndpoints()).thenReturn(Collections.emptyList());

        McpTool found = mappingService.findToolByName("nonexistent");

        assertNull(found);
    }

    // Test controller class
    static class TestController {
        public String testMethod() {
            return "test";
        }
    }
}
