package com.aether.community.anthropic;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.aether.foundation.messages.Content;
import com.aether.foundation.messages.Message;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class AnthropicStreamAccumulator {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private String messageId;
    private String model;
    private final List<Map<String, Object>> contentBlocks = new ArrayList<>();
    private Message.TokenUsage usage;

    @SuppressWarnings("unchecked")
    public void push(Map<String, Object> event) {
        var type = (String) event.get("type");

        switch (type) {
            case "message_start" -> {
                var msg = (Map<String, Object>) event.get("message");
                if (msg != null) {
                    messageId = (String) msg.get("id");
                    model = (String) msg.get("model");
                    var usageNode = (Map<String, Object>) msg.get("usage");
                    if (usageNode != null) {
                        usage = parseUsage(usageNode);
                    }
                }
            }
            case "content_block_start" -> {
                var block = (Map<String, Object>) event.get("content_block");
                if (block != null) {
                    var index = ((Number) event.get("index")).intValue();
                    while (contentBlocks.size() <= index) {
                        contentBlocks.add(new LinkedHashMap<>());
                    }
                    contentBlocks.set(index, new LinkedHashMap<>(block));
                }
            }
            case "content_block_delta" -> {
                var delta = (Map<String, Object>) event.get("delta");
                var index = ((Number) event.get("index")).intValue();
                if (delta != null && index < contentBlocks.size()) {
                    var block = contentBlocks.get(index);
                    var deltaType = (String) delta.get("type");
                    if ("text_delta".equals(deltaType)) {
                        var text = (String) delta.get("text");
                        block.merge("text", text, (a, b) -> (String) a + (String) b);
                    } else if ("thinking_delta".equals(deltaType)) {
                        var thinking = (String) delta.get("thinking");
                        block.merge("thinking", thinking, (a, b) -> (String) a + (String) b);
                    } else if ("input_json_delta".equals(deltaType)) {
                        var partialJson = (String) delta.get("partial_json");
                        block.merge("partial_json", partialJson, (a, b) -> (String) a + (String) b);
                    }
                }
            }
            case "message_delta" -> {
                var delta = (Map<String, Object>) event.get("delta");
                if (delta != null) {
                    var usageNode = (Map<String, Object>) delta.get("usage");
                    if (usageNode != null) {
                        usage = parseUsage(usageNode);
                    }
                }
            }
        }
    }

    @SuppressWarnings("unchecked")
    public Message.AssistantMessage snapshot() {
        var content = new ArrayList<Content>();
        var isFinal = usage != null;

        for (var block : contentBlocks) {
            var type = (String) block.get("type");
            switch (type) {
                case "text" -> {
                    var text = (String) block.get("text");
                    if (text != null && !text.isEmpty()) {
                        content.add(new Content.TextContent(text));
                    }
                }
                case "thinking" -> {
                    var thinking = (String) block.get("thinking");
                    if (thinking != null && !thinking.isEmpty()) {
                        content.add(new Content.ThinkingContent(thinking));
                    }
                }
                case "tool_use" -> {
                    var id = (String) block.get("id");
                    var name = (String) block.get("name");
                    var partialJson = (String) block.get("partial_json");
                    Map<String, Object> input = Map.of();
                    if (partialJson != null) {
                        try {
                            input = MAPPER.readValue(partialJson, Map.class);
                        } catch (Exception e) {
                            if (!isFinal) continue;
                        }
                    } else {
                        input = (Map<String, Object>) block.get("input");
                        if (input == null) input = Map.of();
                    }
                    content.add(new Content.ToolUseContent(id, name, input));
                }
            }
        }

        return new Message.AssistantMessage(
            content,
            usage,
            usage == null ? true : null
        );
    }

    private Message.TokenUsage parseUsage(Map<String, Object> usageNode) {
        return new Message.TokenUsage(
            usageNode.get("input_tokens") instanceof Number n ? n.intValue() : 0,
            usageNode.get("output_tokens") instanceof Number n ? n.intValue() : 0,
            0
        );
    }
}