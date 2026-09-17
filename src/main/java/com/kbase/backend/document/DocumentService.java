package com.kbase.backend.document;

import com.kbase.backend.document.dto.DocumentResponse;
import com.kbase.backend.document.dto.DocumentPageResponse;
import com.kbase.backend.document.dto.UpdateDocumentRequest;
import com.kbase.backend.document.extraction.DocumentExtractionService;
import com.kbase.backend.document.extraction.dto.DocumentContentResponse;
import com.kbase.backend.document.chunk.DocumentChunkService;
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
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

import java.io.IOException;
import java.io.InputStream;
import java.util.Locale;
import java.util.Objects;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Service
public class DocumentService {
    private static final Logger log = LoggerFactory.getLogger(DocumentService.class);

    private final DocumentRepository documentRepository;
    private final ProjectRepository projectRepository;
    private final ProjectMemberRepository memberRepository;
    private final UserRepository userRepository;
    private final StorageService storageService;
    private final DocumentFileValidator fileValidator;
    private final DocumentExtractionService extractionService;
    private final DocumentChunkService chunkService;

    private static final java.util.Set<String> PREVIEWABLE_TYPES = java.util.Set.of(
            "application/pdf", "image/png", "image/jpeg", "text/plain"
    );

    public DocumentService(
            DocumentRepository documentRepository,
            ProjectRepository projectRepository,
            ProjectMemberRepository memberRepository,
            UserRepository userRepository,
            StorageService storageService,
            DocumentFileValidator fileValidator,
            DocumentExtractionService extractionService,
            DocumentChunkService chunkService
    ) {
        this.documentRepository = documentRepository;
        this.projectRepository = projectRepository;
        this.memberRepository = memberRepository;
        this.userRepository = userRepository;
        this.storageService = storageService;
        this.fileValidator = fileValidator;
        this.extractionService = extractionService;
        this.chunkService = chunkService;
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
            Document saved = documentRepository.saveAndFlush(document);
            extractionService.initializeAndExtract(saved);
            log.info("Document upload completed project={} document={} size={} contentType={}",
                    projectId, saved.getId(), validated.size(), validated.contentType());
            return DocumentResponse.from(saved);
        } catch (DataIntegrityViolationException exception) {
            rollbackUpload(storageKey, exception);
            throw new ConflictException("Storage key already exists");
        } catch (RuntimeException exception) {
            log.warn("Document upload failed project={} document={} reason={}", projectId,
                    documentId, exception.getClass().getSimpleName());
            rollbackUpload(storageKey, exception);
            throw exception;
        }
    }

    @Transactional(readOnly = true)
    public DocumentPageResponse list(
            UUID projectId,
            DocumentSearchCriteria filters,
            int page,
            int size,
            String sort,
            String authenticatedEmail
    ) {
        requireProjectMember(projectId, authenticatedEmail);
        if (filters.from() != null && filters.to() != null
                && filters.from().isAfter(filters.to())) {
            throw new BadRequestException("from must be before or equal to to");
        }
        Sort validatedSort = parseSort(sort);
        return DocumentPageResponse.from(documentRepository.findAll(
                DocumentSpecifications.matching(projectId, filters),
                PageRequest.of(page, size, validatedSort)));
    }

    @Transactional(readOnly = true)
    public DocumentResponse get(UUID projectId, UUID documentId, String authenticatedEmail) {
        requireProjectMember(projectId, authenticatedEmail);
        return DocumentResponse.from(activeDocument(projectId, documentId));
    }

    @Transactional(readOnly = true)
    public DocumentContentResponse getExtractedContent(
            UUID projectId, UUID documentId, String authenticatedEmail
    ) {
        requireProjectMember(projectId, authenticatedEmail);
        Document document = activeDocument(projectId, documentId);
        return extractionService.get(document.getId());
    }

    @Transactional
    public DocumentContentResponse retryExtraction(
            UUID projectId, UUID documentId, String authenticatedEmail
    ) {
        AccessContext context = requireProjectMember(projectId, authenticatedEmail);
        Document document = activeDocument(projectId, documentId);
        requireUploaderOrOwner(document, context);
        return extractionService.retry(document);
    }

    @Transactional
    public DocumentContentResponse reindex(
            UUID projectId, UUID documentId, String authenticatedEmail
    ) {
        AccessContext context = requireProjectMember(projectId, authenticatedEmail);
        Document document = activeDocument(projectId, documentId);
        requireUploaderOrOwner(document, context);
        return chunkService.reindex(document);
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

    private Sort parseSort(String value) {
        String requested = value == null || value.isBlank() ? "createdAt,desc" : value.trim();
        String[] parts = requested.split(",", -1);
        java.util.Set<String> allowed = java.util.Set.of(
                "createdAt", "updatedAt", "title", "fileSize");
        if (parts.length > 2 || !allowed.contains(parts[0])) {
            throw new BadRequestException("Invalid sort field");
        }
        Sort.Direction direction;
        try {
            direction = parts.length == 1
                    ? Sort.Direction.ASC
                    : Sort.Direction.fromString(parts[1]);
        } catch (IllegalArgumentException exception) {
            throw new BadRequestException("Invalid sort direction");
        }
        return Sort.by(direction, parts[0]);
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
