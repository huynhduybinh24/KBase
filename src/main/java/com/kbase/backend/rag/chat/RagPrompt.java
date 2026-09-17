package com.kbase.backend.rag.chat;

import com.kbase.backend.rag.llm.LlmRequest;
import com.kbase.backend.rag.retrieval.SemanticSearchResult;

import java.util.List;

public record RagPrompt(LlmRequest request, List<SemanticSearchResult> sources) {
    public RagPrompt {
        sources = List.copyOf(sources);
    }
}
