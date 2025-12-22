package com.app.english.service;

import com.app.english.dto.groups.GroupDetailsResponse;
import com.app.english.dto.groups.GroupResponse;
import com.app.english.dto.groups.JoinCodeResponse;
import com.app.english.dto.groups.MemberResponse;
import com.app.english.exceptions.*;
import com.app.english.models.Group;
import com.app.english.models.GroupRole;
import com.app.english.models.Membership;
import com.app.english.models.User;
import com.app.english.repository.GroupRepository;
import com.app.english.repository.MembershipRepository;
import com.app.english.repository.UserRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class GroupService {

    private final GroupRepository groupRepository;
    private final MembershipRepository membershipRepository;
    private final UserRepository userRepository;
    private final JoinCodeGenerator joinCodeGenerator;

    public GroupService(
            GroupRepository groupRepository,
            MembershipRepository membershipRepository,
            UserRepository userRepository,
            JoinCodeGenerator joinCodeGenerator
    ) {
        this.groupRepository = groupRepository;
        this.membershipRepository = membershipRepository;
        this.userRepository = userRepository;
        this.joinCodeGenerator = joinCodeGenerator;
    }

    @Transactional
    public GroupResponse createGroup(String name, String creatorEmail) {
        User creator = userRepository.findByEmail(creatorEmail)
                .orElseThrow(() -> new IllegalStateException("Authenticated user not found"));

        for (int attempt = 0; attempt < 5; attempt++) {
            String joinCode = joinCodeGenerator.generate();
            try {
                Group group = groupRepository.saveAndFlush(new Group(name, joinCode, creator));
                membershipRepository.save(new Membership(creator, group, GroupRole.TEACHER));
                return new GroupResponse(group.getId(), group.getName(), group.getJoinCode(), GroupRole.TEACHER, group.getCreatedAt());
            } catch (DataIntegrityViolationException e) {
                // kolizja joinCode -> retry
            }
        }
        throw new IllegalStateException("Unable to generate unique join code");
    }

    @Transactional
    public GroupResponse joinByCode(String joinCode, String userEmail) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new IllegalStateException("Authenticated user not found"));

        Group group = groupRepository.findByJoinCode(joinCode)
                .orElseThrow(() -> new GroupJoinCodeNotFoundException("Group join code not found"));

        if (membershipRepository.existsByUserIdAndGroupId(user.getId(), group.getId())) {
            throw new AlreadyMemberException("User already belongs to this group");
        }

        membershipRepository.save(new Membership(user, group, GroupRole.STUDENT));
        return new GroupResponse(group.getId(), group.getName(), null, GroupRole.STUDENT, group.getCreatedAt());
    }

    @Transactional(readOnly = true)
    public List<GroupResponse> myGroups(String userEmail) {
        return membershipRepository.findMyMembershipsWithGroups(userEmail)
                .stream()
                .map(m -> new GroupResponse(
                        m.getGroup().getId(),
                        m.getGroup().getName(),
                        null,
                        m.getRole(),
                        m.getGroup().getCreatedAt()
                ))
                .toList();
    }

    @Transactional(readOnly = true)
    public GroupDetailsResponse getGroupDetails(Long groupId, String userEmail) {
        var membershipOpt = membershipRepository.findByUserEmailAndGroupId(userEmail, groupId);
        if (membershipOpt.isEmpty()) {
            if (!groupRepository.existsById(groupId)) {
                throw new GroupNotFoundException("Group not found");
            }
            throw new ForbiddenException("Not a member of this group");
        }

        Membership m = membershipOpt.get();
        Group g = m.getGroup();

        String joinCode = (m.getRole() == GroupRole.TEACHER) ? g.getJoinCode() : null;
        return new GroupDetailsResponse(g.getId(), g.getName(), m.getRole(), joinCode, g.getCreatedAt());
    }

    @Transactional
    public JoinCodeResponse resetJoinCode(Long groupId, String userEmail) {
        Membership m = membershipRepository.findByUserEmailAndGroupId(userEmail, groupId)
                .orElseThrow(() -> {
                    if (!groupRepository.existsById(groupId)) return new GroupNotFoundException("Group not found");
                    return new ForbiddenException("Not a member of this group");
                });

        if (m.getRole() != GroupRole.TEACHER) {
            throw new ForbiddenException("Teacher role required");
        }

        Group group = m.getGroup();

        for (int attempt = 0; attempt < 5; attempt++) {
            String code = joinCodeGenerator.generate();
            try {
                group.setJoinCode(code);
                groupRepository.saveAndFlush(group);
                return new JoinCodeResponse(code);
            } catch (DataIntegrityViolationException e) {
                // kolizja joinCode -> retry
            }
        }

        throw new IllegalStateException("Unable to generate unique join code");
    }

    @Transactional(readOnly = true)
    public List<MemberResponse> listMembers(Long groupId, String actorEmail) {
        Membership actor = membershipRepository.findByUserEmailAndGroupId(actorEmail, groupId)
                .orElseThrow(() -> {
                    if (!groupRepository.existsById(groupId)) return new GroupNotFoundException("Group not found");
                    return new ForbiddenException("Not a member of this group");
                });

        if (actor.getRole() != GroupRole.TEACHER) {
            throw new ForbiddenException("Teacher role required");
        }

        return membershipRepository.findMembersByGroupId(groupId)
                .stream()
                .map(m -> new MemberResponse(
                        m.getUser().getId(),
                        m.getUser().getEmail(),
                        m.getRole(),
                        m.getJoinedAt()
                ))
                .toList();
    }

    @Transactional
    public void removeMember(Long groupId, Long targetUserId, String actorEmail) {
        Membership actor = membershipRepository.findByUserEmailAndGroupId(actorEmail, groupId)
                .orElseThrow(() -> {
                    if (!groupRepository.existsById(groupId)) return new GroupNotFoundException("Group not found");
                    return new ForbiddenException("Not a member of this group");
                });

        Membership target = membershipRepository.findByGroupIdAndUserIdFetchAll(groupId, targetUserId)
                .orElseThrow(() -> new MembershipNotFoundException("Target user is not a member of this group"));

        Group group = target.getGroup();
        Long ownerId = group.getCreatedBy().getId();

        if (targetUserId.equals(ownerId)) {
            throw new CannotRemoveOwnerException("Cannot remove group owner");
        }

        Long actorUserId = actor.getUser().getId();
        boolean actorIsOwner = actorUserId.equals(ownerId);

        // self-leave is allowed (except owner)
        if (actorUserId.equals(targetUserId)) {
            membershipRepository.delete(target);
            return;
        }

        if (actor.getRole() != GroupRole.TEACHER) {
            throw new ForbiddenException("Teacher role required");
        }

        // removing teachers: only owner can
        if (target.getRole() == GroupRole.TEACHER && !actorIsOwner) {
            throw new ForbiddenException("Only group owner can remove other teachers");
        }

        // removing students: any teacher ok (also owner ok)
        membershipRepository.delete(target);
    }

    @Transactional
    public void leaveGroup(Long groupId, String actorEmail) {
        Membership actor = membershipRepository.findByUserEmailAndGroupId(actorEmail, groupId)
                .orElseThrow(() -> {
                    if (!groupRepository.existsById(groupId)) return new GroupNotFoundException("Group not found");
                    return new ForbiddenException("Not a member of this group");
                });

        Long ownerId = actor.getGroup().getCreatedBy().getId();
        if (actor.getUser().getId().equals(ownerId)) {
            throw new CannotRemoveOwnerException("Owner cannot leave the group (transfer ownership or delete group)");
        }

        membershipRepository.delete(actor);
    }
}
