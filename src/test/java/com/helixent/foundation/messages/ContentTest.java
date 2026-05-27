package com.helixent.foundation.messages;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class ContentTest {

    @Test
    void textContent_createsWithText() {
        var content = new Content.TextContent("hello");
        assertEquals("hello", content.text());
    }

    @Test
    void textContent_throwsOnNullText() {
        assertThrows(IllegalArgumentException.class, () -> new Content.TextContent(null));
    }

    @Test
    void imageURLContent_createsWithUrl() {
        var content = new Content.ImageURLContent(
            new Content.ImageURLContent.ImageUrl("https://example.com/img.png", "auto"));
        assertEquals("https://example.com/img.png", content.image_url().url());
        assertEquals("auto", content.image_url().detail());
    }

    @Test
    void imageURLContent_defaultsDetailToNull() {
        var content = new Content.ImageURLContent(
            new Content.ImageURLContent.ImageUrl("https://example.com/img.png", null));
        assertNull(content.image_url().detail());
    }

    @Test
    void thinkingContent_createsWithThinking() {
        var content = new Content.ThinkingContent("planning...");
        assertEquals("planning...", content.thinking());
    }

    @Test
    void thinkingContent_throwsOnNullThinking() {
        assertThrows(IllegalArgumentException.class, () -> new Content.ThinkingContent(null));
    }

    @Test
    void toolUseContent_createsWithAllFields() {
        Map<String, Object> input = Map.of("command", "ls", "description", "list files");
        var content = new Content.ToolUseContent("t1", "bash", input);
        assertEquals("t1", content.id());
        assertEquals("bash", content.name());
        assertEquals(input, content.input());
    }

    @Test
    void toolUseContent_throwsOnNullId() {
        assertThrows(IllegalArgumentException.class, () ->
            new Content.ToolUseContent(null, "bash", java.util.Collections.emptyMap()));
    }

    @Test
    void toolUseContent_throwsOnNullName() {
        assertThrows(IllegalArgumentException.class, () ->
            new Content.ToolUseContent("t1", null, java.util.Collections.emptyMap()));
    }

    @Test
    void toolResultContent_createsWithAllFields() {
        var content = new Content.ToolResultContent("t1", "result text");
        assertEquals("t1", content.tool_use_id());
        assertEquals("result text", content.content());
    }

    @Test
    void toolResultContent_throwsOnNullToolUseId() {
        assertThrows(IllegalArgumentException.class, () ->
            new Content.ToolResultContent(null, "result"));
    }

    @Test
    void content_isSealedInterface() {
        assertTrue(Content.class.isSealed());
    }
}