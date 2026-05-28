package com.aether.tools;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class ToolResultHelperTest {

    @Test
    void okToolResult_returnsStableSuccessShape() {
        var result = ToolResultHelper.okToolResult("done", Map.of("value", 1));

        assertTrue(result.ok());
        assertEquals("done", result.summary());
        assertEquals(Map.of("value", 1), result.data());
    }

    @Test
    void errorToolResult_returnsStableErrorShape() {
        var result = ToolResultHelper.errorToolResult("failed", "ERR_CODE",
            Map.of("path", "/tmp/x"));

        assertFalse(result.ok());
        assertEquals("failed", result.summary());
        assertEquals("failed", result.error());
        assertEquals("ERR_CODE", result.code());
        assertEquals(Map.of("path", "/tmp/x"), result.details());
    }
}