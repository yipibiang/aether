package com.helixent.coding.tools;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

public class AskUserQuestionManager {

    private static final int MAX_QUEUE_SIZE = 20;

    private final List<AskUserQuestionRequest> queue = new ArrayList<>();
    private AskUserQuestionRequest currentRequest;
    private Consumer<AskUserQuestionRequest> subscriber;

    public CompletableFuture<AskUserQuestionResult> askUserQuestion(AskUserQuestionParameters params) {
        var future = new CompletableFuture<AskUserQuestionResult>();
        if (queue.size() >= MAX_QUEUE_SIZE) {
            System.err.println("[AskUserQuestionManager] Queue overflow; rejecting request.");
            future.completeExceptionally(new RuntimeException("Ask user question queue overflow"));
            return future;
        }
        queue.add(new AskUserQuestionRequest(params, future));
        processQueue();
        return future;
    }

    public void respondWithAnswers(AskUserQuestionResult result) {
        if (currentRequest == null) return;
        currentRequest.resolve().complete(result);
        currentRequest = null;
        processQueue();
    }

    public void subscribe(Consumer<AskUserQuestionRequest> callback) {
        this.subscriber = callback;
        processQueue();
    }

    public void unsubscribe() {
        this.subscriber = null;
    }

    public AskUserQuestionRequest currentRequest() {
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
     * JDK16 record：提问请求（参数 + 异步回调）。
     */
    public record AskUserQuestionRequest(
        AskUserQuestionParameters params,
        CompletableFuture<AskUserQuestionResult> resolve
    ) {}

    /**
     * JDK16 record：单个选项（标签、描述、预览）。
     */
    public record AskUserQuestionOption(
        String label,
        String description,
        String preview
    ) {}

    /**
     * JDK16 record：单个问题（问题文本、标题、选项列表、是否多选）。
     */
    public record AskUserQuestionItem(
        String question,
        String header,
        List<AskUserQuestionOption> options,
        boolean multiSelect
    ) {}

    /**
     * JDK16 record：提问参数（问题列表）。
     */
    public record AskUserQuestionParameters(
        List<AskUserQuestionItem> questions
    ) {}

    /**
     * JDK16 record：单个回答（问题索引、选中的标签列表）。
     */
    public record AskUserQuestionAnswer(
        int questionIndex,
        List<String> selectedLabels
    ) {}

    /**
     * JDK16 record：提问结果（回答列表）。
     */
    public record AskUserQuestionResult(
        List<AskUserQuestionAnswer> answers
    ) {}

    public static void validateResultAgainstParams(
        AskUserQuestionParameters params, AskUserQuestionResult result
    ) {
        if (result.answers().size() != params.questions().size()) {
            throw new IllegalArgumentException(
                "ask_user_question: expected " + params.questions().size()
                + " answers, got " + result.answers().size());
        }
        var byIndex = new java.util.HashMap<Integer, AskUserQuestionAnswer>();
        for (var a : result.answers()) {
            byIndex.put(a.questionIndex(), a);
        }
        for (int i = 0; i < params.questions().size(); i++) {
            var q = params.questions().get(i);
            var a = byIndex.get(i);
            if (a == null) {
                throw new IllegalArgumentException(
                    "ask_user_question: missing answer for question_index " + i);
            }
            var labels = new java.util.HashSet<String>();
            for (var o : q.options()) {
                labels.add(o.label());
            }
            for (var l : a.selectedLabels()) {
                if (!labels.contains(l)) {
                    throw new IllegalArgumentException(
                        "ask_user_question: unknown label \"" + l + "\" for question " + i);
                }
            }
            if (q.multiSelect()) {
                if (a.selectedLabels().isEmpty()) {
                    throw new IllegalArgumentException(
                        "ask_user_question: multi-select question " + i + " requires at least one selection");
                }
            } else if (a.selectedLabels().size() != 1) {
                throw new IllegalArgumentException(
                    "ask_user_question: single-select question " + i + " requires exactly one selection");
            }
        }
    }
}