package com.kbase.backend.document.dto;

import com.kbase.backend.user.User;

import java.util.UUID;

public record DocumentUserResponse(UUID id, String email) {

    public static DocumentUserResponse from(User user) {
        return new DocumentUserResponse(user.getId(), user.getEmail());
    }
}
