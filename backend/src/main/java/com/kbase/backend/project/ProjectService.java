package com.kbase.backend.project;

import com.kbase.backend.exception.ForbiddenException;
import com.kbase.backend.exception.InvalidCredentialsException;
import com.kbase.backend.exception.ResourceNotFoundException;
import com.kbase.backend.project.dto.CreateProjectRequest;
import com.kbase.backend.project.dto.ProjectResponse;
import com.kbase.backend.project.dto.UpdateProjectRequest;
import com.kbase.backend.project.member.ProjectMember;
import com.kbase.backend.project.member.ProjectMemberRepository;
import com.kbase.backend.project.member.ProjectMemberRole;
import com.kbase.backend.user.User;
import com.kbase.backend.user.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.UUID;

@Service
public class ProjectService {

    private final ProjectRepository projectRepository;
    private final ProjectMemberRepository memberRepository;
    private final UserRepository userRepository;

    public ProjectService(
            ProjectRepository projectRepository,
            ProjectMemberRepository memberRepository,
            UserRepository userRepository
    ) {
        this.projectRepository = projectRepository;
        this.memberRepository = memberRepository;
        this.userRepository = userRepository;
    }

    @Transactional
    public ProjectResponse create(CreateProjectRequest request, String authenticatedEmail) {
        User owner = authenticatedUser(authenticatedEmail);
        Project project = projectRepository.saveAndFlush(new Project(
                request.name().trim(),
                normalizeDescription(request.description()),
                owner
        ));
        memberRepository.saveAndFlush(new ProjectMember(project, owner, ProjectMemberRole.OWNER));
        return ProjectResponse.from(project);
    }

    @Transactional(readOnly = true)
    public List<ProjectResponse> listAccessible(String authenticatedEmail) {
        User user = authenticatedUser(authenticatedEmail);
        return projectRepository.findAccessibleByUserId(user.getId()).stream()
                .map(ProjectResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public ProjectResponse get(UUID projectId, String authenticatedEmail) {
        User user = authenticatedUser(authenticatedEmail);
        Project project = project(projectId);
        requireMembership(projectId, user.getId());
        return ProjectResponse.from(project);
    }

    @Transactional
    public ProjectResponse update(
            UUID projectId,
            UpdateProjectRequest request,
            String authenticatedEmail
    ) {
        User user = authenticatedUser(authenticatedEmail);
        Project project = project(projectId);
        requireOwner(project, user);
        project.update(request.name().trim(), normalizeDescription(request.description()));
        return ProjectResponse.from(projectRepository.saveAndFlush(project));
    }

    @Transactional
    public void delete(UUID projectId, String authenticatedEmail) {
        User user = authenticatedUser(authenticatedEmail);
        Project project = project(projectId);
        requireOwner(project, user);
        memberRepository.deleteAllByProjectId(projectId);
        projectRepository.delete(project);
    }

    private User authenticatedUser(String email) {
        return userRepository.findByEmailIgnoreCase(email.trim().toLowerCase(Locale.ROOT))
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

    private String normalizeDescription(String description) {
        return description == null ? null : description.trim();
    }
}
