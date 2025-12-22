package com.app.english.dto.groups;

import com.app.english.models.GroupRole;

import java.time.Instant;

public record GroupDetailsResponse(
        Long id,
        String name,
        GroupRole myRole,
        String joinCode,
        Instant createdAt
) {}
