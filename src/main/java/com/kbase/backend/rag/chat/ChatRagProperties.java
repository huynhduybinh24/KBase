package com.kbase.backend.rag.chat;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;
import jakarta.validation.constraints.Positive;

@ConfigurationProperties(prefix = "app.rag")
@Validated
public record ChatRagProperties(
        @Positive int topK,
        @Positive int maxHistoryMessages,
        @Positive int maxContextChunks,
        @Positive int maxContextChars
) {
}
