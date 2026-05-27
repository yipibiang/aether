package com.helixent.foundation.models;

import com.helixent.foundation.messages.Message;
import reactor.core.publisher.Flux;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

public class Model {

    private final String name;
    private final ModelProvider provider;
    private final Map<String, Object> options;

    public Model(String name, ModelProvider provider, Map<String, Object> options) {
        this.name = name;
        this.provider = provider;
        this.options = options;
    }

    public Model(String name, ModelProvider provider) {
        this(name, provider, null);
    }

    public String name() {
        return name;
    }

    public ModelProvider provider() {
        return provider;
    }

    public Map<String, Object> options() {
        return options;
    }

    public CompletableFuture<Message.AssistantMessage> invoke(ModelContext context) {
        var params = buildModelProviderParams(context);
        return provider.invoke(params);
    }

    public Flux<Message.AssistantMessage> stream(ModelContext context) {
        var params = buildModelProviderParams(context);
        return provider.stream(params);
    }

    private ModelProvider.InvokeParams buildModelProviderParams(ModelContext context) {
        List<Message> messages = new ArrayList<>();
        if (context.prompt() != null && !context.prompt().isEmpty()) {
            messages.add(new Message.SystemMessage(
                List.of(new com.helixent.foundation.messages.Content.TextContent(context.prompt()))
            ));
        }
        messages.addAll(context.messages());
        return new ModelProvider.InvokeParams(
            name,
            messages,
            context.tools(),
            options,
            context.signal()
        );
    }
}