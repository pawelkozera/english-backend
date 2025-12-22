package com.app.english.service;

import com.app.english.dto.invites.*;
import com.app.english.exceptions.*;
import com.app.english.models.*;
import com.app.english.repository.*;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.List;

@Service
public class GroupInviteService {

    private static final long DEFAULT_EXPIRES_MINUTES = 60 * 24 * 7; // 7 dni
    private static final int TOKEN_BYTES = 32;

    private final SecureRandom random = new SecureRandom();

    private final GroupRepository groupRepository;
    private final MembershipRepository membershipRepository;
    private final UserRepository userRepository;
    private final GroupInviteRepository inviteRepository;
    private final GroupInviteUseRepository inviteUseRepository;

    public GroupInviteService(
            GroupRepository groupRepository,
            MembershipRepository membershipRepository,
            UserRepository userRepository,
            GroupInviteRepository inviteRepository,
            GroupInviteUseRepository inviteUseRepository
    ) {
        this.groupRepository = groupRepository;
        this.membershipRepository = membershipRepository;
        this.userRepository = userRepository;
        this.inviteRepository = inviteRepository;
        this.inviteUseRepository = inviteUseRepository;
    }

    @Transactional
    public InviteCreatedResponse createInvite(Long groupId, String creatorEmail, CreateInviteRequest req) {
        Membership creatorMembership = membershipRepository.findByUserEmailAndGroupId(creatorEmail, groupId)
                .orElseThrow(() -> {
                    if (!groupRepository.existsById(groupId)) return new GroupNotFoundException("Group not found");
                    return new ForbiddenException("Not a member of this group");
                });

        if (creatorMembership.getRole() != GroupRole.TEACHER) {
            throw new ForbiddenException("Teacher role required");
        }

        User creator = creatorMembership.getUser();
        Group group = creatorMembership.getGroup();

        GroupRole roleGranted = (req.roleGranted() == null) ? GroupRole.STUDENT : req.roleGranted();
        Integer maxUses = req.maxUses();
        if (maxUses != null && maxUses < 1) {
            throw new InviteInvalidException("maxUses must be >= 1 or null");
        }

        long expiresMinutes = (req.expiresInMinutes() == null) ? DEFAULT_EXPIRES_MINUTES : req.expiresInMinutes();
        if (expiresMinutes < 1) {
            throw new InviteInvalidException("expiresInMinutes must be >= 1");
        }

        Instant expiresAt = Instant.now().plus(expiresMinutes, ChronoUnit.MINUTES);

        for (int attempt = 0; attempt < 5; attempt++) {
            String rawToken = generateToken();
            String tokenHash = sha256Hex(rawToken);

            try {
                GroupInvite invite = inviteRepository.saveAndFlush(
                        new GroupInvite(tokenHash, group, creator, expiresAt, maxUses, roleGranted)
                );
                return new InviteCreatedResponse(invite.getId(), rawToken, invite.getExpiresAt(), invite.getMaxUses(), invite.getRoleGranted());
            } catch (DataIntegrityViolationException e) {
                // tokenHash collision -> retry
            }
        }

        throw new IllegalStateException("Unable to create invite");
    }

    @Transactional
    public AcceptInviteResponse acceptInvite(String token, String userEmail) {
        if (token == null || token.isBlank()) {
            throw new InviteInvalidException("Invite token required");
        }

        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new IllegalStateException("Authenticated user not found"));

        String tokenHash = sha256Hex(token);

        GroupInvite invite = inviteRepository.findByTokenHash(tokenHash)
                .orElseThrow(() -> new InviteInvalidException("Invalid invite token"));

        Instant now = Instant.now();
        if (invite.isRevoked() || invite.isExpired(now) || invite.isExhausted()) {
            throw new InviteInvalidException("Invite is not usable");
        }

        Group group = invite.getGroup();

        if (membershipRepository.existsByUserIdAndGroupId(user.getId(), group.getId())) {
            throw new AlreadyMemberException("User already belongs to this group");
        }

        if (inviteUseRepository.existsByInviteIdAndUserId(invite.getId(), user.getId())) {
            throw new InviteInvalidException("Invite already used by this user");
        }

