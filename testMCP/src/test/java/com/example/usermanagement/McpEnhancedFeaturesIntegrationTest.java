package com.example.usermanagement;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Integration tests for MCP enhanced features as per spec:
 * - Structured error payloads with requestId, errorCode, and details
 * - Enhanced metadata with javaType, graphqlType, and nullable fields
 * - Discovery caching with per-endpoint TTL
 */
@SpringBootTest
@AutoConfigureMockMvc
public class McpEnhancedFeaturesIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    /**
     * Test: Structured Error Payloads - validation_error
     * Spec: feature_structured_errors.md
     * Expected: HTTP 400, errorCode=validation_error, requestId present
     */
    @Test
    public void testValidationError_BlankToolName() throws Exception {
        Map<String, Object> request = new HashMap<>();
        request.put("name", "");  // Blank tool name
        request.put("arguments", new HashMap<>());

        MvcResult result = mockMvc.perform(post("/mcp/tools/execute")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("validation_error"))
                .andExpect(jsonPath("$.requestId").exists())
                .andExpect(jsonPath("$.requestId").isNotEmpty())
                .andExpect(jsonPath("$.isError").value(true))
                .andExpect(jsonPath("$.content").isArray())
                .andReturn();

        String responseBody = result.getResponse().getContentAsString();
        JsonNode response = objectMapper.readTree(responseBody);
        
        assertThat(response.has("requestId")).isTrue();
        assertThat(response.get("requestId").asText()).isNotEmpty();
        assertThat(response.get("errorCode").asText()).isEqualTo("validation_error");
    }

    /**
     * Test: Structured Error Payloads - tool_not_found
     * Spec: feature_structured_errors.md
     * Expected: errorCode=tool_not_found, requestId present
     */
    @Test
    public void testToolNotFoundError() throws Exception {
        Map<String, Object> request = new HashMap<>();
        request.put("name", "nonexistent_tool_xyz");
        request.put("arguments", new HashMap<>());

        MvcResult result = mockMvc.perform(post("/mcp/tools/execute")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("tool_not_found"))
                .andExpect(jsonPath("$.requestId").exists())
                .andExpect(jsonPath("$.requestId").isNotEmpty())
                .andExpect(jsonPath("$.isError").value(true))
                .andReturn();

        String responseBody = result.getResponse().getContentAsString();
        JsonNode response = objectMapper.readTree(responseBody);
        
        assertThat(response.get("errorCode").asText()).isEqualTo("tool_not_found");
        assertThat(response.has("requestId")).isTrue();
    }

    /**
     * Test: Successful execution includes requestId
     * Spec: feature_structured_errors.md
     * Expected: requestId present in successful responses
     */
    @Test
    public void testSuccessfulExecution_IncludesRequestId() throws Exception {
        Map<String, Object> request = new HashMap<>();
        request.put("name", "GET_api_users");
        request.put("arguments", new HashMap<>());

        MvcResult result = mockMvc.perform(post("/mcp/tools/execute")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.requestId").exists())
                .andExpect(jsonPath("$.requestId").isNotEmpty())
                .andExpect(jsonPath("$.isError").doesNotExist())
                .andReturn();

        String responseBody = result.getResponse().getContentAsString();
        JsonNode response = objectMapper.readTree(responseBody);
        
        assertThat(response.has("requestId")).isTrue();
        assertThat(response.get("requestId").asText()).isNotEmpty();
    }

    /**
     * Test: Enhanced Metadata - javaType in REST parameters
     * Spec: feature_graphql_metadata.md
     * Expected: All REST parameters include javaType field
     */
    @Test
    public void testRestParameterMetadata_IncludesJavaType() throws Exception {
        MvcResult result = mockMvc.perform(get("/mcp/tools"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tools").isArray())
                .andReturn();

        String responseBody = result.getResponse().getContentAsString();
        JsonNode response = objectMapper.readTree(responseBody);
        JsonNode tools = response.get("tools");

        // Find a REST endpoint with parameters (e.g., searchUsers)
        JsonNode searchTool = null;
        for (JsonNode tool : tools) {
            if (tool.get("name").asText().contains("search")) {
                searchTool = tool;
                break;
            }
        }

        assertThat(searchTool).isNotNull();
        JsonNode inputSchema = searchTool.get("inputSchema");
        JsonNode properties = inputSchema.get("properties");

        // Verify that all parameters have javaType
        properties.fields().forEachRemaining(entry -> {
            JsonNode paramSchema = entry.getValue();
            assertThat(paramSchema.has("javaType"))
                    .withFailMessage("Parameter %s missing javaType", entry.getKey())
                    .isTrue();
            assertThat(paramSchema.get("javaType").asText()).isNotEmpty();
        });
    }

    /**
     * Test: Enhanced Metadata - nullable field in REST parameters
     * Spec: feature_graphql_metadata.md
     * Expected: All parameters include nullable field (inverse of required)
     */
    @Test
    public void testRestParameterMetadata_IncludesNullable() throws Exception {
        MvcResult result = mockMvc.perform(get("/mcp/tools"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tools").isArray())
                .andReturn();

        String responseBody = result.getResponse().getContentAsString();
        JsonNode response = objectMapper.readTree(responseBody);
        JsonNode tools = response.get("tools");

        // Find the search endpoint with optional parameters
        JsonNode searchTool = null;
        for (JsonNode tool : tools) {
            String toolName = tool.get("name").asText();
            if (toolName.contains("api_users_search") || toolName.contains("searchUsers")) {
                searchTool = tool;
                break;
            }
        }

        if (searchTool != null) {
            JsonNode properties = searchTool.get("inputSchema").get("properties");
            JsonNode requiredList = searchTool.get("inputSchema").get("required");

            // Verify nullable field exists for all parameters
            properties.fields().forEachRemaining(entry -> {
                JsonNode paramSchema = entry.getValue();
                assertThat(paramSchema.has("nullable"))
                        .withFailMessage("Parameter %s missing nullable field", entry.getKey())
                        .isTrue();

                // Verify nullable is inverse of required
                boolean isRequired = requiredList != null && 
                        containsString(requiredList, entry.getKey());
                boolean isNullable = paramSchema.get("nullable").asBoolean();
                
                assertThat(isNullable).isEqualTo(!isRequired);
            });
        }
    }

    /**
     * Test: Enhanced Metadata - GraphQL metadata (graphqlType, javaType, nullable)
     * Spec: feature_graphql_metadata.md
     * Expected: GraphQL tools include graphqlType, javaType, and nullable for all arguments
     */
    @Test
    public void testGraphQLParameterMetadata_EnhancedFields() throws Exception {
        MvcResult result = mockMvc.perform(get("/mcp/tools"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tools").isArray())
                .andReturn();

        String responseBody = result.getResponse().getContentAsString();
        JsonNode response = objectMapper.readTree(responseBody);
        JsonNode tools = response.get("tools");

        // Find a GraphQL tool
        JsonNode graphqlTool = null;
        for (JsonNode tool : tools) {
            if (tool.get("name").asText().startsWith("graphql_")) {
                graphqlTool = tool;
                break;
            }
        }

        if (graphqlTool != null) {
            JsonNode properties = graphqlTool.get("inputSchema").get("properties");

            // Verify all GraphQL parameters have enhanced metadata
            properties.fields().forEachRemaining(entry -> {
                JsonNode paramSchema = entry.getValue();
                
                assertThat(paramSchema.has("graphqlType"))
                        .withFailMessage("GraphQL parameter %s missing graphqlType", entry.getKey())
                        .isTrue();
                assertThat(paramSchema.has("javaType"))
                        .withFailMessage("GraphQL parameter %s missing javaType", entry.getKey())
                        .isTrue();
                assertThat(paramSchema.has("nullable"))
                        .withFailMessage("GraphQL parameter %s missing nullable", entry.getKey())
                        .isTrue();

                assertThat(paramSchema.get("graphqlType").asText()).isNotEmpty();
                assertThat(paramSchema.get("javaType").asText()).isNotEmpty();
            });
        }
    }

    /**
     * Test: Discovery Caching - cache refresh endpoint
     * Spec: feature_discovery_caching.md
     * Expected: /mcp/tools/refresh clears cache and returns success
     */
    @Test
    public void testCacheRefresh_Success() throws Exception {
        mockMvc.perform(post("/mcp/tools/refresh"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").value("Tools refreshed successfully"));
    }

    /**
     * Test: Discovery Caching - tools list is consistent
     * Spec: feature_discovery_caching.md
     * Expected: Multiple calls within TTL return same tool count
     */
    @Test
    public void testCaching_ConsistentResults() throws Exception {
        // First call
        MvcResult result1 = mockMvc.perform(get("/mcp/tools"))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode response1 = objectMapper.readTree(result1.getResponse().getContentAsString());
        int toolCount1 = response1.get("tools").size();

        // Second call (should use cache)
        MvcResult result2 = mockMvc.perform(get("/mcp/tools"))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode response2 = objectMapper.readTree(result2.getResponse().getContentAsString());
        int toolCount2 = response2.get("tools").size();

        // Tool count should be consistent
        assertThat(toolCount1).isEqualTo(toolCount2);
        assertThat(toolCount1).isGreaterThan(0);
    }

    /**
     * Test: Verify batch operation endpoint metadata
     * Tests various primitive types (Long, Integer, Double, Boolean)
     */
    @Test
    public void testBatchOperationMetadata_VariousTypes() throws Exception {
        MvcResult result = mockMvc.perform(get("/mcp/tools"))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode response = objectMapper.readTree(result.getResponse().getContentAsString());
        JsonNode tools = response.get("tools");

        // Find batch operation tool
        JsonNode batchTool = null;
        for (JsonNode tool : tools) {
            if (tool.get("name").asText().contains("batch")) {
                batchTool = tool;
                break;
            }
        }

        if (batchTool != null) {
            JsonNode properties = batchTool.get("inputSchema").get("properties");

            // Verify userId (Long) has correct type
            if (properties.has("userId")) {
                JsonNode userId = properties.get("userId");
                assertThat(userId.get("type").asText()).isEqualTo("integer");
                assertThat(userId.get("javaType").asText()).contains("Long");
            }

            // Verify amount (Double) has correct type
            if (properties.has("amount")) {
                JsonNode amount = properties.get("amount");
                assertThat(amount.get("type").asText()).isEqualTo("number");
                assertThat(amount.get("javaType").asText()).contains("Double");
            }

            // Verify verify (Boolean) has correct type
            if (properties.has("verify")) {
                JsonNode verify = properties.get("verify");
                assertThat(verify.get("type").asText()).isEqualTo("boolean");
                assertThat(verify.get("javaType").asText()).contains("Boolean");
            }
        }
    }

    // Helper method to check if JsonNode array contains a string
    private boolean containsString(JsonNode arrayNode, String value) {
        if (arrayNode == null || !arrayNode.isArray()) {
            return false;
        }
        for (JsonNode element : arrayNode) {
            if (element.asText().equals(value)) {
                return true;
            }
        }
        return false;
    }
}
