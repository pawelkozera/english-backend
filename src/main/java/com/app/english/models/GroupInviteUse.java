package com.app.english.models;

import jakarta.persistence.*;

import java.time.Instant;

@Entity
@Table(
        name = "group_invite_uses",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_invite_use_invite_user",
                        columnNames = {"invite_id", "user_id"}
                )
        },
        indexes = {
                @Index(name = "idx_invite_uses_invite", columnList = "invite_id"),
                @Index(name = "idx_invite_uses_user", columnList = "user_id")
        }
)
public class GroupInviteUse {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    private GroupInvite invite;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    private User user;

    @Column(nullable = false)
    private Instant usedAt;

    protected GroupInviteUse() {}

    public GroupInviteUse(GroupInvite invite, User user) {
        this.invite = invite;
        this.user = user;
        this.usedAt = Instant.now();
    }

    public Long getId() { return id; }

    public GroupInvite getInvite() { return invite; }

    public User getUser() { return user; }

    public Instant getUsedAt() { return usedAt; }
}
