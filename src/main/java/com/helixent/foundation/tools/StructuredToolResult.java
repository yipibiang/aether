package com.helixent.foundation.tools;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Map;

/**
 * 结构化工具结果 — 统一的工具返回值类型。
 *
 * <h3>JDK21 密封接口 + JDK16 record + 泛型</h3>
 * 密封接口限制只有 Success 和 Error 两种结果类型。
 * record 提供不可变数据载体，泛型参数 T 支持 Success 携带任意类型数据。
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public sealed interface StructuredToolResult<T>
    permits StructuredToolResult.Success,
            StructuredToolResult.Error {

    /**
     * JDK16 record：工具执行成功结果，携带泛型数据。
     */
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

    /**
     * JDK16 record：工具执行错误结果，携带错误码和详情。
     */
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