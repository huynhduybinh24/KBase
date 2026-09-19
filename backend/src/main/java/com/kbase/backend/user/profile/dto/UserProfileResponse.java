package com.kbase.backend.user.profile.dto;

import com.kbase.backend.user.User;
import com.kbase.backend.user.profile.AvatarType;
import com.kbase.backend.user.profile.UserProfile;

import java.time.Instant;
import java.util.Set;
import java.util.TreeSet;
import java.util.UUID;
import java.util.stream.Collectors;

public record UserProfileResponse(
        UUID userId,
        String email,
        String fullName,
        String displayName,
        Set<String> roles,
        AvatarType avatarType,
        String avatarPreset,
        boolean hasAvatar,
        Instant avatarUpdatedAt,
        Instant createdAt,
        Instant updatedAt
) {
    public static UserProfileResponse from(User user, UserProfile profile) {
        String fallback = user.getEmail().split("@", 2)[0];
        String display = profile == null || profile.getDisplayName() == null || profile.getDisplayName().isBlank()
                ? fallback : profile.getDisplayName();
        Set<String> roles = user.getRoles().stream().map(Enum::name)
                .collect(Collectors.toCollection(TreeSet::new));
        return new UserProfileResponse(user.getId(), user.getEmail(),
                profile == null ? null : profile.getFullName(), display, roles,
                profile == null ? AvatarType.NONE : profile.getAvatarType(),
                profile == null ? null : profile.getAvatarPreset(),
                profile != null && profile.getAvatarType() != AvatarType.NONE,
                profile == null ? null : profile.getAvatarUpdatedAt(),
                profile == null ? null : profile.getCreatedAt(),
                profile == null ? null : profile.getUpdatedAt());
    }
}
