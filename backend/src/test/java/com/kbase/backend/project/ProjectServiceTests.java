package com.kbase.backend.project;

import com.kbase.backend.exception.ForbiddenException;
import com.kbase.backend.project.dto.CreateProjectRequest;
import com.kbase.backend.project.dto.ProjectResponse;
import com.kbase.backend.project.dto.UpdateProjectRequest;
import com.kbase.backend.project.member.ProjectMember;
import com.kbase.backend.project.member.ProjectMemberRepository;
import com.kbase.backend.project.member.ProjectMemberRole;
import com.kbase.backend.user.User;
import com.kbase.backend.user.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ProjectServiceTests {

    private ProjectRepository projectRepository;
    private ProjectMemberRepository memberRepository;
    private UserRepository userRepository;
    private ProjectService projectService;

    @BeforeEach
    void setUp() {
        projectRepository = mock(ProjectRepository.class);
        memberRepository = mock(ProjectMemberRepository.class);
        userRepository = mock(UserRepository.class);
        projectService = new ProjectService(projectRepository, memberRepository, userRepository);
    }

    @Test
    void createsProjectAndAutomaticallyCreatesOwnerMembership() {
        User owner = user(UUID.randomUUID(), "owner@example.com");
        when(userRepository.findByEmailIgnoreCase("owner@example.com"))
                .thenReturn(Optional.of(owner));
        when(projectRepository.saveAndFlush(any(Project.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(memberRepository.saveAndFlush(any(ProjectMember.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        ProjectResponse response = projectService.create(
                new CreateProjectRequest(" Project Alpha ", " Knowledge base "),
                "owner@example.com"
        );

        ArgumentCaptor<Project> projectCaptor = ArgumentCaptor.forClass(Project.class);
        verify(projectRepository).saveAndFlush(projectCaptor.capture());
        Project project = projectCaptor.getValue();
        assertEquals("Project Alpha", project.getName());
        assertEquals("Knowledge base", project.getDescription());
        assertSame(owner, project.getOwner());

        ArgumentCaptor<ProjectMember> memberCaptor = ArgumentCaptor.forClass(ProjectMember.class);
        verify(memberRepository).saveAndFlush(memberCaptor.capture());
        ProjectMember membership = memberCaptor.getValue();
        assertSame(project, membership.getProject());
        assertSame(owner, membership.getUser());
        assertEquals(ProjectMemberRole.OWNER, membership.getRole());
        assertEquals("owner@example.com", response.owner().email());
    }

    @Test
    void listsOnlyProjectsReturnedForAuthenticatedUser() {
        User user = user(UUID.randomUUID(), "user@example.com");
        Project project = new Project("Accessible", null, user);
        when(userRepository.findByEmailIgnoreCase("user@example.com"))
                .thenReturn(Optional.of(user));
        when(projectRepository.findAccessibleByUserId(user.getId()))
                .thenReturn(List.of(project));

        List<ProjectResponse> result = projectService.listAccessible("user@example.com");

        assertEquals(1, result.size());
        assertEquals("Accessible", result.getFirst().name());
        verify(projectRepository).findAccessibleByUserId(user.getId());
    }

    @Test
    void rejectsProjectViewForNonMember() {
        UUID projectId = UUID.randomUUID();
        User owner = user(UUID.randomUUID(), "owner@example.com");
        User outsider = user(UUID.randomUUID(), "outsider@example.com");
        when(userRepository.findByEmailIgnoreCase("outsider@example.com"))
                .thenReturn(Optional.of(outsider));
        when(projectRepository.findById(projectId))
                .thenReturn(Optional.of(new Project("Private", null, owner)));
        when(memberRepository.existsByProjectIdAndUserId(projectId, outsider.getId()))
                .thenReturn(false);

        assertThrows(
                ForbiddenException.class,
                () -> projectService.get(projectId, "outsider@example.com")
        );
    }

    @Test
    void rejectsUpdateByNonOwner() {
        UUID projectId = UUID.randomUUID();
        User owner = user(UUID.randomUUID(), "owner@example.com");
        User member = user(UUID.randomUUID(), "member@example.com");
        Project project = new Project("Original", null, owner);
        when(userRepository.findByEmailIgnoreCase("member@example.com"))
                .thenReturn(Optional.of(member));
        when(projectRepository.findById(projectId)).thenReturn(Optional.of(project));

        assertThrows(
                ForbiddenException.class,
                () -> projectService.update(
                        projectId,
                        new UpdateProjectRequest("Changed", null),
                        "member@example.com"
                )
        );
        verify(projectRepository, never()).saveAndFlush(project);
    }

    @Test
    void rejectsDeleteByNonOwner() {
        UUID projectId = UUID.randomUUID();
        User owner = user(UUID.randomUUID(), "owner@example.com");
        User member = user(UUID.randomUUID(), "member@example.com");
        Project project = new Project("Project", null, owner);
        when(userRepository.findByEmailIgnoreCase("member@example.com"))
                .thenReturn(Optional.of(member));
        when(projectRepository.findById(projectId)).thenReturn(Optional.of(project));

        assertThrows(
                ForbiddenException.class,
                () -> projectService.delete(projectId, "member@example.com")
        );
        verify(projectRepository, never()).delete(project);
    }

    @Test
    void ownerDeleteRemovesMembershipsBeforeProject() {
        UUID projectId = UUID.randomUUID();
        User owner = user(UUID.randomUUID(), "owner@example.com");
        Project project = new Project("Project", null, owner);
        when(userRepository.findByEmailIgnoreCase("owner@example.com"))
                .thenReturn(Optional.of(owner));
        when(projectRepository.findById(projectId)).thenReturn(Optional.of(project));

        projectService.delete(projectId, "owner@example.com");

        var ordered = org.mockito.Mockito.inOrder(memberRepository, projectRepository);
        ordered.verify(memberRepository).deleteAllByProjectId(projectId);
        ordered.verify(projectRepository).delete(project);
    }

    private User user(UUID id, String email) {
        User user = mock(User.class);
        when(user.getId()).thenReturn(id);
        when(user.getEmail()).thenReturn(email);
        return user;
    }
}
