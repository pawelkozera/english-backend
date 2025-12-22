package com.app.english.service;

import com.app.english.exceptions.EmailAlreadyExistsException;
import com.app.english.models.Role;
import com.app.english.models.User;
import com.app.english.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public UserService(UserRepository userRepository,
                       PasswordEncoder passwordEncoder,
                       JwtService jwtService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    @Transactional
    public String register(String email, String password) {
        if (userRepository.existsByEmail(email)) {
            throw new EmailAlreadyExistsException("Email already exists");
        }

        String hash = passwordEncoder.encode(password);

        User user = new User(
                email,
                hash,
                Role.USER
        );

        userRepository.save(user);

        return jwtService.generateAccessToken(user.getEmail(), user.getRole());
    }

    @Transactional(readOnly = true)
    public String login(String email, String password) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("Invalid credentials"));

        if (!passwordEncoder.matches(password, user.getPasswordHash())) {
            throw new IllegalArgumentException("Invalid credentials");
        }

        return jwtService.generateAccessToken(user.getEmail(), user.getRole());
    }
}

