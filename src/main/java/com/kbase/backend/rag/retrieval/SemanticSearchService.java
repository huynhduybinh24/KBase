package com.kbase.backend.rag.retrieval;

import com.kbase.backend.document.DocumentRepository;
import com.kbase.backend.document.DocumentStatus;
import com.kbase.backend.document.chunk.DocumentChunkRepository;
import com.kbase.backend.exception.BadRequestException;
import com.kbase.backend.exception.ForbiddenException;
import com.kbase.backend.exception.InvalidCredentialsException;
import com.kbase.backend.exception.ResourceNotFoundException;
import com.kbase.backend.project.ProjectRepository;
import com.kbase.backend.project.member.ProjectMemberRepository;
import com.kbase.backend.rag.embedding.EmbeddingService;
import com.kbase.backend.user.User;
import com.kbase.backend.user.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;
import java.util.UUID;

@Service
public class SemanticSearchService {

    private final DocumentChunkRepository chunkRepository;
    private final DocumentRepository documentRepository;
    private final ProjectRepository projectRepository;
    private final ProjectMemberRepository memberRepository;
    private final UserRepository userRepository;
    private final EmbeddingService embeddingService;

    public SemanticSearchService(
            DocumentChunkRepository chunkRepository,
            DocumentRepository documentRepository,
            ProjectRepository projectRepository,
            ProjectMemberRepository memberRepository,
            UserRepository userRepository,
            EmbeddingService embeddingService
    ) {
        this.chunkRepository = chunkRepository;
        this.documentRepository = documentRepository;
        this.projectRepository = projectRepository;
        this.memberRepository = memberRepository;
        this.userRepository = userRepository;
        this.embeddingService = embeddingService;
    }

    @Transactional(readOnly = true)
    public SemanticSearchResponse search(
            UUID projectId,
            String query,
            int topK,
            UUID documentId,
            String authenticatedEmail
    ) {
        if (query == null || query.isBlank()) {
            throw new BadRequestException("Search query must not be blank");
        }
        User user = userRepository
                .findByEmailIgnoreCase(authenticatedEmail.trim().toLowerCase(Locale.ROOT))
                .orElseThrow(InvalidCredentialsException::new);
        if (!projectRepository.existsById(projectId)) {
            throw new ResourceNotFoundException("Project not found");
        }
        if (!memberRepository.existsByProjectIdAndUserId(projectId, user.getId())) {
            throw new ForbiddenException("You are not a member of this project");
        }
        if (documentId != null && documentRepository.findByIdAndProjectIdAndStatus(
                documentId, projectId, DocumentStatus.ACTIVE).isEmpty()) {
            throw new ResourceNotFoundException("Document not found");
        }
        var queryEmbedding = embeddingService.embed(query.trim());
        return new SemanticSearchResponse(query.trim(), chunkRepository.search(
                projectId, documentId, queryEmbedding, topK));
    }
}
