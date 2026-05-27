package com.aether.agent;

import com.aether.foundation.messages.Content;
import com.aether.foundation.messages.Message;
import com.aether.foundation.models.ModelContext;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

class AgentDebugLogMiddlewareTest {

    @Test
    void create_returnsNoOpMiddlewareWhenNoLogPath() {
        var options = new AgentDebugLogMiddleware.AgentDebugLogOptions("test-model", 100, null);
        var middleware = AgentDebugLogMiddleware.create(options);
        assertNotNull(middleware);
    }

    @Test
    void create_returnsActiveMiddlewareWithLogPath(@TempDir Path tempDir) throws Exception {
        var logPath = tempDir.resolve("debug.jsonl").toString();
        var options = new AgentDebugLogMiddleware.AgentDebugLogOptions("test-model", 100, logPath);
        var middleware = AgentDebugLogMiddleware.create(options);
        assertNotNull(middleware);

        var ctx = new TestAgentContextView();
        var result = middleware.beforeAgentRun(ctx).get(5, TimeUnit.SECONDS);
        assertNull(result);
    }

    @Test
    void beforeAgentStep_emitsLog(@TempDir Path tempDir) throws Exception {
        var logPath = tempDir.resolve("debug.jsonl").toString();
        var options = new AgentDebugLogMiddleware.AgentDebugLogOptions("test-model", 100, logPath);
        var middleware = AgentDebugLogMiddleware.create(options);

        var ctx = new TestAgentContextView();
        middleware.beforeAgentRun(ctx).get(5, TimeUnit.SECONDS);
        middleware.beforeAgentStep(ctx, 1).get(5, TimeUnit.SECONDS);

        var logContent = java.nio.file.Files.readString(Path.of(logPath));
        assertTrue(logContent.contains("beforeAgentStep"));
    }

    @Test
    void beforeModel_emitsLog(@TempDir Path tempDir) throws Exception {
        var logPath = tempDir.resolve("debug.jsonl").toString();
        var options = new AgentDebugLogMiddleware.AgentDebugLogOptions("test-model", 100, logPath);
        var middleware = AgentDebugLogMiddleware.create(options);

        var ctx = new TestAgentContextView();
        middleware.beforeAgentRun(ctx).get(5, TimeUnit.SECONDS);

        var modelCtx = new ModelContext("prompt", List.of(), List.of(), null);
        middleware.beforeModel(modelCtx, ctx).get(5, TimeUnit.SECONDS);

        var logContent = java.nio.file.Files.readString(Path.of(logPath));
        assertTrue(logContent.contains("beforeModel"));
    }

    @Test
    void afterModel_emitsLog(@TempDir Path tempDir) throws Exception {
        var logPath = tempDir.resolve("debug.jsonl").toString();
        var options = new AgentDebugLogMiddleware.AgentDebugLogOptions("test-model", 100, logPath);
        var middleware = AgentDebugLogMiddleware.create(options);

        var ctx = new TestAgentContextView();
        middleware.beforeAgentRun(ctx).get(5, TimeUnit.SECONDS);

        var msg = new Message.AssistantMessage(
            List.of(new Content.TextContent("response")),
            new Message.TokenUsage(10, 5, 15)
        );
        middleware.afterModel(ctx, msg).get(5, TimeUnit.SECONDS);

        var logContent = java.nio.file.Files.readString(Path.of(logPath));
        assertTrue(logContent.contains("afterModel"));
    }

    @Test
    void beforeToolUse_emitsLog(@TempDir Path tempDir) throws Exception {
        var logPath = tempDir.resolve("debug.jsonl").toString();
        var options = new AgentDebugLogMiddleware.AgentDebugLogOptions("test-model", 100, logPath);
        var middleware = AgentDebugLogMiddleware.create(options);

        var ctx = new TestAgentContextView();
        middleware.beforeAgentRun(ctx).get(5, TimeUnit.SECONDS);

        var toolUse = new Content.ToolUseContent("id1", "bash", Map.of("command", (Object) "ls"));
        middleware.beforeToolUse(ctx, toolUse).get(5, TimeUnit.SECONDS);

        var logContent = java.nio.file.Files.readString(Path.of(logPath));
        assertTrue(logContent.contains("tool_use_start"));
    }

    @Test
    void afterToolUse_emitsLog(@TempDir Path tempDir) throws Exception {
        var logPath = tempDir.resolve("debug.jsonl").toString();
        var options = new AgentDebugLogMiddleware.AgentDebugLogOptions("test-model", 100, logPath);
        var middleware = AgentDebugLogMiddleware.create(options);

        var ctx = new TestAgentContextView();
        middleware.beforeAgentRun(ctx).get(5, TimeUnit.SECONDS);

        var toolUse = new Content.ToolUseContent("id1", "bash", Map.of("command", (Object) "ls"));
        middleware.beforeToolUse(ctx, toolUse).get(5, TimeUnit.SECONDS);
        middleware.afterToolUse(ctx, toolUse, "output").get(5, TimeUnit.SECONDS);

        var logContent = java.nio.file.Files.readString(Path.of(logPath));
        assertTrue(logContent.contains("tool_use_complete"));
    }

    @Test
    void finalizeAgentStream_flushes(@TempDir Path tempDir) throws Exception {
        var logPath = tempDir.resolve("debug.jsonl").toString();
        var options = new AgentDebugLogMiddleware.AgentDebugLogOptions("test-model", 100, logPath);
        var middleware = AgentDebugLogMiddleware.create(options);

        var ctx = new TestAgentContextView();
        middleware.beforeAgentRun(ctx).get(5, TimeUnit.SECONDS);
        middleware.finalizeAgentStream(ctx, null).get(5, TimeUnit.SECONDS);

        var logContent = java.nio.file.Files.readString(Path.of(logPath));
        assertTrue(logContent.contains("beforeAgentRun"));
    }

    private static class TestAgentContextView implements AgentMiddleware.AgentContextView {
        @Override public String prompt() { return "test prompt"; }
        @Override public List<Message> messages() { return List.of(); }
        @Override public List<com.aether.foundation.tools.Tool> tools() { return List.of(); }
        @Override public List<SkillFrontmatter> skills() { return List.of(); }
        @Override public String requestedSkillName() { return null; }
    }
}