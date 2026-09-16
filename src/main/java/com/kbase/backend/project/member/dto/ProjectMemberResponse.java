package com.kbase.backend.project.member.dto;

import com.kbase.backend.project.member.ProjectMember;
import com.kbase.backend.project.member.ProjectMemberRole;

import java.time.Instant;
import java.util.UUID;

public record ProjectMemberResponse(
        UUID id,
        UUID userId,
        String email,
        ProjectMemberRole role,
        Instant joinedAt
) {

    public static ProjectMemberResponse from(ProjectMember member) {
        return new ProjectMemberResponse(
                member.getId(),
                member.getUser().getId(),
                member.getUser().getEmail(),
                member.getRole(),
                member.getJoinedAt()
        );
    }
}
