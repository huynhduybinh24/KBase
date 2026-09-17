package com.kbase.backend.chat;

import com.kbase.backend.chat.dto.ChatMessageResponse;
import com.kbase.backend.chat.dto.ChatSessionPageResponse;
import com.kbase.backend.chat.dto.ChatSessionResponse;
import com.kbase.backend.exception.ForbiddenException;
import com.kbase.backend.exception.InvalidCredentialsException;
import com.kbase.backend.exception.ResourceNotFoundException;
import com.kbase.backend.project.Project;
import com.kbase.backend.project.ProjectRepository;
import com.kbase.backend.project.member.ProjectMemberRepository;
import com.kbase.backend.rag.llm.LlmResponse;
import com.kbase.backend.rag.retrieval.SemanticSearchResult;
import com.kbase.backend.user.User;
import com.kbase.backend.user.UserRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

@Service
public class ChatPersistenceService {

    private final ChatSessionRepository sessionRepository;
    private final ChatMessageRepository messageRepository;
    private final ProjectRepository projectRepository;
    private final ProjectMemberRepository memberRepository;
    private final UserRepository userRepository;

    public ChatPersistenceService(
            ChatSessionRepository sessionRepository,
            ChatMessageRepository messageRepository,
            ProjectRepository projectRepository,
            ProjectMemberRepository memberRepository,
            UserRepository userRepository
    ) {
        this.sessionRepository = sessionRepository;
        this.messageRepository = messageRepository;
        this.projectRepository = projectRepository;
        this.memberRepository = memberRepository;
        this.userRepository = userRepository;
    }

    @Transactional
    public ChatSessionResponse create(UUID projectId, String title, String email) {
        AccessContext context = requireMember(projectId, email);
        String normalized = title == null || title.isBlank() ? null : title.trim();
        return ChatSessionResponse.from(sessionRepository.saveAndFlush(
                new ChatSession(context.project(), context.user(), normalized)));
    }

    @Transactional(readOnly = true)
    public ChatSessionPageResponse list(
            UUID projectId, int page, int size, String email
    ) {
        AccessContext context = requireMember(projectId, email);
        return ChatSessionPageResponse.from(sessionRepository.findAllByProjectIdAndCreatedById(
                projectId, context.user().getId(), PageRequest.of(page, size,
                        Sort.by(Sort.Direction.DESC, "updatedAt"))));
    }

    @Transactional(readOnly = true)
    public ChatSessionResponse get(UUID projectId, UUID sessionId, String email) {
        return ChatSessionResponse.from(requireOwnedSession(projectId, sessionId, email).session());
    }

    @Transactional(readOnly = true)
    public List<ChatMessageResponse> messages(UUID projectId, UUID sessionId, String email) {
        OwnedSession owned = requireOwnedSession(projectId, sessionId, email);
        return messageRepository.findAllBySessionIdOrderByCreatedAtAsc(owned.session().getId())
                .stream().map(ChatMessageResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public List<ChatMessage> recentHistory(
            UUID projectId, UUID sessionId, int maximum, String email
    ) {
        OwnedSession owned = requireOwnedSession(projectId, sessionId, email);
        if (maximum == 0) {
            return List.of();
        }
        List<ChatMessage> descending = new ArrayList<>(messageRepository
                .findBySessionIdOrderByCreatedAtDesc(
                        owned.session().getId(), PageRequest.of(0, maximum)));
        Collections.reverse(descending);
        return List.copyOf(descending);
    }

    @Transactional
    public ChatMessage saveUserMessage(
            UUID projectId, UUID sessionId, String content, String email
    ) {
        OwnedSession owned = requireOwnedSession(projectId, sessionId, email);
        owned.session().assignTitleIfAbsent(titleFromQuestion(content));
        owned.session().touch();
        sessionRepository.save(owned.session());
        return messageRepository.saveAndFlush(new ChatMessage(
                owned.session(), ChatRole.USER, content, null, null, null));
    }

    @Transactional
    public ChatMessageResponse saveAssistantMessage(
            UUID projectId,
            UUID sessionId,
            LlmResponse response,
            List<SemanticSearchResult> sources,
            String email
    ) {
        OwnedSession owned = requireOwnedSession(projectId, sessionId, email);
        ChatMessage message = new ChatMessage(
                owned.session(), ChatRole.ASSISTANT, response.answer(), response.model(),
                response.inputTokenCount(), response.outputTokenCount());
        sources.forEach(source -> message.addSource(new ChatMessageSource(message, source)));
        owned.session().touch();
        sessionRepository.save(owned.session());
        return ChatMessageResponse.from(messageRepository.saveAndFlush(message));
    }

    @Transactional
    public void delete(UUID projectId, UUID sessionId, String email) {
        OwnedSession owned = requireOwnedSession(projectId, sessionId, email);
        sessionRepository.delete(owned.session());
    }

    private OwnedSession requireOwnedSession(UUID projectId, UUID sessionId, String email) {
        AccessContext context = requireMember(projectId, email);
        ChatSession session = sessionRepository.findByIdAndProjectIdAndCreatedById(
                        sessionId, projectId, context.user().getId())
                .orElseThrow(() -> new ResourceNotFoundException("Chat session not found"));
        return new OwnedSession(session, context.user());
    }

    private AccessContext requireMember(UUID projectId, String email) {
        User user = userRepository.findByEmailIgnoreCase(email.trim().toLowerCase(Locale.ROOT))
                .orElseThrow(InvalidCredentialsException::new);
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new ResourceNotFoundException("Project not found"));
        if (!memberRepository.existsByProjectIdAndUserId(projectId, user.getId())) {
            throw new ForbiddenException("You are not a member of this project");
        }
        return new AccessContext(project, user);
    }

    private String titleFromQuestion(String question) {
        String normalized = question.trim().replaceAll("\\s+", " ");
        int maximumCodePoints = Math.min(80, normalized.codePointCount(0, normalized.length()));
        return normalized.substring(0, normalized.offsetByCodePoints(0, maximumCodePoints));
    }

    private record AccessContext(Project project, User user) { }
    private record OwnedSession(ChatSession session, User user) { }
}
