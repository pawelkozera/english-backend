package com.app.english.models;

import jakarta.persistence.*;

import java.time.Instant;

@Entity
@Table(
        name = "memberships",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_memberships_user_group", columnNames = {"user_id", "group_id"})
        },
        indexes = {
                @Index(name = "idx_memberships_user", columnList = "user_id"),
                @Index(name = "idx_memberships_group", columnList = "group_id")
        }
)
public class Membership {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    private User user;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    private Group group;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private GroupRole role;

    @Column(nullable = false)
    private Instant joinedAt;

    protected Membership() {}

    public Membership(User user, Group group, GroupRole role) {
        this.user = user;
        this.group = group;
        this.role = role;
        this.joinedAt = Instant.now();
    }

    public Long getId() { return id; }

    public User getUser() { return user; }

    public Group getGroup() { return group; }

    public GroupRole getRole() { return role; }

    public Instant getJoinedAt() { return joinedAt; }
}
