package com.app.english.models;

import jakarta.persistence.*;

import java.time.Instant;

@Entity
@Table(name = "refresh_tokens", indexes = {
        @Index(name = "idx_refresh_token_hash", columnList = "tokenHash", unique = true),
        @Index(name = "idx_refresh_token_user", columnList = "user_id")
})
public class RefreshToken {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 128)
    private String tokenHash;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    private User user;

    @Column(nullable = false)
    private Instant expiresAt;

    @Column(nullable = false)
    private boolean revoked;

    protected RefreshToken() {}

    public RefreshToken(String tokenHash, User user, Instant expiresAt) {
        this.tokenHash = tokenHash;
        this.user = user;
        this.expiresAt = expiresAt;
        this.revoked = false;
    }

    public Long getId() { return id; }

    public String getTokenHash() { return tokenHash; }

    public User getUser() { return user; }

    public Instant getExpiresAt() { return expiresAt; }

    public boolean isRevoked() { return revoked; }

    public void revoke() { this.revoked = true; }

    public boolean isExpired(Instant now) { return expiresAt.isBefore(now) || expiresAt.equals(now); }
}
