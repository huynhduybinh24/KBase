package com.kbase.backend.rag.llm;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;

@ConfigurationProperties(prefix = "app.llm")
@Validated
public record LlmProperties(
        @NotBlank String provider,
        @NotBlank String baseUrl,
        String apiKey,
        @NotBlank String model,
        @DecimalMin("0.0") @DecimalMax("2.0") double temperature,
        @Positive int maxOutputTokens,
        @Positive int timeoutSeconds
) {
}
