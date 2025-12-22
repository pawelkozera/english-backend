package com.app.english.models;

import jakarta.persistence.*;

import java.time.Instant;

@Entity
@Table(name = "groups", indexes = {
        @Index(name = "idx_groups_join_code", columnList = "joinCode", unique = true)
})
public class Group {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 120)
    private String name;

    @Column(nullable = false, unique = true, length = 32)
    private String joinCode;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    private User createdBy;

    @Column(nullable = false)
    private Instant createdAt;

    protected Group() {}

    public Group(String name, String joinCode, User createdBy) {
        this.name = name;
        this.joinCode = joinCode;
        this.createdBy = createdBy;
        this.createdAt = Instant.now();
    }

    public Long getId() { return id; }

    public String getName() { return name; }

    public String getJoinCode() { return joinCode; }

    public User getCreatedBy() { return createdBy; }

    public Instant getCreatedAt() { return createdAt; }

    public void setJoinCode(String joinCode) {
        this.joinCode = joinCode;
    }
}
