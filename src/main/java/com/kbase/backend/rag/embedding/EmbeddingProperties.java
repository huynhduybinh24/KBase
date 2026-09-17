package com.kbase.backend.rag.embedding;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.embedding")
public record EmbeddingProperties(
        String provider,
        String baseUrl,
        String apiKey,
        String model,
        int dimension
) {
}
