package com.aether.coding.tools;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static org.junit.jupiter.api.Assertions.*;

class AskUserQuestionManagerTest {

    @Test
    void askUserQuestion_returnsResult() throws Exception {
        var mgr = new AskUserQuestionManager();
        var params = new AskUserQuestionManager.AskUserQuestionParameters(List.of(
            new AskUserQuestionManager.AskUserQuestionItem(
                "What color?", "Color",
                List.of(new AskUserQuestionManager.AskUserQuestionOption("red", "Red color", null),
                    new AskUserQuestionManager.AskUserQuestionOption("blue", "Blue color", null)),
                false
            )
        ));

        var future = mgr.askUserQuestion(params);
        assertNotNull(mgr.currentRequest());

        var answer = new AskUserQuestionManager.AskUserQuestionAnswer(0, List.of("red"));
        var result = new AskUserQuestionManager.AskUserQuestionResult(List.of(answer));
        mgr.respondWithAnswers(result);

        var resolved = future.get(5, TimeUnit.SECONDS);
        assertEquals(1, resolved.answers().size());
        assertEquals(0, resolved.answers().get(0).questionIndex());
        assertEquals("red", resolved.answers().get(0).selectedLabels().get(0));
    }

    @Test
    void subscribe_notifiesSubscriber() throws Exception {
        var mgr = new AskUserQuestionManager();
        var params = new AskUserQuestionManager.AskUserQuestionParameters(List.of(
            new AskUserQuestionManager.AskUserQuestionItem(
                "Test?", "Header",
                List.of(new AskUserQuestionManager.AskUserQuestionOption("a", "A", null)),
                false
            )
        ));

        var received = new CompletableFuture<AskUserQuestionManager.AskUserQuestionRequest>();
        mgr.subscribe(req -> {
            if (req != null) received.complete(req);
        });

        mgr.askUserQuestion(params);
        var req = received.get(5, TimeUnit.SECONDS);
        assertNotNull(req);
        assertEquals("Test?", req.params().questions().get(0).question());
    }

    @Test
    void validateResultAgainstParams_validSingleSelect() {
        var params = new AskUserQuestionManager.AskUserQuestionParameters(List.of(
            new AskUserQuestionManager.AskUserQuestionItem(
                "Q", "H",
                List.of(new AskUserQuestionManager.AskUserQuestionOption("a", "A", null)),
                false
            )
        ));
        var result = new AskUserQuestionManager.AskUserQuestionResult(List.of(
            new AskUserQuestionManager.AskUserQuestionAnswer(0, List.of("a"))
        ));
        assertDoesNotThrow(() -> AskUserQuestionManager.validateResultAgainstParams(params, result));
    }

    @Test
    void validateResultAgainstParams_missingAnswerThrows() {
        var params = new AskUserQuestionManager.AskUserQuestionParameters(List.of(
            new AskUserQuestionManager.AskUserQuestionItem(
                "Q", "H",
                List.of(new AskUserQuestionManager.AskUserQuestionOption("a", "A", null)),
                false
            )
        ));
        var result = new AskUserQuestionManager.AskUserQuestionResult(List.of());
        assertThrows(IllegalArgumentException.class,
            () -> AskUserQuestionManager.validateResultAgainstParams(params, result));
    }

    @Test
    void validateResultAgainstParams_unknownLabelThrows() {
        var params = new AskUserQuestionManager.AskUserQuestionParameters(List.of(
            new AskUserQuestionManager.AskUserQuestionItem(
                "Q", "H",
                List.of(new AskUserQuestionManager.AskUserQuestionOption("a", "A", null)),
                false
            )
        ));
        var result = new AskUserQuestionManager.AskUserQuestionResult(List.of(
            new AskUserQuestionManager.AskUserQuestionAnswer(0, List.of("unknown"))
        ));
        assertThrows(IllegalArgumentException.class,
            () -> AskUserQuestionManager.validateResultAgainstParams(params, result));
    }

    @Test
    void validateResultAgainstParams_multiSelectRequiresAtLeastOne() {
        var params = new AskUserQuestionManager.AskUserQuestionParameters(List.of(
            new AskUserQuestionManager.AskUserQuestionItem(
                "Q", "H",
                List.of(new AskUserQuestionManager.AskUserQuestionOption("a", "A", null)),
                true
            )
        ));
        var result = new AskUserQuestionManager.AskUserQuestionResult(List.of(
            new AskUserQuestionManager.AskUserQuestionAnswer(0, List.of())
        ));
        assertThrows(IllegalArgumentException.class,
            () -> AskUserQuestionManager.validateResultAgainstParams(params, result));
    }

    @Test
    void validateResultAgainstParams_singleSelectRequiresExactlyOne() {
        var params = new AskUserQuestionManager.AskUserQuestionParameters(List.of(
            new AskUserQuestionManager.AskUserQuestionItem(
                "Q", "H",
                List.of(new AskUserQuestionManager.AskUserQuestionOption("a", "A", null),
                    new AskUserQuestionManager.AskUserQuestionOption("b", "B", null)),
                false
            )
        ));
        var result = new AskUserQuestionManager.AskUserQuestionResult(List.of(
            new AskUserQuestionManager.AskUserQuestionAnswer(0, List.of("a", "b"))
        ));
        assertThrows(IllegalArgumentException.class,
            () -> AskUserQuestionManager.validateResultAgainstParams(params, result));
    }
}