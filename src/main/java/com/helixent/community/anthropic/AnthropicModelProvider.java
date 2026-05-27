package com.helixent.community.anthropic;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.helixent.foundation.messages.Message;
import com.helixent.foundation.models.ModelProvider;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

public class AnthropicModelProvider implements ModelProvider {

    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final String DEFAULT_BASE_URL = "https://api.anthropic.com/v1";
    private static final String ANTHROPIC_VERSION = "2023-06-01";

    private final WebClient webClient;

    public AnthropicModelProvider(String baseUrl, String apiKey) {
        var url = baseUrl != null && !baseUrl.isEmpty() ? baseUrl : DEFAULT_BASE_URL;
        this.webClient = WebClient.builder()
            .baseUrl(url)
            .defaultHeader("x-api-key", apiKey)
            .defaultHeader("anthropic-version", ANTHROPIC_VERSION)
            .defaultHeader("Content-Type", "application/json")
            .build();
    }

    @Override
    public CompletableFuture<Message.AssistantMessage> invoke(InvokeParams params) {
        var body = buildRequestBody(params, false);
        return webClient.post()
            .uri("/messages")
            .bodyValue(body)
            .retrieve()
            .bodyToMono(Map.class)
            .map(response -> parseResponse(response))
            .toFuture();
    }

    @Override
    public Flux<Message.AssistantMessage> stream(InvokeParams params) {
        var body = buildRequestBody(params, true);
        return webClient.post()
            .uri("/messages")
            .accept(MediaType.TEXT_EVENT_STREAM)
            .bodyValue(body)
            .retrieve()
            .bodyToFlux(String.class)
            .filter(line -> line != null && !line.isEmpty())
            .flatMap(line -> {
                try {
                    @SuppressWarnings("unchecked")
                    var event = (Map<String, Object>) MAPPER.readValue(line, Map.class);
                    return Flux.just(event);
                } catch (JsonProcessingException e) {
                    return Flux.empty();
                }
            })
            .transformDeferred(flux -> {
                var acc = new AnthropicStreamAccumulator();
                return flux.doOnNext(acc::push).map(v -> acc.snapshot());
            });
    }

    @SuppressWarnings("unchecked")
    private Message.AssistantMessage parseResponse(Map<String, Object> response) {
        var usage = parseUsage((Map<String, Object>) response.get("usage"));
        return AnthropicUtils.parseAssistantMessage(response, usage);
    }

    private Message.TokenUsage parseUsage(Map<String, Object> usage) {
        if (usage == null) return null;
        return new Message.TokenUsage(
            ((Number) usage.getOrDefault("input_tokens", 0)).intValue(),
            ((Number) usage.getOrDefault("output_tokens", 0)).intValue(),
            0
        );
    }

    private Map<String, Object> buildRequestBody(InvokeParams params, boolean stream) {
        var body = new LinkedHashMap<String, Object>();
        body.put("model", params.model());
        body.put("max_tokens", 4096);

        var messages = params.messages();
        var systemContent = new StringBuilder();
        var nonSystemMessages = new ArrayList<Message>();

        for (var msg : messages) {
            if (msg instanceof Message.SystemMessage sys) {
                for (var c : sys.content()) {
                    systemContent.append(c.text()).append("\n");
                }
            } else {
                nonSystemMessages.add(msg);
            }
        }

        if (!systemContent.isEmpty()) {
            body.put("system", systemContent.toString().trim());
        }

        body.put("messages", AnthropicUtils.convertToAnthropicMessages(nonSystemMessages));

        if (params.tools() != null && !params.tools().isEmpty()) {
            body.put("tools", AnthropicUtils.convertToAnthropicTools(params.tools()));
        }

        if (params.options() != null) {
            body.putAll(params.options());
        }

        if (stream) {
            body.put("stream", true);
        }

        return body;
    }
}