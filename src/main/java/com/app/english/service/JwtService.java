package com.app.english.service;

import com.app.english.config.JwtProperties;
import com.app.english.models.Role;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Service;

import java.security.Key;
import java.util.Date;

@Service
public class JwtService {

    private final JwtProperties props;
    private final Key key;

    public JwtService(JwtProperties props) {
        this.props = props;

        byte[] keyBytes = Decoders.BASE64.decode(props.secretBase64());
        if (keyBytes.length < 32) {
            throw new IllegalStateException("JWT secret too short: need >= 32 bytes after Base64 decoding");
        }

        this.key = Keys.hmacShaKeyFor(keyBytes);
    }

    public String generateAccessToken(String email, Role role) {
        long now = System.currentTimeMillis();
        return Jwts.builder()
                .setSubject(email)
                .claim("role", role.name())
                .setIssuedAt(new Date(now))
                .setExpiration(new Date(now + props.accessExpirationMs()))
                .signWith(key)
                .compact();
    }

    public String extractEmail(String token) {
        return extractAllClaims(token).getSubject();
    }

    public String extractRole(String token) {
        return extractAllClaims(token).get("role", String.class);
    }

    public boolean isTokenValid(String token) {
        try {
            extractAllClaims(token);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private Claims extractAllClaims(String token) {
        return Jwts.parserBuilder().setSigningKey(key).build()
                .parseClaimsJws(token)
                .getBody();
    }
}

