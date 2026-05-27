package com.helixent.coding.permissions;

import com.helixent.foundation.messages.Content;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

public class ApprovalManager {

    private static final int MAX_QUEUE_SIZE = 20;

    private final List<ApprovalRequest> queue = new ArrayList<>();
    private ApprovalRequest currentRequest;
    private Consumer<ApprovalRequest> subscriber;

    public CompletableFuture<ApprovalDecision> askUser(Content.ToolUseContent toolUse) {
        var future = new CompletableFuture<ApprovalDecision>();
        if (queue.size() >= MAX_QUEUE_SIZE) {
            System.err.println("[ApprovalManager] Queue overflow. Denying tool " + toolUse.name() + ".");
            future.complete(ApprovalDecision.DENY);
            return future;
        }
        queue.add(new ApprovalRequest(toolUse, future));
        processQueue();
        return future;
    }

    public void respond(ApprovalDecision decision) {
        if (currentRequest == null) return;
        currentRequest.resolve().complete(decision);
        currentRequest = null;
        processQueue();
    }

    public void subscribe(Consumer<ApprovalRequest> callback) {
        this.subscriber = callback;
        processQueue();
    }

    public void unsubscribe() {
        this.subscriber = null;
    }

    public ApprovalRequest currentRequest() {
        return currentRequest;
    }

    private void processQueue() {
        if (currentRequest != null || queue.isEmpty()) {
            if (queue.isEmpty() && currentRequest == null && subscriber != null) {
                subscriber.accept(null);
            }
            return;
        }

        currentRequest = queue.remove(0);
        if (subscriber != null) {
            subscriber.accept(currentRequest);
        }
    }

    /**
     * JDK16 record：审批请求（工具调用 + 异步回调）。
     */
    public record ApprovalRequest(
        Content.ToolUseContent toolUse,
        CompletableFuture<ApprovalDecision> resolve
    ) {}
}