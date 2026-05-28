package com.aether.runtime;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.aether.foundation.messages.Content;
import com.aether.foundation.messages.Message;
import com.aether.foundation.tools.Tool;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

public final class AgentDebugLog {

    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final String DEFAULT_RELATIVE_LOG = "agent-debug.jsonl";
    private static final int MAX_PREVIEW_CHARS = 240;
    private static final int MAX_JSON_CHARS = 4000;
    private static final int ASSISTANT_RESPONSE_LOG_MAX_CHARS = 48_000;
    public static final String RECORD_SEPARATOR = "\n--- aether-debug-log ---\n";

    private AgentDebugLog() {}

    public static String parseAetherDebugLogEnv(String value) {
        if (value == null || value.isBlank()) return null;
        var trimmed = value.trim();
        if ("1".equals(trimmed) || "true".equalsIgnoreCase(trimmed)) {
            return Paths.get("").toAbsolutePath().resolve(DEFAULT_RELATIVE_LOG).toString();
        }
        return Paths.get("").toAbsolutePath().resolve(trimmed).toString();
    }

    public static String truncateForLog(String value) {
        return truncateForLog(value, MAX_JSON_CHARS);
    }

    public static String truncateForLog(String value, int max) {
        if (value.length() <= max) return value;
        return value.substring(0, max) + "\u2026(truncated,len=" + value.length() + ")";
    }

    public static String stringifyForLog(Object value) {
        if (value instanceof String s) return truncateForLog(s);
        try {
            return truncateForLog(MAPPER.writerWithDefaultPrettyPrinter().writeValueAsString(value));
        } catch (Exception e) {
            return truncateForLog(String.valueOf(value));
        }
    }

    public static Map<String, Object> summarizeUserMessageForLog(Message.UserMessage message) {
        var parts = new StringBuilder();
        for (var c : message.content()) {
            if (c instanceof Content.TextContent t) parts.append(t.text());
        }
        var full = parts.toString();
        var preview = full.length() <= MAX_PREVIEW_CHARS
            ? full
            : full.substring(0, MAX_PREVIEW_CHARS) + "\u2026(truncated,len=" + full.length() + ")";
        return Map.of("textLen", full.length(), "preview", preview);
    }

    public static Map<String, Object> summarizeAssistantForLog(Message.AssistantMessage message) {
        int textLen = 0;
        var toolNames = new ArrayList<String>();
        for (var c : message.content()) {
            if (c instanceof Content.TextContent t) textLen += t.text().length();
            if (c instanceof Content.ToolUseContent t) toolNames.add(t.name());
        }
        return Map.of("toolUseCount", toolNames.size(), "textLen", textLen, "toolNames", toolNames);
    }

    public static Map<String, Object> assistantMessageContentForLog(Message.AssistantMessage message) {
        var textSegments = new ArrayList<Map<String, Object>>();
        var thinkingSegments = new ArrayList<Map<String, Object>>();
        var toolUses = new ArrayList<Map<String, Object>>();

        for (var c : message.content()) {
            if (c instanceof Content.TextContent t) {
                textSegments.add(Map.of(
                    "charLen", t.text().length(),
                    "text", truncateForLog(t.text(), ASSISTANT_RESPONSE_LOG_MAX_CHARS)
                ));
            } else if (c instanceof Content.ThinkingContent t) {
                thinkingSegments.add(Map.of(
                    "charLen", t.thinking().length(),
                    "thinking", truncateForLog(t.thinking(), ASSISTANT_RESPONSE_LOG_MAX_CHARS)
                ));
            } else if (c instanceof Content.ToolUseContent t) {
                toolUses.add(Map.of(
                    "toolUseId", t.id(),
                    "name", t.name(),
                    "input", stringifyForLog(t.input())
                ));
            }
        }

        return Map.of("textSegments", textSegments, "thinkingSegments", thinkingSegments, "toolUses", toolUses);
    }

    public static int roughTokenEstimateFromChars(int charCount) {
        return (int) Math.ceil(charCount / 4.0);
    }

    public static int approximateTranscriptChars(List<Message> messages, String prompt, List<Tool> tools) {
        var toolWire = new ArrayList<Map<String, String>>();
        if (tools != null) {
            for (var t : tools) {
                toolWire.add(Map.of("name", t.name(), "description", t.description()));
            }
        }
        try {
            return prompt.length()
                + MAPPER.writeValueAsString(messages).length()
                + MAPPER.writeValueAsString(toolWire).length();
        } catch (Exception e) {
            return prompt.length();
        }
    }

    public static Map<String, Object> messagesJsonForLog(List<Message> messages) {
        return messagesJsonForLog(messages, 96_000);
    }

    public static Map<String, Object> messagesJsonForLog(List<Message> messages, int maxChars) {
        try {
            var raw = MAPPER.writeValueAsString(messages);
            var pretty = MAPPER.writerWithDefaultPrettyPrinter().writeValueAsString(messages);
            return Map.of("approxChars", raw.length(), "json", truncateForLog(pretty, maxChars));
        } catch (Exception e) {
            return Map.of("approxChars", 0, "json", "{}");
        }
    }

    public static List<Map<String, String>> summarizeToolRegistry(List<Tool> tools) {
        var result = new ArrayList<Map<String, String>>();
        if (tools != null) {
            for (var t : tools) {
                result.add(Map.of(
                    "name", t.name(),
                    "descriptionPreview", truncateForLog(t.description(), 280)
                ));
            }
        }
        return result;
    }

    public static Message.UserMessage findLastUserMessage(List<Message> messages) {
        for (int i = messages.size() - 1; i >= 0; i--) {
            var m = messages.get(i);
            if (m instanceof Message.UserMessage u) return u;
        }
        return null;
    }

    public static class AgentJsonlDebugWriter {
        private final Path path;
        private final Object writeLock = new Object();
        private boolean firstWrite = true;

        public AgentJsonlDebugWriter(String path) {
            this.path = Path.of(path);
        }

        public void emit(Map<String, Object> record) {
            var enriched = new LinkedHashMap<String, Object>();
            enriched.put("ts", Instant.now().toString());
            enriched.put("pid", ProcessHandle.current().pid());
            enriched.putAll(record);

            synchronized (writeLock) {
                try {
                    var body = MAPPER.writerWithDefaultPrettyPrinter().writeValueAsString(enriched);
                    var dir = this.path.getParent();
                    if (dir != null) {
                        Files.createDirectories(dir);
                    }
                    if (firstWrite) {
                        firstWrite = false;
                        Files.writeString(this.path, "\uFEFF" + body + RECORD_SEPARATOR, StandardCharsets.UTF_8,
                            StandardOpenOption.CREATE, StandardOpenOption.APPEND);
                    } else {
                        Files.writeString(this.path, body + RECORD_SEPARATOR, StandardCharsets.UTF_8,
                            StandardOpenOption.CREATE, StandardOpenOption.APPEND);
                    }
                } catch (IOException e) {
                    System.err.println("[AgentDebugLog] Failed to write: " + e.getMessage());
                }
            }
        }

        public CompletableFuture<Void> flush() {
            return CompletableFuture.completedFuture(null);
        }
    }
}