package com.shaibachar.springbootmcplib.service;

import com.shaibachar.springbootmcplib.model.EndpointMetadata;
import com.shaibachar.springbootmcplib.model.GraphQLEndpointMetadata;
import com.shaibachar.springbootmcplib.model.McpTool;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;

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

    @Mock
    private GraphQLDiscoveryService graphQLDiscoveryService;

    private McpToolMappingService mappingService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        mappingService = new McpToolMappingService(discoveryService, graphQLDiscoveryService);
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
        when(graphQLDiscoveryService.discoverGraphQLEndpoints()).thenReturn(Collections.emptyList());

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
        when(graphQLDiscoveryService.discoverGraphQLEndpoints()).thenReturn(Collections.emptyList());

        // Call twice
        mappingService.getAllTools();
        mappingService.getAllTools();

        // discoveryService should only be called once (caching)
        verify(discoveryService, times(1)).discoverEndpoints();
        verify(graphQLDiscoveryService, times(1)).discoverGraphQLEndpoints();
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
        when(graphQLDiscoveryService.discoverGraphQLEndpoints()).thenReturn(Collections.emptyList());

        mappingService.getAllTools();
        mappingService.refreshTools();

        // After refresh, discoveryService should be called again
        verify(discoveryService, times(2)).discoverEndpoints();
        verify(graphQLDiscoveryService, times(2)).discoverGraphQLEndpoints();
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
        when(graphQLDiscoveryService.discoverGraphQLEndpoints()).thenReturn(Collections.emptyList());

        List<McpTool> tools = mappingService.getAllTools();
        String toolName = tools.get(0).getName();

        McpTool found = mappingService.findToolByName(toolName);

        assertNotNull(found);
        assertEquals(toolName, found.getName());
    }

    @Test
    void testFindToolByNameNotFound() {
        when(discoveryService.discoverEndpoints()).thenReturn(Collections.emptyList());
        when(graphQLDiscoveryService.discoverGraphQLEndpoints()).thenReturn(Collections.emptyList());

        McpTool found = mappingService.findToolByName("nonexistent");

        assertNull(found);
    }

    @Test
    void testGetAllTools_WithGraphQL() throws NoSuchMethodException {
        Method restMethod = TestController.class.getMethod("testMethod");
        EndpointMetadata restEndpoint = new EndpointMetadata(
                "/test",
                RequestMethod.GET,
                TestController.class,
                restMethod,
                ""
        );

        Method graphqlMethod = TestGraphQLController.class.getMethod("createUser", String.class);
        GraphQLEndpointMetadata graphqlEndpoint = new GraphQLEndpointMetadata(
                "createUser",
                GraphQLEndpointMetadata.OperationType.MUTATION,
                TestGraphQLController.class,
                graphqlMethod
        );

        when(discoveryService.discoverEndpoints()).thenReturn(Collections.singletonList(restEndpoint));
        when(graphQLDiscoveryService.discoverGraphQLEndpoints()).thenReturn(Collections.singletonList(graphqlEndpoint));

        List<McpTool> tools = mappingService.getAllTools();

        assertNotNull(tools);
        assertEquals(2, tools.size());

        // Verify REST tool
        McpTool restTool = tools.stream()
                .filter(t -> t.getName().contains("get"))
                .findFirst()
                .orElse(null);
        assertNotNull(restTool);

        // Verify GraphQL tool
        McpTool graphqlTool = tools.stream()
                .filter(t -> t.getName().contains("graphql"))
                .findFirst()
                .orElse(null);
        assertNotNull(graphqlTool);
        assertTrue(graphqlTool.getName().contains("mutation"));
    }

    @Test
    void testToolWithParameters() throws NoSuchMethodException {
        Method method = TestControllerWithParams.class.getMethod("getById", Long.class, String.class);
        EndpointMetadata endpoint = new EndpointMetadata(
                "/test/{id}",
                RequestMethod.GET,
                TestControllerWithParams.class,
                method,
                ""
        );

        when(discoveryService.discoverEndpoints()).thenReturn(Collections.singletonList(endpoint));
        when(graphQLDiscoveryService.discoverGraphQLEndpoints()).thenReturn(Collections.emptyList());

        List<McpTool> tools = mappingService.getAllTools();

        assertNotNull(tools);
        assertEquals(1, tools.size());

        McpTool tool = tools.get(0);
        Map<String, Object> schema = tool.getInputSchema();
        assertNotNull(schema);
        
        @SuppressWarnings("unchecked")
        Map<String, Object> properties = (Map<String, Object>) schema.get("properties");
        assertNotNull(properties);
        assertTrue(properties.size() > 0, "Should have parameter properties");
    }

    @Test
    void testFindToolByName_PartialMatch() throws NoSuchMethodException {
        Method method = TestController.class.getMethod("testMethod");
        EndpointMetadata endpoint = new EndpointMetadata(
                "/test",
                RequestMethod.GET,
                TestController.class,
                method,
                ""
        );

        when(discoveryService.discoverEndpoints()).thenReturn(Collections.singletonList(endpoint));
        when(graphQLDiscoveryService.discoverGraphQLEndpoints()).thenReturn(Collections.emptyList());

        List<McpTool> tools = mappingService.getAllTools();
        String fullName = tools.get(0).getName();
        
        // Test partial match
        String partialName = fullName.substring(0, Math.min(5, fullName.length()));
        McpTool found = mappingService.findToolByName(partialName);

        assertNotNull(found, "Should find tool by partial name match");
    }

    @Test
    void testFindToolByName_NullQuery() {
        when(discoveryService.discoverEndpoints()).thenReturn(Collections.emptyList());
        when(graphQLDiscoveryService.discoverGraphQLEndpoints()).thenReturn(Collections.emptyList());

        McpTool found = mappingService.findToolByName(null);

        assertNull(found);
    }

    @Test
    void testToolWithPostRequest() throws NoSuchMethodException {
        Method method = TestControllerWithParams.class.getMethod("createItem", String.class);
        EndpointMetadata endpoint = new EndpointMetadata(
                "/items",
                RequestMethod.POST,
                TestControllerWithParams.class,
                method,
                ""
        );

        when(discoveryService.discoverEndpoints()).thenReturn(Collections.singletonList(endpoint));
        when(graphQLDiscoveryService.discoverGraphQLEndpoints()).thenReturn(Collections.emptyList());

        List<McpTool> tools = mappingService.getAllTools();

        assertNotNull(tools);
        assertEquals(1, tools.size());
        
        McpTool tool = tools.get(0);
        assertTrue(tool.getName().contains("post"), "Tool name should indicate POST method");
        assertTrue(tool.getDescription().contains("POST"), "Description should mention POST");
    }

    // Test controller class
    static class TestController {
        public String testMethod() {
            return "test";
        }
    }

    // Test controller with parameters
    static class TestControllerWithParams {
        public String getById(@PathVariable Long id, @RequestParam String query) {
            return "test";
        }

        public String createItem(@RequestBody String data) {
            return "created";
        }
    }

    // Test GraphQL controller
    static class TestGraphQLController {
        public String createUser(@Argument String name) {
            return "user created";
        }
    }
}
