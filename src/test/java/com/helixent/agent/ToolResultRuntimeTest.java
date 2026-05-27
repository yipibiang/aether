package com.helixent.agent;

import com.helixent.foundation.tools.StructuredToolResult;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class ToolResultRuntimeTest {

    @Test
    void inferToolErrorKind_mapsCommonErrorCodeFamilies() {
        assertEquals(ToolResultRuntime.ErrorKind.INVALID_INPUT,
            ToolResultRuntime.inferToolErrorKind("INVALID_PATH"));
        assertEquals(ToolResultRuntime.ErrorKind.UNSUPPORTED,
            ToolResultRuntime.inferToolErrorKind("DELETE_NOT_SUPPORTED"));
        assertEquals(ToolResultRuntime.ErrorKind.NOT_FOUND,
            ToolResultRuntime.inferToolErrorKind("FILE_NOT_FOUND"));
        assertEquals(ToolResultRuntime.ErrorKind.ENVIRONMENT_MISSING,
            ToolResultRuntime.inferToolErrorKind("RG_NOT_FOUND"));
        assertEquals(ToolResultRuntime.ErrorKind.EXECUTION_FAILED,
            ToolResultRuntime.inferToolErrorKind("PATCH_APPLY_FAILED"));
    }

    @Test
    void inferToolErrorKind_returnsUnknownForNull() {
        assertEquals(ToolResultRuntime.ErrorKind.UNKNOWN,
            ToolResultRuntime.inferToolErrorKind(null));
    }

    @Test
    void normalizeToolResult_preservesStructuredSuccess() {
        var result = ToolResultRuntime.normalizeToolResult(
            new StructuredToolResult.Success<>("Read file: /tmp/demo.ts",
                Map.of("path", "/tmp/demo.ts", "content", "const x = 1;"))
        );

        assertInstanceOf(ToolResultRuntime.NormalizedSuccess.class, result);
        var success = (ToolResultRuntime.NormalizedSuccess) result;
        assertTrue(success.ok());
        assertEquals("Read file: /tmp/demo.ts", success.summary());
    }

    @Test
    void normalizeToolResult_preservesStructuredErrorAndInfersErrorKind() {
        var result = ToolResultRuntime.normalizeToolResult(
            new StructuredToolResult.Error("File not found", "File not found",
                "FILE_NOT_FOUND", null)
        );

        assertInstanceOf(ToolResultRuntime.NormalizedError.class, result);
        var error = (ToolResultRuntime.NormalizedError) result;
        assertFalse(error.ok());
        assertEquals("File not found", error.summary());
        assertEquals("FILE_NOT_FOUND", error.code());
        assertEquals(ToolResultRuntime.ErrorKind.NOT_FOUND, error.errorKind());
    }

    @Test
    void normalizeToolResult_normalizesLegacyStringErrors() {
        var result = ToolResultRuntime.normalizeToolResult("Error: something failed");

        assertInstanceOf(ToolResultRuntime.NormalizedError.class, result);
        var error = (ToolResultRuntime.NormalizedError) result;
        assertFalse(error.ok());
        assertEquals("something failed", error.summary());
    }

    @Test
    void normalizeToolResult_normalizesPlainSuccessStrings() {
        var result = ToolResultRuntime.normalizeToolResult("done");

        assertInstanceOf(ToolResultRuntime.NormalizedSuccess.class, result);
        var success = (ToolResultRuntime.NormalizedSuccess) result;
        assertTrue(success.ok());
        assertEquals("done", success.summary());
    }

    @Test
    void formatToolResultForMessage_omitsDataForSummaryFirstTools() throws Exception {
        var formatted = ToolResultRuntime.formatToolResultForMessage("list_files",
            new StructuredToolResult.Success<>("Listed 5 items under /tmp/demo",
                Map.of("entries", java.util.List.of("a", "b"))));

        var tree = new com.fasterxml.jackson.databind.ObjectMapper().readTree(formatted);
        assertTrue(formatted.contains("Listed 5 items"));
        assertFalse(tree.has("data"));
    }

    @Test
    void formatToolResultForMessage_preservesDataForContentCarryingTools() {
        var formatted = ToolResultRuntime.formatToolResultForMessage("read_file",
            new StructuredToolResult.Success<>("Read file: /tmp/demo.ts",
                Map.of("path", "/tmp/demo.ts", "content", "const x = 1;")));

        assertDoesNotThrow(() -> new com.fasterxml.jackson.databind.ObjectMapper().readTree(formatted));
        assertTrue(formatted.contains("Read file: /tmp/demo.ts"));
        assertTrue(formatted.contains("/tmp/demo.ts"));
    }

    @Test
    void formatToolResultForMessage_formatsErrorsWithStableShape() {
        var formatted = ToolResultRuntime.formatToolResultForMessage("grep_search",
            new StructuredToolResult.Error("Failed to run rg", "Failed to run rg",
                "RG_NOT_FOUND", null));

        assertDoesNotThrow(() -> new com.fasterxml.jackson.databind.ObjectMapper().readTree(formatted));
        assertTrue(formatted.contains("Failed to run rg"));
        assertTrue(formatted.contains("RG_NOT_FOUND"));
    }

    @Test
    void formatToolResultForMessage_alwaysReturnsValidJsonWhenPayloadExceedsLimits() {
        var longPatch = "x".repeat(10000);
        var formatted = ToolResultRuntime.formatToolResultForMessage("apply_patch",
            new StructuredToolResult.Success<>("Applied patch",
                Map.of("patch", longPatch)));

        assertDoesNotThrow(() -> new com.fasterxml.jackson.databind.ObjectMapper().readTree(formatted));
        assertTrue(formatted.contains("Applied patch"));
        assertFalse(formatted.contains(longPatch));
    }
}