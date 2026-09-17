package com.kbase.backend.project.dto;

import com.kbase.backend.user.User;

import java.util.UUID;

public record ProjectUserResponse(UUID id, String email) {

    public static ProjectUserResponse from(User user) {
        return new ProjectUserResponse(user.getId(), user.getEmail());
    }
}
