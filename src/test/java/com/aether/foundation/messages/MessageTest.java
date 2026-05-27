package com.aether.foundation.messages;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class MessageTest {

    @Test
    void systemMessage_createsWithContent() {
        var content = List.of(new Content.TextContent("system prompt"));
        var msg = new Message.SystemMessage(content);
        assertEquals(content, msg.content());
    }

    @Test
    void userMessage_createsWithContent() {
        List<Content> content = List.of(new Content.TextContent("hello"));
        var msg = new Message.UserMessage(content);
        assertEquals(content, msg.content());
    }

    @Test
    void assistantMessage_createsWithContent() {
        List<Content> content = List.of(new Content.TextContent("response"));
        var msg = new Message.AssistantMessage(content, null);
        assertEquals(content, msg.content());
        assertNull(msg.usage());
    }

    @Test
    void assistantMessage_createsWithUsage() {
        List<Content> content = List.of(new Content.TextContent("response"));
        var usage = new Message.TokenUsage(10, 20, 30);
        var msg = new Message.AssistantMessage(content, usage);
        assertEquals(usage, msg.usage());
        assertEquals(10, msg.usage().promptTokens());
        assertEquals(20, msg.usage().completionTokens());
        assertEquals(30, msg.usage().totalTokens());
    }

    @Test
    void toolMessage_createsWithContent() {
        var content = List.of(new Content.ToolResultContent("t1", "result"));
        var msg = new Message.ToolMessage(content);
        assertEquals(content, msg.content());
    }

    @Test
    void tokenUsage_createsWithAllFields() {
        var usage = new Message.TokenUsage(100, 200, 300);
        assertEquals(100, usage.promptTokens());
        assertEquals(200, usage.completionTokens());
        assertEquals(300, usage.totalTokens());
    }
}