package com.aether.coding.tools;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.aether.foundation.tools.Tool;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

public class AskUserQuestionTool implements Tool {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final AskUserQuestionManager manager;

    public AskUserQuestionTool(AskUserQuestionManager manager) {
        this.manager = manager;
    }

    @Override
    public String name() { return "ask_user_question"; }

    @Override
    public String description() {
        return "Ask the user one or more independent questions with fixed choices. "
            + "Prefer this over free-form questions when options are clear. "
            + "Questions are parallel (no dependency between them). "
            + "You may send 1-4 questions in one call. "
            + "For each question set multiSelect true only when multiple answers make sense.";
    }

    @Override
    public Map<String, Object> toJsonSchema() {
        return Map.of(
            "type", "object",
            "properties", Map.of(
                "questions", Map.of(
                    "type", "array",
                    "minItems", 1,
                    "maxItems", 4,
                    "items", Map.of(
                        "type", "object",
                        "properties", Map.of(
                            "question", Map.of("type", "string",
                                "description", "Full question text; be specific and end with a question mark where appropriate."),
                            "header", Map.of("type", "string",
                                "description", "Very short tab/tag label (max 12 characters), e.g. Auth, Library."),
                            "options", Map.of(
                                "type", "array",
                                "minItems", 2,
                                "maxItems", 4,
                                "items", Map.of(
                                    "type", "object",
                                    "properties", Map.of(
                                        "label", Map.of("type", "string",
                                            "description", "Short display label for this choice (1-5 words)."),
                                        "description", Map.of("type", "string",
                                            "description", "What this choice means or implies."),
                                        "preview", Map.of("type", "string",
                                            "description", "Optional markdown preview when this option is focused (single-select only).")
                                    ),
                                    "required", List.of("label", "description")
                                )
                            ),
                            "multiSelect", Map.of("type", "boolean",
                                "description", "If true, the user may pick multiple options; if false, exactly one.")
                        ),
                        "required", List.of("question", "header", "options", "multiSelect")
                    )
                )
            ),
            "required", List.of("questions")
        );
    }

    @Override
    public CompletableFuture<Object> invoke(Map<String, Object> input, Object signal) {
        @SuppressWarnings("unchecked")
        var questionsRaw = (List<Map<String, Object>>) input.get("questions");

        if (questionsRaw == null || questionsRaw.isEmpty()) {
            return CompletableFuture.completedFuture(
                ToolResultHelper.errorToolResult("Questions are required", "INVALID_QUESTIONS", Map.of()));
        }

        var items = new ArrayList<AskUserQuestionManager.AskUserQuestionItem>();
        for (var q : questionsRaw) {
            @SuppressWarnings("unchecked")
            var optionsRaw = (List<Map<String, Object>>) q.get("options");
            var options = new ArrayList<AskUserQuestionManager.AskUserQuestionOption>();
            if (optionsRaw != null) {
                for (var o : optionsRaw) {
                    options.add(new AskUserQuestionManager.AskUserQuestionOption(
                        (String) o.get("label"),
                        (String) o.get("description"),
                        (String) o.get("preview")
                    ));
                }
            }
            items.add(new AskUserQuestionManager.AskUserQuestionItem(
                (String) q.get("question"),
                (String) q.get("header"),
                options,
                Boolean.TRUE.equals(q.get("multiSelect"))
            ));
        }

        var params = new AskUserQuestionManager.AskUserQuestionParameters(items);

        return manager.askUserQuestion(params)
            .thenApply(result -> {
                AskUserQuestionManager.validateResultAgainstParams(params, result);
                try {
                    return ToolResultHelper.okToolResult(
                        "User answered " + result.answers().size() + " question(s)",
                        Map.of("answers", MAPPER.convertValue(result, Map.class))
                    );
                } catch (Exception e) {
                    return ToolResultHelper.errorToolResult(
                        "Failed to serialize result: " + e.getMessage(),
                        "SERIALIZATION_ERROR", Map.of());
                }
            });
    }
}