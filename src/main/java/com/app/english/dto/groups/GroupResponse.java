package com.app.english.dto.groups;

import com.app.english.models.GroupRole;

import java.time.Instant;

public record GroupResponse(
        Long id,
        String name,
        String joinCode,
        GroupRole myRole,
        Instant createdAt
) {}