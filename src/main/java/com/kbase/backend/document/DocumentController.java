package com.kbase.backend.document;

import com.kbase.backend.document.dto.DocumentResponse;
import com.kbase.backend.document.dto.DocumentPageResponse;
import com.kbase.backend.document.dto.UpdateDocumentRequest;
import com.kbase.backend.document.extraction.dto.DocumentContentResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.validation.annotation.Validated;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.Instant;
import java.util.UUID;

@RestController
@Validated
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

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Upload a document and create its metadata")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Document uploaded"),
            @ApiResponse(responseCode = "413", description = "File exceeds the configured limit"),
            @ApiResponse(responseCode = "503", description = "Object storage unavailable")
    })
    public ResponseEntity<DocumentResponse> create(
            @PathVariable UUID projectId,
            @RequestParam("file") MultipartFile file,
            @RequestParam("title") @NotBlank @Size(max = 200) String title,
            @RequestParam(value = "description", required = false) @Size(max = 5000)
            String description,
            @AuthenticationPrincipal UserDetails principal
    ) {
        return ResponseEntity.status(HttpStatus.CREATED).body(
                documentService.create(projectId, file, title, description,
                        principal.getUsername()));
    }

    @GetMapping
    @Operation(summary = "Search and filter active project documents",
            description = "Combines optional filters with project isolation and pagination. "
                    + "Sort format is field,direction; allowed fields: createdAt, updatedAt, title, fileSize.")
    public DocumentPageResponse list(
            @PathVariable UUID projectId,
            @RequestParam(required = false) String q,
            @RequestParam(required = false) String contentType,
            @RequestParam(required = false) UUID uploadedBy,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant from,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant to,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size,
            @RequestParam(defaultValue = "createdAt,desc") String sort,
            @AuthenticationPrincipal UserDetails principal
    ) {
        return documentService.list(projectId,
                new DocumentSearchCriteria(q, contentType, uploadedBy, from, to),
                page, size, sort, principal.getUsername());
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

    @GetMapping("/{documentId}/content")
    @Operation(summary = "Get extracted document text and extraction status")
    public DocumentContentResponse extractedContent(
            @PathVariable UUID projectId,
            @PathVariable UUID documentId,
            @AuthenticationPrincipal UserDetails principal
    ) {
        return documentService.getExtractedContent(
                projectId, documentId, principal.getUsername());
    }

    @PostMapping("/{documentId}/extract")
    @Operation(summary = "Retry text extraction",
            description = "Available to the uploader or project owner for pending, failed, or unsupported documents.")
    @ApiResponse(responseCode = "409", description = "Extraction is processing or already complete")
    public DocumentContentResponse retryExtraction(
            @PathVariable UUID projectId,
            @PathVariable UUID documentId,
            @AuthenticationPrincipal UserDetails principal
    ) {
        return documentService.retryExtraction(projectId, documentId, principal.getUsername());
    }

    @GetMapping("/{documentId}/download")
    @Operation(summary = "Download a document", description = "Returns the stored object as an attachment")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Document stream"),
            @ApiResponse(responseCode = "503", description = "Object storage unavailable")
    })
    public ResponseEntity<InputStreamResource> download(
            @PathVariable UUID projectId,
            @PathVariable UUID documentId,
            @AuthenticationPrincipal UserDetails principal
    ) {
        return stream(documentService.download(projectId, documentId, principal.getUsername()),
                false);
    }

    @GetMapping("/{documentId}/preview")
    @Operation(summary = "Preview a document inline",
            description = "Supports PDF, PNG, JPEG, and plain text")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Inline document stream"),
            @ApiResponse(responseCode = "415", description = "Document type is not previewable"),
            @ApiResponse(responseCode = "503", description = "Object storage unavailable")
    })
    public ResponseEntity<InputStreamResource> preview(
            @PathVariable UUID projectId,
            @PathVariable UUID documentId,
            @AuthenticationPrincipal UserDetails principal
    ) {
        return stream(documentService.preview(projectId, documentId, principal.getUsername()),
                true);
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

    private ResponseEntity<InputStreamResource> stream(
            DocumentService.DocumentContent content,
            boolean inline
    ) {
        ContentDisposition disposition = (inline
                ? ContentDisposition.inline()
                : ContentDisposition.attachment())
                .filename(content.fileName(), java.nio.charset.StandardCharsets.UTF_8)
                .build();
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(content.contentType()))
                .contentLength(content.size())
                .header(HttpHeaders.CONTENT_DISPOSITION, disposition.toString())
                .body(new InputStreamResource(content.stream()));
    }
}
