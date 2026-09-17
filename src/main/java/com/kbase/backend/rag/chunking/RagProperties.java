package com.kbase.backend.rag.chunking;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;

@ConfigurationProperties(prefix = "app.rag")
@Validated
public record RagProperties(@Positive int chunkSize, @PositiveOrZero int chunkOverlap) {
    @AssertTrue(message = "chunk overlap must be smaller than chunk size")
    public boolean isOverlapValid() {
        return chunkOverlap < chunkSize;
    }
}
