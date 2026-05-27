package com.aether.agent;

import com.aether.foundation.messages.Content;
import com.aether.foundation.messages.Message;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class AgentDebugLogTest {

    @Test
    void parseAetherDebugLogEnv_returnsNullForNull() {
        assertNull(AgentDebugLog.parseAetherDebugLogEnv(null));
    }

    @Test
    void parseAetherDebugLogEnv_returnsNullForBlank() {
        assertNull(AgentDebugLog.parseAetherDebugLogEnv("   "));
    }

    @Test
    void parseAetherDebugLogEnv_returnsDefaultForTrue() {
        var result = AgentDebugLog.parseAetherDebugLogEnv("1");
        assertNotNull(result);
        assertTrue(result.endsWith("agent-debug.jsonl"));
    }

    @Test
    void truncateForLog_doesNotTruncateShortText() {
        var result = AgentDebugLog.truncateForLog("short");
        assertEquals("short", result);
    }

    @Test
    void truncateForLog_truncatesLongText() {
        var longText = "a".repeat(5000);
        var result = AgentDebugLog.truncateForLog(longText);
        assertTrue(result.length() < longText.length());
        assertTrue(result.contains("truncated"));
    }

    @Test
    void stringifyForLog_handlesString() {
        var result = AgentDebugLog.stringifyForLog("hello");
        assertEquals("hello", result);
    }

    @Test
    void stringifyForLog_handlesNull() {
        var result = AgentDebugLog.stringifyForLog(null);
        assertEquals("null", result);
    }

    @Test
    void summarizeUserMessageForLog() {
        var msg = new Message.UserMessage(List.of(
            new Content.TextContent("Hello world")
        ));
        var result = AgentDebugLog.summarizeUserMessageForLog(msg);
        assertEquals(11, result.get("textLen"));
        assertEquals("Hello world", result.get("preview"));
    }

    @Test
    void summarizeAssistantForLog() {
        var msg = new Message.AssistantMessage(
            List.of(
                new Content.TextContent("response"),
                new Content.ToolUseContent("id1", "bash", Map.of("command", (Object) "ls"))
            ),
            null
        );
        var result = AgentDebugLog.summarizeAssistantForLog(msg);
        assertEquals(1, result.get("toolUseCount"));
        assertEquals(8, result.get("textLen"));
    }

    @Test
    void assistantMessageContentForLog() {
        var msg = new Message.AssistantMessage(
            List.of(new Content.TextContent("hello")),
            null
        );
        var result = AgentDebugLog.assistantMessageContentForLog(msg);
        assertNotNull(result);
        @SuppressWarnings("unchecked")
        var textSegments = (List<Map<String, Object>>) result.get("textSegments");
        assertEquals(1, textSegments.size());
    }

    @Test
    void findLastUserMessage_returnsLastUserMessage() {
        var messages = List.<Message>of(
            new Message.UserMessage(List.of(new Content.TextContent("first"))),
            new Message.AssistantMessage(List.of(new Content.TextContent("reply")), null),
            new Message.UserMessage(List.of(new Content.TextContent("second")))
        );
        var result = AgentDebugLog.findLastUserMessage(messages);
        assertNotNull(result);
        assertTrue(result instanceof Message.UserMessage);
    }

    @Test
    void approximateTranscriptChars_returnsEstimate() {
        var messages = List.<Message>of(
            new Message.UserMessage(List.of(new Content.TextContent("hello")))
        );
        var result = AgentDebugLog.approximateTranscriptChars(messages, "prompt", List.of());
        assertTrue(result > 0);
    }

    @Test
    void roughTokenEstimateFromChars() {
        var result = AgentDebugLog.roughTokenEstimateFromChars(1000);
        assertTrue(result > 0);
        assertTrue(result < 1000);
    }
}