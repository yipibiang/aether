package com.aether.coding.agents;

import com.aether.agent.Agent;
import com.aether.agent.AgentDebugLogMiddleware;
import com.aether.agent.AgentMiddleware;
import com.aether.agent.ReActAgent;
import com.aether.agent.SkillsMiddleware;
import com.aether.agent.todos.TodoSystem;
import com.aether.coding.permissions.ApprovalDecision;
import com.aether.coding.permissions.ApprovalManager;
import com.aether.coding.permissions.ApprovalPersistence;
import com.aether.coding.permissions.CodingApprovalMiddleware;
import com.aether.coding.tools.ApplyPatchTool;
import com.aether.coding.tools.AskUserQuestionManager;
import com.aether.coding.tools.AskUserQuestionTool;
import com.aether.coding.tools.BashTool;
import com.aether.coding.tools.FileInfoTool;
import com.aether.coding.tools.GlobSearchTool;
import com.aether.coding.tools.GrepSearchTool;
import com.aether.coding.tools.ListFilesTool;
import com.aether.coding.tools.MkdirTool;
import com.aether.coding.tools.MovePathTool;
import com.aether.coding.tools.ReadFileTool;
import com.aether.coding.tools.StrReplaceTool;
import com.aether.coding.tools.WriteFileTool;
import com.aether.foundation.messages.Content;
import com.aether.foundation.messages.Message;
import com.aether.foundation.models.Model;
import com.aether.foundation.tools.Tool;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;

/**
 * 编码 Agent 工厂 — 创建工具、中间件、提示词和 Agent 实例。
 *
 * <h3>独立工厂方法</h3>
 * 每个方法可独立调用和测试，无需完整组装：
 * <ul>
 *   <li>{@link #createTools()} — 创建所有编码工具列表</li>
 *   <li>{@link #createMiddlewares} — 创建中间件链</li>
 *   <li>{@link #createPrompt} — 生成系统提示词</li>
 *   <li>{@link #loadAgentsFile} — 加载 AGENTS.md</li>
 *   <li>{@link #createCodingAgent} — 完整组装</li>
 * </ul>
 */
public final class LeadAgent {

    private LeadAgent() {}

    /**
     * 创建所有编码工具（不含 Todo 和 AskUserQuestion）。
     * 每个工具可独立 new 和测试。
     */
    public static List<Tool> createTools() {
        var tools = new ArrayList<Tool>();
        tools.add(new BashTool());
        tools.add(new FileInfoTool());
        tools.add(new ListFilesTool());
        tools.add(new GlobSearchTool());
        tools.add(new GrepSearchTool());
        tools.add(new MkdirTool());
        tools.add(new MovePathTool());
        tools.add(new ReadFileTool());
        tools.add(new WriteFileTool());
        tools.add(new StrReplaceTool());
        tools.add(new ApplyPatchTool());
        return tools;
    }

    /**
     * 创建中间件链。
     *
     * @param modelName 模型名称（用于调试日志）
     * @param skillsDirs 技能目录列表
     * @param askUser 用户审批回调（null 则跳过审批中间件）
     * @param approvalPersistence 审批持久化
     * @param cwd 工作目录
     */
    public static List<AgentMiddleware> createMiddlewares(
        String modelName,
        List<String> skillsDirs,
        Function<Content.ToolUseContent, CompletableFuture<ApprovalDecision>> askUser,
        ApprovalPersistence approvalPersistence,
        String cwd,
        String debugLogPath
    ) {
        var todoSystemResult = TodoSystem.create();
        var todoMiddleware = todoSystemResult.middleware();

        var middlewares = new ArrayList<AgentMiddleware>();
        middlewares.add(AgentDebugLogMiddleware.create(
            new AgentDebugLogMiddleware.AgentDebugLogOptions(modelName, 100, debugLogPath)));
        middlewares.add(new SkillsMiddleware(skillsDirs));
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

    /**
     * 生成系统提示词。
     */
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

    /**
     * 加载工作目录下的 AGENTS.md 文件。
     *
     * @return 包含 AGENTS.md 内容的消息列表，文件不存在时返回空列表
     */
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

    /**
     * 完整组装编码 Agent。
     */
    public static Agent createCodingAgent(CodingAgentOptions options) {
        var model = options.model();
        var cwd = options.cwd() != null ? options.cwd() : System.getProperty("user.dir");
        var skillsDirs = options.skillsDirs() != null ? options.skillsDirs()
            : List.of(Path.of(cwd, ".agents", "skills").toString());
        var askUser = options.askUser();
        var askUserQuestion = options.askUserQuestion();
        var approvalPersistence = options.approvalPersistence();

        var messages = loadAgentsFile(cwd);

        var todoSystemResult = TodoSystem.create();
        var todoTool = todoSystemResult.tool();

        var tools = createTools();
        tools.add(todoTool);

        if (askUserQuestion != null) {
            var askUserQuestionTool = new AskUserQuestionTool(new AskUserQuestionManager() {
                @Override
                public CompletableFuture<AskUserQuestionManager.AskUserQuestionResult> askUserQuestion(
                    AskUserQuestionManager.AskUserQuestionParameters params
                ) {
                    return askUserQuestion.apply(params);
                }
            });
            tools.add(askUserQuestionTool);
        }

        var middlewares = createMiddlewares(model.name(), skillsDirs, askUser, approvalPersistence, cwd, options.debugLogPath());
        var prompt = createPrompt(cwd);

        return new ReActAgent("aether", model, prompt, messages, tools, middlewares, 100);
    }

    public record CodingAgentOptions(
        Model model,
        String cwd,
        List<String> skillsDirs,
        Function<Content.ToolUseContent, CompletableFuture<ApprovalDecision>> askUser,
        Function<AskUserQuestionManager.AskUserQuestionParameters,
            CompletableFuture<AskUserQuestionManager.AskUserQuestionResult>> askUserQuestion,
        ApprovalPersistence approvalPersistence,
        String debugLogPath
    ) {
        public CodingAgentOptions {
            if (model == null) throw new IllegalArgumentException("model is required");
        }

        public CodingAgentOptions(Model model) {
            this(model, null, null, null, null, null, null);
        }
    }
}