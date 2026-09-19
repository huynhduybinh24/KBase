package com.kbase.backend.auth.dto;

import com.kbase.backend.auth.validation.ValidPassword;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.Pattern;

public record RegisterRequest(
        @Size(max = 120) @Pattern(regexp = "(?s).*\\S.*", message = "must not be blank") String fullName,
        @NotBlank @Email @Size(max = 320) String email,
        @NotBlank @ValidPassword(minCharacters = 8) String password
) {
}
