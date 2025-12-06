package com.shaibachar.springbootmcplib.model;

import org.junit.jupiter.api.Test;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for McpTool model.
 */
class McpToolTest {

    @Test
    void testConstructorAndGetters() {
        Map<String, Object> schema = new HashMap<>();
        schema.put("type", "object");

        McpTool tool = new McpTool("testTool", "Test description", schema);

        assertEquals("testTool", tool.getName());
        assertEquals("Test description", tool.getDescription());
        assertEquals(schema, tool.getInputSchema());
    }

    @Test
    void testSetters() {
        McpTool tool = new McpTool();

        tool.setName("newTool");
        tool.setDescription("New description");

        Map<String, Object> schema = new HashMap<>();
        schema.put("type", "string");
        tool.setInputSchema(schema);

        assertEquals("newTool", tool.getName());
        assertEquals("New description", tool.getDescription());
        assertEquals(schema, tool.getInputSchema());
    }

    @Test
    void testToString() {
        McpTool tool = new McpTool("testTool", "Test description", new HashMap<>());
        String toString = tool.toString();

        assertTrue(toString.contains("testTool"));
        assertTrue(toString.contains("Test description"));
    }
}
