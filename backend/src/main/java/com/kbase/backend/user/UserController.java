package com.kbase.backend.user;

import com.kbase.backend.auth.dto.UserResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/users")
@Tag(name = "Users")
@SecurityRequirement(name = "bearerAuth")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/me")
    @Operation(
            summary = "Get the authenticated user",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Authenticated user"),
                    @ApiResponse(responseCode = "401", description = "Missing or invalid JWT")
            }
    )
    public UserResponse getCurrentUser(@AuthenticationPrincipal UserDetails principal) {
        return userService.getCurrentUser(principal.getUsername());
    }
}
