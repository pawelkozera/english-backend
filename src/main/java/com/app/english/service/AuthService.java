package com.app.english.service;

import com.app.english.exceptions.EmailAlreadyExistsException;
import com.app.english.exceptions.InvalidCredentialsException;
import com.app.english.models.Role;
import com.app.english.models.User;
import com.app.english.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final RefreshTokenService refreshTokenService;

    public AuthService(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            JwtService jwtService,
            RefreshTokenService refreshTokenService
    ) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.refreshTokenService = refreshTokenService;
    }

    public record AuthResult(String accessToken, String refreshToken) {}

    @Transactional
    public AuthResult register(String email, String password) {
        if (userRepository.existsByEmail(email)) {
            throw new EmailAlreadyExistsException("Email already exists");
        }

        String hash = passwordEncoder.encode(password);
        User user = userRepository.save(new User(email, hash, Role.USER));

        String access = jwtService.generateAccessToken(user.getEmail(), user.getRole());
        String refresh = refreshTokenService.issue(user).rawToken();

        return new AuthResult(access, refresh);
    }

    @Transactional
    public AuthResult login(String email, String password) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new InvalidCredentialsException("Invalid credentials"));

        if (!passwordEncoder.matches(password, user.getPasswordHash())) {
            throw new InvalidCredentialsException("Invalid credentials");
        }

        String access = jwtService.generateAccessToken(user.getEmail(), user.getRole());
        String refresh = refreshTokenService.issue(user).rawToken();

        return new AuthResult(access, refresh);
    }

    @Transactional
    public AuthResult refresh(String oldRefreshToken) {
        User user = refreshTokenService.validateAndGetUser(oldRefreshToken);

        refreshTokenService.rotate(oldRefreshToken);

        String access = jwtService.generateAccessToken(user.getEmail(), user.getRole());
        String refresh = refreshTokenService.issue(user).rawToken();

        return new AuthResult(access, refresh);
    }

    @Transactional
    public void logout(String refreshToken) {
        refreshTokenService.revoke(refreshToken);
    }
}
