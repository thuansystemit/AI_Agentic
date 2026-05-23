package com.darkness.system.management.controller;

import com.darkness.system.management.dto.request.ChangePasswordRequest;
import com.darkness.system.management.dto.request.LoginRequest;
import com.darkness.system.management.dto.request.UpdateProfileRequest;
import com.darkness.system.management.dto.response.AuthResponse;
import com.darkness.system.management.dto.response.LoginResult;
import com.darkness.system.management.dto.response.ProfileResponse;
import com.darkness.system.management.security.UserPrincipal;
import com.darkness.system.management.service.AuthService;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.Optional;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final AuthService authService;

    @Value("${app.jwt.access-token-ttl-minutes}")
    private int accessTokenTtlMinutes;

    @Value("${app.jwt.refresh-token-ttl-days}")
    private int refreshTokenTtlDays;

    @Value("${app.cookie.secure}")
    private boolean cookieSecure;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/login")
    ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request,
                                       HttpServletResponse response) {
        LoginResult result = authService.login(request);
        setAccessCookie(response, result.authResponse().accessToken(), accessTokenTtlMinutes * 60);
        setRefreshCookie(response, result.rawRefreshToken(), refreshTokenTtlDays * 86400);
        return ResponseEntity.ok(result.authResponse());  // accessToken is @JsonIgnore — not in body
    }

    @PostMapping("/refresh")
    ResponseEntity<AuthResponse> refresh(HttpServletRequest request,
                                         HttpServletResponse response) {
        String rawToken = extractRefreshCookie(request)
                .orElseThrow(() -> new com.darkness.system.management.exception.InvalidTokenException("No refresh token"));
        LoginResult result = authService.refresh(rawToken);
        setAccessCookie(response, result.authResponse().accessToken(), accessTokenTtlMinutes * 60);
        setRefreshCookie(response, result.rawRefreshToken(), refreshTokenTtlDays * 86400);
        return ResponseEntity.ok(result.authResponse());
    }

    @PostMapping("/logout")
    ResponseEntity<Void> logout(HttpServletRequest request, HttpServletResponse response) {
        extractRefreshCookie(request).ifPresent(authService::logout);
        clearAccessCookie(response);
        clearRefreshCookie(response);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/logout-all")
    ResponseEntity<Void> logoutAll(@AuthenticationPrincipal UserPrincipal principal,
                                   HttpServletResponse response) {
        authService.logoutAll(principal.userId());
        clearAccessCookie(response);
        clearRefreshCookie(response);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/me")
    ResponseEntity<ProfileResponse> me(@AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(authService.getProfile(principal.userId()));
    }

    @PatchMapping("/me")
    ResponseEntity<ProfileResponse> updateProfile(@AuthenticationPrincipal UserPrincipal principal,
                                                  @Valid @RequestBody UpdateProfileRequest request) {
        return ResponseEntity.ok(authService.updateProfile(principal.userId(), request));
    }

    @PostMapping("/me/password")
    ResponseEntity<Void> changePassword(@AuthenticationPrincipal UserPrincipal principal,
                                        @Valid @RequestBody ChangePasswordRequest request) {
        authService.changePassword(principal.userId(), request);
        return ResponseEntity.noContent().build();
    }

    // ── Cookie helpers ────────────────────────────────────────────────────────

    private void setAccessCookie(HttpServletResponse response, String token, int maxAge) {
        Cookie cookie = new Cookie("access_token", token);
        cookie.setHttpOnly(true);
        cookie.setSecure(cookieSecure);
        cookie.setPath("/api/v1");
        cookie.setMaxAge(maxAge);
        cookie.setAttribute("SameSite", "Strict");
        response.addCookie(cookie);
    }

    private void clearAccessCookie(HttpServletResponse response) {
        Cookie cookie = new Cookie("access_token", "");
        cookie.setHttpOnly(true);
        cookie.setSecure(cookieSecure);
        cookie.setPath("/api/v1");
        cookie.setMaxAge(0);
        response.addCookie(cookie);
    }

    private void setRefreshCookie(HttpServletResponse response, String token, int maxAge) {
        Cookie cookie = new Cookie("refresh_token", token);
        cookie.setHttpOnly(true);
        cookie.setSecure(cookieSecure);
        cookie.setPath("/api/v1/auth");
        cookie.setMaxAge(maxAge);
        cookie.setAttribute("SameSite", "Strict");
        response.addCookie(cookie);
    }

    private void clearRefreshCookie(HttpServletResponse response) {
        Cookie cookie = new Cookie("refresh_token", "");
        cookie.setHttpOnly(true);
        cookie.setSecure(cookieSecure);
        cookie.setPath("/api/v1/auth");  // must match setRefreshCookie path
        cookie.setMaxAge(0);
        response.addCookie(cookie);
    }

    private Optional<String> extractRefreshCookie(HttpServletRequest request) {
        if (request.getCookies() == null) return Optional.empty();
        return Arrays.stream(request.getCookies())
                .filter(c -> "refresh_token".equals(c.getName()))
                .map(Cookie::getValue)
                .findFirst();
    }
}
