package com.kbase.backend.rag.llm;

import com.kbase.backend.rag.retrieval.SemanticSearchResult;

import java.util.List;

public record LlmRequest(
        String systemPrompt,
        List<LlmMessage> messages,
        List<SemanticSearchResult> sources
) {
    public LlmRequest {
        messages = List.copyOf(messages);
        sources = List.copyOf(sources);
    }
}
