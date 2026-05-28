package com.aether.coding.agents;

import com.aether.coding.permissions.ApprovalDecision;
import com.aether.coding.permissions.ApprovalManager;
import com.aether.coding.permissions.ApprovalPersistence;
import com.aether.coding.permissions.CodingApprovalMiddleware;
import com.aether.foundation.messages.Content;
import com.aether.foundation.messages.Message;
import com.aether.foundation.models.Model;
import com.aether.foundation.skills.SkillCatalog;
import com.aether.foundation.tools.CompositeToolRegistry;
import com.aether.foundation.tools.Tool;
import com.aether.foundation.tools.ToolRegistry;
import com.aether.runtime.Agent;
import com.aether.runtime.AgentDebugLogMiddleware;
import com.aether.runtime.AgentMiddleware;
import com.aether.runtime.ReActAgent;
import com.aether.skills.FileSystemSkillCatalog;
import com.aether.skills.SkillsMiddleware;
import com.aether.tools.AskUserQuestionManager;
import com.aether.tools.AskUserQuestionToolRegistry;
import com.aether.tools.CodingToolRegistry;
import com.aether.tools.todo.TodoToolRegistry;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;

/**
 * 编码 Agent 组装 — 组合 {@code skills}、{@code tools}、{@code runtime}，不包含具体 tool 实现。
 */
public final class LeadAgent {

    private LeadAgent() {}

    public static List<String> defaultSkillsDirs(String cwd) {
        var home = System.getProperty("user.home");
        return List.of(
            Path.of(cwd, "skills").toString(),
            Path.of(cwd, ".agents", "skills").toString(),
            Path.of(home, ".aether", "skills").toString(),
            Path.of(home, ".agents", "skills").toString()
        );
    }

    public static List<AgentMiddleware> createMiddlewares(
        String modelName,
        SkillCatalog skillCatalog,
        AgentMiddleware todoMiddleware,
        Function<Content.ToolUseContent, CompletableFuture<ApprovalDecision>> askUser,
        ApprovalPersistence approvalPersistence,
        String cwd,
        String debugLogPath
    ) {
        if (todoMiddleware == null) {
            throw new IllegalArgumentException("todoMiddleware is required");
        }
        if (skillCatalog == null) {
            throw new IllegalArgumentException("skillCatalog is required");
        }

        var middlewares = new ArrayList<AgentMiddleware>();
        middlewares.add(AgentDebugLogMiddleware.create(
            new AgentDebugLogMiddleware.AgentDebugLogOptions(modelName, 100, debugLogPath)));
        middlewares.add(new SkillsMiddleware(skillCatalog));
        middlewares.add(todoMiddleware);

        if (askUser != null) {
            var approvalMgr = new ApprovalManager() {
                @Override
                public CompletableFuture<ApprovalDecision> askUser(Content.ToolUseContent toolUse) {
                    return askUser.apply(toolUse);
                }
            };
            middlewares.add(CodingApprovalMiddleware.create(
                new CodingApprovalMiddleware.CodingApprovalOptions(cwd, approvalPersistence, approvalMgr)));
        }

        return middlewares;
    }

    public static String createPrompt(String cwd) {
        return """
            <agent name="Aether" role="leading_agent" description="A coding agent">
            Use the given tools and skills to perform parallel/sequential operations and solve the user's problem in the given working directory.
            </agent>

            <working_directory dir="%s/" />

            <tool_usage>
            - Inspect directories before assuming file paths.
            - Prefer list_files or glob_search to discover files.
            - Prefer grep_search to locate relevant content.
            - Read a file before editing it.
            - Prefer apply_patch for targeted edits.
            - If apply_patch fails, re-read the file and choose a safer edit strategy.
            - Do not repeat the same failing tool call with unchanged invalid input.
            - Use tool result summaries and error codes to decide the next step.
            </tool_usage>

            <notes>
            - Never try to start a local static server. Let the user do it.
            - If the user's input is a simple task or a greeting, you should just respond with a simple answer and then stop.
            </notes>
            """.formatted(cwd);
    }

    public static List<Message> loadAgentsFile(String cwd) {
        var messages = new ArrayList<Message>();
        var agentsFilePath = Path.of(cwd, "AGENTS.md");
        if (Files.exists(agentsFilePath)) {
            try {
                var content = Files.readString(agentsFilePath);
                messages.add(new Message.UserMessage(List.of(
                    new Content.TextContent(
                        "> The `AGENTS.md` file has been automatically loaded. Here is the content:\n\n" + content)
                )));
            } catch (IOException ignored) {
            }
        }
        return messages;
    }

    public static Agent createCodingAgent(CodingAgentOptions options) {
        if (options == null) {
            throw new IllegalArgumentException("options is required");
        }

        var model = options.model();
        var cwd = options.cwd() != null ? options.cwd() : System.getProperty("user.dir");
        var skillsDirs = options.skillsDirs() != null ? options.skillsDirs() : defaultSkillsDirs(cwd);
        var skillCatalog = options.skillCatalog() != null
            ? options.skillCatalog()
            : new FileSystemSkillCatalog(skillsDirs);

        var todoRegistry = new TodoToolRegistry();
        var tools = assembleTools(options, todoRegistry);
        var middlewares = createMiddlewares(
            model.name(),
            skillCatalog,
            todoRegistry.middleware(),
            options.askUser(),
            options.approvalPersistence(),
            cwd,
            options.debugLogPath()
        );
        var prompt = createPrompt(cwd);
        var messages = loadAgentsFile(cwd);

        return new ReActAgent("aether", model, prompt, messages, tools, middlewares, 100);
    }

    static List<Tool> assembleTools(CodingAgentOptions options, TodoToolRegistry todoRegistry) {
        var registries = new ArrayList<ToolRegistry>();
        registries.add(options.toolRegistry() != null ? options.toolRegistry() : new CodingToolRegistry());
        registries.add(todoRegistry);
        if (options.askUserQuestionManager() != null) {
            registries.add(new AskUserQuestionToolRegistry(options.askUserQuestionManager()));
        }
        return new CompositeToolRegistry(registries).tools();
    }

    public record CodingAgentOptions(
        Model model,
        String cwd,
        List<String> skillsDirs,
        SkillCatalog skillCatalog,
        ToolRegistry toolRegistry,
        Function<Content.ToolUseContent, CompletableFuture<ApprovalDecision>> askUser,
        AskUserQuestionManager askUserQuestionManager,
        ApprovalPersistence approvalPersistence,
        String debugLogPath
    ) {
        public CodingAgentOptions {
            if (model == null) {
                throw new IllegalArgumentException("model is required");
            }
        }

        public CodingAgentOptions(Model model) {
            this(model, null, null, null, null, null, null, null, null);
        }
    }
}
