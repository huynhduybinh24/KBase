package com.kbase.backend.project;

import com.kbase.backend.project.dto.CreateProjectRequest;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.userdetails.UserDetails;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ProjectControllerTests {

    @Test
    void createUsesAuthenticatedPrincipalWithoutAcceptingCurrentUserId() {
        ProjectService service = mock(ProjectService.class);
        UserDetails principal = mock(UserDetails.class);
        CreateProjectRequest request = new CreateProjectRequest("Project", null);
        when(principal.getUsername()).thenReturn("owner@example.com");

        new ProjectController(service).create(request, principal);

        verify(service).create(request, "owner@example.com");
    }
}
