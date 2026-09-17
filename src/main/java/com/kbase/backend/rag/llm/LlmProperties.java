package com.kbase.backend.rag.llm;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.llm")
public record LlmProperties(
        String provider,
        String baseUrl,
        String apiKey,
        String model,
        double temperature,
        int maxOutputTokens,
        int timeoutSeconds
) {
}
