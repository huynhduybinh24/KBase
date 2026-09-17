package com.kbase.backend.rag.retrieval;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@Validated
@RequestMapping("/api/projects/{projectId}/search")
@Tag(name = "Semantic Search")
@SecurityRequirement(name = "bearerAuth")
public class SemanticSearchController {

    private final SemanticSearchService service;

    public SemanticSearchController(SemanticSearchService service) {
        this.service = service;
    }

    @GetMapping("/semantic")
    @Operation(summary = "Search indexed document chunks by cosine similarity",
            description = "A score closer to 1 indicates greater cosine similarity. Raw vectors are never returned.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Ranked chunks"),
            @ApiResponse(responseCode = "400", description = "Invalid query or topK"),
            @ApiResponse(responseCode = "401", description = "Unauthenticated"),
            @ApiResponse(responseCode = "403", description = "Not a project member"),
            @ApiResponse(responseCode = "404", description = "Project or document not found"),
            @ApiResponse(responseCode = "503", description = "Embedding provider unavailable")
    })
    public SemanticSearchResponse search(
            @PathVariable UUID projectId,
            @RequestParam @NotBlank String q,
            @RequestParam(defaultValue = "5") @Min(1) @Max(20) int topK,
            @RequestParam(required = false) UUID documentId,
            @AuthenticationPrincipal UserDetails principal
    ) {
        return service.search(projectId, q, topK, documentId, principal.getUsername());
    }
}
