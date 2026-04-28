package com.darkness.system.management.service;

import com.darkness.system.management.config.JwtConfig;
import com.darkness.system.management.domain.RefreshToken;
import com.darkness.system.management.domain.User;
import com.darkness.system.management.dto.request.LoginRequest;
import com.darkness.system.management.dto.response.AuthResponse;
import com.darkness.system.management.dto.response.LoginResult;
import com.darkness.system.management.exception.AccountLockedException;
import com.darkness.system.management.exception.InvalidTokenException;
import com.darkness.system.management.exception.ResourceNotFoundException;
import com.darkness.system.management.repository.RefreshTokenRepository;
import com.darkness.system.management.repository.UserRepository;
import com.darkness.system.management.security.JwtTokenProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.Base64;
import java.util.HexFormat;
import java.util.Optional;
import java.util.UUID;

@Service
public class AuthService {

    private static final Logger log = LoggerFactory.getLogger(AuthService.class);

    private static final String DUMMY_HASH = "$2a$12$dummyhashfortimingnobodycanloginwiththis00000000000000";
    private static final int MAX_FAILED_ATTEMPTS = 5;
    private static final int LOCK_DURATION_MINUTES = 15;

    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    private final JwtConfig jwtConfig;

    public AuthService(UserRepository userRepository,
                       RefreshTokenRepository refreshTokenRepository,
                       PasswordEncoder passwordEncoder,
                       JwtTokenProvider jwtTokenProvider,
                       JwtConfig jwtConfig) {
        this.userRepository = userRepository;
        this.refreshTokenRepository = refreshTokenRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtTokenProvider = jwtTokenProvider;
        this.jwtConfig = jwtConfig;
    }

    @Transactional
    public LoginResult login(LoginRequest request) {
        log.info("Login attempt for email={}", request.email());
        Optional<User> found = userRepository.findByEmailIgnoreCase(request.email());

        // FINDING-007: BCrypt runs first regardless — prevents timing-based user enumeration
        String hashToCheck = found.map(User::getPasswordHash).orElse(DUMMY_HASH);
        boolean passwordMatches = passwordEncoder.matches(request.password(), hashToCheck);

        if (found.isEmpty() || !passwordMatches) {
            found.ifPresent(u -> {
                u.setFailedLoginAttempts(u.getFailedLoginAttempts() + 1);
                if (u.getFailedLoginAttempts() >= MAX_FAILED_ATTEMPTS) {
                    u.setLockedUntil(Instant.now().plusSeconds(LOCK_DURATION_MINUTES * 60L));
                    log.warn("Account locked after {} failed attempts for email={}", MAX_FAILED_ATTEMPTS, request.email());
                }
                userRepository.save(u);
            });
            log.warn("Login failed for email={} — invalid credentials or unknown user", request.email());
            throw new InvalidTokenException("Invalid credentials");
        }

        User user = found.get();

        if (user.isLocked()) {
            log.warn("Login rejected — account locked until={} for email={}", user.getLockedUntil(), user.getEmail());
            throw new AccountLockedException(user.getLockedUntil());
        }

        if (!user.isActive()) {
            log.warn("Login rejected — inactive account for email={}", user.getEmail());
            throw new InvalidTokenException("Invalid credentials");
        }

        user.setFailedLoginAttempts(0);
        user.setLockedUntil(null);
        userRepository.save(user);

        String accessToken = jwtTokenProvider.generateAccessToken(user.getId(), user.getEmail());
        String rawRefreshToken = generateSecureToken();
        RefreshToken refreshToken = buildRefreshToken(rawRefreshToken, user.getId(), UUID.randomUUID());
        refreshTokenRepository.save(refreshToken);

        log.info("Login successful userId={} email={} role={}", user.getId(), user.getEmail(), user.getGlobalRole());
        AuthResponse authResponse = new AuthResponse(user.getId(), user.getEmail(), user.getFullName(),
                user.getGlobalRole(), accessToken);
        return new LoginResult(authResponse, rawRefreshToken);
    }

    @Transactional
    public LoginResult refresh(String rawToken) {
        log.debug("Token refresh requested");
        String tokenHash = hashToken(rawToken);

        // FINDING-014: SELECT FOR UPDATE on active-only lookup
        Optional<RefreshToken> active = refreshTokenRepository.findByTokenHashAndIsRevokedFalse(tokenHash);

        if (active.isEmpty()) {
            // Token not found or already revoked — check for reuse attack
            refreshTokenRepository.findByTokenHash(tokenHash).ifPresent(revoked -> {
                log.warn("Refresh token reuse detected — revoking entire family familyId={}", revoked.getFamilyId());
                refreshTokenRepository.revokeAllByFamilyId(revoked.getFamilyId());
            });
            log.warn("Refresh token not found or already revoked");
            throw new InvalidTokenException("Refresh token invalid or expired");
        }

        RefreshToken current = active.get();

        if (current.isExpired()) {
            current.setRevoked(true);
            refreshTokenRepository.save(current);
            log.warn("Refresh token expired for userId={}", current.getUserId());
            throw new InvalidTokenException("Refresh token expired");
        }

        User user = userRepository.findById(current.getUserId())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        if (!user.isActive() || user.isLocked()) {
            refreshTokenRepository.revokeAllByUserId(user.getId());
            log.warn("Refresh rejected — account not accessible userId={} active={} locked={}",
                    user.getId(), user.isActive(), user.isLocked());
            throw new InvalidTokenException("Account not accessible");
        }

        // Rotate: revoke current, issue new in same family
        current.setRevoked(true);
        refreshTokenRepository.save(current);

        String newRaw = generateSecureToken();
        RefreshToken newToken = buildRefreshToken(newRaw, user.getId(), current.getFamilyId());
        refreshTokenRepository.save(newToken);

        String accessToken = jwtTokenProvider.generateAccessToken(user.getId(), user.getEmail());
        log.info("Token refreshed userId={} email={}", user.getId(), user.getEmail());
        AuthResponse authResponse = new AuthResponse(user.getId(), user.getEmail(), user.getFullName(),
                user.getGlobalRole(), accessToken);
        return new LoginResult(authResponse, newRaw);
    }

    @Transactional
    public void logout(String rawToken) {
        String tokenHash = hashToken(rawToken);
        refreshTokenRepository.findByTokenHashAndIsRevokedFalse(tokenHash).ifPresent(t -> {
            t.setRevoked(true);
            refreshTokenRepository.save(t);
            log.info("Logout: refresh token revoked userId={}", t.getUserId());
        });
    }

    @Transactional
    public void logoutAll(UUID userId) {
        refreshTokenRepository.revokeAllByUserId(userId);
        log.info("Logout-all: all refresh tokens revoked userId={}", userId);
    }

    private RefreshToken buildRefreshToken(String rawToken, UUID userId, UUID familyId) {
        RefreshToken token = new RefreshToken();
        token.setTokenHash(hashToken(rawToken));
        token.setUserId(userId);
        token.setFamilyId(familyId);
        token.setExpiresAt(Instant.now().plusSeconds(jwtConfig.refreshTokenTtlDays() * 86400L));
        token.setRevoked(false);
        return token;
    }

    private String generateSecureToken() {
        byte[] bytes = new byte[32];
        new java.security.SecureRandom().nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    public String hashToken(String rawToken) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(rawToken.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 not available", e);
        }
    }
}
