package com.kbase.backend.document;

import com.kbase.backend.document.dto.CreateDocumentRequest;
import com.kbase.backend.document.dto.DocumentResponse;
import com.kbase.backend.document.dto.UpdateDocumentRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
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
@RequestMapping("/api/projects/{projectId}/documents")
@Tag(name = "Documents")
@SecurityRequirement(name = "bearerAuth")
@ApiResponses({
        @ApiResponse(responseCode = "400", description = "Invalid request"),
        @ApiResponse(responseCode = "401", description = "Unauthenticated"),
        @ApiResponse(responseCode = "403", description = "Insufficient project permission"),
        @ApiResponse(responseCode = "404", description = "Project or document not found")
})
public class DocumentController {

    private final DocumentService documentService;

    public DocumentController(DocumentService documentService) {
        this.documentService = documentService;
    }

    @PostMapping
    @Operation(summary = "Create document metadata")
    @ApiResponse(responseCode = "409", description = "Storage key already exists")
    public ResponseEntity<DocumentResponse> create(
            @PathVariable UUID projectId,
            @Valid @RequestBody CreateDocumentRequest request,
            @AuthenticationPrincipal UserDetails principal
    ) {
        return ResponseEntity.status(HttpStatus.CREATED).body(
                documentService.create(projectId, request, principal.getUsername()));
    }

    @GetMapping
    @Operation(summary = "List active project documents")
    public List<DocumentResponse> list(
            @PathVariable UUID projectId,
            @AuthenticationPrincipal UserDetails principal
    ) {
        return documentService.list(projectId, principal.getUsername());
    }

    @GetMapping("/{documentId}")
    @Operation(summary = "Get an active project document")
    public DocumentResponse get(
            @PathVariable UUID projectId,
            @PathVariable UUID documentId,
            @AuthenticationPrincipal UserDetails principal
    ) {
        return documentService.get(projectId, documentId, principal.getUsername());
    }

    @PutMapping("/{documentId}")
    @Operation(summary = "Update document title and description")
    public DocumentResponse update(
            @PathVariable UUID projectId,
            @PathVariable UUID documentId,
            @Valid @RequestBody UpdateDocumentRequest request,
            @AuthenticationPrincipal UserDetails principal
    ) {
        return documentService.update(projectId, documentId, request, principal.getUsername());
    }

    @DeleteMapping("/{documentId}")
    @Operation(summary = "Soft-delete a document")
    public ResponseEntity<Void> delete(
            @PathVariable UUID projectId,
            @PathVariable UUID documentId,
            @AuthenticationPrincipal UserDetails principal
    ) {
        documentService.delete(projectId, documentId, principal.getUsername());
        return ResponseEntity.noContent().build();
    }
}
