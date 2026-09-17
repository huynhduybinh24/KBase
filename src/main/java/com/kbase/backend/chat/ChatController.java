package com.kbase.backend.chat;

import com.kbase.backend.chat.dto.AskChatRequest;
import com.kbase.backend.chat.dto.ChatMessageResponse;
import com.kbase.backend.chat.dto.ChatSessionPageResponse;
import com.kbase.backend.chat.dto.ChatSessionResponse;
import com.kbase.backend.chat.dto.CreateChatSessionRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@Validated
@RequestMapping("/api/projects/{projectId}/chat/sessions")
@Tag(name = "Chat")
@SecurityRequirement(name = "bearerAuth")
@ApiResponses({
        @ApiResponse(responseCode = "400", description = "Invalid request"),
        @ApiResponse(responseCode = "401", description = "Unauthenticated"),
        @ApiResponse(responseCode = "403", description = "Not a project member"),
        @ApiResponse(responseCode = "404", description = "Project or private session not found")
})
public class ChatController {

    private final ChatPersistenceService persistence;
    private final ChatService chatService;

    public ChatController(ChatPersistenceService persistence, ChatService chatService) {
        this.persistence = persistence;
        this.chatService = chatService;
    }

    @PostMapping
    @Operation(summary = "Create a private chat session")
    @ApiResponse(responseCode = "201", description = "Session created")
    public ResponseEntity<ChatSessionResponse> create(
            @PathVariable UUID projectId,
            @Valid @RequestBody CreateChatSessionRequest request,
            @AuthenticationPrincipal UserDetails principal
    ) {
        return ResponseEntity.status(HttpStatus.CREATED).body(
                persistence.create(projectId, request.title(), principal.getUsername()));
    }

    @GetMapping
    @Operation(summary = "List the current user's project chat sessions")
    public ChatSessionPageResponse list(
            @PathVariable UUID projectId,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size,
            @AuthenticationPrincipal UserDetails principal
    ) {
        return persistence.list(projectId, page, size, principal.getUsername());
    }

    @GetMapping("/{sessionId}")
    @Operation(summary = "Get private chat session metadata")
    public ChatSessionResponse get(
            @PathVariable UUID projectId,
            @PathVariable UUID sessionId,
            @AuthenticationPrincipal UserDetails principal
    ) {
        return persistence.get(projectId, sessionId, principal.getUsername());
    }

    @GetMapping("/{sessionId}/messages")
    @Operation(summary = "Get private chat history in chronological order")
    public List<ChatMessageResponse> messages(
            @PathVariable UUID projectId,
            @PathVariable UUID sessionId,
            @AuthenticationPrincipal UserDetails principal
    ) {
        return persistence.messages(projectId, sessionId, principal.getUsername());
    }

    @PostMapping("/{sessionId}/messages")
    @Operation(summary = "Ask the grounded project assistant")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Assistant answer with structured sources"),
            @ApiResponse(responseCode = "503", description = "Embedding or language model provider unavailable")
    })
    public ChatMessageResponse ask(
            @PathVariable UUID projectId,
            @PathVariable UUID sessionId,
            @Valid @RequestBody AskChatRequest request,
            @AuthenticationPrincipal UserDetails principal
    ) {
        return chatService.ask(
                projectId, sessionId, request.message(), principal.getUsername());
    }

    @DeleteMapping("/{sessionId}")
    @Operation(summary = "Delete the current user's private chat session")
    @ApiResponse(responseCode = "204", description = "Session and chat history deleted")
    public ResponseEntity<Void> delete(
            @PathVariable UUID projectId,
            @PathVariable UUID sessionId,
            @AuthenticationPrincipal UserDetails principal
    ) {
        persistence.delete(projectId, sessionId, principal.getUsername());
        return ResponseEntity.noContent().build();
    }
}
