package com.aether.agent;

import com.fasterxml.jackson.databind.ObjectMapper;

public final class ToolResultSummary {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private ToolResultSummary() {}

    public static String summarizeToolResultText(String content) {
        if (content.startsWith("Error:")) {
            return content;
        }
        try {
            var node = MAPPER.readTree(content);
            var ok = node.get("ok");
            var summary = node.get("summary");
            var error = node.get("error");
            var code = node.get("code");

            if (ok != null && ok.asBoolean() && summary != null && summary.isTextual()) {
                return summary.asText();
            }
            if (ok != null && !ok.asBoolean()) {
                var message = summary != null && summary.isTextual()
                    ? summary.asText()
                    : error != null && error.isTextual() ? error.asText() : content;
                var codeStr = code != null && code.isTextual() ? code.asText() : null;
                return codeStr != null
                    ? "Error [" + codeStr + "]: " + message
                    : "Error: " + message;
            }
        } catch (Exception ignored) {
            return null;
        }
        return null;
    }
}