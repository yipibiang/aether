package com.aether.coding.tools;

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

    public record AskUserQuestionRequest(
        AskUserQuestionParameters params,
        CompletableFuture<AskUserQuestionResult> resolve
    ) {}

    public record AskUserQuestionOption(
        String label,
        String description,
        String preview
    ) {}

    public record AskUserQuestionItem(
        String question,
        String header,
        List<AskUserQuestionOption> options,
        boolean multiSelect
    ) {}

    public record AskUserQuestionParameters(
        List<AskUserQuestionItem> questions
    ) {}

    public record AskUserQuestionAnswer(
        int questionIndex,
        List<String> selectedLabels
    ) {}

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