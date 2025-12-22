package com.app.english.dto.invites;

import com.app.english.models.GroupRole;

public record AcceptInviteResponse(
        Long groupId,
        String groupName,
        GroupRole myRole
) {}
