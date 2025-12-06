package com.example.usermanagement;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.shaibachar.springbootmcplib.model.McpToolExecutionRequest;
import com.shaibachar.springbootmcplib.model.McpToolExecutionResponse;
import com.shaibachar.springbootmcplib.model.McpToolsResponse;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Integration tests for GraphQL endpoints exposed through MCP.
 */
@SpringBootTest
@AutoConfigureMockMvc
class GraphQLMcpIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void testGraphQLEndpointsAreDiscovered() throws Exception {
        MvcResult result = mockMvc.perform(get("/mcp/tools"))
                .andExpect(status().isOk())
                .andReturn();

        String content = result.getResponse().getContentAsString();
        McpToolsResponse response = objectMapper.readValue(content, McpToolsResponse.class);

        assertNotNull(response);
        assertNotNull(response.getTools());

        // Check that GraphQL endpoints are discovered
        boolean hasGraphQLQueryTool = response.getTools().stream()
                .anyMatch(tool -> tool.getName().startsWith("graphql_query_"));
        assertTrue(hasGraphQLQueryTool, "Should discover GraphQL query tools");

        boolean hasGraphQLMutationTool = response.getTools().stream()
                .anyMatch(tool -> tool.getName().startsWith("graphql_mutation_"));
        assertTrue(hasGraphQLMutationTool, "Should discover GraphQL mutation tools");

        // Verify specific tools exist
        boolean hasGetUserById = response.getTools().stream()
                .anyMatch(tool -> tool.getName().contains("getUserById"));
        assertTrue(hasGetUserById, "Should discover getUserById GraphQL query");

