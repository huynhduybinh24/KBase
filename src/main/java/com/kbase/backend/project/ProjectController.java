package com.kbase.backend.project;

import com.kbase.backend.project.dto.CreateProjectRequest;
import com.kbase.backend.project.dto.ProjectResponse;
import com.kbase.backend.project.dto.UpdateProjectRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/projects")
@Tag(name = "Projects")
@SecurityRequirement(name = "bearerAuth")
public class ProjectController {

    private final ProjectService projectService;

    public ProjectController(ProjectService projectService) {
        this.projectService = projectService;
    }

    @PostMapping
    @Operation(summary = "Create a project")
    public ResponseEntity<ProjectResponse> create(
            @Valid @RequestBody CreateProjectRequest request,
            @AuthenticationPrincipal UserDetails principal
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(projectService.create(request, principal.getUsername()));
    }

    @GetMapping
    @Operation(summary = "List projects accessible to the authenticated user")
    public List<ProjectResponse> list(@AuthenticationPrincipal UserDetails principal) {
        return projectService.listAccessible(principal.getUsername());
    }

    @GetMapping("/{projectId}")
    @Operation(
            summary = "Get an accessible project",
            responses = @ApiResponse(responseCode = "403", description = "Not a project member")
    )
    public ProjectResponse get(
            @PathVariable UUID projectId,
            @AuthenticationPrincipal UserDetails principal
    ) {
        return projectService.get(projectId, principal.getUsername());
    }

    @PutMapping("/{projectId}")
    @Operation(summary = "Update a project (owner only)")
    public ProjectResponse update(
            @PathVariable UUID projectId,
            @Valid @RequestBody UpdateProjectRequest request,
            @AuthenticationPrincipal UserDetails principal
    ) {
        return projectService.update(projectId, request, principal.getUsername());
    }

    @DeleteMapping("/{projectId}")
    @Operation(summary = "Delete a project (owner only)")
    public ResponseEntity<Void> delete(
            @PathVariable UUID projectId,
            @AuthenticationPrincipal UserDetails principal
    ) {
        projectService.delete(projectId, principal.getUsername());
        return ResponseEntity.noContent().build();
    }
}
