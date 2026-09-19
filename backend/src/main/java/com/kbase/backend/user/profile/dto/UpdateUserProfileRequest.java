package com.kbase.backend.user.profile.dto;

import com.kbase.backend.user.profile.AvatarType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UpdateUserProfileRequest(
        @NotBlank @Size(max = 120) String displayName,
        AvatarType avatarType,
        @Size(max = 40) String avatarPreset
) {}
