package com.helixent;

import com.helixent.agent.AgentDebugLog;
import com.helixent.agent.AgentEvent;
import com.helixent.cli.io.ConsoleCharset;
import com.helixent.coding.agents.LeadAgent;
import com.helixent.coding.permissions.ApprovalDecision;
import com.helixent.coding.permissions.ApprovalPersistence;
import com.helixent.coding.tools.AskUserQuestionManager;
import com.helixent.community.openai.OpenAIModelProvider;
import com.helixent.foundation.messages.Content;
import com.helixent.foundation.messages.Message;
import com.helixent.foundation.models.Model;

import java.io.BufferedReader;
import java.io.FileDescriptor;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * Helixent 应用入口 — 唯一的组装点 (Composition Root)。
 *
 * <h3>启动方式</h3>
 * <pre>.\gradlew.bat run</pre>
 *
 * <h3>环境变量</h3>
 * <ul>
 *   <li>{@code ARK_BASE_URL} — API 地址</li>
 *   <li>{@code ARK_API_KEY} — API 密钥</li>
 *   <li>{@code HELIXENT_MODEL} — 模型名称（默认 deepseek-v4-flash）</li>
 *   <li>{@code HELIXENT_DEBUG_LOG} — 未设置时默认写入 {@code %USERPROFILE%\.helixent\agent-debug.jsonl}；
 *       设为 {@code 1}/{@code true} 表示当前工作目录下的 {@code agent-debug.jsonl}；其它非空值为路径；
 *       {@code 0}/{@code false}/{@code off} 关闭</li>
 *   <li>{@code HELIXENT_CONSOLE_CHARSET} — 控制台编码（可选，例如 UTF-8 的 Windows Terminal）</li>
 * </ul>
 *
 * <p>本入口为简易行模式：无审批 UI，危险工具在通过 allow-list 校验后仍会在未列入名单时
 * 以 {@link ApprovalDecision#ALLOW_ONCE} 自动放行一次，避免 stdin 单线程死锁并保证调试日志能写完。</p>
 */
public class Main {

    public static void main(String[] args) {
        var env = loadEnvFile(Path.of(".env"));
        syncEnvToProperties(env);
        installConsoleStreams();

        var baseUrl = firstNonBlank(System.getenv("ARK_BASE_URL"), env.get("ARK_BASE_URL"));
        var apiKey = firstNonBlank(System.getenv("ARK_API_KEY"), env.get("ARK_API_KEY"));
        var modelName = firstNonBlank(System.getenv("HELIXENT_MODEL"), env.get("HELIXENT_MODEL"), "deepseek-v4-flash");

        if (baseUrl == null || apiKey == null) {
            System.out.println("ARK_BASE_URL and ARK_API_KEY must be set.");
            return;
        }

        var provider = new OpenAIModelProvider(baseUrl, apiKey);
        var model = new Model(modelName, provider);

        var cwd = System.getProperty("user.dir");
        var approvalPersistence = ApprovalPersistence.fileBased(
            Path.of(System.getProperty("user.home"), ".helixent"));

        var askUserQuestionManager = new AskUserQuestionManager();

        var debugLogPath = resolveDebugLogPath(env);

        var options = new LeadAgent.CodingAgentOptions(
            model,
            cwd,
            null,
            toolUse -> CompletableFuture.completedFuture(ApprovalDecision.ALLOW_ONCE),
            params -> askUserQuestionManager.askUserQuestion(params),
            approvalPersistence,
            debugLogPath
        );

        var agent = LeadAgent.createCodingAgent(options);

        System.out.println("Helixent ready. Type /exit to quit.");
        System.out.println();

        var stdin = getStdin();
        var consoleCs = ConsoleCharset.forConsoleIo();
        try (var reader = new BufferedReader(new InputStreamReader(stdin, consoleCs))) {
            while (true) {
                System.out.print("> ");
                System.out.flush();
                var line = reader.readLine();
                if (line == null) break;

                var trimmed = line.trim();
                if (trimmed.isEmpty()) continue;
                if ("/exit".equals(trimmed) || "/quit".equals(trimmed)) break;

                var userMsg = new Message.UserMessage(List.of(new Content.TextContent(trimmed)));
                agent.stream(userMsg)
                    .doOnNext(event -> {
                        if (event instanceof AgentEvent.MessageEvent me) {
                            var msg = me.message();
                            if (msg instanceof Message.AssistantMessage am) {
                                for (var c : am.content()) {
                                    if (c instanceof Content.TextContent tc) {
                                        System.out.println(tc.text());
                                    }
                                }
                            }
                        }
                    })
                    .doOnError(e -> System.err.println("Error: " + e.getMessage()))
                    .blockLast();
            }
        } catch (Exception e) {
            System.err.println("Error: " + e.getMessage());
        }

        System.out.println("Goodbye.");
    }

    static InputStream getStdin() {
        try {
            var ch = System.inheritedChannel();
            if (ch instanceof ReadableByteChannel rbc) {
                return Channels.newInputStream(rbc);
            }
        } catch (IOException ignored) {
        }
        return System.in;
    }

    static void syncEnvToProperties(Map<String, String> env) {
        var consoleCharset = env.get("HELIXENT_CONSOLE_CHARSET");
        if (consoleCharset != null && !consoleCharset.isBlank()
            && (System.getenv("HELIXENT_CONSOLE_CHARSET") == null
                || System.getenv("HELIXENT_CONSOLE_CHARSET").isBlank())) {
            System.setProperty("helixent.console.charset", consoleCharset);
        }
    }

    static void installConsoleStreams() {
        try {
            var cs = ConsoleCharset.forConsoleIo();
            System.err.println("[DEBUG] ConsoleCharset.forConsoleIo() = " + cs.name());
            System.setOut(new PrintStream(new FileOutputStream(FileDescriptor.out), true, cs));
            System.setErr(new PrintStream(new FileOutputStream(FileDescriptor.err), true, cs));
        } catch (Exception ignored) {
        }
    }

    static String resolveDebugLogPath(Map<String, String> env) {
        var raw = firstNonBlank(System.getenv("HELIXENT_DEBUG_LOG"), env.get("HELIXENT_DEBUG_LOG"));
        if (raw != null && isDebugLogDisabled(raw.toString())) {
            return null;
        }
        if (raw == null || raw.toString().isBlank()) {
            var dir = Path.of(System.getProperty("user.home"), ".helixent");
            try {
                Files.createDirectories(dir);
                return dir.resolve("agent-debug.jsonl").toAbsolutePath().normalize().toString();
            } catch (IOException e) {
                System.err.println("Could not create log directory " + dir + ": " + e.getMessage());
                return null;
            }
        }
        return AgentDebugLog.parseHelixentDebugLogEnv(raw.toString());
    }

    static boolean isDebugLogDisabled(String raw) {
        var t = raw.trim();
        return "0".equals(t) || "false".equalsIgnoreCase(t) || "off".equalsIgnoreCase(t) || "no".equalsIgnoreCase(t);
    }

    static Map<String, String> loadEnvFile(Path path) {
        var map = new HashMap<String, String>();
        try {
            for (var line : Files.readAllLines(path, StandardCharsets.UTF_8)) {
                line = line.strip();
                if (line.isEmpty() || line.startsWith("#")) continue;
                var eq = line.indexOf('=');
                if (eq < 1) continue;
                var key = line.substring(0, eq).strip();
                var val = line.substring(eq + 1).strip();
                if ((val.startsWith("\"") && val.endsWith("\"")) || (val.startsWith("'") && val.endsWith("'"))) {
                    val = val.substring(1, val.length() - 1);
                }
                map.put(key.toUpperCase(Locale.ROOT), val);
            }
        } catch (Exception ignored) {
        }
        return map;
    }

    @SafeVarargs
    static <T> T firstNonBlank(T... values) {
        for (var v : values) {
            if (v != null && !v.toString().isBlank()) return v;
        }
        return null;
    }
}