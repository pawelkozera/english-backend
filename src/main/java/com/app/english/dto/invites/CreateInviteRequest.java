package com.app.english.dto.invites;

import com.app.english.models.GroupRole;

public record CreateInviteRequest(
        GroupRole roleGranted,      // default: STUDENT jeśli null
        Integer maxUses,            // 1 = jednorazowe, >1 = wielorazowe, null = bez limitu
        Long expiresInMinutes       // null => default
) {}
