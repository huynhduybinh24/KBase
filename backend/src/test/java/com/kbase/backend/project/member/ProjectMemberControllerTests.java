package com.kbase.backend.project.member;

import com.kbase.backend.project.member.dto.AddProjectMemberRequest;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.UUID;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ProjectMemberControllerTests {

    @Test
    void addUsesAuthenticatedPrincipalAsActingUser() {
        ProjectMemberService service = mock(ProjectMemberService.class);
        UserDetails principal = mock(UserDetails.class);
        UUID projectId = UUID.randomUUID();
        AddProjectMemberRequest request = new AddProjectMemberRequest("member@example.com");
        when(principal.getUsername()).thenReturn("owner@example.com");

        new ProjectMemberController(service).add(projectId, request, principal);

        verify(service).add(projectId, request, "owner@example.com");
    }
}
