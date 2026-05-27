package com.aether.coding.permissions;

import com.aether.agent.AgentMiddleware;
import com.aether.foundation.messages.Content;

import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

public final class CodingApprovalMiddleware {

    private CodingApprovalMiddleware() {}

    public static final List<String> CODING_TOOLS_REQUIRING_APPROVAL = List.of(
        "bash", "write_file", "str_replace", "apply_patch", "mkdir", "move_path"
    );

    public static AgentMiddleware create(CodingApprovalOptions options) {
        var loadAllowList = options.approvalPersistence() != null
            ? options.approvalPersistence()
            : ApprovalPersistence.fileBased(java.nio.file.Path.of(
                System.getProperty("user.home"), ".aether"));

        return new AgentMiddleware() {
            @Override
            public CompletableFuture<BeforeToolUseResult> beforeToolUse(
                AgentContextView ctx, Content.ToolUseContent toolUse
            ) {
                if (!CODING_TOOLS_REQUIRING_APPROVAL.contains(toolUse.name())) {
                    return CompletableFuture.completedFuture(null);
                }

                return loadAllowList.loadAllowList(options.cwd())
                    .thenCompose(allowed -> {
                        if (allowed.contains(toolUse.name())) {
                            return CompletableFuture.completedFuture(null);
                        }
                        return options.askUser().askUser(toolUse)
                            .thenCompose(decision -> {
                                if (decision == ApprovalDecision.DENY) {
                                    return CompletableFuture.completedFuture(
                                        new BeforeToolUseResult.Skip(
                                            "User denied execution of tool: " + toolUse.name()
                                            + ". You must either find an alternative approach"
                                            + " or ask the user for clarification."
                                        ));
                                }
                                if (decision == ApprovalDecision.ALLOW_ALWAYS_PROJECT) {
                                    return loadAllowList.persistAllowedTool(options.cwd(), toolUse.name())
                                        .thenApply(v -> null);
                                }
                                return CompletableFuture.completedFuture(null);
                            });
                    });
            }
        };
    }

    public record CodingApprovalOptions(
        String cwd,
        ApprovalPersistence approvalPersistence,
        ApprovalManager askUser
    ) {}
}