package com.aether.foundation.tools;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Map;

/** 结构化工具结果 — Success / Error 统一返回类型。 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public sealed interface StructuredToolResult<T>
    permits StructuredToolResult.Success,
            StructuredToolResult.Error {

    record Success<T>(
        @JsonProperty("ok") boolean ok,
        String summary,
        T data
    ) implements StructuredToolResult<T> {
        public Success {
            ok = true;
        }

        public Success(String summary, T data) {
            this(true, summary, data);
        }
    }

    record Error(
        @JsonProperty("ok") boolean ok,
        String summary,
        String error,
        String code,
        Map<String, Object> details
    ) implements StructuredToolResult<Void> {
        public Error {
            ok = false;
        }

        public Error(String summary, String error, String code, Map<String, Object> details) {
            this(false, summary, error, code, details);
        }

        public Error(String summary, String error) {
            this(false, summary, error, null, null);
        }
    }
}