package com.app.english.service;

import com.app.english.exceptions.InvalidRefreshTokenException;
import com.app.english.models.RefreshToken;
import com.app.english.models.User;
import com.app.english.repository.RefreshTokenRepository;
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
public class RefreshTokenService {

    private static final int TOKEN_BYTES = 64;
    private static final long REFRESH_TTL_DAYS = 14;
    private static final int MAX_ACTIVE_TOKENS = 5;

    private final SecureRandom secureRandom = new SecureRandom();
    private final RefreshTokenRepository refreshTokenRepository;

    public RefreshTokenService(RefreshTokenRepository refreshTokenRepository) {
        this.refreshTokenRepository = refreshTokenRepository;
    }

    public record IssuedRefreshToken(String rawToken, Instant expiresAt) {}

    @Transactional
    public IssuedRefreshToken issue(User user) {
        enforceLimit(user.getId());

        String raw = generateRandomToken();
        String hash = sha256Hex(raw);
        Instant expiresAt = Instant.now().plus(REFRESH_TTL_DAYS, ChronoUnit.DAYS);

        refreshTokenRepository.save(new RefreshToken(hash, user, expiresAt));
        return new IssuedRefreshToken(raw, expiresAt);
    }

    private void enforceLimit(Long userId) {
        Instant now = Instant.now();
        long active = refreshTokenRepository.countByUserIdAndRevokedFalseAndExpiresAtAfter(userId, now);
        if (active < MAX_ACTIVE_TOKENS) return;

        List<RefreshToken> tokens =
                refreshTokenRepository.findByUserIdAndRevokedFalseAndExpiresAtAfterOrderByIdAsc(userId, now);

        int toRevoke = (int) (active - MAX_ACTIVE_TOKENS + 1);
        for (int i = 0; i < Math.min(toRevoke, tokens.size()); i++) {
            tokens.get(i).revoke();
        }
    }

    @Transactional
    public User validateAndGetUser(String rawToken) {
        String hash = sha256Hex(rawToken);

        RefreshToken token = refreshTokenRepository.findByTokenHash(hash)
                .orElseThrow(() -> new InvalidRefreshTokenException("Invalid refresh token"));

        if (token.isRevoked() || token.isExpired(Instant.now())) {
            throw new InvalidRefreshTokenException("Invalid refresh token");
        }

        return token.getUser();
    }

    @Transactional
    public void revoke(String rawToken) {
        String hash = sha256Hex(rawToken);
        refreshTokenRepository.findByTokenHash(hash).ifPresent(RefreshToken::revoke);
    }

    @Transactional
    public void rotate(String oldRawToken) {
        revoke(oldRawToken);
    }

    private String generateRandomToken() {
        byte[] bytes = new byte[TOKEN_BYTES];
        secureRandom.nextBytes(bytes);
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
