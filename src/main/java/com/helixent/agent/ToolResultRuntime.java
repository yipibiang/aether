package com.helixent.agent;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.helixent.foundation.tools.StructuredToolResult;

import java.util.Map;

public final class ToolResultRuntime {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private ToolResultRuntime() {}

    public enum ErrorKind {
        INVALID_INPUT, UNSUPPORTED, NOT_FOUND, ENVIRONMENT_MISSING, EXECUTION_FAILED, UNKNOWN
    }

    /**
 * JDK16 record：标准化成功结果。
 */
public record NormalizedSuccess(boolean ok, String summary, Object data, Object raw)
    implements NormalizedToolResult {}

/**
 * JDK16 record：标准化错误结果。
 */
public record NormalizedError(
    boolean ok, String summary, String error, String code,
    Map<String, Object> details, ErrorKind errorKind, Object raw
) implements NormalizedToolResult {}

/**
 * JDK21 密封接口：限制 NormalizedToolResult 只有 Success 和 Error 两种实现。
 */
public sealed interface NormalizedToolResult
    permits NormalizedSuccess, NormalizedError {}

    public static ErrorKind inferToolErrorKind(String code) {
        if (code == null) return ErrorKind.UNKNOWN;
        if (code.startsWith("INVALID_")) return ErrorKind.INVALID_INPUT;
        if (code.endsWith("_NOT_SUPPORTED")) return ErrorKind.UNSUPPORTED;
        if ("RG_NOT_FOUND".equals(code)) return ErrorKind.ENVIRONMENT_MISSING;
        if ("FILE_NOT_FOUND".equals(code) || code.endsWith("_NOT_FOUND")) return ErrorKind.NOT_FOUND;
        if (code.endsWith("_FAILED")) return ErrorKind.EXECUTION_FAILED;
        return ErrorKind.UNKNOWN;
    }

    @SuppressWarnings("unchecked")
    public static NormalizedToolResult normalizeToolResult(Object result) {
        if (result instanceof StructuredToolResult.Success<?> s) {
            return new NormalizedSuccess(true, s.summary(), s.data(), result);
        }
        if (result instanceof StructuredToolResult.Error e) {
            return new NormalizedError(
                false, e.summary(), e.error(), e.code(), e.details(),
                inferToolErrorKind(e.code()), result
            );
        }
        if (result instanceof String s && s.startsWith("Error:")) {
            var error = s.substring(6).trim();
            if (error.isEmpty()) error = "Tool execution failed.";
            return new NormalizedError(false, error, error, null, null, ErrorKind.UNKNOWN, result);
        }
        var summary = stringifyValue(result);
        return new NormalizedSuccess(true, summary, result, result);
    }

    public static String formatToolResultForMessage(String toolName, Object result) {
        var normalized = normalizeToolResult(result);
        var policy = ToolResultPolicy.forTool(toolName);

        if (normalized instanceof NormalizedError e) {
            return stringifyWithinLimit(
                buildErrorPayload(e), policy.maxStringLength(),
                buildFallbackError(e)
            );
        }

        var success = (NormalizedSuccess) normalized;
        if (policy.preferSummaryOnly() || !policy.includeData()) {
            return toJson(new StructuredToolResult.Success<>(truncateSummary(success.summary()), null));
        }

        return stringifyWithinLimit(
            buildSuccessPayload(success), policy.maxStringLength(),
            new StructuredToolResult.Success<>(truncateSummary(success.summary()), null)
        );
    }

    private static StructuredToolResult.Error buildErrorPayload(NormalizedError e) {
        return new StructuredToolResult.Error(e.summary(), e.error(), e.code(), e.details());
    }

    private static StructuredToolResult.Error buildFallbackError(NormalizedError e) {
        return new StructuredToolResult.Error(
            truncateSummary(e.summary()), truncateSummary(e.error()), e.code(), null
        );
    }

    private static StructuredToolResult.Success<Object> buildSuccessPayload(NormalizedSuccess s) {
        return new StructuredToolResult.Success<>(s.summary(), s.data());
    }

    private static String stringifyValue(Object value) {
        if (value == null) return "null";
        if (value instanceof String s) return s;
        try {
            return MAPPER.writeValueAsString(value);
        } catch (Exception e) {
            return "[unserializable object]";
        }
    }

    private static String toJson(Object payload) {
        try {
            return MAPPER.writeValueAsString(payload);
        } catch (Exception e) {
            return "{}";
        }
    }

    private static String stringifyWithinLimit(
        StructuredToolResult<?> payload, Integer maxLength, StructuredToolResult<?> fallback
    ) {
        var serialized = toJson(payload);
        if (maxLength == null || serialized.length() <= maxLength) {
            return serialized;
        }
        var fallbackSerialized = toJson(fallback);
        if (maxLength == null || fallbackSerialized.length() <= maxLength) {
            return fallbackSerialized;
        }
        if (fallback instanceof StructuredToolResult.Success<?> s) {
            var truncated = s.summary().substring(0, Math.max(0, maxLength - 32));
            return toJson(new StructuredToolResult.Success<>(truncated, null));
        }
        var e = (StructuredToolResult.Error) fallback;
        var truncatedSummary = e.summary().substring(0, Math.max(0, maxLength - 64));
        var truncatedError = e.error().substring(0, Math.max(0, maxLength - 64));
        return toJson(new StructuredToolResult.Error(truncatedSummary, truncatedError, e.code(), null));
    }

    private static String truncateSummary(String value) {
        return truncateSummary(value, 500);
    }

    private static String truncateSummary(String value, int maxLength) {
        if (value.length() <= maxLength) return value;
        return value.substring(0, maxLength) + "... [truncated " + (value.length() - maxLength) + " chars]";
    }
}