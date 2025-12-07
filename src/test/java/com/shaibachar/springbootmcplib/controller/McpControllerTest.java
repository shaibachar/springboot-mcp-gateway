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
import org.springframework.validation.BindingResult;

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

    @Mock
    private BindingResult bindingResult;

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
        
        McpToolExecutionResponse.ContentItem content = 
            new McpToolExecutionResponse.ContentItem("text", "{\"tools\":[]}");
        McpToolExecutionResponse executionResponse = 
            new McpToolExecutionResponse(Collections.singletonList(content), false);
        executionResponse.setRequestId("test-request-id");

        when(toolMappingService.getAllTools()).thenReturn(tools);
        when(toolExecutionService.createSuccessResponse(any(), anyString())).thenReturn(executionResponse);

        // Execute
        ResponseEntity<McpToolExecutionResponse> response = controller.listTools();

        // Verify
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertFalse(response.getBody().getIsError());
        assertNotNull(response.getBody().getContent());

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
        executionResponse.setRequestId("test-request-id");

        when(bindingResult.hasErrors()).thenReturn(false);
        when(toolExecutionService.executeTool(anyString(), any(), anyString())).thenReturn(executionResponse);

        // Execute
        ResponseEntity<McpToolExecutionResponse> response = controller.executeTool(request, bindingResult);

        // Verify
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertFalse(response.getBody().getIsError());
        assertNotNull(response.getBody().getRequestId());

        verify(toolExecutionService, times(1)).executeTool(eq("test_tool"), any(), anyString());
    }

    @Test
    void testExecuteToolWithError() {
        // Prepare test data
        McpToolExecutionRequest request = new McpToolExecutionRequest("test_tool", new HashMap<>());
        
        McpToolExecutionResponse.ContentItem content = 
            new McpToolExecutionResponse.ContentItem("text", "Error occurred");
        McpToolExecutionResponse executionResponse = 
            new McpToolExecutionResponse(Collections.singletonList(content), true);
        executionResponse.setRequestId("test-request-id");

        when(bindingResult.hasErrors()).thenReturn(false);
        when(toolExecutionService.executeTool(anyString(), any(), anyString())).thenReturn(executionResponse);

        // Execute
        ResponseEntity<McpToolExecutionResponse> response = controller.executeTool(request, bindingResult);

        // Verify
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertNotNull(response.getBody());
        assertTrue(response.getBody().getIsError());
        assertNotNull(response.getBody().getRequestId());
    }

    @Test
    void testExecuteToolWithEmptyName() {
        // Prepare test data
        McpToolExecutionRequest request = new McpToolExecutionRequest("", new HashMap<>());
        
        McpToolExecutionResponse.ContentItem content = 
            new McpToolExecutionResponse.ContentItem("text", "Validation error");
        McpToolExecutionResponse errorResponse = 
            new McpToolExecutionResponse(Collections.singletonList(content), true);
        errorResponse.setRequestId("test-request-id");
        errorResponse.setErrorCode("validation_error");

        when(bindingResult.hasErrors()).thenReturn(true);
        when(bindingResult.getFieldErrors()).thenReturn(Collections.emptyList());
        when(bindingResult.getGlobalErrors()).thenReturn(Collections.emptyList());
        when(toolExecutionService.createErrorResponse(any(), anyString(), anyString(), any())).thenReturn(errorResponse);

        // Execute
        ResponseEntity<McpToolExecutionResponse> response = controller.executeTool(request, bindingResult);

        // Verify
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertNotNull(response.getBody());
        assertTrue(response.getBody().getIsError());
        assertNotNull(response.getBody().getRequestId());
        assertEquals("validation_error", response.getBody().getErrorCode());

        verify(toolExecutionService, never()).executeTool(anyString(), any(), anyString());
    }

    @Test
    void testRefreshTools() {
        McpToolExecutionResponse.ContentItem content = 
            new McpToolExecutionResponse.ContentItem("text", "{\"message\":\"Tools refreshed\"}");
        McpToolExecutionResponse executionResponse = 
            new McpToolExecutionResponse(Collections.singletonList(content), false);
        executionResponse.setRequestId("test-request-id");

        when(toolExecutionService.createSuccessResponse(any(), anyString())).thenReturn(executionResponse);

        // Execute
        ResponseEntity<McpToolExecutionResponse> response = controller.refreshTools();

        // Verify
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertFalse(response.getBody().getIsError());
        assertNotNull(response.getBody().getContent());

        verify(toolMappingService, times(1)).refreshTools();
    }
}
