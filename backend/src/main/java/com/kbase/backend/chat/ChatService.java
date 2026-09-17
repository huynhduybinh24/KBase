package com.kbase.backend.chat;

import com.kbase.backend.chat.dto.ChatMessageResponse;
import com.kbase.backend.config.ChatProtectionProperties;
import com.kbase.backend.exception.BadRequestException;
import com.kbase.backend.exception.ResourceNotFoundException;
import com.kbase.backend.rag.chat.ChatRagProperties;
import com.kbase.backend.rag.chat.RagPrompt;
import com.kbase.backend.rag.chat.RagPromptBuilder;
import com.kbase.backend.rag.llm.LlmResponse;
import com.kbase.backend.rag.llm.LlmService;
import com.kbase.backend.rag.retrieval.SemanticSearchResponse;
import com.kbase.backend.rag.retrieval.SemanticSearchService;
import org.springframework.stereotype.Service;
import com.kbase.backend.user.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.UUID;

@Service
public class ChatService {
    private static final Logger log = LoggerFactory.getLogger(ChatService.class);

    public static final String INSUFFICIENT_CONTEXT =
            "The available project documents do not contain enough information to answer this question.";

    private final ChatPersistenceService persistence;
    private final SemanticSearchService semanticSearch;
    private final RagPromptBuilder promptBuilder;
    private final LlmService llmService;
    private final ChatRagProperties properties;
    private final ChatProtectionProperties protection;
    private final ChatRateLimiter rateLimiter;
    private final UserRepository users;

    public ChatService(
            ChatPersistenceService persistence,
            SemanticSearchService semanticSearch,
            RagPromptBuilder promptBuilder,
            LlmService llmService,
            ChatRagProperties properties,
            ChatProtectionProperties protection,
            ChatRateLimiter rateLimiter,
            UserRepository users
    ) {
        this.persistence = persistence;
        this.semanticSearch = semanticSearch;
        this.promptBuilder = promptBuilder;
        this.llmService = llmService;
        this.properties = properties;
        this.protection = protection;
        this.rateLimiter = rateLimiter;
        this.users = users;
    }

    public ChatMessageResponse ask(
            UUID projectId, UUID sessionId, String question, String email
    ) {
        String normalized = question == null ? "" : question.trim();
        if (normalized.isEmpty()) {
            throw new BadRequestException("Message must not be blank");
        }
        if (normalized.codePointCount(0, normalized.length()) > protection.maxMessageChars()) {
            throw new BadRequestException("Message exceeds the configured maximum length");
        }
        var user = users.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new ResourceNotFoundException("Authenticated user not found"));
        rateLimiter.check(user.getId());
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
        try {
            LlmResponse response = llmService.generate(prompt.request());
            ChatMessageResponse saved = persistence.saveAssistantMessage(
                    projectId, sessionId, response, prompt.sources(), email);
            log.info("Chat answer completed for project {} session {} with {} sources",
                    projectId, sessionId, prompt.sources().size());
            return saved;
        } catch (RuntimeException exception) {
            log.warn("Chat answer failed for project {} session {}: {}",
                    projectId, sessionId, exception.getClass().getSimpleName());
            throw exception;
        }
    }
}
