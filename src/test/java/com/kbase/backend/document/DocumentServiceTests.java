package com.kbase.backend.document;

import com.kbase.backend.document.dto.UpdateDocumentRequest;
import com.kbase.backend.document.extraction.DocumentExtractionService;
import com.kbase.backend.document.chunk.DocumentChunkService;
import com.kbase.backend.exception.ForbiddenException;
import com.kbase.backend.exception.ResourceNotFoundException;
import com.kbase.backend.exception.UnsupportedPreviewTypeException;
import com.kbase.backend.project.Project;
import com.kbase.backend.project.ProjectRepository;
import com.kbase.backend.project.member.ProjectMemberRepository;
import com.kbase.backend.user.User;
import com.kbase.backend.user.UserRepository;
import com.kbase.backend.storage.StorageException;
import com.kbase.backend.storage.StorageProperties;
import com.kbase.backend.storage.StorageService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.data.domain.PageImpl;

import java.io.ByteArrayInputStream;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.doThrow;

class DocumentServiceTests {

    private DocumentRepository documentRepository;
    private ProjectRepository projectRepository;
    private ProjectMemberRepository memberRepository;
    private UserRepository userRepository;
    private StorageService storageService;
    private DocumentExtractionService extractionService;
    private DocumentChunkService chunkService;
    private DocumentService documentService;

    private UUID projectId;
    private UUID documentId;
    private User owner;
    private User uploader;
    private User member;
    private User outsider;
    private Project project;

    @BeforeEach
    void setUp() {
        documentRepository = mock(DocumentRepository.class);
        projectRepository = mock(ProjectRepository.class);
        memberRepository = mock(ProjectMemberRepository.class);
        userRepository = mock(UserRepository.class);
        storageService = mock(StorageService.class);
        extractionService = mock(DocumentExtractionService.class);
        chunkService = mock(DocumentChunkService.class);
        documentService = new DocumentService(
                documentRepository,
                projectRepository,
                memberRepository,
                userRepository,
                storageService,
                new DocumentFileValidator(new StorageProperties(
                        "http://localhost:9000", "key", "secret", "bucket", 1024)),
                extractionService,
                chunkService
        );

        projectId = UUID.randomUUID();
        documentId = UUID.randomUUID();
        owner = user(UUID.randomUUID(), "owner@example.com");
        uploader = user(UUID.randomUUID(), "uploader@example.com");
        member = user(UUID.randomUUID(), "member@example.com");
        outsider = user(UUID.randomUUID(), "outsider@example.com");
        project = project(projectId, owner);
    }

