package com.kbase.backend.chat;

import com.kbase.backend.chat.dto.ChatMessageResponse;
import com.kbase.backend.rag.chat.ChatRagProperties;
import com.kbase.backend.rag.chat.RagPrompt;
import com.kbase.backend.rag.chat.RagPromptBuilder;
import com.kbase.backend.rag.llm.LlmResponse;
import com.kbase.backend.rag.llm.LlmService;
import com.kbase.backend.rag.retrieval.SemanticSearchResponse;
import com.kbase.backend.rag.retrieval.SemanticSearchService;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
public class ChatService {

    public static final String INSUFFICIENT_CONTEXT =
            "The available project documents do not contain enough information to answer this question.";

    private final ChatPersistenceService persistence;
    private final SemanticSearchService semanticSearch;
    private final RagPromptBuilder promptBuilder;
    private final LlmService llmService;
    private final ChatRagProperties properties;

    public ChatService(
            ChatPersistenceService persistence,
            SemanticSearchService semanticSearch,
            RagPromptBuilder promptBuilder,
            LlmService llmService,
            ChatRagProperties properties
    ) {
        this.persistence = persistence;
        this.semanticSearch = semanticSearch;
        this.promptBuilder = promptBuilder;
        this.llmService = llmService;
        this.properties = properties;
    }

    public ChatMessageResponse ask(
            UUID projectId, UUID sessionId, String question, String email
    ) {
        String normalized = question.trim();
        List<ChatMessage> history = persistence.recentHistory(
                projectId, sessionId, properties.maxHistoryMessages(), email);
        persistence.saveUserMessage(projectId, sessionId, normalized, email);

        SemanticSearchResponse retrieval = semanticSearch.search(
                projectId, normalized, promptBuilder.topK(), null, email);
        if (retrieval.results().isEmpty()) {
            return persistence.saveAssistantMessage(projectId, sessionId,
                    new LlmResponse(INSUFFICIENT_CONTEXT, null, null, null), List.of(), email);
        }

        RagPrompt prompt = promptBuilder.build(normalized, history, retrieval.results());
        if (prompt.sources().isEmpty()) {
            return persistence.saveAssistantMessage(projectId, sessionId,
                    new LlmResponse(INSUFFICIENT_CONTEXT, null, null, null), List.of(), email);
        }
        LlmResponse response = llmService.generate(prompt.request());
        return persistence.saveAssistantMessage(
                projectId, sessionId, response, prompt.sources(), email);
    }
}
