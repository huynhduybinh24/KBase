package com.kbase.backend.rag.llm;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpHeaders;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.net.http.HttpClient;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

@Service
@ConditionalOnProperty(name = "app.llm.provider", havingValue = "openai-compatible")
public class OpenAiCompatibleLlmService implements LlmService {

    private final RestClient client;
    private final LlmProperties properties;

    @Autowired
    public OpenAiCompatibleLlmService(LlmProperties properties) {
        this(buildClient(properties), properties);
    }

    OpenAiCompatibleLlmService(RestClient client, LlmProperties properties) {
        this.client = client;
        this.properties = properties;
    }

    @Override
    public LlmResponse generate(LlmRequest request) {
        try {
            List<ProviderMessage> messages = new ArrayList<>();
            messages.add(new ProviderMessage("system", request.systemPrompt()));
            request.messages().forEach(message ->
                    messages.add(new ProviderMessage(message.role(), message.content())));
            ProviderResponse response = client.post()
                    .uri("/chat/completions")
                    .body(new ProviderRequest(properties.model(), messages,
                            properties.temperature(), properties.maxOutputTokens()))
                    .retrieve()
                    .body(ProviderResponse.class);
            if (response == null || response.choices() == null || response.choices().isEmpty()
                    || response.choices().getFirst().message() == null
                    || response.choices().getFirst().message().content() == null
                    || response.choices().getFirst().message().content().isBlank()) {
                throw new IllegalStateException("Malformed language model response");
            }
            Usage usage = response.usage();
            return new LlmResponse(
                    response.choices().getFirst().message().content().trim(),
                    response.model() == null ? properties.model() : response.model(),
                    usage == null ? null : usage.promptTokens(),
                    usage == null ? null : usage.completionTokens());
        } catch (Exception exception) {
            throw new LlmUnavailableException(exception);
        }
    }

    private static RestClient buildClient(LlmProperties properties) {
        HttpClient httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(properties.timeoutSeconds()))
                .build();
        JdkClientHttpRequestFactory factory = new JdkClientHttpRequestFactory(httpClient);
        factory.setReadTimeout(Duration.ofSeconds(properties.timeoutSeconds()));
        RestClient.Builder builder = RestClient.builder()
                .baseUrl(properties.baseUrl())
                .requestFactory(factory);
        if (properties.apiKey() != null && !properties.apiKey().isBlank()) {
            builder.defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + properties.apiKey());
        }
        return builder.build();
    }

    private record ProviderRequest(
            String model,
            List<ProviderMessage> messages,
            double temperature,
            @JsonProperty("max_tokens") int maxTokens
    ) { }
    private record ProviderMessage(String role, String content) { }
    private record ProviderResponse(String model, List<Choice> choices, Usage usage) { }
    private record Choice(ProviderMessage message) { }
    private record Usage(
            @JsonProperty("prompt_tokens") Integer promptTokens,
            @JsonProperty("completion_tokens") Integer completionTokens
    ) { }
}
