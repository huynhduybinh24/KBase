package com.kbase.backend.rag.embedding;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;

@ConfigurationProperties(prefix = "app.embedding")
@Validated
public record EmbeddingProperties(
        @NotBlank String provider,
        @NotBlank String baseUrl,
        String apiKey,
        @NotBlank String model,
        @Positive int dimension
) {
}
