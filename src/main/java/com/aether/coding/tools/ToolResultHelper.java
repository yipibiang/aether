package com.aether.coding.tools;

import com.aether.foundation.tools.StructuredToolResult;

import java.util.Map;

public final class ToolResultHelper {

    private ToolResultHelper() {}

    public static StructuredToolResult.Success<Map<String, Object>> okToolResult(String message, Map<String, Object> metadata) {
        return new StructuredToolResult.Success<>(message, metadata);
    }

    public static StructuredToolResult.Error errorToolResult(String message, String errorType, Map<String, Object> metadata) {
        return new StructuredToolResult.Error(message, message, errorType, metadata);
    }
}