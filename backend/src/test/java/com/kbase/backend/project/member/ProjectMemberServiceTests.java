package com.kbase.backend.project.member;

import com.kbase.backend.exception.BadRequestException;
import com.kbase.backend.exception.ConflictException;
import com.kbase.backend.exception.ForbiddenException;
import com.kbase.backend.project.Project;
import com.kbase.backend.project.ProjectRepository;
import com.kbase.backend.project.member.dto.AddProjectMemberRequest;
import com.kbase.backend.user.User;
import com.kbase.backend.user.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ProjectMemberServiceTests {

    private ProjectRepository projectRepository;
    private ProjectMemberRepository memberRepository;
    private UserRepository userRepository;
    private ProjectMemberService memberService;

    @BeforeEach
    void setUp() {
        projectRepository = mock(ProjectRepository.class);
        memberRepository = mock(ProjectMemberRepository.class);
        userRepository = mock(UserRepository.class);
        memberService = new ProjectMemberService(projectRepository, memberRepository, userRepository);
    }

    @Test
    void ownerAddsMemberWithMemberRole() {
        UUID projectId = UUID.randomUUID();
        User owner = user(UUID.randomUUID(), "owner@example.com");
        User target = user(UUID.randomUUID(), "member@example.com");
        Project project = project(projectId, owner);
        arrangeUsersAndProject(projectId, owner, project);
        when(userRepository.findByEmailIgnoreCase("member@example.com"))
                .thenReturn(Optional.of(target));
        when(memberRepository.existsByProjectIdAndUserId(projectId, target.getId()))
                .thenReturn(false);
        when(memberRepository.saveAndFlush(any(ProjectMember.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        var response = memberService.add(
                projectId,
                new AddProjectMemberRequest(" MEMBER@EXAMPLE.COM "),
                "owner@example.com"
        );

        assertEquals(ProjectMemberRole.MEMBER, response.role());
        assertEquals("member@example.com", response.email());
    }

    @Test
    void preventsDuplicateMembership() {
        UUID projectId = UUID.randomUUID();
        User owner = user(UUID.randomUUID(), "owner@example.com");
        User target = user(UUID.randomUUID(), "member@example.com");
        Project project = project(projectId, owner);
        arrangeUsersAndProject(projectId, owner, project);
        when(userRepository.findByEmailIgnoreCase("member@example.com"))
                .thenReturn(Optional.of(target));
        when(memberRepository.existsByProjectIdAndUserId(projectId, target.getId()))
                .thenReturn(true);

        assertThrows(
                ConflictException.class,
                () -> memberService.add(
                        projectId,
                        new AddProjectMemberRequest("member@example.com"),
                        "owner@example.com"
                )
        );
        verify(memberRepository, never()).saveAndFlush(any(ProjectMember.class));
    }

    @Test
    void nonOwnerCannotAddMember() {
        UUID projectId = UUID.randomUUID();
        User owner = user(UUID.randomUUID(), "owner@example.com");
        User member = user(UUID.randomUUID(), "member@example.com");
        Project project = project(projectId, owner);
        when(userRepository.findByEmailIgnoreCase("member@example.com"))
                .thenReturn(Optional.of(member));
        when(projectRepository.findById(projectId)).thenReturn(Optional.of(project));

        assertThrows(
                ForbiddenException.class,
                () -> memberService.add(
                        projectId,
                        new AddProjectMemberRequest("other@example.com"),
                        "member@example.com"
                )
        );
    }

    @Test
    void nonOwnerCannotRemoveMember() {
        UUID projectId = UUID.randomUUID();
        User owner = user(UUID.randomUUID(), "owner@example.com");
        User member = user(UUID.randomUUID(), "member@example.com");
        Project project = project(projectId, owner);
        when(userRepository.findByEmailIgnoreCase("member@example.com"))
                .thenReturn(Optional.of(member));
        when(projectRepository.findById(projectId)).thenReturn(Optional.of(project));

        assertThrows(
                ForbiddenException.class,
                () -> memberService.remove(projectId, UUID.randomUUID(), "member@example.com")
        );
        verify(memberRepository, never()).delete(any(ProjectMember.class));
    }

    @Test
    void ownerCannotRemoveThemselves() {
        UUID projectId = UUID.randomUUID();
        User owner = user(UUID.randomUUID(), "owner@example.com");
        Project project = project(projectId, owner);
        ProjectMember ownerMembership = new ProjectMember(project, owner, ProjectMemberRole.OWNER);
        arrangeUsersAndProject(projectId, owner, project);
        when(memberRepository.findByProjectIdAndUserId(projectId, owner.getId()))
                .thenReturn(Optional.of(ownerMembership));

        assertThrows(
                BadRequestException.class,
                () -> memberService.remove(projectId, owner.getId(), "owner@example.com")
        );
        verify(memberRepository, never()).delete(ownerMembership);
    }

    private void arrangeUsersAndProject(UUID projectId, User owner, Project project) {
        when(userRepository.findByEmailIgnoreCase("owner@example.com"))
                .thenReturn(Optional.of(owner));
        when(projectRepository.findById(projectId)).thenReturn(Optional.of(project));
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
