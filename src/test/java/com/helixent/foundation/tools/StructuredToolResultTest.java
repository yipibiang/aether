package com.helixent.foundation.tools;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class StructuredToolResultTest {

    @Test
    void success_createsWithOkTrue() {
        var result = new StructuredToolResult.Success<>("done", Map.of("key", "value"));
        assertTrue(result.ok());
        assertEquals("done", result.summary());
        assertEquals(Map.of("key", "value"), result.data());
    }

    @Test
    void error_createsWithOkFalse() {
        var result = new StructuredToolResult.Error("failed", "error msg", "ERR", Map.of("detail", 1));
        assertFalse(result.ok());
        assertEquals("failed", result.summary());
        assertEquals("error msg", result.error());
        assertEquals("ERR", result.code());
        assertEquals(Map.of("detail", 1), result.details());
    }

    @Test
    void error_allowsNullCodeAndDetails() {
        var result = new StructuredToolResult.Error("failed", "error msg", null, null);
        assertFalse(result.ok());
        assertEquals("failed", result.summary());
        assertNull(result.code());
        assertNull(result.details());
    }

    @Test
    void structuredToolResult_isSealedInterface() {
        assertTrue(StructuredToolResult.class.isSealed());
    }
}