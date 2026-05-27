package com.aether.community.openai;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.aether.foundation.messages.Message;
import com.aether.foundation.models.ModelProvider;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

public class OpenAIModelProvider implements ModelProvider {

    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final String DEFAULT_BASE_URL = "https://api.openai.com/v1";

    private final WebClient webClient;

    public OpenAIModelProvider(String baseUrl, String apiKey) {
        var url = baseUrl != null && !baseUrl.isEmpty() ? baseUrl : DEFAULT_BASE_URL;
        this.webClient = WebClient.builder()
            .baseUrl(url)
            .defaultHeader("Authorization", "Bearer " + apiKey)
            .defaultHeader("Content-Type", "application/json")
            .build();
    }

    @Override
    public CompletableFuture<Message.AssistantMessage> invoke(InvokeParams params) {
        var body = buildRequestBody(params, false);
        return webClient.post()
            .uri("/chat/completions")
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
            .uri("/chat/completions")
            .accept(MediaType.TEXT_EVENT_STREAM)
            .bodyValue(body)
            .retrieve()
            .bodyToFlux(String.class)
            .filter(line -> line != null && !line.isEmpty())
            .flatMap(line -> {
                if ("[DONE]".equals(line)) return Flux.empty();
                try {
                    @SuppressWarnings("unchecked")
                    var chunk = (Map<String, Object>) MAPPER.readValue(line, Map.class);
                    return Flux.just(chunk);
                } catch (JsonProcessingException e) {
                    return Flux.empty();
                }
            })
            .transformDeferred(flux -> {
                var acc = new OpenAIStreamAccumulator();
                return flux.doOnNext(acc::push).map(v -> acc.snapshot());
            });
    }

    @SuppressWarnings("unchecked")
    private Message.AssistantMessage parseResponse(Map<String, Object> response) {
        var choices = (List<Map<String, Object>>) response.get("choices");
        if (choices == null || choices.isEmpty()) {
            return new Message.AssistantMessage(List.of());
        }
        var message = (Map<String, Object>) choices.get(0).get("message");
        var usage = parseUsage((Map<String, Object>) response.get("usage"));
        return OpenAIUtils.parseAssistantMessage(message, usage);
    }

    private Message.TokenUsage parseUsage(Map<String, Object> usage) {
        if (usage == null) return null;
        return new Message.TokenUsage(
            ((Number) usage.getOrDefault("prompt_tokens", 0)).intValue(),
            ((Number) usage.getOrDefault("completion_tokens", 0)).intValue(),
            ((Number) usage.getOrDefault("total_tokens", 0)).intValue()
        );
    }

    private Map<String, Object> buildRequestBody(InvokeParams params, boolean stream) {
        var body = new LinkedHashMap<String, Object>();
        body.put("model", params.model());
        body.put("messages", OpenAIUtils.convertToOpenAIMessages(params.messages()));
        body.put("temperature", 0);

        if (params.tools() != null && !params.tools().isEmpty()) {
            body.put("tools", OpenAIUtils.convertToOpenAITools(params.tools()));
        }

        if (params.options() != null) {
            body.putAll(params.options());
        }

        if (stream) {
            body.put("stream", true);
            body.put("stream_options", Map.of("include_usage", true));
        }

        return body;
    }
}