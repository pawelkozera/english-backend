package com.app.english.controller;

import com.app.english.dto.auth.AuthResponse;
import com.app.english.dto.auth.LoginRequest;
import com.app.english.dto.auth.RegisterRequest;
import com.app.english.service.AuthService;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private static final String REFRESH_COOKIE = "refresh_token";

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/register")
    public AuthResponse register(@RequestBody RegisterRequest request, HttpServletResponse response) {
        var result = authService.register(request.email(), request.password());
        setRefreshCookie(response, result.refreshToken());
        return new AuthResponse(result.accessToken());
    }

    @PostMapping("/login")
    public AuthResponse login(@RequestBody LoginRequest request, HttpServletResponse response) {
        var result = authService.login(request.email(), request.password());
        setRefreshCookie(response, result.refreshToken());
        return new AuthResponse(result.accessToken());
    }

    @PostMapping("/refresh")
    public AuthResponse refresh(
            @CookieValue(name = REFRESH_COOKIE, required = false) String refreshToken,
            HttpServletResponse response
    ) {
        if (refreshToken == null || refreshToken.isBlank()) {
            throw new com.app.english.exceptions.InvalidRefreshTokenException("Invalid refresh token");
        }

        var result = authService.refresh(refreshToken);
        setRefreshCookie(response, result.refreshToken());
        return new AuthResponse(result.accessToken());
    }

    @PostMapping("/logout")
    public void logout(
            @CookieValue(name = REFRESH_COOKIE, required = false) String refreshToken,
            HttpServletResponse response
    ) {
        if (refreshToken != null && !refreshToken.isBlank()) {
            authService.logout(refreshToken);
        }
        clearRefreshCookie(response);
    }

    private void setRefreshCookie(HttpServletResponse response, String refreshToken) {
        Cookie cookie = new Cookie(REFRESH_COOKIE, refreshToken);
        cookie.setHttpOnly(true);
        cookie.setSecure(false); // TODO: true na HTTPS
        cookie.setPath("/");
        cookie.setMaxAge(14 * 24 * 60 * 60);
        response.addCookie(cookie);
    }

    private void clearRefreshCookie(HttpServletResponse response) {
        Cookie cookie = new Cookie(REFRESH_COOKIE, "");
        cookie.setHttpOnly(true);
        cookie.setSecure(false); // TODO: true na HTTPS
        cookie.setPath("/");
        cookie.setMaxAge(0);
        response.addCookie(cookie);
    }
}

