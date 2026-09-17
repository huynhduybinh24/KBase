package com.kbase.backend.chat;

import com.kbase.backend.exception.ForbiddenException;
import com.kbase.backend.exception.ResourceNotFoundException;
import com.kbase.backend.project.Project;
import com.kbase.backend.project.ProjectRepository;
import com.kbase.backend.project.member.ProjectMemberRepository;
import com.kbase.backend.rag.llm.LlmResponse;
import com.kbase.backend.rag.retrieval.SemanticSearchResult;
import com.kbase.backend.user.User;
import com.kbase.backend.user.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageImpl;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ChatPersistenceServiceTests {

    private ChatSessionRepository sessions;
    private ChatMessageRepository messages;
    private ProjectRepository projects;
    private ProjectMemberRepository members;
    private UserRepository users;
    private ChatPersistenceService service;
    private UUID projectId;
    private User user;
    private Project project;
    private ChatSession session;

    @BeforeEach
    void setUp() {
        sessions = mock(ChatSessionRepository.class);
        messages = mock(ChatMessageRepository.class);
        projects = mock(ProjectRepository.class);
        members = mock(ProjectMemberRepository.class);
        users = mock(UserRepository.class);
        service = new ChatPersistenceService(sessions, messages, projects, members, users);
        projectId = UUID.randomUUID();
        user = mock(User.class);
        project = mock(Project.class);
        when(user.getId()).thenReturn(UUID.randomUUID());
        when(user.getEmail()).thenReturn("user@example.com");
        when(project.getId()).thenReturn(projectId);
        when(users.findByEmailIgnoreCase("user@example.com")).thenReturn(Optional.of(user));
        when(projects.findById(projectId)).thenReturn(Optional.of(project));
        when(members.existsByProjectIdAndUserId(projectId, user.getId())).thenReturn(true);
        session = new ChatSession(project, user, null);
        when(sessions.findByIdAndProjectIdAndCreatedById(
                session.getId(), projectId, user.getId())).thenReturn(Optional.of(session));
        when(messages.saveAndFlush(any(ChatMessage.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    void memberCreatesSessionAndFirstQuestionAssignsDeterministicTitle() {
        when(sessions.saveAndFlush(any(ChatSession.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        var created = service.create(projectId, null, "user@example.com");
        service.saveUserMessage(projectId, session.getId(),
                "  How does authentication work in this application?  ", "user@example.com");

        assertEquals(projectId, created.projectId());
        assertEquals("How does authentication work in this application?", session.getTitle());
    }

    @Test
    void outsiderCannotCreateAndOtherUsersPrivateSessionIsHidden() {
        when(members.existsByProjectIdAndUserId(projectId, user.getId())).thenReturn(false);
        assertThrows(ForbiddenException.class,
                () -> service.create(projectId, null, "user@example.com"));

        when(members.existsByProjectIdAndUserId(projectId, user.getId())).thenReturn(true);
        when(sessions.findByIdAndProjectIdAndCreatedById(
                session.getId(), projectId, user.getId())).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class,
                () -> service.get(projectId, session.getId(), "user@example.com"));
    }

    @Test
    void assistantSourcesArePersistedAndMappedExactly() {
        SemanticSearchResult source = new SemanticSearchResult(
                UUID.randomUUID(), UUID.randomUUID(), "Auth Guide", 4,
                "content", 0.88);

        var response = service.saveAssistantMessage(projectId, session.getId(),
                new LlmResponse("answer", "model", 12, 4), List.of(source),
                "user@example.com");

        assertEquals(ChatRole.ASSISTANT, response.role());
        assertEquals(1, response.sources().size());
        assertEquals(source.chunkId(), response.sources().getFirst().chunkId());
        assertEquals(source.documentTitle(), response.sources().getFirst().documentTitle());
        assertEquals(source.score(), response.sources().getFirst().score());
    }

    @Test
    void messagesAreReturnedInRepositoryChronologicalOrderAndDeleteIsScoped() {
        ChatMessage first = new ChatMessage(session, ChatRole.USER, "first", null, null, null);
        ChatMessage second = new ChatMessage(session, ChatRole.ASSISTANT, "second", null, null, null);
        when(messages.findAllBySessionIdOrderByCreatedAtAsc(session.getId()))
                .thenReturn(List.of(first, second));

        var history = service.messages(projectId, session.getId(), "user@example.com");
        service.delete(projectId, session.getId(), "user@example.com");

        assertEquals(List.of("first", "second"),
                history.stream().map(message -> message.content()).toList());
        verify(sessions).delete(session);
    }

    @Test
    void listsOnlyCurrentUsersSessionsWithinProject() {
        var userId = user.getId();
        when(sessions.findAllByProjectIdAndCreatedById(
                org.mockito.ArgumentMatchers.eq(projectId),
                org.mockito.ArgumentMatchers.eq(userId), any()))
                .thenReturn(new PageImpl<>(List.of(session)));

        var page = service.list(projectId, 0, 20, "user@example.com");

        assertEquals(1, page.totalElements());
        assertEquals(session.getId(), page.content().getFirst().id());
    }
}
