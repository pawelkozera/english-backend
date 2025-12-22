package com.app.english.controller;

import com.app.english.dto.invites.*;
import com.app.english.service.GroupInviteService;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
public class GroupInviteController {

    private final GroupInviteService inviteService;

    public GroupInviteController(GroupInviteService inviteService) {
        this.inviteService = inviteService;
    }

    // Teacher-only
    @PostMapping("/api/groups/{groupId}/invites")
    public InviteCreatedResponse createInvite(
            @PathVariable Long groupId,
            @RequestBody CreateInviteRequest request,
            Authentication auth
    ) {
        return inviteService.createInvite(groupId, auth.getName(), request);
    }

    // Teacher-only
    @GetMapping("/api/groups/{groupId}/invites")
    public List<InviteSummaryResponse> listInvites(
            @PathVariable Long groupId,
            Authentication auth
    ) {
        return inviteService.listInvites(groupId, auth.getName());
    }

    // Teacher-only
    @PostMapping("/api/groups/{groupId}/invites/{inviteId}/revoke")
    public void revokeInvite(
            @PathVariable Long groupId,
            @PathVariable Long inviteId,
            Authentication auth
    ) {
        inviteService.revokeInvite(groupId, inviteId, auth.getName());
    }

    // Teacher-only
    @PostMapping("/api/groups/{groupId}/invites/{inviteId}/recreate")
    public InviteCreatedResponse recreateInvite(
            @PathVariable Long groupId,
            @PathVariable Long inviteId,
            Authentication auth
    ) {
        return inviteService.recreateInvite(groupId, inviteId, auth.getName());
    }

    // Accept invite (authenticated user).
    @PostMapping("/api/invites/accept")
    public AcceptInviteResponse acceptInvite(
            @RequestBody AcceptInviteRequest request,
            Authentication auth
    ) {
        return inviteService.acceptInvite(request.token(), auth.getName());
    }
}
