package com.kbase.backend.project.member;

import com.kbase.backend.project.member.dto.AddProjectMemberRequest;
import com.kbase.backend.project.member.dto.ProjectMemberResponse;
import io.swagger.v3.oas.annotations.Operation;
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
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/projects/{projectId}/members")
@Tag(name = "Project Members")
@SecurityRequirement(name = "bearerAuth")
public class ProjectMemberController {

    private final ProjectMemberService memberService;

    public ProjectMemberController(ProjectMemberService memberService) {
        this.memberService = memberService;
    }

    @GetMapping
    @Operation(summary = "List project members")
    public List<ProjectMemberResponse> list(
            @PathVariable UUID projectId,
            @AuthenticationPrincipal UserDetails principal
    ) {
        return memberService.list(projectId, principal.getUsername());
    }

    @PostMapping
    @Operation(summary = "Add a project member by email (owner only)")
    public ResponseEntity<ProjectMemberResponse> add(
            @PathVariable UUID projectId,
            @Valid @RequestBody AddProjectMemberRequest request,
            @AuthenticationPrincipal UserDetails principal
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(memberService.add(projectId, request, principal.getUsername()));
    }

    @DeleteMapping("/{userId}")
    @Operation(summary = "Remove a project member (owner only)")
    public ResponseEntity<Void> remove(
            @PathVariable UUID projectId,
            @PathVariable UUID userId,
            @AuthenticationPrincipal UserDetails principal
    ) {
        memberService.remove(projectId, userId, principal.getUsername());
        return ResponseEntity.noContent().build();
    }
}
