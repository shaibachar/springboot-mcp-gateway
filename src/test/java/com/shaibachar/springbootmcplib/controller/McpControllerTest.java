package com.shaibachar.springbootmcplib.controller;

import com.shaibachar.springbootmcplib.model.*;
import com.shaibachar.springbootmcplib.service.McpToolExecutionService;
import com.shaibachar.springbootmcplib.service.McpToolMappingService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for McpController.
 */
class McpControllerTest {

    @Mock
    private McpToolMappingService toolMappingService;

    @Mock
    private McpToolExecutionService toolExecutionService;

    private McpController controller;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        controller = new McpController(toolMappingService, toolExecutionService);
    }

    @Test
    void testListTools() {
        // Prepare test data
        McpTool tool = new McpTool("test_tool", "Test description", new HashMap<>());
        List<McpTool> tools = Collections.singletonList(tool);

        when(toolMappingService.getAllTools()).thenReturn(tools);

        // Execute
        ResponseEntity<McpToolsResponse> response = controller.listTools();

        // Verify
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(1, response.getBody().getTools().size());
        assertEquals("test_tool", response.getBody().getTools().get(0).getName());

        verify(toolMappingService, times(1)).getAllTools();
    }

    @Test
    void testExecuteTool() {
        // Prepare test data
        McpToolExecutionRequest request = new McpToolExecutionRequest("test_tool", new HashMap<>());
        
        McpToolExecutionResponse.ContentItem content = 
            new McpToolExecutionResponse.ContentItem("text", "Success");
        McpToolExecutionResponse executionResponse = 
            new McpToolExecutionResponse(Collections.singletonList(content), false);

        when(toolExecutionService.executeTool(anyString(), any())).thenReturn(executionResponse);

        // Execute
        ResponseEntity<McpToolExecutionResponse> response = controller.executeTool(request);

        // Verify
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertFalse(response.getBody().getIsError());

        verify(toolExecutionService, times(1)).executeTool("test_tool", new HashMap<>());
    }

    @Test
    void testExecuteToolWithError() {
        // Prepare test data
        McpToolExecutionRequest request = new McpToolExecutionRequest("test_tool", new HashMap<>());
        
        McpToolExecutionResponse.ContentItem content = 
            new McpToolExecutionResponse.ContentItem("text", "Error occurred");
        McpToolExecutionResponse executionResponse = 
            new McpToolExecutionResponse(Collections.singletonList(content), true);

        when(toolExecutionService.executeTool(anyString(), any())).thenReturn(executionResponse);

        // Execute
        ResponseEntity<McpToolExecutionResponse> response = controller.executeTool(request);

        // Verify
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertNotNull(response.getBody());
        assertTrue(response.getBody().getIsError());
    }

    @Test
    void testExecuteToolWithEmptyName() {
        // Prepare test data
        McpToolExecutionRequest request = new McpToolExecutionRequest("", new HashMap<>());

        // Execute
        ResponseEntity<McpToolExecutionResponse> response = controller.executeTool(request);

        // Verify
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertNotNull(response.getBody());
        assertTrue(response.getBody().getIsError());

        verify(toolExecutionService, never()).executeTool(anyString(), any());
    }

    @Test
    void testRefreshTools() {
        // Execute
        ResponseEntity<String> response = controller.refreshTools();

        // Verify
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertTrue(response.getBody().contains("refreshed"));

        verify(toolMappingService, times(1)).refreshTools();
    }
}
