package com.app.english.dto.invites;

import com.app.english.models.GroupRole;

import java.time.Instant;

public record InviteSummaryResponse(
        Long inviteId,
        Instant createdAt,
        Instant expiresAt,
        boolean revoked,
        Integer maxUses,
        int usedCount,
        GroupRole roleGranted
) {}
