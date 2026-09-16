package com.kbase.backend.document;

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
import com.kbase.backend.exception.BadRequestException;
import com.kbase.backend.exception.UnsupportedPreviewTypeException;
import com.kbase.backend.storage.StorageService;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
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
    private final StorageService storageService;
    private final DocumentFileValidator fileValidator;

    private static final java.util.Set<String> PREVIEWABLE_TYPES = java.util.Set.of(
            "application/pdf", "image/png", "image/jpeg", "text/plain"
    );

    public DocumentService(
            DocumentRepository documentRepository,
            ProjectRepository projectRepository,
            ProjectMemberRepository memberRepository,
            UserRepository userRepository,
            StorageService storageService,
            DocumentFileValidator fileValidator
    ) {
        this.documentRepository = documentRepository;
        this.projectRepository = projectRepository;
        this.memberRepository = memberRepository;
        this.userRepository = userRepository;
        this.storageService = storageService;
        this.fileValidator = fileValidator;
    }

    @Transactional
    public DocumentResponse create(
            UUID projectId,
            MultipartFile file,
            String title,
            String description,
            String authenticatedEmail
    ) {
        AccessContext context = requireProjectMember(projectId, authenticatedEmail);
        String normalizedTitle = normalizeTitle(title);
        DocumentFileValidator.ValidatedFile validated = fileValidator.validate(file);
        UUID documentId = UUID.randomUUID();
        String storageKey = "projects/%s/documents/%s/%s".formatted(
                projectId, documentId, validated.safeFileName());

        Document document = new Document(
                documentId,
                context.project(),
                context.user(),
                normalizedTitle,
                normalizeDescription(description),
                validated.originalFileName(),
                storageKey,
                validated.contentType(),
                validated.size()
        );
        try (InputStream input = file.getInputStream()) {
            storageService.upload(storageKey, input, validated.size(),
                    validated.contentType());
        } catch (IOException exception) {
            throw new com.kbase.backend.storage.StorageException(
                    "Unable to read the uploaded file", exception);
        }
        try {
            return DocumentResponse.from(documentRepository.saveAndFlush(document));
        } catch (DataIntegrityViolationException exception) {
            rollbackUpload(storageKey, exception);
            throw new ConflictException("Storage key already exists");
        } catch (RuntimeException exception) {
            rollbackUpload(storageKey, exception);
            throw exception;
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

    @Transactional(readOnly = true)
    public DocumentContent download(UUID projectId, UUID documentId, String authenticatedEmail) {
        requireProjectMember(projectId, authenticatedEmail);
        Document document = activeDocument(projectId, documentId);
        return content(document);
    }

    @Transactional(readOnly = true)
    public DocumentContent preview(UUID projectId, UUID documentId, String authenticatedEmail) {
        requireProjectMember(projectId, authenticatedEmail);
        Document document = activeDocument(projectId, documentId);
        if (!PREVIEWABLE_TYPES.contains(document.getContentType())) {
            throw new UnsupportedPreviewTypeException(
                    "This document type cannot be previewed");
        }
        return content(document);
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

    private String normalizeTitle(String title) {
        if (title == null || title.isBlank() || title.trim().length() > 200) {
            throw new BadRequestException("Title is required and must not exceed 200 characters");
        }
        return title.trim();
    }

    private DocumentContent content(Document document) {
        InputStream stream = storageService.download(document.getStorageKey());
        return new DocumentContent(document.getOriginalFileName(), document.getContentType(),
                document.getFileSize(), stream);
    }

    private void rollbackUpload(String storageKey, RuntimeException original) {
        try {
            storageService.delete(storageKey);
        } catch (RuntimeException rollbackFailure) {
            original.addSuppressed(rollbackFailure);
        }
    }

    public record DocumentContent(
            String fileName,
            String contentType,
            long size,
            InputStream stream
    ) {
    }

    private record AccessContext(Project project, User user) {
    }
}
