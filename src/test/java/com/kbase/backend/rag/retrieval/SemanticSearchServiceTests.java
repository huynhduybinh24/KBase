package com.kbase.backend.rag.retrieval;

import com.kbase.backend.document.Document;
import com.kbase.backend.document.DocumentRepository;
import com.kbase.backend.document.DocumentStatus;
import com.kbase.backend.document.chunk.DocumentChunkRepository;
import com.kbase.backend.exception.ForbiddenException;
import com.kbase.backend.exception.ResourceNotFoundException;
import com.kbase.backend.project.ProjectRepository;
import com.kbase.backend.project.member.ProjectMemberRepository;
import com.kbase.backend.rag.embedding.EmbeddingResult;
import com.kbase.backend.rag.embedding.EmbeddingService;
import com.kbase.backend.user.User;
import com.kbase.backend.user.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class SemanticSearchServiceTests {

    private DocumentChunkRepository chunks;
    private DocumentRepository documents;
    private ProjectRepository projects;
    private ProjectMemberRepository members;
    private UserRepository users;
    private EmbeddingService embeddings;
    private SemanticSearchService service;
    private UUID projectId;
    private User user;

    @BeforeEach
    void setUp() {
        chunks = mock(DocumentChunkRepository.class);
        documents = mock(DocumentRepository.class);
        projects = mock(ProjectRepository.class);
        members = mock(ProjectMemberRepository.class);
        users = mock(UserRepository.class);
        embeddings = mock(EmbeddingService.class);
        service = new SemanticSearchService(
                chunks, documents, projects, members, users, embeddings);
        projectId = UUID.randomUUID();
        user = mock(User.class);
        when(user.getId()).thenReturn(UUID.randomUUID());
        when(users.findByEmailIgnoreCase("member@example.com")).thenReturn(Optional.of(user));
        when(projects.existsById(projectId)).thenReturn(true);
        when(members.existsByProjectIdAndUserId(projectId, user.getId())).thenReturn(true);
    }

    @Test
    void delegatesRankingAndTopKToVectorRepository() {
        EmbeddingResult query = new EmbeddingResult(List.of(1.0, 0.0), "test", 2);
        when(embeddings.embed("authentication")).thenReturn(query);
        List<SemanticSearchResult> ranked = List.of(
                result("Authentication uses JWT", 0.95), result("Token details", 0.70));
        when(chunks.search(projectId, null, query, 2)).thenReturn(ranked);

        var response = service.search(
                projectId, " authentication ", 2, null, "member@example.com");

        assertEquals(ranked, response.results());
        assertEquals(0.95, response.results().getFirst().score());
        verify(chunks).search(projectId, null, query, 2);
    }

    @Test
    void optionalDocumentMustBelongToProjectAndBeActive() {
        UUID documentId = UUID.randomUUID();
        when(documents.findByIdAndProjectIdAndStatus(
                documentId, projectId, DocumentStatus.ACTIVE)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> service.search(
                projectId, "query", 5, documentId, "member@example.com"));
        verify(embeddings, never()).embed(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void outsiderIsRejectedBeforeEmbedding() {
        when(members.existsByProjectIdAndUserId(projectId, user.getId())).thenReturn(false);
        assertThrows(ForbiddenException.class, () -> service.search(
                projectId, "query", 5, null, "member@example.com"));
        verify(embeddings, never()).embed(org.mockito.ArgumentMatchers.any());
    }

    private SemanticSearchResult result(String content, double score) {
        return new SemanticSearchResult(
                UUID.randomUUID(), UUID.randomUUID(), "Document", 0, content, score);
    }
}
