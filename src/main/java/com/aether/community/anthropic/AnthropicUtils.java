package com.aether.community.anthropic;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.aether.foundation.messages.Content;
import com.aether.foundation.messages.Message;
import com.aether.foundation.tools.Tool;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/** Anthropic 消息/工具格式转换。 */
public final class AnthropicUtils {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private AnthropicUtils() {}

    public static List<Map<String, Object>> convertToAnthropicMessages(List<Message> messages) {
        var result = new ArrayList<Map<String, Object>>();
        for (var message : messages) {
            switch (message) {
                case Message.UserMessage user -> {
                    var content = new ArrayList<Map<String, Object>>();
                    for (var c : user.content()) {
                        if (c instanceof Content.TextContent tc) {
                            content.add(Map.of("type", "text", "text", tc.text()));
                        } else if (c instanceof Content.ImageURLContent ic) {
                            content.add(Map.of(
                                "type", "image",
                                "source", Map.of(
                                    "type", "url",
                                    "url", ic.image_url().url()
                                )
                            ));
                        }
                    }
                    result.add(Map.of("role", "user", "content", content));
                }
                case Message.AssistantMessage assistant -> {
                    var content = new ArrayList<Map<String, Object>>();
                    for (var c : assistant.content()) {
                        if (c instanceof Content.ThinkingContent tc) {
                            content.add(Map.of("type", "thinking", "thinking", tc.thinking()));
                        } else if (c instanceof Content.TextContent tc) {
                            content.add(Map.of("type", "text", "text", tc.text()));
                        } else if (c instanceof Content.ToolUseContent tuc) {
                            content.add(Map.of(
                                "type", "tool_use",
                                "id", tuc.id(),
                                "name", tuc.name(),
                                "input", tuc.input()
                            ));
                        }
                    }
                    result.add(Map.of("role", "assistant", "content", content));
                }
                case Message.ToolMessage tool -> {
                    for (var c : tool.content()) {
                        result.add(Map.of(
                            "role", "user",
                            "content", List.of(Map.of(
                                "type", "tool_result",
                                "tool_use_id", c.tool_use_id(),
                                "content", c.content()
                            ))
                        ));
                    }
                }
                default -> {}
            }
        }
        return result;
    }

    @SuppressWarnings("unchecked")
    public static Message.AssistantMessage parseAssistantMessage(
        Map<String, Object> message, Message.TokenUsage usage
    ) {
        var content = new ArrayList<Content>();
        var contentBlocks = (List<Map<String, Object>>) message.get("content");

        if (contentBlocks != null) {
            for (var block : contentBlocks) {
                var type = (String) block.get("type");
                switch (type) {
                    case "text" -> content.add(new Content.TextContent((String) block.get("text")));
                    case "thinking" -> content.add(new Content.ThinkingContent((String) block.get("thinking")));
                    case "tool_use" -> content.add(new Content.ToolUseContent(
                        (String) block.get("id"),
                        (String) block.get("name"),
                        (Map<String, Object>) block.get("input")
                    ));
                }
            }
        }

        return new Message.AssistantMessage(content, usage);
    }

    public static List<Map<String, Object>> convertToAnthropicTools(List<Tool> tools) {
        return tools.stream()
            .map(t -> Map.<String, Object>of(
                "name", t.name(),
                "description", t.description(),
                "input_schema", t.toJsonSchema()
            ))
            .toList();
    }
}