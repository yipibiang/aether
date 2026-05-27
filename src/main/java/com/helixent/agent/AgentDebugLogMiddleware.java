package com.helixent.agent;

import com.helixent.foundation.messages.Content;
import com.helixent.foundation.messages.Message;

import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public final class AgentDebugLogMiddleware {

    private AgentDebugLogMiddleware() {}

    public static AgentMiddleware create(AgentDebugLogOptions options) {
        var resolvedPath = options.logPath() != null
            ? options.logPath()
            : AgentDebugLog.parseHelixentDebugLogEnv(System.getenv("HELIXENT_DEBUG_LOG"));

        if (resolvedPath == null) {
            return new AgentMiddleware() {};
        }

        var writer = new AgentDebugLog.AgentJsonlDebugWriter(resolvedPath);
        var state = new DebugLogState(writer);

        return new AgentMiddleware() {
            @Override
            public CompletableFuture<Map<String, Object>> beforeAgentRun(AgentContextView agentContext) {
                state.runId = UUID.randomUUID().toString();
                state.runStartedAt = System.currentTimeMillis();
                state.currentStep = 0;

                var lastUser = AgentDebugLog.findLastUserMessage(agentContext.messages());
                var data = new LinkedHashMap<String, Object>();
                data.put("modelName", options.modelName());
                data.put("maxSteps", options.maxSteps());
                data.put("messageCount", agentContext.messages().size());
                data.put("requestedSkillName",
                    agentContext.requestedSkillName() != null ? agentContext.requestedSkillName() : null);
                data.put("skillsCountAtThisHook",
                    agentContext.skills() != null ? agentContext.skills().size() : 0);
                data.put("lastUserTurn",
                    lastUser != null ? AgentDebugLog.summarizeUserMessageForLog(lastUser) : null);

                state.emit("beforeAgentRun", "session_start", "info", data);
                return CompletableFuture.completedFuture(null);
            }

            @Override
            public CompletableFuture<Map<String, Object>> beforeAgentStep(AgentContextView agentContext, int step) {
                state.currentStep = step;
                state.messagesBaselineAtStep = agentContext.messages().size();

                var chars = AgentDebugLog.approximateTranscriptChars(
                    agentContext.messages(), agentContext.prompt(), agentContext.tools());
                var data = new LinkedHashMap<String, Object>();
                data.put("messageCount", agentContext.messages().size());
                data.put("transcriptApproxChars", chars);
                data.put("transcriptRoughTokens", AgentDebugLog.roughTokenEstimateFromChars(chars));

                state.emit("beforeAgentStep", "react_step_" + step, "debug", data);

                if (step == 1 && !state.skillsCatalogEmitted) {
                    state.skillsCatalogEmitted = true;
                    var skills = agentContext.skills() != null ? agentContext.skills() : java.util.List.<SkillFrontmatter>of();
                    var skillsData = new ArrayList<Map<String, Object>>();
                    for (var s : skills) {
                        skillsData.add(Map.of(
                            "name", s.name(),
                            "path", s.path(),
                            "descriptionPreview", AgentDebugLog.truncateForLog(s.description(), 800)
                        ));
                    }
                    var catalogData = new LinkedHashMap<String, Object>();
                    catalogData.put("note",
                        "Skill discovery runs in `createSkillsMiddleware.beforeAgentRun`. This snapshot is `agentContext.skills` before step 1's model call.");
                    catalogData.put("requestedSkillName",
                        agentContext.requestedSkillName() != null ? agentContext.requestedSkillName() : null);
                    catalogData.put("skillCount", skills.size());
                    catalogData.put("skills", skillsData);

                    state.emit("skills_catalog", "skills_loaded_into_context", "info", catalogData);
                }

                return CompletableFuture.completedFuture(null);
            }

            @Override
            public CompletableFuture<Map<String, Object>> beforeModel(
                com.helixent.foundation.models.ModelContext modelContext, AgentContextView agentContext
            ) {
                state.modelRequestStartedAt = System.currentTimeMillis();

                var snapshot = AgentDebugLog.messagesJsonForLog(modelContext.messages());
                var tools = AgentDebugLog.summarizeToolRegistry(modelContext.tools());

                var data = new LinkedHashMap<String, Object>();
                data.put("promptLen", modelContext.prompt().length());
                data.put("messageCount", modelContext.messages().size());
                data.put("messagesJsonApproxChars", snapshot.get("approxChars"));
                data.put("messagesJson", snapshot.get("json"));
                data.put("toolDefinitions", tools);
                data.put("maxSteps", options.maxSteps());
                data.put("signalAborted", false);
                data.put("requestedSkillName",
                    agentContext.requestedSkillName() != null ? agentContext.requestedSkillName() : null);
                data.put("skillsCountForSkillSystemBlock",
                    agentContext.skills() != null ? agentContext.skills().size() : 0);

                state.emit("beforeModel", "llm_request", "debug", data);
                return CompletableFuture.completedFuture(null);
            }

            @Override
            public CompletableFuture<Map<String, Object>> afterModel(
                AgentContextView agentContext, Message.AssistantMessage message
            ) {
                var durationMs = System.currentTimeMillis() - state.modelRequestStartedAt;

                var data = new LinkedHashMap<String, Object>();
                data.put("durationMs", durationMs);
                data.put("usage", message.usage() != null ? Map.of(
                    "promptTokens", message.usage().promptTokens(),
                    "completionTokens", message.usage().completionTokens(),
                    "totalTokens", message.usage().totalTokens()
                ) : null);
                data.put("summary", AgentDebugLog.summarizeAssistantForLog(message));
                data.put("assistantContent", AgentDebugLog.assistantMessageContentForLog(message));

                state.emit("afterModel", "llm_response", "info", data);
                return CompletableFuture.completedFuture(null);
            }

            @Override
            public CompletableFuture<BeforeToolUseResult> beforeToolUse(
                AgentContextView agentContext, Content.ToolUseContent toolUse
            ) {
                var wallStartedAt = System.currentTimeMillis();
                state.toolInvocationStartedAt.put(toolUse.id(), wallStartedAt);

                var inputStr = AgentDebugLog.stringifyForLog(toolUse.input());
                var skillContext = skillContextFromReadFileTool(
                    agentContext.skills(), toolUse.name(), toolUse.input());

                var data = new LinkedHashMap<String, Object>();
                data.put("wallStartedAtMs", wallStartedAt);
                data.put("inputPreview", inputStr);
                data.put("inputApproxChars", inputStr.length());
                data.put("skillContext", skillContext);

                state.emit("tool_use_start", "tool_start_" + toolUse.name(), "info", data,
                    Map.of("tool", toolUse.name(), "toolUseId", toolUse.id()));
                return CompletableFuture.completedFuture(null);
            }

            @Override
            public CompletableFuture<Map<String, Object>> afterToolUse(
                AgentContextView agentContext, Content.ToolUseContent toolUse, Object toolResult
            ) {
                var started = state.toolInvocationStartedAt.remove(toolUse.id());
                var wallEndedAt = System.currentTimeMillis();
                var durationMs = started != null ? wallEndedAt - started : null;

                var resultStr = AgentDebugLog.stringifyForLog(toolResult);
                var looksLikeError = toolResult instanceof String s && s.startsWith("Error:");
                var skillContext = skillContextFromReadFileTool(
                    agentContext.skills(), toolUse.name(), toolUse.input());

                var data = new LinkedHashMap<String, Object>();
                data.put("wallEndedAtMs", wallEndedAt);
                data.put("durationMs", durationMs);
                data.put("resultPreview", resultStr);
                data.put("resultApproxChars", resultStr.length());
                data.put("resultKind", classifyToolResult(toolResult));
                data.put("looksLikeError", looksLikeError);
                data.put("skillContext", skillContext);

                state.emit("tool_use_complete", "tool_done_" + toolUse.name(), "info", data,
                    Map.of("tool", toolUse.name(), "toolUseId", toolUse.id()));
                return CompletableFuture.completedFuture(null);
            }

            @Override
            public CompletableFuture<Map<String, Object>> afterAgentStep(AgentContextView agentContext, int step) {
                var messagesAdded = agentContext.messages().size() - state.messagesBaselineAtStep;

                var data = new LinkedHashMap<String, Object>();
                data.put("messagesAddedThisStep", messagesAdded);
                data.put("messageCount", agentContext.messages().size());
                data.put("willContinue", true);

                state.emit("afterAgentStep", "step_done_" + step, "debug", data);
                return CompletableFuture.completedFuture(null);
            }

            @Override
            public CompletableFuture<Map<String, Object>> afterAgentRun(AgentContextView agentContext) {
                var messages = agentContext.messages();
                var last = !messages.isEmpty() ? messages.get(messages.size() - 1) : null;
                Object answerSummary = null;
                if (last instanceof Message.AssistantMessage a) {
                    answerSummary = AgentDebugLog.summarizeAssistantForLog(a);
                }

                var data = new LinkedHashMap<String, Object>();
                data.put("totalRunDurationMs", System.currentTimeMillis() - state.runStartedAt);
                data.put("stepsCompleted", state.currentStep);
                data.put("messageCount", messages.size());
                data.put("finalAssistantSummary", answerSummary);

                state.emit("afterAgentRun", "agent_stop_normal", "info", data);
                return CompletableFuture.completedFuture(null);
            }

            @Override
            public CompletableFuture<Void> finalizeAgentStream(AgentContextView agentContext, Throwable error) {
                if (error != null) {
                    var msg = error.getMessage() != null ? error.getMessage() : error.getClass().getSimpleName();
                    var data = new LinkedHashMap<String, Object>();
                    data.put("errorName", error.getClass().getSimpleName());
                    data.put("runDurationMs",
                        state.runStartedAt > 0 ? System.currentTimeMillis() - state.runStartedAt : null);
                    data.put("lastStep", state.currentStep);

                    state.emit("finalizeAgentStream", msg, "error", data);
                }
                return writer.flush().thenApply(v -> null);
            }
        };
    }

    private static String classifyToolResult(Object result) {
        if (result instanceof String s) {
            return s.startsWith("Error:") ? "legacy_error" : "legacy_string";
        }
        if (result instanceof Map<?, ?> m && m.containsKey("ok")) {
            var ok = m.get("ok");
            return Boolean.TRUE.equals(ok) ? "structured_ok" : "structured_error";
        }
        return "unknown";
    }

    private static Map<String, Object> skillContextFromReadFileTool(
        java.util.List<SkillFrontmatter> skills, String toolName, Map<String, Object> toolInput
    ) {
        if (skills == null || skills.isEmpty() || !"read_file".equals(toolName)) return null;
        var pathRaw = toolInput.get("path");
        if (!(pathRaw instanceof String pathStr)) return null;

        var req = comparablePath(pathStr);
        for (var s : skills) {
            var main = comparablePath(s.path());
            var folder = comparablePath(Path.of(s.path()).getParent().toString()) + java.io.File.separator;
            if (req.equals(main)) {
                return Map.of("skillName", s.name(), "skillMainFile", s.path(), "match", "skill_main");
            }
            if (req.startsWith(folder)) {
                return Map.of("skillName", s.name(), "skillMainFile", s.path(), "match", "skill_folder");
            }
        }
        return null;
    }

    private static String comparablePath(String p) {
        var normalized = Path.of(p).normalize().toString();
        return System.getProperty("os.name", "").toLowerCase().contains("win")
            ? normalized.toLowerCase()
            : normalized;
    }

    /**
     * JDK16 record：调试日志配置选项。
     */
    public record AgentDebugLogOptions(
        String modelName,
        int maxSteps,
        String logPath
    ) {
        public AgentDebugLogOptions(String modelName, int maxSteps) {
            this(modelName, maxSteps, null);
        }
    }

    private static class DebugLogState {
        final AgentDebugLog.AgentJsonlDebugWriter writer;
        String runId = "";
        long runStartedAt = 0;
        int currentStep = 0;
        int messagesBaselineAtStep = 0;
        long modelRequestStartedAt = 0;
        final Map<String, Long> toolInvocationStartedAt = new HashMap<>();
        boolean skillsCatalogEmitted = false;

        DebugLogState(AgentDebugLog.AgentJsonlDebugWriter writer) {
            this.writer = writer;
        }

        void emit(String event, String msg, String level, Map<String, Object> data) {
            emit(event, msg, level, data, Map.of());
        }

        void emit(String event, String msg, String level, Map<String, Object> data, Map<String, Object> extra) {
            var record = new LinkedHashMap<String, Object>();
            record.put("runId", runId);
            record.put("component", "agent");
            record.put("level", level);
            record.put("event", event);
            record.put("hook", event);
            record.put("step", currentStep);
            record.put("msg", msg);
            record.put("data", data);
            record.put("sinceRunStartMs", runStartedAt > 0 ? System.currentTimeMillis() - runStartedAt : null);
            record.putAll(extra);
            writer.emit(record);
        }
    }
}