package com.kbase.backend.auth.dto;

import com.kbase.backend.user.Role;
import com.kbase.backend.user.User;

import java.util.Set;
import java.util.TreeSet;
import java.util.UUID;
import java.util.stream.Collectors;

public record UserResponse(UUID id, String email, Set<String> roles) {

    public static UserResponse from(User user) {
        Set<String> roles = user.getRoles().stream()
                .map(Role::name)
                .collect(Collectors.toCollection(TreeSet::new));
        return new UserResponse(user.getId(), user.getEmail(), roles);
    }
}
