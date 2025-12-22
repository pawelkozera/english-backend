package com.app.english.dto.invites;

import com.app.english.models.GroupRole;

import java.time.Instant;

public record InviteCreatedResponse(
        Long inviteId,
        String token,
        Instant expiresAt,
        Integer maxUses,
        GroupRole roleGranted
) {}
