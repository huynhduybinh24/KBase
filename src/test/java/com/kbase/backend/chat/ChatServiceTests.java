package com.kbase.backend.chat;

import com.kbase.backend.chat.dto.ChatMessageResponse;
import com.kbase.backend.rag.chat.ChatRagProperties;
import com.kbase.backend.rag.chat.RagPromptBuilder;
import com.kbase.backend.rag.llm.LlmProperties;
import com.kbase.backend.rag.llm.LlmRequest;
import com.kbase.backend.rag.llm.LlmResponse;
import com.kbase.backend.rag.llm.LlmService;
import com.kbase.backend.rag.llm.LlmUnavailableException;
import com.kbase.backend.rag.retrieval.SemanticSearchResponse;
import com.kbase.backend.rag.retrieval.SemanticSearchResult;
import com.kbase.backend.rag.retrieval.SemanticSearchService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ChatServiceTests {

    private ChatPersistenceService persistence;
    private SemanticSearchService search;
    private LlmService llm;
    private ChatService service;
    private UUID projectId;
    private UUID sessionId;

    @BeforeEach
    void setUp() {
        persistence = mock(ChatPersistenceService.class);
        search = mock(SemanticSearchService.class);
        llm = mock(LlmService.class);
        ChatRagProperties properties = new ChatRagProperties(5, 10, 5, 2000);
        service = new ChatService(persistence, search,
                new RagPromptBuilder(properties), llm, properties);
        projectId = UUID.randomUUID();
        sessionId = UUID.randomUUID();
        when(persistence.recentHistory(any(), any(), any(Integer.class), any()))
                .thenReturn(List.of());
    }

    @Test
    void reusesSemanticSearchPassesContextAndPersistsStructuredSources() {
        SemanticSearchResult result = source();
        when(search.search(projectId, "How does auth work?", 5, null, "user@example.com"))
                .thenReturn(new SemanticSearchResponse("How does auth work?", List.of(result)));
        when(llm.generate(any())).thenAnswer(invocation -> {
            LlmRequest request = invocation.getArgument(0);
            assertEquals(result.chunkId(), request.sources().getFirst().chunkId());
            return new LlmResponse("Grounded answer", "dev", 10, 3);
        });
        ChatMessageResponse expected = mock(ChatMessageResponse.class);
        when(persistence.saveAssistantMessage(any(), any(), any(), any(), any()))
                .thenReturn(expected);

        ChatMessageResponse actual = service.ask(
                projectId, sessionId, " How does auth work? ", "user@example.com");

        assertEquals(expected, actual);
        verify(persistence).saveUserMessage(
                projectId, sessionId, "How does auth work?", "user@example.com");
        verify(persistence).saveAssistantMessage(projectId, sessionId,
                new LlmResponse("Grounded answer", "dev", 10, 3),
                List.of(result), "user@example.com");
    }

    @Test
    void zeroContextReturnsSafeAnswerWithoutCallingProviderOrFakeSources() {
        when(search.search(any(), any(), any(Integer.class), any(), any()))
                .thenReturn(new SemanticSearchResponse("unknown", List.of()));

        service.ask(projectId, sessionId, "unknown", "user@example.com");

        verify(llm, never()).generate(any());
        verify(persistence).saveAssistantMessage(projectId, sessionId,
                new LlmResponse(ChatService.INSUFFICIENT_CONTEXT, null, null, null),
                List.of(), "user@example.com");
    }

    @Test
    void providerFailureKeepsUserMessageAndDoesNotPersistAssistant() {
        when(search.search(any(), any(), any(Integer.class), any(), any()))
                .thenReturn(new SemanticSearchResponse("question", List.of(source())));
        when(llm.generate(any())).thenThrow(new LlmUnavailableException(new RuntimeException()));

        assertThrows(LlmUnavailableException.class,
                () -> service.ask(projectId, sessionId, "question", "user@example.com"));

        verify(persistence).saveUserMessage(
                projectId, sessionId, "question", "user@example.com");
        verify(persistence, never()).saveAssistantMessage(any(), any(), any(), any(), any());
    }

    private SemanticSearchResult source() {
        return new SemanticSearchResult(
                UUID.randomUUID(), UUID.randomUUID(), "Auth Guide", 2,
                "JWT bearer tokens authenticate requests.", 0.91);
    }
}
