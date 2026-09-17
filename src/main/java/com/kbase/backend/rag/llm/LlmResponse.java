package com.kbase.backend.rag.llm;

public record LlmResponse(
        String answer,
        String model,
        Integer inputTokenCount,
        Integer outputTokenCount
) {
}
