package com.app.english.models;

import jakarta.persistence.*;

import java.time.Instant;

@Entity
@Table(name = "group_invites", indexes = {
        @Index(name = "idx_group_invites_token_hash", columnList = "tokenHash", unique = true),
        @Index(name = "idx_group_invites_group", columnList = "group_id")
})
public class GroupInvite {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 128)
    private String tokenHash;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    private Group group;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    private User createdBy;

    @Column(nullable = false)
    private Instant createdAt;

    @Column(nullable = false)
    private Instant expiresAt;

    @Column(nullable = false)
    private boolean revoked;

    // null = bez limitu
    @Column
    private Integer maxUses;

    @Column(nullable = false)
    private int usedCount;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private GroupRole roleGranted;

    @Version
    private long version;

    protected GroupInvite() {}

    public GroupInvite(
            String tokenHash,
            Group group,
            User createdBy,
            Instant expiresAt,
            Integer maxUses,
            GroupRole roleGranted
    ) {
        this.tokenHash = tokenHash;
        this.group = group;
        this.createdBy = createdBy;
        this.createdAt = Instant.now();
        this.expiresAt = expiresAt;
        this.maxUses = maxUses;
        this.usedCount = 0;
        this.revoked = false;
        this.roleGranted = roleGranted;
    }

    public Long getId() { return id; }

    public String getTokenHash() { return tokenHash; }

    public Group getGroup() { return group; }

    public User getCreatedBy() { return createdBy; }

    public Instant getCreatedAt() { return createdAt; }

    public Instant getExpiresAt() { return expiresAt; }

    public boolean isRevoked() { return revoked; }

    public Integer getMaxUses() { return maxUses; }

    public int getUsedCount() { return usedCount; }

    public GroupRole getRoleGranted() { return roleGranted; }

    public void revoke() { this.revoked = true; }

    public boolean isExpired(Instant now) { return !expiresAt.isAfter(now); }

    public boolean isExhausted() {
        return maxUses != null && usedCount >= maxUses;
    }

    public void incrementUsed() {
        this.usedCount++;
    }
}
