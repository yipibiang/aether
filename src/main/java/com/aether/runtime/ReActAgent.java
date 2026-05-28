package com.aether.runtime;

import com.aether.foundation.skills.SkillDescriptor;
import com.aether.foundation.messages.Content;
import com.aether.foundation.messages.Message;
import com.aether.foundation.models.Model;
import com.aether.foundation.models.ModelContext;
import com.aether.foundation.tools.Tool;
import reactor.core.publisher.Flux;
import reactor.core.publisher.FluxSink;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

/**
 * ReAct 风格 Agent 循环 — 核心引擎。
 *
 * <h3>循环流程 (Think → Act → Observe)</h3>
 * <ol>
 *   <li><b>Think</b>: 调用模型，流式获取响应</li>
 *   <li><b>Act</b>: 如果响应包含工具调用，并行执行所有工具</li>
 *   <li><b>Observe</b>: 将工具结果追加到消息历史，回到 Think</li>
 * </ol>
 * 循环终止条件：模型返回纯文本（无工具调用）或达到 maxSteps。
 */
public class ReActAgent implements Agent, AgentMiddleware.AgentContextView {

    private final String name;
    private final Model model;
    private final String prompt;
    private final List<Message> messages;
    private final List<Tool> tools;
    private final List<AgentMiddleware> middlewares;
    private final int maxSteps;
    private List<SkillDescriptor> skills = List.of();
    private String requestedSkillName;

    private final AtomicBoolean streaming = new AtomicBoolean(false);
    private final AtomicReference<Object> abortSignal = new AtomicReference<>();

    public ReActAgent(
        String name,
        Model model,
        String prompt,
        List<Message> messages,
        List<Tool> tools,
        List<AgentMiddleware> middlewares,
        int maxSteps
    ) {
        this.name = name;
        this.model = model;
        this.prompt = prompt;
        this.messages = messages != null ? new ArrayList<>(messages) : new ArrayList<>();
        this.tools = tools != null ? tools : List.of();
        this.middlewares = middlewares != null ? middlewares : List.of();
        this.maxSteps = maxSteps;
    }

    @Override
    public String prompt() { return prompt; }

    @Override
    public List<Message> messages() { return messages; }

    @Override
    public List<Tool> tools() { return tools; }

    @Override
    public List<SkillDescriptor> skills() { return skills; }

    @Override
    public String requestedSkillName() { return requestedSkillName; }

    public void setSkills(List<SkillDescriptor> skills) { this.skills = skills; }

    public void setRequestedSkillName(String requestedSkillName) { this.requestedSkillName = requestedSkillName; }

    @Override
    public String name() { return name; }

    @Override
    public Model model() { return model; }

    @Override
    public boolean isStreaming() { return streaming.get(); }

    @Override
    public void clearMessages() { messages.clear(); }

    @Override
    public void abort() {
        abortSignal.set(new Object());
    }

    @Override
    public Flux<AgentEvent> stream(Message.UserMessage userMessage) {
        return Flux.create(sink -> {
            if (!streaming.compareAndSet(false, true)) {
                sink.error(new IllegalStateException("Agent is already streaming"));
                return;
            }

            abortSignal.set(new Object());
            messages.add(userMessage);

            runBeforeAgentRun()
                .thenCompose(v -> runLoop(sink))
                .whenComplete((v, error) -> {
                    streaming.set(false);
                    abortSignal.set(null);
                    runFinalizeAgentStream(error)
                        .thenRun(() -> {
                            if (error != null) {
                                sink.error(error);
                            } else {
                                sink.complete();
                            }
                        });
                });
        });
    }

