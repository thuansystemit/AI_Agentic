package com.darkness.videoplatform.controller;

import com.darkness.videoplatform.dto.AuthResponse;
import com.darkness.videoplatform.dto.LoginRequest;
import com.darkness.videoplatform.dto.RegisterRequest;
import com.darkness.videoplatform.service.AuthService;
import com.darkness.videoplatform.service.AuthService.TokenPair;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @Value("${app.cookie.secure:false}")
    private boolean cookieSecure;

    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(
            @Valid @RequestBody RegisterRequest request,
            HttpServletResponse response) {
        TokenPair pair = authService.register(request);
        setAccessCookie(response, pair.accessToken());
        setRefreshCookie(response, pair.refreshToken());
        return ResponseEntity.status(HttpStatus.CREATED).body(authService.toResponse(pair.user()));
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(
            @Valid @RequestBody LoginRequest request,
            HttpServletResponse response) {
        TokenPair pair = authService.login(request);
        setAccessCookie(response, pair.accessToken());
        setRefreshCookie(response, pair.refreshToken());
        return ResponseEntity.ok(authService.toResponse(pair.user()));
    }

    @PostMapping("/refresh")
    public ResponseEntity<AuthResponse> refresh(
            HttpServletRequest request,
            HttpServletResponse response) {
        String refreshToken = extractCookieValue(request, "refresh_token");
        TokenPair pair = authService.refresh(refreshToken);
        setAccessCookie(response, pair.accessToken());
        setRefreshCookie(response, pair.refreshToken());
        return ResponseEntity.ok(authService.toResponse(pair.user()));
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(HttpServletResponse response) {
        clearCookie(response, "access_token", "/api");
        clearCookie(response, "refresh_token", "/api/auth");
        return ResponseEntity.noContent().build();
    }

    // ── cookie helpers ──────────────────────────────────────────────────────

    private void setAccessCookie(HttpServletResponse response, String token) {
        Cookie cookie = new Cookie("access_token", token);
        cookie.setHttpOnly(true);
        cookie.setSecure(cookieSecure);
        cookie.setPath("/api");
        cookie.setMaxAge(24 * 60 * 60); // 24 h — matches JWT expiry
        cookie.setAttribute("SameSite", "Strict");
        response.addCookie(cookie);
    }

    private void setRefreshCookie(HttpServletResponse response, String token) {
        Cookie cookie = new Cookie("refresh_token", token);
        cookie.setHttpOnly(true);
        cookie.setSecure(cookieSecure);
        cookie.setPath("/api/auth"); // scoped: only sent to /api/auth/*
        cookie.setMaxAge(7 * 24 * 60 * 60); // 7 days — matches JWT expiry
        cookie.setAttribute("SameSite", "Strict");
        response.addCookie(cookie);
    }

    private void clearCookie(HttpServletResponse response, String name, String path) {
        Cookie cookie = new Cookie(name, "");
        cookie.setHttpOnly(true);
        cookie.setSecure(cookieSecure);
        cookie.setPath(path);
        cookie.setMaxAge(0);
        response.addCookie(cookie);
    }

    private String extractCookieValue(HttpServletRequest request, String name) {
        if (request.getCookies() == null) {
            throw new com.darkness.videoplatform.exception.UnauthorizedException("Missing refresh token cookie");
        }
        return Arrays.stream(request.getCookies())
                .filter(c -> name.equals(c.getName()))
                .map(Cookie::getValue)
                .findFirst()
                .orElseThrow(() -> new com.darkness.videoplatform.exception.UnauthorizedException("Missing refresh token cookie"));
    }
}
