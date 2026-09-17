package com.kbase.backend.rag.chat;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.rag")
public record ChatRagProperties(
        int topK,
        int maxHistoryMessages,
        int maxContextChunks,
        int maxContextChars
) {
}
