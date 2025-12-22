package com.app.english.controller;

import com.app.english.dto.groups.*;
import com.app.english.service.GroupService;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/groups")
public class GroupController {

    private final GroupService groupService;

    public GroupController(GroupService groupService) {
        this.groupService = groupService;
    }

    @PostMapping
    public GroupResponse create(@RequestBody CreateGroupRequest request, Authentication auth) {
        return groupService.createGroup(request.name(), auth.getName());
    }

    @PostMapping("/join")
    public GroupResponse join(@RequestBody JoinGroupRequest request, Authentication auth) {
        return groupService.joinByCode(request.code(), auth.getName());
    }

    @GetMapping("/me")
    public List<GroupResponse> myGroups(Authentication auth) {
        return groupService.myGroups(auth.getName());
    }

    @GetMapping("/{groupId}")
    public GroupDetailsResponse details(@PathVariable Long groupId, Authentication auth) {
        return groupService.getGroupDetails(groupId, auth.getName());
    }

    @PostMapping("/{groupId}/join-code/reset")
    public JoinCodeResponse resetJoinCode(@PathVariable Long groupId, Authentication auth) {
        return groupService.resetJoinCode(groupId, auth.getName());
    }
}