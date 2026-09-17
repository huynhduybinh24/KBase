package com.kbase.backend.rag.chunking;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.rag")
public record RagProperties(int chunkSize, int chunkOverlap) {
}