        boolean hasCreateUser = response.getTools().stream()
                .anyMatch(tool -> tool.getName().contains("createUser"));
        assertTrue(hasCreateUser, "Should discover createUser GraphQL mutation");
    }

    @Test
    void testExecuteGraphQLQuery() throws Exception {
        // First, get the list of tools to find the exact tool name
        MvcResult listResult = mockMvc.perform(get("/mcp/tools"))
                .andExpect(status().isOk())
                .andReturn();

        String listContent = listResult.getResponse().getContentAsString();
        McpToolsResponse toolsResponse = objectMapper.readValue(listContent, McpToolsResponse.class);

        // Find the getAllUsers query tool
        String toolName = toolsResponse.getTools().stream()
                .filter(tool -> tool.getName().contains("getAllUsers"))
                .map(tool -> tool.getName())
                .findFirst()
                .orElseThrow(() -> new AssertionError("getAllUsers GraphQL tool not found"));

        // Execute the tool
        McpToolExecutionRequest request = new McpToolExecutionRequest(
                toolName,
                new HashMap<>()
        );

        MvcResult execResult = mockMvc.perform(post("/mcp/tools/execute")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andReturn();

        String execContent = execResult.getResponse().getContentAsString();
        McpToolExecutionResponse response = objectMapper.readValue(execContent, McpToolExecutionResponse.class);

        assertNotNull(response);
        assertFalse(response.getIsError());
        assertNotNull(response.getContent());
        assertTrue(response.getContent().size() > 0);
    }

    @Test
    void testExecuteGraphQLQueryWithArguments() throws Exception {
        // First create a user using REST endpoint
        Map<String, Object> createArgs = new HashMap<>();
        Map<String, Object> userBody = new HashMap<>();
        userBody.put("name", "GraphQL Test User");
        userBody.put("email", "graphql@test.com");
        createArgs.put("body", userBody);

        // Get REST create tool
        MvcResult listResult = mockMvc.perform(get("/mcp/tools"))
                .andExpect(status().isOk())
                .andReturn();

        String listContent = listResult.getResponse().getContentAsString();
        McpToolsResponse toolsResponse = objectMapper.readValue(listContent, McpToolsResponse.class);

        String createToolName = toolsResponse.getTools().stream()
                .filter(tool -> tool.getName().contains("createUser") && !tool.getName().startsWith("graphql_"))
                .map(tool -> tool.getName())
                .findFirst()
                .orElseThrow(() -> new AssertionError("createUser REST tool not found"));

        McpToolExecutionRequest createRequest = new McpToolExecutionRequest(createToolName, createArgs);

        MvcResult createResult = mockMvc.perform(post("/mcp/tools/execute")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createRequest)))
                .andExpect(status().isOk())
                .andReturn();

        String createContent = createResult.getResponse().getContentAsString();
        McpToolExecutionResponse createResponse = objectMapper.readValue(createContent, McpToolExecutionResponse.class);
        
        // Extract user ID from response - the response is a ResponseEntity, so we need to extract the body
        String userJson = createResponse.getContent().get(0).getText();
        Map<String, Object> responseMap = objectMapper.readValue(userJson, Map.class);
        Map<String, Object> userMap = (Map<String, Object>) responseMap.get("body");
        Object userId = userMap.get("id");

        // Now use GraphQL query to get the user by ID
        String getUserByIdToolName = toolsResponse.getTools().stream()
                .filter(tool -> tool.getName().contains("getUserById"))
                .map(tool -> tool.getName())
                .findFirst()
                .orElseThrow(() -> new AssertionError("getUserById GraphQL tool not found"));

        Map<String, Object> queryArgs = new HashMap<>();
        queryArgs.put("id", userId);

        McpToolExecutionRequest queryRequest = new McpToolExecutionRequest(getUserByIdToolName, queryArgs);

        MvcResult queryResult = mockMvc.perform(post("/mcp/tools/execute")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(queryRequest)))
                .andExpect(status().isOk())
                .andReturn();

        String queryContent = queryResult.getResponse().getContentAsString();
        McpToolExecutionResponse queryResponse = objectMapper.readValue(queryContent, McpToolExecutionResponse.class);

        assertNotNull(queryResponse);
        assertFalse(queryResponse.getIsError());
        assertTrue(queryResponse.getContent().get(0).getText().contains("GraphQL Test User"));
    }

    @Test
    void testExecuteGraphQLMutation() throws Exception {
        // Get the list of tools
        MvcResult listResult = mockMvc.perform(get("/mcp/tools"))
                .andExpect(status().isOk())
                .andReturn();

        String listContent = listResult.getResponse().getContentAsString();
        McpToolsResponse toolsResponse = objectMapper.readValue(listContent, McpToolsResponse.class);

        // Find the createUser mutation tool
        String toolName = toolsResponse.getTools().stream()
                .filter(tool -> tool.getName().contains("createUser") && tool.getName().startsWith("graphql_"))
                .map(tool -> tool.getName())
                .findFirst()
                .orElseThrow(() -> new AssertionError("createUser GraphQL mutation tool not found"));

        // Execute the mutation
        Map<String, Object> args = new HashMap<>();
        args.put("name", "GraphQL Mutation User");
        args.put("email", "mutation@test.com");

        McpToolExecutionRequest request = new McpToolExecutionRequest(toolName, args);

        MvcResult execResult = mockMvc.perform(post("/mcp/tools/execute")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andReturn();

        String execContent = execResult.getResponse().getContentAsString();
        McpToolExecutionResponse response = objectMapper.readValue(execContent, McpToolExecutionResponse.class);

        assertNotNull(response);
        assertFalse(response.getIsError());
        assertNotNull(response.getContent());
        assertTrue(response.getContent().get(0).getText().contains("GraphQL Mutation User"));
        assertTrue(response.getContent().get(0).getText().contains("mutation@test.com"));
    }

    @Test
    void testGraphQLSearchQuery() throws Exception {
        // First create a test user
        MvcResult listResult = mockMvc.perform(get("/mcp/tools"))
                .andExpect(status().isOk())
                .andReturn();

        String listContent = listResult.getResponse().getContentAsString();
        McpToolsResponse toolsResponse = objectMapper.readValue(listContent, McpToolsResponse.class);

        // Create user via GraphQL mutation
        String createToolName = toolsResponse.getTools().stream()
                .filter(tool -> tool.getName().contains("createUser") && tool.getName().startsWith("graphql_"))
                .map(tool -> tool.getName())
                .findFirst()
                .orElseThrow(() -> new AssertionError("createUser GraphQL mutation not found"));

        Map<String, Object> createArgs = new HashMap<>();
        createArgs.put("name", "Searchable User");
        createArgs.put("email", "search@test.com");

        McpToolExecutionRequest createRequest = new McpToolExecutionRequest(createToolName, createArgs);
        mockMvc.perform(post("/mcp/tools/execute")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createRequest)))
                .andExpect(status().isOk());

        // Now search for the user
        String searchToolName = toolsResponse.getTools().stream()
                .filter(tool -> tool.getName().contains("searchUsers"))
                .map(tool -> tool.getName())
                .findFirst()
                .orElseThrow(() -> new AssertionError("searchUsers GraphQL query not found"));

        Map<String, Object> searchArgs = new HashMap<>();
        searchArgs.put("name", "Searchable");

        McpToolExecutionRequest searchRequest = new McpToolExecutionRequest(searchToolName, searchArgs);

        MvcResult searchResult = mockMvc.perform(post("/mcp/tools/execute")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(searchRequest)))
                .andExpect(status().isOk())
                .andReturn();

        String searchContent = searchResult.getResponse().getContentAsString();
        McpToolExecutionResponse searchResponse = objectMapper.readValue(searchContent, McpToolExecutionResponse.class);

        assertNotNull(searchResponse);
        assertFalse(searchResponse.getIsError());
        assertTrue(searchResponse.getContent().get(0).getText().contains("Searchable User"));
    }
}
