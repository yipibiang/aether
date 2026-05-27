package com.aether.community.openai;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.aether.foundation.messages.Content;
import com.aether.foundation.messages.Message;
import com.aether.foundation.tools.Tool;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** OpenAI 消息/工具格式转换。 */
public final class OpenAIUtils {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private OpenAIUtils() {}

    public static List<Map<String, Object>> convertToOpenAIMessages(List<Message> messages) {
        var result = new ArrayList<Map<String, Object>>();
        for (var message : messages) {
            switch (message) {
                case Message.SystemMessage sys -> {
                    var content = sys.content().stream()
                        .map(c -> Map.<String, Object>of("type", "text", "text", c.text()))
                        .toList();
                    result.add(Map.of("role", "system", "content", content));
                }
                case Message.UserMessage user -> {
                    var content = user.content().stream()
                        .map(c -> {
                            if (c instanceof Content.TextContent tc) {
                                return Map.<String, Object>of("type", "text", "text", tc.text());
                            } else if (c instanceof Content.ImageURLContent ic) {
                                return Map.<String, Object>of(
                                    "type", "image_url",
                                    "image_url", Map.of(
                                        "url", ic.image_url().url(),
                                        "detail", ic.image_url().detail() != null ? ic.image_url().detail() : "auto"
                                    )
                                );
                            }
                            return Map.<String, Object>of("type", "text", "text", "");
                        })
                        .toList();
                    result.add(Map.of("role", "user", "content", content));
                }
                case Message.AssistantMessage assistant -> {
                    var msg = new LinkedHashMap<String, Object>();
                    msg.put("role", "assistant");
                    var contentList = new ArrayList<Map<String, Object>>();
                    var toolCalls = new ArrayList<Map<String, Object>>();
                    var reasoningContent = new StringBuilder();

                    for (var c : assistant.content()) {
                        if (c instanceof Content.ThinkingContent tc) {
                            reasoningContent.append(tc.thinking());
                        } else if (c instanceof Content.ToolUseContent tuc) {
                            toolCalls.add(Map.of(
                                "type", "function",
                                "id", tuc.id(),
                                "function", Map.of(
                                    "name", tuc.name(),
                                    "arguments", toJson(tuc.input())
                                )
                            ));
                        } else if (c instanceof Content.TextContent tc) {
                            contentList.add(Map.of("type", "text", "text", tc.text()));
                        }
                    }

                    if (!reasoningContent.isEmpty()) {
                        msg.put("reasoning_content", reasoningContent.toString());
                    }
                    if (!toolCalls.isEmpty()) {
                        msg.put("tool_calls", toolCalls);
                    }
                    if (contentList.isEmpty() && toolCalls.isEmpty()) {
                        msg.put("content", "");
                    } else if (!contentList.isEmpty()) {
                        msg.put("content", contentList);
                    } else {
                        msg.put("content", "");
                    }
                    result.add(msg);
                }
                case Message.ToolMessage tool -> {
                    for (var c : tool.content()) {
                        result.add(Map.of(
                            "role", "tool",
                            "tool_call_id", c.tool_use_id(),
                            "content", c.content()
                        ));
                    }
                }
            }
        }
        return result;
    }

    @SuppressWarnings("unchecked")
    public static Message.AssistantMessage parseAssistantMessage(
        Map<String, Object> message, Message.TokenUsage usage
    ) {
        var content = new ArrayList<Content>();

        var reasoningContent = (String) message.get("reasoning_content");
        if (reasoningContent != null && !reasoningContent.isEmpty()) {
            content.add(new Content.ThinkingContent(reasoningContent));
        }

        var msgContent = message.get("content");
        if (msgContent instanceof String s && !s.isEmpty()) {
            content.add(new Content.TextContent(s));
        } else if (msgContent instanceof List<?> list) {
            for (var item : list) {
                if (item instanceof Map<?, ?> m && "text".equals(m.get("type"))) {
                    content.add(new Content.TextContent((String) m.get("text")));
                }
            }
        }

        var toolCalls = (List<Map<String, Object>>) message.get("tool_calls");
        if (toolCalls != null) {
            for (var tc : toolCalls) {
                if ("function".equals(tc.get("type"))) {
                    var func = (Map<String, Object>) tc.get("function");
                    Map<String, Object> input;
                    try {
                        var argsStr = (String) func.get("arguments");
                        input = MAPPER.readValue(argsStr, Map.class);
                    } catch (Exception e) {
                        input = Map.of();
                    }
                    content.add(new Content.ToolUseContent(
                        (String) tc.get("id"),
                        (String) func.get("name"),
                        input
                    ));
                }
            }
        }

        return new Message.AssistantMessage(content, usage);
    }

    public static List<Map<String, Object>> convertToOpenAITools(List<Tool> tools) {
        return tools.stream()
            .map(t -> Map.<String, Object>of(
                "type", "function",
                "function", Map.of(
                    "name", t.name(),
                    "description", t.description(),
                    "parameters", t.toJsonSchema()
                )
            ))
            .toList();
    }

    private static String toJson(Object obj) {
        try {
            return MAPPER.writeValueAsString(obj);
        } catch (Exception e) {
            return "{}";
        }
    }
}