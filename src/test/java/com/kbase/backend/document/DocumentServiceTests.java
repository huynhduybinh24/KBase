package com.kbase.backend.document;

import com.kbase.backend.document.dto.CreateDocumentRequest;
import com.kbase.backend.document.dto.UpdateDocumentRequest;
import com.kbase.backend.exception.ConflictException;
import com.kbase.backend.exception.ForbiddenException;
import com.kbase.backend.exception.ResourceNotFoundException;
import com.kbase.backend.project.Project;
import com.kbase.backend.project.ProjectRepository;
import com.kbase.backend.project.member.ProjectMemberRepository;
import com.kbase.backend.user.User;
import com.kbase.backend.user.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class DocumentServiceTests {

    private DocumentRepository documentRepository;
    private ProjectRepository projectRepository;
    private ProjectMemberRepository memberRepository;
    private UserRepository userRepository;
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
        documentService = new DocumentService(
                documentRepository,
                projectRepository,
                memberRepository,
                userRepository
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
        when(documentRepository.existsByStorageKey("projects/key.pdf")).thenReturn(false);
        when(documentRepository.saveAndFlush(any(Document.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        var response = documentService.create(projectId, createRequest(), uploader.getEmail());

        assertEquals("Architecture Guide", response.title());
        assertEquals("projects/key.pdf", response.storageKey());
        assertEquals(DocumentStatus.ACTIVE, response.status());
        assertEquals(uploader.getEmail(), response.uploadedBy().email());
    }

    @Test
    void outsiderCannotAccessProjectDocuments() {
        arrangeUserAndProject(outsider);
        when(memberRepository.existsByProjectIdAndUserId(projectId, outsider.getId()))
                .thenReturn(false);

        assertThrows(
                ForbiddenException.class,
                () -> documentService.list(projectId, outsider.getEmail())
        );
        verify(documentRepository, never())
                .findAllByProjectIdAndStatusOrderByCreatedAtDesc(any(), any());
    }

    @Test
    void memberCanListActiveDocuments() {
        arrangeMember(member);
        Document document = document(uploader);
        when(documentRepository.findAllByProjectIdAndStatusOrderByCreatedAtDesc(
                projectId,
                DocumentStatus.ACTIVE
        )).thenReturn(List.of(document));

        var result = documentService.list(projectId, member.getEmail());

        assertEquals(1, result.size());
        assertEquals(DocumentStatus.ACTIVE, result.getFirst().status());
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
    void duplicateStorageKeyReturnsConflict() {
        arrangeMember(uploader);
        when(documentRepository.existsByStorageKey("projects/key.pdf")).thenReturn(true);

        assertThrows(
                ConflictException.class,
                () -> documentService.create(projectId, createRequest(), uploader.getEmail())
        );
        verify(documentRepository, never()).saveAndFlush(any(Document.class));
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

    private CreateDocumentRequest createRequest() {
        return new CreateDocumentRequest(
                " Architecture Guide ",
                " System architecture ",
                " architecture.pdf ",
                " projects/key.pdf ",
                " application/pdf ",
                123456
        );
    }

    private Document document(User uploadedBy) {
        return new Document(
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
