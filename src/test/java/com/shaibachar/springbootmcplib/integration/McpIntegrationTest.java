package com.shaibachar.springbootmcplib.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.shaibachar.springbootmcplib.model.*;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration tests for the MCP library.
 * Tests the full flow from endpoint discovery to tool execution.
 */
@SpringBootTest(classes = TestApplication.class)
@AutoConfigureMockMvc
class McpIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void testListToolsEndpoint() throws Exception {
        MvcResult result = mockMvc.perform(get("/mcp/tools"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andReturn();

        String content = result.getResponse().getContentAsString();
        System.out.println("List tools response: " + content);
        McpToolsResponse response = objectMapper.readValue(content, McpToolsResponse.class);

        assertNotNull(response);
        assertNotNull(response.getTools());
        
        // Print all discovered tools for debugging
        System.out.println("Discovered " + response.getTools().size() + " tools:");
        response.getTools().forEach(tool -> System.out.println("  - " + tool.getName()));
        
        assertTrue(response.getTools().size() > 0, "Should discover at least one tool");

        // Check that test controller endpoints are discovered
        boolean hasHelloEndpoint = response.getTools().stream()
                .anyMatch(tool -> tool.getName().contains("hello"));
        assertTrue(hasHelloEndpoint, "Should discover /api/hello endpoint");
    }

    @Test
    void testExecuteSimpleGetTool() throws Exception {
        // First, get the list of tools to find the exact tool name
        MvcResult listResult = mockMvc.perform(get("/mcp/tools"))
                .andExpect(status().isOk())
                .andReturn();

        String listContent = listResult.getResponse().getContentAsString();
        McpToolsResponse toolsResponse = objectMapper.readValue(listContent, McpToolsResponse.class);

        // Find the hello endpoint tool
        McpTool helloTool = toolsResponse.getTools().stream()
                .filter(tool -> tool.getName().contains("hello"))
                .findFirst()
                .orElseThrow(() -> new AssertionError("Hello tool not found"));

        // Execute the tool
        McpToolExecutionRequest request = new McpToolExecutionRequest(
                helloTool.getName(),
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
        assertTrue(response.getContent().get(0).getText().contains("Hello"));
    }

    @Test
    void testExecuteToolWithPathVariable() throws Exception {
        // Get tools
        MvcResult listResult = mockMvc.perform(get("/mcp/tools"))
                .andExpect(status().isOk())
                .andReturn();

        String listContent = listResult.getResponse().getContentAsString();
        McpToolsResponse toolsResponse = objectMapper.readValue(listContent, McpToolsResponse.class);

        // Find the getUser endpoint tool
        McpTool getUserTool = toolsResponse.getTools().stream()
                .filter(tool -> tool.getName().contains("getUser") || tool.getName().contains("users_id"))
                .findFirst()
                .orElseThrow(() -> new AssertionError("GetUser tool not found"));

        // Execute with path variable
        Map<String, Object> args = new HashMap<>();
        args.put("id", "123");

        McpToolExecutionRequest request = new McpToolExecutionRequest(
                getUserTool.getName(),
                args
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
        assertTrue(response.getContent().get(0).getText().contains("123"));
    }

    @Test
    void testExecuteToolWithRequestParam() throws Exception {
        // Get tools
        MvcResult listResult = mockMvc.perform(get("/mcp/tools"))
                .andExpect(status().isOk())
                .andReturn();

        String listContent = listResult.getResponse().getContentAsString();
        McpToolsResponse toolsResponse = objectMapper.readValue(listContent, McpToolsResponse.class);

        // Find the greet endpoint tool
        McpTool greetTool = toolsResponse.getTools().stream()
                .filter(tool -> tool.getName().contains("greet"))
                .findFirst()
                .orElseThrow(() -> new AssertionError("Greet tool not found"));

        // Execute with request param
        Map<String, Object> args = new HashMap<>();
        args.put("name", "John");

        McpToolExecutionRequest request = new McpToolExecutionRequest(
                greetTool.getName(),
                args
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
        assertTrue(response.getContent().get(0).getText().contains("John"));
    }

    @Test
    void testExecuteNonExistentTool() throws Exception {
        McpToolExecutionRequest request = new McpToolExecutionRequest(
                "nonexistent_tool",
                new HashMap<>()
        );

        MvcResult result = mockMvc.perform(post("/mcp/tools/execute")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andReturn();

        String content = result.getResponse().getContentAsString();
        McpToolExecutionResponse response = objectMapper.readValue(content, McpToolExecutionResponse.class);

        assertNotNull(response);
        assertTrue(response.getIsError());
    }

    @Test
    void testRefreshToolsEndpoint() throws Exception {
        mockMvc.perform(post("/mcp/tools/refresh"))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("refreshed")));
    }
}
