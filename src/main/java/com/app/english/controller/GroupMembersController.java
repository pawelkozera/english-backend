package com.app.english.controller;

import com.app.english.dto.groups.MemberResponse;
import com.app.english.service.GroupService;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/groups/{groupId}/members")
public class GroupMembersController {

    private final GroupService groupService;

    public GroupMembersController(GroupService groupService) {
        this.groupService = groupService;
    }

    // Teacher-only
    @GetMapping
    public List<MemberResponse> list(@PathVariable Long groupId, Authentication auth) {
        return groupService.listMembers(groupId, auth.getName());
    }

    // Remove user from group (policy in service)
    @DeleteMapping("/remove/{userId}")
    public void remove(@PathVariable Long groupId, @PathVariable Long userId, Authentication auth) {
        groupService.removeMember(groupId, userId, auth.getName());
    }

    // Self-leave shortcut
    @PostMapping("/leave")
    public void leave(@PathVariable Long groupId, Authentication auth) {
        groupService.leaveGroup(groupId, auth.getName());
    }
}
