package com.kbase.backend.rag.embedding;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.Comparator;
import java.util.List;

@Service
@ConditionalOnProperty(name = "app.embedding.provider", havingValue = "openai-compatible")
public class OpenAiCompatibleEmbeddingService implements EmbeddingService {

    private final RestClient client;
    private final EmbeddingProperties properties;

    public OpenAiCompatibleEmbeddingService(
            RestClient.Builder builder,
            EmbeddingProperties properties
    ) {
        this.properties = properties;
        RestClient.Builder configured = builder.baseUrl(properties.baseUrl());
        if (properties.apiKey() != null && !properties.apiKey().isBlank()) {
            configured.defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + properties.apiKey());
        }
        this.client = configured.build();
    }

    @Override
    public EmbeddingResult embed(String text) {
        return embedAll(List.of(text)).getFirst();
    }

    @Override
    public List<EmbeddingResult> embedAll(List<String> texts) {
        try {
            EmbeddingResponse response = client.post()
                    .uri("/embeddings")
                    .body(new EmbeddingRequest(properties.model(), texts))
                    .retrieve()
                    .body(EmbeddingResponse.class);
            if (response == null || response.data() == null
                    || response.data().size() != texts.size()) {
                throw new IllegalStateException("Invalid embedding response");
            }
            return response.data().stream()
                    .sorted(Comparator.comparingInt(EmbeddingData::index))
                    .map(item -> new EmbeddingResult(
                            item.embedding(), properties.model(), item.embedding().size()))
                    .toList();
        } catch (Exception exception) {
            throw new EmbeddingUnavailableException(exception);
        }
    }

    private record EmbeddingRequest(String model, List<String> input) { }
    private record EmbeddingResponse(List<EmbeddingData> data) { }
    private record EmbeddingData(int index, List<Double> embedding) { }
}
