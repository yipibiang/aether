package com.helixent.community.openai;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.helixent.foundation.messages.Content;
import com.helixent.foundation.messages.Message;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

public class OpenAIStreamAccumulator {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private String reasoningContent = "";
    private String textContent = "";
    private final TreeMap<Integer, ToolCallState> toolCalls = new TreeMap<>();
    private Message.TokenUsage usage;

    /**
     * JDK16 record：流式工具调用累积状态（id、名称、参数 JSON 片段）。
     */
    private record ToolCallState(String id, String name, String arguments) {}

    @SuppressWarnings("unchecked")
    public void push(Map<String, Object> chunk) {
        var choices = (List<Map<String, Object>>) chunk.get("choices");
        if (choices != null && !choices.isEmpty()) {
            var delta = (Map<String, Object>) choices.get(0).get("delta");
            if (delta != null) {
                var reasoning = (String) delta.get("reasoning_content");
                if (reasoning != null) {
                    reasoningContent += reasoning;
                }

                var content = delta.get("content");
                if (content instanceof String s) {
                    textContent += s;
                }

                var toolCallsDelta = (List<Map<String, Object>>) delta.get("tool_calls");
                if (toolCallsDelta != null) {
                    for (var tc : toolCallsDelta) {
                        var index = ((Number) tc.get("index")).intValue();
                        var entry = toolCalls.get(index);
                        if (entry == null) {
                            entry = new ToolCallState(
                                (String) tc.getOrDefault("id", ""),
                                "",
                                ""
                            );
                        }
                        var func = (Map<String, Object>) tc.get("function");
                        var newId = (String) tc.get("id");
                        var newName = func != null ? (String) func.get("name") : null;
                        var newArgs = func != null ? (String) func.get("arguments") : null;

                        var id = newId != null ? newId : entry.id();
                        var name = newName != null ? newName : entry.name();
                        var args = entry.arguments() + (newArgs != null ? newArgs : "");

                        toolCalls.put(index, new ToolCallState(id, name, args));
                    }
                }
            }
        }

        var usageNode = chunk.get("usage");
        if (usageNode instanceof Map<?, ?> u) {
            var promptTokens = u.get("prompt_tokens");
            var completionTokens = u.get("completion_tokens");
            var totalTokens = u.get("total_tokens");
            usage = new Message.TokenUsage(
                promptTokens instanceof Number n ? n.intValue() : 0,
                completionTokens instanceof Number n ? n.intValue() : 0,
                totalTokens instanceof Number n ? n.intValue() : 0
            );
        }
    }

    @SuppressWarnings("unchecked")
    public Message.AssistantMessage snapshot() {
        var content = new ArrayList<Content>();

        if (!reasoningContent.isEmpty()) {
            content.add(new Content.ThinkingContent(reasoningContent));
        }
        if (!textContent.isEmpty()) {
            content.add(new Content.TextContent(textContent));
        }

        var isFinal = usage != null;
        for (var tc : toolCalls.values()) {
            Map<String, Object> input = Map.of();
            var parsed = false;
            try {
                input = MAPPER.readValue(tc.arguments(), Map.class);
                parsed = true;
            } catch (Exception e) {
                // arguments still streaming
            }
            if (!parsed && !isFinal) continue;
            content.add(new Content.ToolUseContent(tc.id(), tc.name(), input));
        }

        return new Message.AssistantMessage(
            content,
            usage,
            usage == null ? true : null
        );
    }
}