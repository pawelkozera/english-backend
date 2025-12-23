package com.app.english.dto.invites;

import com.app.english.models.GroupRole;

import java.time.Instant;

public record InvitePreviewResponse(
        boolean valid,
        String groupName,
        GroupRole roleGranted,
        Instant expiresAt,
        Integer maxUses,
        int usedCount,
        boolean exhausted,
        boolean revoked,
        boolean expired,
        boolean alreadyMember
) {}
