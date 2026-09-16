package com.kbase.backend.project.member;

import com.kbase.backend.exception.BadRequestException;
import com.kbase.backend.exception.ConflictException;
import com.kbase.backend.exception.ForbiddenException;
import com.kbase.backend.exception.InvalidCredentialsException;
import com.kbase.backend.exception.ResourceNotFoundException;
import com.kbase.backend.project.Project;
import com.kbase.backend.project.ProjectRepository;
import com.kbase.backend.project.member.dto.AddProjectMemberRequest;
import com.kbase.backend.project.member.dto.ProjectMemberResponse;
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
public class ProjectMemberService {

    private final ProjectRepository projectRepository;
    private final ProjectMemberRepository memberRepository;
    private final UserRepository userRepository;

    public ProjectMemberService(
            ProjectRepository projectRepository,
            ProjectMemberRepository memberRepository,
            UserRepository userRepository
    ) {
        this.projectRepository = projectRepository;
        this.memberRepository = memberRepository;
        this.userRepository = userRepository;
    }

    @Transactional(readOnly = true)
    public List<ProjectMemberResponse> list(UUID projectId, String authenticatedEmail) {
        User currentUser = authenticatedUser(authenticatedEmail);
        Project project = project(projectId);
        requireMembership(project.getId(), currentUser.getId());
        return memberRepository.findAllByProjectIdOrderByJoinedAtAsc(projectId).stream()
                .map(ProjectMemberResponse::from)
                .toList();
    }

    @Transactional
    public ProjectMemberResponse add(
            UUID projectId,
            AddProjectMemberRequest request,
            String authenticatedEmail
    ) {
        User currentUser = authenticatedUser(authenticatedEmail);
        Project project = project(projectId);
        requireOwner(project, currentUser);

        User targetUser = userRepository.findByEmailIgnoreCase(normalizeEmail(request.email()))
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        if (memberRepository.existsByProjectIdAndUserId(projectId, targetUser.getId())) {
            throw new ConflictException("User is already a project member");
        }

        try {
            ProjectMember member = memberRepository.saveAndFlush(
                    new ProjectMember(project, targetUser, ProjectMemberRole.MEMBER));
            return ProjectMemberResponse.from(member);
        } catch (DataIntegrityViolationException exception) {
            throw new ConflictException("User is already a project member");
        }
    }

    @Transactional
    public void remove(UUID projectId, UUID userId, String authenticatedEmail) {
        User currentUser = authenticatedUser(authenticatedEmail);
        Project project = project(projectId);
        requireOwner(project, currentUser);

        ProjectMember member = memberRepository.findByProjectIdAndUserId(projectId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Project member not found"));
        if (Objects.equals(project.getOwner().getId(), userId)
                || member.getRole() == ProjectMemberRole.OWNER) {
            throw new BadRequestException("The project owner cannot be removed");
        }
        memberRepository.delete(member);
    }

    private User authenticatedUser(String email) {
        return userRepository.findByEmailIgnoreCase(normalizeEmail(email))
                .orElseThrow(InvalidCredentialsException::new);
    }

    private Project project(UUID projectId) {
        return projectRepository.findById(projectId)
                .orElseThrow(() -> new ResourceNotFoundException("Project not found"));
    }

    private void requireMembership(UUID projectId, UUID userId) {
        if (!memberRepository.existsByProjectIdAndUserId(projectId, userId)) {
            throw new ForbiddenException("You are not a member of this project");
        }
    }

    private void requireOwner(Project project, User user) {
        if (!Objects.equals(project.getOwner().getId(), user.getId())) {
            throw new ForbiddenException("Only the project owner can perform this action");
        }
    }

    private String normalizeEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }
}