    private CompletableFuture<Void> runLoop(FluxSink<AgentEvent> sink) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                for (int step = 1; step <= maxSteps; step++) {
                    checkAborted();

                    runBeforeAgentStep(step).join();

                    var assistantMessage = think(sink).join();
                    runAfterModel(assistantMessage).join();
                    sink.next(new AgentEvent.MessageEvent(assistantMessage));

                    var toolUses = extractToolUses(assistantMessage);
                    if (toolUses.isEmpty()) {
                        runAfterAgentRun().join();
                        return null;
                    }

                    act(sink, toolUses).join();
                    runAfterAgentStep(step).join();
                }
                throw new RuntimeException("Maximum number of steps reached");
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });
    }

    private CompletableFuture<Message.AssistantMessage> think(FluxSink<AgentEvent> sink) {
        var modelContext = new ModelContext(prompt, List.copyOf(messages), tools, abortSignal.get());
        return runBeforeModel(modelContext)
            .thenCompose(updatedCtx -> {
                var ctx = updatedCtx != null ? updatedCtx : modelContext;
                var latestRef = new AtomicReference<Message.AssistantMessage>();

                return model.stream(ctx)
                    .doOnNext(snapshot -> {
                        latestRef.set(snapshot);
                        if (Boolean.TRUE.equals(snapshot.streaming())) {
                            sink.next(deriveProgress(snapshot));
                        }
                    })
                    .last()
                    .map(latest -> {
                        var finalMessage = new Message.AssistantMessage(
                            latest.content(),
                            latest.usage(),
                            null
                        );
                        messages.add(finalMessage);
                        return finalMessage;
                    })
                    .switchIfEmpty(Mono.error(
                        new RuntimeException("Model stream ended without producing a message")
                    ))
                    .toFuture();
            });
    }

    private AgentEvent deriveProgress(Message.AssistantMessage snapshot) {
        var toolUses = snapshot.content().stream()
            .filter(c -> c instanceof Content.ToolUseContent)
            .map(c -> (Content.ToolUseContent) c)
            .toList();

        if (toolUses.isEmpty()) {
            return new AgentEvent.ProgressThinking();
        }
        var last = toolUses.get(toolUses.size() - 1);
        return new AgentEvent.ProgressTool(last.name(), last.input());
    }

    private List<Content.ToolUseContent> extractToolUses(Message.AssistantMessage message) {
        return message.content().stream()
            .filter(c -> c instanceof Content.ToolUseContent)
            .map(c -> (Content.ToolUseContent) c)
            .toList();
    }

    private CompletableFuture<Void> act(FluxSink<AgentEvent> sink, List<Content.ToolUseContent> toolUses) {
        var signal = abortSignal.get();
        var pending = new ArrayList<CompletableFuture<ToolResult>>();

        for (int i = 0; i < toolUses.size(); i++) {
            var toolUse = toolUses.get(i);
            var index = i;
            pending.add(
                runBeforeToolUse(toolUse).thenCompose(beforeResult -> {
                    if (beforeResult instanceof AgentMiddleware.BeforeToolUseResult.Skip skip) {
                        return CompletableFuture.completedFuture(
                            new ToolResult(index, toolUse.id(), toolUse.name(), skip.result())
                        );
                    }
                    var tool = tools.stream()
                        .filter(t -> t.name().equals(toolUse.name()))
                        .findFirst()
                        .orElse(null);
                    if (tool == null) {
                        return CompletableFuture.completedFuture(
                            new ToolResult(index, toolUse.id(), toolUse.name(),
                                "Error: Tool " + toolUse.name() + " not found")
                        );
                    }
                    return tool.invoke(toolUse.input(), signal)
                        .thenCompose(result ->
                            runAfterToolUse(toolUse, result)
                                .thenApply(v -> new ToolResult(index, toolUse.id(), toolUse.name(), result))
                        );
                }).exceptionally(error -> {
                    var msg = error.getMessage() != null ? error.getMessage() : String.valueOf(error);
                    return new ToolResult(index, toolUse.id(), toolUse.name(), "Error: " + msg);
                })
            );
        }

        return CompletableFuture.allOf(pending.toArray(new CompletableFuture[0]))
            .thenAccept(v -> {
                var results = pending.stream()
                    .map(CompletableFuture::join)
                    .sorted((a, b) -> Integer.compare(a.index, b.index))
                    .toList();

                for (var r : results) {
                    var toolMessage = new Message.ToolMessage(List.of(
                        new Content.ToolResultContent(
                            r.toolUseId,
                            ToolResultRuntime.formatToolResultForMessage(r.toolName, r.result)
                        )
                    ));
                    messages.add(toolMessage);
                    sink.next(new AgentEvent.MessageEvent(toolMessage));
                }
            });
    }

    private void checkAborted() {
        if (abortSignal.get() == null) {
            throw new RuntimeException("Aborted");
        }
    }

    private CompletableFuture<ModelContext> runBeforeModel(ModelContext ctx) {
        return runMiddlewareChain(ctx, (mw, c) -> mw.beforeModel(c, this));
    }

    private CompletableFuture<Void> runAfterModel(Message.AssistantMessage msg) {
        return runMiddlewareChainVoid(mw -> mw.afterModel(this, msg));
    }

    private CompletableFuture<Void> runBeforeAgentRun() {
        return runAgentContextMiddlewareChain(mw -> mw.beforeAgentRun(this));
    }

    private CompletableFuture<Void> runAfterAgentRun() {
        return runAgentContextMiddlewareChain(mw -> mw.afterAgentRun(this));
    }

    private CompletableFuture<Void> runBeforeAgentStep(int step) {
        return runAgentContextMiddlewareChain(mw -> mw.beforeAgentStep(this, step));
    }

    private CompletableFuture<Void> runAfterAgentStep(int step) {
        return runAgentContextMiddlewareChain(mw -> mw.afterAgentStep(this, step));
    }

    private CompletableFuture<AgentMiddleware.BeforeToolUseResult> runBeforeToolUse(Content.ToolUseContent toolUse) {
        return runMiddlewareChainForToolUse(toolUse);
    }

    private CompletableFuture<Void> runAfterToolUse(Content.ToolUseContent toolUse, Object result) {
        return runMiddlewareChainVoid(mw -> mw.afterToolUse(this, toolUse, result));
    }

    private CompletableFuture<Void> runFinalizeAgentStream(Throwable error) {
        return runMiddlewareChainVoid(mw -> mw.finalizeAgentStream(this, error));
    }

    @FunctionalInterface
    private interface MiddlewareCall {
        CompletableFuture<?> call(AgentMiddleware mw);
    }

    private CompletableFuture<Void> runMiddlewareChainVoid(MiddlewareCall call) {
        CompletableFuture<Void> chain = CompletableFuture.completedFuture(null);
        for (var mw : middlewares) {
            chain = chain.thenCompose(v -> call.call(mw).thenApply(r -> null));
        }
        return chain;
    }

    @FunctionalInterface
    private interface AgentContextMiddlewareCall {
        CompletableFuture<Map<String, Object>> call(AgentMiddleware mw);
    }

    private CompletableFuture<Void> runAgentContextMiddlewareChain(AgentContextMiddlewareCall call) {
        CompletableFuture<Void> chain = CompletableFuture.completedFuture(null);
        for (var mw : middlewares) {
            chain = chain.thenCompose(v -> call.call(mw).thenAccept(this::applyAgentContextUpdates));
        }
        return chain;
    }

    @SuppressWarnings("unchecked")
    private void applyAgentContextUpdates(Map<String, Object> updates) {
        if (updates == null || updates.isEmpty()) {
            return;
        }
        if (updates.containsKey("skills")) {
            var raw = updates.get("skills");
            if (raw instanceof List<?> list) {
                setSkills((List<SkillDescriptor>) list);
            }
        }
        if (updates.containsKey("requestedSkillName")) {
            var raw = updates.get("requestedSkillName");
            if (raw instanceof String name) {
                setRequestedSkillName(name);
            } else if (raw == null) {
                setRequestedSkillName(null);
            }
        }
    }

    private CompletableFuture<ModelContext> runMiddlewareChain(
        ModelContext initial,
        java.util.function.BiFunction<AgentMiddleware, ModelContext, CompletableFuture<Map<String, Object>>> call
    ) {
        var ctxRef = new AtomicReference<>(initial);
        CompletableFuture<Void> chain = CompletableFuture.completedFuture(null);
        for (var mw : middlewares) {
            chain = chain.thenCompose(v ->
                call.apply(mw, ctxRef.get()).thenAccept(updates -> {
                    if (updates != null) {
                        var current = ctxRef.get();
                        var newPrompt = updates.containsKey("prompt")
                            ? (String) updates.get("prompt") : current.prompt();
                        @SuppressWarnings("unchecked")
                        var newMessages = updates.containsKey("messages")
                            ? (List<Message>) updates.get("messages") : current.messages();
                        @SuppressWarnings("unchecked")
                        var newTools = updates.containsKey("tools")
                            ? (List<Tool>) updates.get("tools") : current.tools();
                        ctxRef.set(new ModelContext(newPrompt, newMessages, newTools, current.signal()));
                    }
                })
            );
        }
        return chain.thenApply(v -> ctxRef.get());
    }

    private CompletableFuture<AgentMiddleware.BeforeToolUseResult> runMiddlewareChainForToolUse(
        Content.ToolUseContent toolUse
    ) {
        var resultRef = new AtomicReference<AgentMiddleware.BeforeToolUseResult>(null);
        CompletableFuture<Void> chain = CompletableFuture.completedFuture(null);
        for (var mw : middlewares) {
            chain = chain.thenCompose(v -> {
                if (resultRef.get() != null) {
                    return CompletableFuture.completedFuture(null);
                }
                return mw.beforeToolUse(this, toolUse).thenAccept(r -> {
                    if (r instanceof AgentMiddleware.BeforeToolUseResult.Skip) {
                        resultRef.set(r);
                    }
                });
            });
        }
        return chain.thenApply(v -> resultRef.get());
    }

    private record ToolResult(int index, String toolUseId, String toolName, Object result) {}
}