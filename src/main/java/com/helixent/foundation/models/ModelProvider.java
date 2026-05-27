package com.helixent.foundation.models;

import com.helixent.foundation.messages.Message;
import reactor.core.publisher.Flux;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

public interface ModelProvider {

    record InvokeParams(
        String model,
        List<Message> messages,
        List<com.helixent.foundation.tools.Tool> tools,
        Map<String, Object> options,
        Object signal
    ) {}

    CompletableFuture<Message.AssistantMessage> invoke(InvokeParams params);

    Flux<Message.AssistantMessage> stream(InvokeParams params);
}