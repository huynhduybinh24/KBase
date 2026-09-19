package com.kbase.backend.user.profile;

import com.kbase.backend.user.profile.dto.UpdateUserProfileRequest;
import com.kbase.backend.user.profile.dto.UserProfileResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.*;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/users/me")
@Tag(name = "User Profile")
@SecurityRequirement(name = "bearerAuth")
public class UserProfileController {
    private final UserProfileService service;
    public UserProfileController(UserProfileService service) { this.service = service; }

    @GetMapping("/profile")
    @Operation(summary = "Get the authenticated user's persisted profile")
    public UserProfileResponse getProfile(@AuthenticationPrincipal UserDetails principal) {
        return service.getCurrentProfile(principal.getUsername());
    }

    @PutMapping("/profile")
    @Operation(summary = "Update display name or select/reset a preset avatar")
    public UserProfileResponse updateProfile(@AuthenticationPrincipal UserDetails principal,
                                             @Valid @org.springframework.web.bind.annotation.RequestBody UpdateUserProfileRequest request) {
        return service.updateCurrentProfile(principal.getUsername(), request);
    }

    @PostMapping(value = "/avatar", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Upload a persisted avatar",
            requestBody = @RequestBody(content = @Content(mediaType = MediaType.MULTIPART_FORM_DATA_VALUE,
                    schema = @Schema(type = "object"))),
            responses = {@ApiResponse(responseCode = "200"), @ApiResponse(responseCode = "413"), @ApiResponse(responseCode = "415"), @ApiResponse(responseCode = "503")})
    public UserProfileResponse uploadAvatar(@AuthenticationPrincipal UserDetails principal,
                                            @RequestPart("file") MultipartFile file) {
        return service.uploadAvatar(principal.getUsername(), file);
    }

    @GetMapping("/avatar")
    @Operation(summary = "Download the authenticated user's uploaded avatar",
            responses = {@ApiResponse(responseCode = "200"), @ApiResponse(responseCode = "404"), @ApiResponse(responseCode = "503")})
    public ResponseEntity<InputStreamResource> getAvatar(@AuthenticationPrincipal UserDetails principal) {
        AvatarDownload avatar = service.downloadAvatar(principal.getUsername());
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(avatar.contentType()))
                .contentLength(avatar.size())
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline")
                .body(new InputStreamResource(avatar.content()));
    }

    @DeleteMapping("/avatar")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Reset the authenticated user's avatar")
    public void deleteAvatar(@AuthenticationPrincipal UserDetails principal) {
        service.deleteAvatar(principal.getUsername());
    }
}
