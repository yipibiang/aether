package com.aether.foundation.messages;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

/** 消息内容类型 — 与 {@link Message} 配合使用。 */
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
@JsonSubTypes({
    @JsonSubTypes.Type(value = Content.TextContent.class, name = "text"),
    @JsonSubTypes.Type(value = Content.ImageURLContent.class, name = "image_url"),
    @JsonSubTypes.Type(value = Content.ThinkingContent.class, name = "thinking"),
    @JsonSubTypes.Type(value = Content.ToolUseContent.class, name = "tool_use"),
    @JsonSubTypes.Type(value = Content.ToolResultContent.class, name = "tool_result")
})
public sealed interface Content
    permits Content.TextContent,
            Content.ImageURLContent,
            Content.ThinkingContent,
            Content.ToolUseContent,
            Content.ToolResultContent {

    record TextContent(String text) implements Content {
        public TextContent {
            if (text == null) throw new IllegalArgumentException("text must not be null");
        }
    }

    record ImageURLContent(ImageUrl image_url) implements Content {
        public record ImageUrl(String url, String detail) {
            public ImageUrl {
                if (url == null) throw new IllegalArgumentException("url must not be null");
            }
        }
    }

    record ThinkingContent(String thinking) implements Content {
        public ThinkingContent {
            if (thinking == null) throw new IllegalArgumentException("thinking must not be null");
        }
    }

    record ToolUseContent(String id, String name, java.util.Map<String, Object> input) implements Content {
        public ToolUseContent {
            if (id == null) throw new IllegalArgumentException("id must not be null");
            if (name == null) throw new IllegalArgumentException("name must not be null");
            if (input == null) throw new IllegalArgumentException("input must not be null");
        }
    }

    record ToolResultContent(String tool_use_id, String content) implements Content {
        public ToolResultContent {
            if (tool_use_id == null) throw new IllegalArgumentException("tool_use_id must not be null");
            if (content == null) throw new IllegalArgumentException("content must not be null");
        }
    }
}