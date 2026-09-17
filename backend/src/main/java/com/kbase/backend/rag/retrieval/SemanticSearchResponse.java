package com.kbase.backend.rag.retrieval;

import java.util.List;

public record SemanticSearchResponse(String query, List<SemanticSearchResult> results) {
}
