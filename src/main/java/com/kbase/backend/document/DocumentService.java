package com.kbase.backend.document;

import com.kbase.backend.document.dto.CreateDocumentRequest;
import com.kbase.backend.document.dto.DocumentResponse;
import com.kbase.backend.document.dto.UpdateDocumentRequest;
import com.kbase.backend.exception.ConflictException;
import com.kbase.backend.exception.ForbiddenException;
import com.kbase.backend.exception.InvalidCredentialsException;
import com.kbase.backend.exception.ResourceNotFoundException;
import com.kbase.backend.project.Project;
import com.kbase.backend.project.ProjectRepository;
import com.kbase.backend.project.member.ProjectMemberRepository;
import com.kbase.backend.user.User;
import com.kbase.backend.user.UserRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.UUID;

@Service
public class DocumentService {

    private final DocumentRepository documentRepository;
    private final ProjectRepository projectRepository;
    private final ProjectMemberRepository memberRepository;
    private final UserRepository userRepository;

    public DocumentService(
            DocumentRepository documentRepository,
            ProjectRepository projectRepository,
            ProjectMemberRepository memberRepository,
            UserRepository userRepository
    ) {
        this.documentRepository = documentRepository;
        this.projectRepository = projectRepository;
        this.memberRepository = memberRepository;
        this.userRepository = userRepository;
    }

    @Transactional
    public DocumentResponse create(
            UUID projectId,
            CreateDocumentRequest request,
            String authenticatedEmail
    ) {
        AccessContext context = requireProjectMember(projectId, authenticatedEmail);
        String storageKey = request.storageKey().trim();
        if (documentRepository.existsByStorageKey(storageKey)) {
            throw new ConflictException("Storage key already exists");
        }

        Document document = new Document(
                context.project(),
                context.user(),
                request.title().trim(),
                normalizeDescription(request.description()),
                request.originalFileName().trim(),
                storageKey,
                request.contentType().trim(),
                request.fileSize()
        );
        try {
            return DocumentResponse.from(documentRepository.saveAndFlush(document));
        } catch (DataIntegrityViolationException exception) {
            throw new ConflictException("Storage key already exists");
        }
    }

    @Transactional(readOnly = true)
    public List<DocumentResponse> list(UUID projectId, String authenticatedEmail) {
        requireProjectMember(projectId, authenticatedEmail);
        return documentRepository
                .findAllByProjectIdAndStatusOrderByCreatedAtDesc(projectId, DocumentStatus.ACTIVE)
                .stream()
                .map(DocumentResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public DocumentResponse get(UUID projectId, UUID documentId, String authenticatedEmail) {
        requireProjectMember(projectId, authenticatedEmail);
        return DocumentResponse.from(activeDocument(projectId, documentId));
    }

    @Transactional
    public DocumentResponse update(
            UUID projectId,
            UUID documentId,
            UpdateDocumentRequest request,
            String authenticatedEmail
    ) {
        AccessContext context = requireProjectMember(projectId, authenticatedEmail);
        Document document = activeDocument(projectId, documentId);
        requireUploaderOrOwner(document, context);
        document.updateMetadata(request.title().trim(), normalizeDescription(request.description()));
        return DocumentResponse.from(documentRepository.saveAndFlush(document));
    }

    @Transactional
    public void delete(UUID projectId, UUID documentId, String authenticatedEmail) {
        AccessContext context = requireProjectMember(projectId, authenticatedEmail);
        Document document = activeDocument(projectId, documentId);
        requireUploaderOrOwner(document, context);
        document.softDelete();
        documentRepository.saveAndFlush(document);
    }

    private AccessContext requireProjectMember(UUID projectId, String authenticatedEmail) {
        User user = userRepository
                .findByEmailIgnoreCase(authenticatedEmail.trim().toLowerCase(Locale.ROOT))
                .orElseThrow(InvalidCredentialsException::new);
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new ResourceNotFoundException("Project not found"));
        if (!memberRepository.existsByProjectIdAndUserId(projectId, user.getId())) {
            throw new ForbiddenException("You are not a member of this project");
        }
        return new AccessContext(project, user);
    }

    private Document activeDocument(UUID projectId, UUID documentId) {
        return documentRepository
                .findByIdAndProjectIdAndStatus(documentId, projectId, DocumentStatus.ACTIVE)
                .orElseThrow(() -> new ResourceNotFoundException("Document not found"));
    }

    private void requireUploaderOrOwner(Document document, AccessContext context) {
        UUID currentUserId = context.user().getId();
        boolean uploader = Objects.equals(document.getUploadedBy().getId(), currentUserId);
        boolean owner = Objects.equals(context.project().getOwner().getId(), currentUserId);
        if (!uploader && !owner) {
            throw new ForbiddenException(
                    "Only the document uploader or project owner can perform this action");
        }
    }

    private String normalizeDescription(String description) {
        return description == null ? null : description.trim();
    }

    private record AccessContext(Project project, User user) {
    }
}
