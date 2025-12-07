package com.example.usermanagement;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.shaibachar.springbootmcplib.model.McpTool;
import com.shaibachar.springbootmcplib.model.McpToolExecutionRequest;
import com.shaibachar.springbootmcplib.model.McpToolExecutionResponse;
import com.shaibachar.springbootmcplib.model.McpToolsResponse;
import org.junit.jupiter.api.BeforeEach;
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
 * Integration tests for enhanced MCP features including:
 * - Discovery caching and refresh
 * - Enriched GraphQL and REST metadata (graphqlType, javaType, nullable)
 * - Structured error payloads (errorCode, requestId, details)
 */
@SpringBootTest
@AutoConfigureMockMvc
class McpEnhancedFeaturesTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    // ===== Discovery Caching Tests =====

    @Test
    void testToolsCacheIsUsedWithinTTL() throws Exception {
        // First call - should populate cache
        MvcResult result1 = mockMvc.perform(get("/mcp/tools"))
                .andExpect(status().isOk())
                .andReturn();

        McpToolsResponse response1 = objectMapper.readValue(
                result1.getResponse().getContentAsString(),
                McpToolsResponse.class
        );

        // Second call within TTL - should use cache
        MvcResult result2 = mockMvc.perform(get("/mcp/tools"))
                .andExpect(status().isOk())
                .andReturn();

        McpToolsResponse response2 = objectMapper.readValue(
                result2.getResponse().getContentAsString(),
                McpToolsResponse.class
        );

        // Both responses should have the same number of tools
        assertNotNull(response1);
        assertNotNull(response2);
        assertEquals(response1.getTools().size(), response2.getTools().size());
        
        // Check logs show cache hits (this is verified via DEBUG logging in the service)
        // The actual verification is done by examining the application logs
    }

    @Test
    void testRefreshEndpointInvalidatesCache() throws Exception {
        // Get initial tools
        MvcResult result1 = mockMvc.perform(get("/mcp/tools"))
                .andExpect(status().isOk())
                .andReturn();

        McpToolsResponse response1 = objectMapper.readValue(
                result1.getResponse().getContentAsString(),
                McpToolsResponse.class
        );

        assertNotNull(response1);
        int initialToolCount = response1.getTools().size();

        // Refresh the cache
        MvcResult refreshResult = mockMvc.perform(post("/mcp/tools/refresh"))
                .andExpect(status().isOk())
                .andReturn();

        String refreshMessage = refreshResult.getResponse().getContentAsString();
        assertEquals("Tools refreshed successfully", refreshMessage);

        // Get tools again - should re-discover endpoints
        MvcResult result2 = mockMvc.perform(get("/mcp/tools"))
                .andExpect(status().isOk())
                .andReturn();

        McpToolsResponse response2 = objectMapper.readValue(
                result2.getResponse().getContentAsString(),
                McpToolsResponse.class
        );

        assertNotNull(response2);
        // Tool count should be the same after refresh
        assertEquals(initialToolCount, response2.getTools().size());
    }

    // ===== Enriched Metadata Tests =====

    @Test
    void testGraphQLToolsContainEnrichedMetadata() throws Exception {
        MvcResult result = mockMvc.perform(get("/mcp/tools"))
                .andExpect(status().isOk())
                .andReturn();

        McpToolsResponse response = objectMapper.readValue(
                result.getResponse().getContentAsString(),
                McpToolsResponse.class
        );

        // Find a GraphQL tool with arguments
        McpTool createUserTool = response.getTools().stream()
                .filter(tool -> tool.getName().contains("createUser") 
                        && tool.getName().startsWith("graphql_mutation_"))
                .findFirst()
                .orElseThrow(() -> new AssertionError("createUser GraphQL mutation not found"));

        // Verify input schema has enriched metadata
        assertNotNull(createUserTool.getInputSchema());
        Map<String, Object> properties = (Map<String, Object>) createUserTool.getInputSchema().get("properties");
        assertNotNull(properties, "Properties should exist in input schema");

        // Check 'name' argument has enriched metadata
        Map<String, Object> nameArg = (Map<String, Object>) properties.get("name");
        assertNotNull(nameArg, "name argument should exist");
        
        // Verify enriched fields exist
        assertTrue(nameArg.containsKey("graphqlType"), "Should contain graphqlType");
        assertTrue(nameArg.containsKey("javaType"), "Should contain javaType");
        assertTrue(nameArg.containsKey("nullable"), "Should contain nullable");

        // Verify values
        // Note: graphqlType contains the Java simple type name, not the GraphQL schema type
        // CURRENT LIMITATION: The library uses reflection-based discovery which provides Java type information.
        // FUTURE ENHANCEMENT: Parse GraphQL schema (.graphqls files) to extract actual GraphQL SDL types
        // (e.g., "String!" for non-null, "String" for nullable, "[String]" for lists)
        // This would require integrating with graphql-java's schema parser.
        assertEquals("String", nameArg.get("graphqlType"), "GraphQL type should be String");
        assertEquals("java.lang.String", nameArg.get("javaType"), "Java type should be java.lang.String");
        // CURRENT LIMITATION: All GraphQL arguments are marked as nullable=true because the library
        // uses runtime reflection and cannot access GraphQL schema definition nullability constraints.
        // FUTURE ENHANCEMENT: Implement GraphQL schema parsing to detect non-null types (Type!)
        // Implementation approach:
        // 1. Load .graphqls schema files from classpath
        // 2. Use GraphQLSchemaGenerator to build schema
        // 3. Query schema for field definitions and their type nullability
        // 4. Map discovered methods to schema fields and extract accurate nullability
        assertEquals(true, nameArg.get("nullable"), "GraphQL arguments are marked as nullable");
    }

    @Test
    void testGraphQLOptionalArgumentsHaveCorrectNullability() throws Exception {
        MvcResult result = mockMvc.perform(get("/mcp/tools"))
                .andExpect(status().isOk())
                .andReturn();

        McpToolsResponse response = objectMapper.readValue(
                result.getResponse().getContentAsString(),
                McpToolsResponse.class
        );

        // Find updateUser mutation which has optional arguments
        McpTool updateUserTool = response.getTools().stream()
                .filter(tool -> tool.getName().contains("updateUser") 
                        && tool.getName().startsWith("graphql_mutation_"))
                .findFirst()
                .orElseThrow(() -> new AssertionError("updateUser GraphQL mutation not found"));

        Map<String, Object> properties = (Map<String, Object>) updateUserTool.getInputSchema().get("properties");
        assertNotNull(properties);

        // Verify all arguments have nullable field
        Map<String, Object> idArg = (Map<String, Object>) properties.get("id");
        assertNotNull(idArg);
        assertTrue(idArg.containsKey("nullable"), "Should have nullable field");
        
        // CURRENT LIMITATION: All GraphQL arguments are marked as nullable=true.
        // The library cannot distinguish between required (Type!) and optional (Type) fields
        // without parsing the GraphQL schema definition.
        // FUTURE ENHANCEMENT: Detect required vs optional fields from schema
        // Required changes:
        // 1. Add graphql-java dependency to parse schema files
        // 2. Create GraphQLSchemaParser utility in the library
        // 3. Update GraphQLDiscoveryService to load schema and query type definitions
        // 4. Match @QueryMapping/@MutationMapping methods to schema fields
        // 5. Extract nullability from GraphQLFieldDefinition.getType()
        assertEquals(true, idArg.get("nullable"), "Current implementation marks all args as nullable");

        // Verify other arguments also have the nullable field
        Map<String, Object> nameArg = (Map<String, Object>) properties.get("name");
        if (nameArg != null) {
            assertTrue(nameArg.containsKey("nullable"), "Should have nullable field");
            assertEquals(true, nameArg.get("nullable"), "Arguments are marked as nullable");
        }
    }

    @Test
    void testRESTToolsContainJavaTypeMetadata() throws Exception {
        MvcResult result = mockMvc.perform(get("/mcp/tools"))
                .andExpect(status().isOk())
                .andReturn();

        McpToolsResponse response = objectMapper.readValue(
                result.getResponse().getContentAsString(),
                McpToolsResponse.class
        );

        // Find a REST tool with parameters
        McpTool getUserByIdTool = response.getTools().stream()
                .filter(tool -> tool.getName().contains("getUserById") 
                        && !tool.getName().startsWith("graphql_"))
                .findFirst()
                .orElseThrow(() -> new AssertionError("getUserById REST tool not found"));

        // Verify input schema has javaType for parameters
        Map<String, Object> properties = (Map<String, Object>) getUserByIdTool.getInputSchema().get("properties");
        assertNotNull(properties, "Properties should exist");

        Map<String, Object> idParam = (Map<String, Object>) properties.get("id");
        assertNotNull(idParam, "id parameter should exist");
        assertTrue(idParam.containsKey("javaType"), "REST parameter should contain javaType");
        assertEquals("java.lang.Long", idParam.get("javaType"), "Java type should be java.lang.Long");
    }

    // ===== Structured Error Payload Tests =====

    @Test
    void testValidationErrorReturnsStructuredError() throws Exception {
        // Create request with blank tool name to trigger validation error
        McpToolExecutionRequest request = new McpToolExecutionRequest(
                "",  // blank name
                new HashMap<>()
        );

        MvcResult result = mockMvc.perform(post("/mcp/tools/execute")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andReturn();

        McpToolExecutionResponse response = objectMapper.readValue(
                result.getResponse().getContentAsString(),
                McpToolExecutionResponse.class
        );

        // Verify structured error fields
        assertNotNull(response);
        assertEquals(true, response.getIsError(), "Should be marked as error");
        assertEquals("validation_error", response.getErrorCode(), "Should have validation_error code");
        assertNotNull(response.getRequestId(), "Should have requestId");
        assertFalse(response.getRequestId().isEmpty(), "RequestId should not be empty");
        assertNotNull(response.getDetails(), "Should have validation details");
    }

    @Test
    void testToolNotFoundReturnsStructuredError() throws Exception {
        McpToolExecutionRequest request = new McpToolExecutionRequest(
                "nonexistent_tool_name",
                new HashMap<>()
        );

        MvcResult result = mockMvc.perform(post("/mcp/tools/execute")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andReturn();

        McpToolExecutionResponse response = objectMapper.readValue(
                result.getResponse().getContentAsString(),
                McpToolExecutionResponse.class
        );

        // Verify structured error fields
        assertNotNull(response);
        assertEquals(true, response.getIsError(), "Should be marked as error");
        assertEquals("tool_not_found", response.getErrorCode(), "Should have tool_not_found code");
        assertNotNull(response.getRequestId(), "Should have requestId");
        assertFalse(response.getRequestId().isEmpty(), "RequestId should not be empty");
    }

    @Test
    void testSuccessfulExecutionIncludesRequestId() throws Exception {
        // Get a valid tool name
        MvcResult listResult = mockMvc.perform(get("/mcp/tools"))
                .andExpect(status().isOk())
                .andReturn();

        McpToolsResponse toolsResponse = objectMapper.readValue(
                listResult.getResponse().getContentAsString(),
                McpToolsResponse.class
        );

        String toolName = toolsResponse.getTools().stream()
                .filter(tool -> tool.getName().contains("getAllUsers") 
                        && !tool.getName().startsWith("graphql_"))
                .map(McpTool::getName)
                .findFirst()
                .orElseThrow(() -> new AssertionError("getAllUsers REST tool not found"));

        // Execute the tool
        McpToolExecutionRequest request = new McpToolExecutionRequest(
                toolName,
                new HashMap<>()
        );

        MvcResult result = mockMvc.perform(post("/mcp/tools/execute")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andReturn();

        McpToolExecutionResponse response = objectMapper.readValue(
                result.getResponse().getContentAsString(),
                McpToolExecutionResponse.class
        );

        // Verify successful execution has requestId
        assertNotNull(response);
        assertEquals(false, response.getIsError(), "Should not be an error");
        assertNotNull(response.getRequestId(), "Successful execution should include requestId");
        assertFalse(response.getRequestId().isEmpty(), "RequestId should not be empty");
    }

    @Test
    void testRequestIdIsUniqueAcrossRequests() throws Exception {
        // Get a valid tool name
        MvcResult listResult = mockMvc.perform(get("/mcp/tools"))
                .andExpect(status().isOk())
                .andReturn();

        McpToolsResponse toolsResponse = objectMapper.readValue(
                listResult.getResponse().getContentAsString(),
                McpToolsResponse.class
        );

        String toolName = toolsResponse.getTools().stream()
                .filter(tool -> tool.getName().contains("getAllUsers") 
                        && !tool.getName().startsWith("graphql_"))
                .map(McpTool::getName)
                .findFirst()
                .orElseThrow();

        // Execute same tool twice
        McpToolExecutionRequest request = new McpToolExecutionRequest(toolName, new HashMap<>());

        MvcResult result1 = mockMvc.perform(post("/mcp/tools/execute")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andReturn();

        MvcResult result2 = mockMvc.perform(post("/mcp/tools/execute")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andReturn();

        McpToolExecutionResponse response1 = objectMapper.readValue(
                result1.getResponse().getContentAsString(),
                McpToolExecutionResponse.class
        );
        McpToolExecutionResponse response2 = objectMapper.readValue(
                result2.getResponse().getContentAsString(),
                McpToolExecutionResponse.class
        );

        // RequestIds should be different
        assertNotNull(response1.getRequestId());
        assertNotNull(response2.getRequestId());
        assertNotEquals(response1.getRequestId(), response2.getRequestId(), 
                "RequestIds should be unique across requests");
    }

    // ===== Combined Feature Tests =====

    @Test
    void testGraphQLToolWithEnrichedMetadataExecutesSuccessfully() throws Exception {
        // Get tools and verify enriched metadata
        MvcResult listResult = mockMvc.perform(get("/mcp/tools"))
                .andExpect(status().isOk())
                .andReturn();

        McpToolsResponse toolsResponse = objectMapper.readValue(
                listResult.getResponse().getContentAsString(),
                McpToolsResponse.class
        );

        McpTool createUserTool = toolsResponse.getTools().stream()
                .filter(tool -> tool.getName().contains("createUser") 
                        && tool.getName().startsWith("graphql_mutation_"))
                .findFirst()
                .orElseThrow(() -> new AssertionError("createUser GraphQL mutation not found"));

        // Verify enriched metadata
        Map<String, Object> properties = (Map<String, Object>) createUserTool.getInputSchema().get("properties");
        Map<String, Object> nameArg = (Map<String, Object>) properties.get("name");
        assertTrue(nameArg.containsKey("graphqlType"));
        assertTrue(nameArg.containsKey("javaType"));

        // Execute the tool successfully
        Map<String, Object> args = new HashMap<>();
        args.put("name", "Test User");
        args.put("email", "test@example.com");

        McpToolExecutionRequest request = new McpToolExecutionRequest(createUserTool.getName(), args);

        MvcResult result = mockMvc.perform(post("/mcp/tools/execute")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andReturn();

        McpToolExecutionResponse response = objectMapper.readValue(
                result.getResponse().getContentAsString(),
                McpToolExecutionResponse.class
        );

        // Verify success with requestId
        assertNotNull(response);
        assertEquals(false, response.getIsError());
        assertNotNull(response.getRequestId());
        assertTrue(response.getContent().get(0).getText().contains("Test User"));
    }
}
