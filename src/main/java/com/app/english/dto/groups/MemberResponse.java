package com.app.english.dto.groups;

import com.app.english.models.GroupRole;

import java.time.Instant;

public record MemberResponse(
        Long userId,
        String email,
        GroupRole role,
        Instant joinedAt,
        boolean owner
) {}