    @Test
    void memberCanCreateDocumentMetadata() {
        arrangeMember(uploader);
        when(documentRepository.saveAndFlush(any(Document.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        var response = documentService.create(projectId, file(), " Architecture Guide ",
                " System architecture ", uploader.getEmail());

        assertEquals("Architecture Guide", response.title());
        org.junit.jupiter.api.Assertions.assertTrue(response.storageKey().startsWith(
                "projects/" + projectId + "/documents/"));
        assertEquals(DocumentStatus.ACTIVE, response.status());
        assertEquals(uploader.getEmail(), response.uploadedBy().email());
        verify(extractionService).initializeAndExtract(any(Document.class));
    }

    @Test
    void outsiderCannotAccessProjectDocuments() {
        arrangeUserAndProject(outsider);
        when(memberRepository.existsByProjectIdAndUserId(projectId, outsider.getId()))
                .thenReturn(false);

        assertThrows(
                ForbiddenException.class,
                () -> documentService.list(projectId, filters(), 0, 20,
                        "createdAt,desc", outsider.getEmail())
        );
        verify(documentRepository, never())
                .findAll(any(org.springframework.data.jpa.domain.Specification.class),
                        any(org.springframework.data.domain.Pageable.class));
    }

    @Test
    void memberCanListActiveDocuments() {
        arrangeMember(member);
        Document document = document(uploader);
        when(documentRepository.findAll(
                any(org.springframework.data.jpa.domain.Specification.class),
                any(org.springframework.data.domain.Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(document)));

        var result = documentService.list(projectId, filters(), 0, 20,
                "createdAt,desc", member.getEmail());

        assertEquals(1, result.content().size());
        assertEquals(DocumentStatus.ACTIVE, result.content().getFirst().status());
    }

    @Test
    void searchUsesRequestedPaginationAndSafeSorting() {
        arrangeMember(member);
        when(documentRepository.findAll(
                any(org.springframework.data.jpa.domain.Specification.class),
                any(org.springframework.data.domain.Pageable.class)))
                .thenReturn(new PageImpl<>(List.of()));

        documentService.list(projectId,
                new DocumentSearchCriteria("GUIDE", "application/pdf", uploader.getId(),
                        java.time.Instant.parse("2026-01-01T00:00:00Z"),
                        java.time.Instant.parse("2026-12-31T23:59:59Z")),
                2, 10, "title,asc", member.getEmail());

        var captor = org.mockito.ArgumentCaptor.forClass(
                org.springframework.data.domain.Pageable.class);
        verify(documentRepository).findAll(
                any(org.springframework.data.jpa.domain.Specification.class), captor.capture());
        assertEquals(2, captor.getValue().getPageNumber());
        assertEquals(10, captor.getValue().getPageSize());
        assertTrue(captor.getValue().getSort().getOrderFor("title").isAscending());
    }

    @Test
    void invalidSortAndReversedDateRangeAreRejected() {
        arrangeMember(member);
        assertThrows(com.kbase.backend.exception.BadRequestException.class,
                () -> documentService.list(projectId, filters(), 0, 20,
                        "storageKey,asc", member.getEmail()));
        assertThrows(com.kbase.backend.exception.BadRequestException.class,
                () -> documentService.list(projectId,
                        new DocumentSearchCriteria(null, null, null,
                                java.time.Instant.parse("2026-02-01T00:00:00Z"),
                                java.time.Instant.parse("2026-01-01T00:00:00Z")),
                        0, 20, "createdAt,desc", member.getEmail()));
    }

    @Test
    void memberCanViewDocument() {
        arrangeMember(member);
        Document document = document(uploader);
        arrangeActiveDocument(document);

        var response = documentService.get(projectId, documentId, member.getEmail());

        assertEquals("Architecture Guide", response.title());
    }

    @Test
    void uploaderCanUpdateOwnDocument() {
        arrangeMember(uploader);
        Document document = document(uploader);
        arrangeActiveDocument(document);
        when(documentRepository.saveAndFlush(document)).thenReturn(document);

        var response = documentService.update(
                projectId,
                documentId,
                new UpdateDocumentRequest("Updated title", "Updated description"),
                uploader.getEmail()
        );

        assertEquals("Updated title", response.title());
        assertEquals("Updated description", response.description());
    }

    @Test
    void normalMemberCannotUpdateAnotherUsersDocument() {
        arrangeMember(member);
        Document document = document(uploader);
        arrangeActiveDocument(document);

        assertThrows(
                ForbiddenException.class,
                () -> documentService.update(
                        projectId,
                        documentId,
                        new UpdateDocumentRequest("Forbidden", null),
                        member.getEmail()
                )
        );
        verify(documentRepository, never()).saveAndFlush(document);
    }

    @Test
    void projectOwnerCanUpdateAnotherUsersDocument() {
        arrangeMember(owner);
        Document document = document(uploader);
        arrangeActiveDocument(document);
        when(documentRepository.saveAndFlush(document)).thenReturn(document);

        var response = documentService.update(
                projectId,
                documentId,
                new UpdateDocumentRequest("Owner update", null),
                owner.getEmail()
        );

        assertEquals("Owner update", response.title());
    }

    @Test
    void uploaderCanSoftDeleteOwnDocument() {
        arrangeMember(uploader);
        Document document = document(uploader);
        arrangeActiveDocument(document);
        when(documentRepository.saveAndFlush(document)).thenReturn(document);

        documentService.delete(projectId, documentId, uploader.getEmail());

        assertEquals(DocumentStatus.DELETED, document.getStatus());
        verify(documentRepository).saveAndFlush(document);
    }

    @Test
    void projectOwnerCanDeleteAnotherUsersDocument() {
        arrangeMember(owner);
        Document document = document(uploader);
        arrangeActiveDocument(document);
        when(documentRepository.saveAndFlush(document)).thenReturn(document);

        documentService.delete(projectId, documentId, owner.getEmail());

        assertEquals(DocumentStatus.DELETED, document.getStatus());
    }

    @Test
    void outsiderCannotDeleteDocument() {
        arrangeUserAndProject(outsider);
        when(memberRepository.existsByProjectIdAndUserId(projectId, outsider.getId()))
                .thenReturn(false);

        assertThrows(
                ForbiddenException.class,
                () -> documentService.delete(projectId, documentId, outsider.getEmail())
        );
        verify(documentRepository, never())
                .findByIdAndProjectIdAndStatus(any(), any(), any());
    }

    @Test
    void softDeletedDocumentIsHidden() {
        arrangeMember(member);
        when(documentRepository.findByIdAndProjectIdAndStatus(
                documentId,
                projectId,
                DocumentStatus.ACTIVE
        )).thenReturn(Optional.empty());

        assertThrows(
                ResourceNotFoundException.class,
                () -> documentService.get(projectId, documentId, member.getEmail())
        );
    }

    @Test
    void databaseFailureRemovesUploadedObject() {
        arrangeMember(uploader);
        when(documentRepository.saveAndFlush(any(Document.class)))
                .thenThrow(new DataIntegrityViolationException("conflict"));

        assertThrows(
                com.kbase.backend.exception.ConflictException.class,
                () -> documentService.create(projectId, file(), "Guide", null,
                        uploader.getEmail())
        );
        verify(storageService).delete(org.mockito.ArgumentMatchers.anyString());
    }

    @Test
    void storageFailureDoesNotPersistMetadata() {
        arrangeMember(uploader);
        doThrow(new StorageException("unavailable", new RuntimeException()))
                .when(storageService).upload(any(), any(), any(Long.class), any());

        assertThrows(StorageException.class,
                () -> documentService.create(projectId, file(), "Guide", null,
                        uploader.getEmail()));
        verify(documentRepository, never()).saveAndFlush(any(Document.class));
    }

    @Test
    void memberCanDownloadAndPreviewPdf() {
        arrangeMember(member);
        Document document = document(uploader);
        arrangeActiveDocument(document);
        when(storageService.download(document.getStorageKey()))
                .thenReturn(new ByteArrayInputStream(new byte[]{1}));

        assertEquals("application/pdf",
                documentService.download(projectId, documentId, member.getEmail()).contentType());
        assertEquals("application/pdf",
                documentService.preview(projectId, documentId, member.getEmail()).contentType());
    }

    @Test
    void unsupportedTypeCannotBePreviewed() {
        arrangeMember(member);
        Document document = new Document(documentId, project, uploader, "Sheet", null,
                "sheet.xlsx", "projects/key.xlsx",
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", 5);
        arrangeActiveDocument(document);

        assertThrows(UnsupportedPreviewTypeException.class,
                () -> documentService.preview(projectId, documentId, member.getEmail()));
        verify(storageService, never()).download(any());
    }

    @Test
    void documentCannotBeAccessedThroughAnotherProjectPath() {
        UUID otherProjectId = UUID.randomUUID();
        Project otherProject = project(otherProjectId, owner);
        when(userRepository.findByEmailIgnoreCase(member.getEmail()))
                .thenReturn(Optional.of(member));
        when(projectRepository.findById(otherProjectId)).thenReturn(Optional.of(otherProject));
        when(memberRepository.existsByProjectIdAndUserId(otherProjectId, member.getId()))
                .thenReturn(true);
        when(documentRepository.findByIdAndProjectIdAndStatus(
                documentId,
                otherProjectId,
                DocumentStatus.ACTIVE
        )).thenReturn(Optional.empty());

        assertThrows(
                ResourceNotFoundException.class,
                () -> documentService.get(otherProjectId, documentId, member.getEmail())
        );
    }

    @Test
    void outsiderCannotReadExtractedContent() {
        arrangeUserAndProject(outsider);
        when(memberRepository.existsByProjectIdAndUserId(projectId, outsider.getId()))
                .thenReturn(false);

        assertThrows(ForbiddenException.class,
                () -> documentService.getExtractedContent(
                        projectId, documentId, outsider.getEmail()));
        verify(extractionService, never()).get(any());
    }

    @Test
    void deletedOrWrongProjectDocumentContentIsNotFound() {
        arrangeMember(member);
        when(documentRepository.findByIdAndProjectIdAndStatus(
                documentId, projectId, DocumentStatus.ACTIVE)).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class,
                () -> documentService.getExtractedContent(
                        projectId, documentId, member.getEmail()));
    }

    @Test
    void onlyUploaderOrOwnerCanRetryExtraction() {
        arrangeMember(member);
        Document document = document(uploader);
        arrangeActiveDocument(document);
        assertThrows(ForbiddenException.class,
                () -> documentService.retryExtraction(
                        projectId, documentId, member.getEmail()));

        arrangeMember(owner);
        documentService.retryExtraction(projectId, documentId, owner.getEmail());
        verify(extractionService).retry(document);
    }

    @Test
    void onlyUploaderOrOwnerCanReindexAndDeletedDocumentIsNotIndexable() {
        arrangeMember(member);
        Document document = document(uploader);
        arrangeActiveDocument(document);
        assertThrows(ForbiddenException.class,
                () -> documentService.reindex(projectId, documentId, member.getEmail()));

        arrangeMember(owner);
        documentService.reindex(projectId, documentId, owner.getEmail());
        verify(chunkService).reindex(document);

        when(documentRepository.findByIdAndProjectIdAndStatus(
                documentId, projectId, DocumentStatus.ACTIVE)).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class,
                () -> documentService.reindex(projectId, documentId, owner.getEmail()));
    }

    private void arrangeMember(User user) {
        arrangeUserAndProject(user);
        when(memberRepository.existsByProjectIdAndUserId(projectId, user.getId()))
                .thenReturn(true);
    }

    private void arrangeUserAndProject(User user) {
        when(userRepository.findByEmailIgnoreCase(user.getEmail())).thenReturn(Optional.of(user));
        when(projectRepository.findById(projectId)).thenReturn(Optional.of(project));
    }

    private void arrangeActiveDocument(Document document) {
        when(documentRepository.findByIdAndProjectIdAndStatus(
                documentId,
                projectId,
                DocumentStatus.ACTIVE
        )).thenReturn(Optional.of(document));
    }

    private MockMultipartFile file() {
        return new MockMultipartFile("file", "architecture.pdf", "application/pdf",
                new byte[]{1, 2, 3});
    }

    private DocumentSearchCriteria filters() {
        return new DocumentSearchCriteria(null, null, null, null, null);
    }

    private Document document(User uploadedBy) {
        return new Document(
                documentId,
                project,
                uploadedBy,
                "Architecture Guide",
                "System architecture",
                "architecture.pdf",
                "projects/key.pdf",
                "application/pdf",
                123456
        );
    }

    private Project project(UUID id, User owner) {
        Project project = mock(Project.class);
        when(project.getId()).thenReturn(id);
        when(project.getOwner()).thenReturn(owner);
        return project;
    }

    private User user(UUID id, String email) {
        User user = mock(User.class);
        when(user.getId()).thenReturn(id);
        when(user.getEmail()).thenReturn(email);
        return user;
    }
}