        membershipRepository.save(new Membership(user, group, invite.getRoleGranted()));
        invite.incrementUsed();
        inviteUseRepository.save(new GroupInviteUse(invite, user));

        return new AcceptInviteResponse(group.getId(), group.getName(), invite.getRoleGranted());
    }

    @Transactional(readOnly = true)
    public List<InviteSummaryResponse> listInvites(Long groupId, String teacherEmail) {
        Membership m = membershipRepository.findByUserEmailAndGroupId(teacherEmail, groupId)
                .orElseThrow(() -> {
                    if (!groupRepository.existsById(groupId)) return new GroupNotFoundException("Group not found");
                    return new ForbiddenException("Not a member of this group");
                });

        if (m.getRole() != GroupRole.TEACHER) {
            throw new ForbiddenException("Teacher role required");
        }

        return inviteRepository.findByGroupIdOrderByCreatedAtDesc(groupId)
                .stream()
                .map(i -> new InviteSummaryResponse(
                        i.getId(),
                        i.getCreatedAt(),
                        i.getExpiresAt(),
                        i.isRevoked(),
                        i.getMaxUses(),
                        i.getUsedCount(),
                        i.getRoleGranted()
                ))
                .toList();
    }

    @Transactional
    public void revokeInvite(Long groupId, Long inviteId, String teacherEmail) {
        Membership m = membershipRepository.findByUserEmailAndGroupId(teacherEmail, groupId)
                .orElseThrow(() -> {
                    if (!groupRepository.existsById(groupId)) return new GroupNotFoundException("Group not found");
                    return new ForbiddenException("Not a member of this group");
                });

        if (m.getRole() != GroupRole.TEACHER) {
            throw new ForbiddenException("Teacher role required");
        }

        GroupInvite invite = inviteRepository.findByIdAndGroupId(inviteId, groupId)
                .orElseThrow(() -> new InviteInvalidException("Invite not found"));

        invite.revoke();
    }

    @Transactional
    public InviteCreatedResponse recreateInvite(Long groupId, Long inviteId, String teacherEmail) {
        Membership teacherMembership = membershipRepository.findByUserEmailAndGroupId(teacherEmail, groupId)
                .orElseThrow(() -> {
                    if (!groupRepository.existsById(groupId)) return new GroupNotFoundException("Group not found");
                    return new ForbiddenException("Not a member of this group");
                });

        if (teacherMembership.getRole() != GroupRole.TEACHER) {
            throw new ForbiddenException("Teacher role required");
        }

        GroupInvite oldInvite = inviteRepository.findByIdAndGroupId(inviteId, groupId)
                .orElseThrow(() -> new InviteInvalidException("Invite not found"));

        oldInvite.revoke();

        Group group = teacherMembership.getGroup();
        User creator = teacherMembership.getUser();

        Instant now = Instant.now();
        Instant expiresAt = oldInvite.getExpiresAt().isAfter(now)
                ? oldInvite.getExpiresAt()
                : now.plus(DEFAULT_EXPIRES_MINUTES, ChronoUnit.MINUTES);

        Integer maxUses = oldInvite.getMaxUses();
        GroupRole roleGranted = oldInvite.getRoleGranted();

        for (int attempt = 0; attempt < 5; attempt++) {
            String rawToken = generateToken();
            String tokenHash = sha256Hex(rawToken);

            try {
                GroupInvite newInvite = inviteRepository.saveAndFlush(
                        new GroupInvite(tokenHash, group, creator, expiresAt, maxUses, roleGranted)
                );
                return new InviteCreatedResponse(
                        newInvite.getId(),
                        rawToken,
                        newInvite.getExpiresAt(),
                        newInvite.getMaxUses(),
                        newInvite.getRoleGranted()
                );
            } catch (DataIntegrityViolationException e) {
                // collision -> retry
            }
        }

        throw new IllegalStateException("Unable to recreate invite");
    }

    private String generateToken() {
        byte[] bytes = new byte[TOKEN_BYTES];
        random.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private String sha256Hex(String raw) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] dig = md.digest(raw.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder(dig.length * 2);
            for (byte b : dig) sb.append(String.format("%02x", b));
            return sb.toString();
        } catch (Exception e) {
            throw new IllegalStateException("Cannot hash token", e);
        }
    }
}
