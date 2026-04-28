package com.darkness.system.management.service;

import com.darkness.system.management.config.JwtConfig;
import com.darkness.system.management.domain.RefreshToken;
import com.darkness.system.management.domain.User;
import com.darkness.system.management.domain.enums.GlobalRole;
import com.darkness.system.management.dto.request.LoginRequest;
import com.darkness.system.management.dto.response.AuthResponse;
import com.darkness.system.management.dto.response.LoginResult;
import com.darkness.system.management.exception.AccountLockedException;
import com.darkness.system.management.exception.InvalidTokenException;
import com.darkness.system.management.repository.RefreshTokenRepository;
import com.darkness.system.management.repository.UserRepository;
import com.darkness.system.management.security.JwtTokenProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock UserRepository userRepository;
    @Mock RefreshTokenRepository refreshTokenRepository;
    @Mock PasswordEncoder passwordEncoder;
    @Mock JwtTokenProvider jwtTokenProvider;
    @Mock JwtConfig jwtConfig;

    @InjectMocks AuthService authService;

    User activeUser;

    @BeforeEach
    void setUp() {
        activeUser = new User();
        activeUser.setId(UUID.randomUUID());
        activeUser.setEmail("user@example.com");
        activeUser.setFullName("Test User");
        activeUser.setPasswordHash("$2a$hashed");
        activeUser.setGlobalRole(GlobalRole.EDITOR);
        activeUser.setActive(true);
        activeUser.setFailedLoginAttempts(0);
    }

    // FINDING-007: BCrypt check runs BEFORE account-state checks
    @Test
    void login_wrongPassword_doesNotRevealAccountState() {
        when(userRepository.findByEmailIgnoreCase("user@example.com")).thenReturn(Optional.of(activeUser));
        when(passwordEncoder.matches("wrong", "$2a$hashed")).thenReturn(false);

        assertThatThrownBy(() -> authService.login(new LoginRequest("user@example.com", "wrong")))
                .hasMessageContaining("Invalid credentials");

        // BCrypt was invoked — timing attack prevention
        verify(passwordEncoder).matches("wrong", "$2a$hashed");
    }

    @Test
    void login_nonexistentEmail_runsTimingDummyCheck() {
        when(userRepository.findByEmailIgnoreCase("ghost@example.com")).thenReturn(Optional.empty());
        // even for missing user, BCrypt check should still run (dummy hash)
        when(passwordEncoder.matches(anyString(), anyString())).thenReturn(false);

        assertThatThrownBy(() -> authService.login(new LoginRequest("ghost@example.com", "pass")))
                .hasMessageContaining("Invalid credentials");

        verify(passwordEncoder).matches(anyString(), anyString());
    }

    @Test
    void login_lockedAccount_throwsAccountLockedException() {
        activeUser.setLockedUntil(Instant.now().plusSeconds(3600));
        when(userRepository.findByEmailIgnoreCase("user@example.com")).thenReturn(Optional.of(activeUser));
        when(passwordEncoder.matches(anyString(), anyString())).thenReturn(true);

        assertThatThrownBy(() -> authService.login(new LoginRequest("user@example.com", "correctPass")))
                .isInstanceOf(AccountLockedException.class);
    }

    @Test
    void login_success_returnsAuthResponseAndSetsRefreshToken() {
        when(userRepository.findByEmailIgnoreCase("user@example.com")).thenReturn(Optional.of(activeUser));
        when(passwordEncoder.matches(anyString(), anyString())).thenReturn(true);
        when(jwtTokenProvider.generateAccessToken(any(), anyString())).thenReturn("access.token.here");
        when(jwtConfig.refreshTokenTtlDays()).thenReturn(7);
        when(refreshTokenRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        LoginResult result = authService.login(new LoginRequest("user@example.com", "correctPass"));

        assertThat(result.authResponse().accessToken()).isEqualTo("access.token.here");
        assertThat(result.authResponse().email()).isEqualTo("user@example.com");
        assertThat(result.rawRefreshToken()).isNotBlank();
        verify(refreshTokenRepository).save(any(RefreshToken.class));
        verify(userRepository).save(activeUser); // failedLoginAttempts reset
    }

    @Test
    void login_success_resetsFailedAttempts() {
        activeUser.setFailedLoginAttempts(3);
        when(userRepository.findByEmailIgnoreCase("user@example.com")).thenReturn(Optional.of(activeUser));
        when(passwordEncoder.matches(anyString(), anyString())).thenReturn(true);
        when(jwtTokenProvider.generateAccessToken(any(), anyString())).thenReturn("token");
        when(jwtConfig.refreshTokenTtlDays()).thenReturn(7);
        when(refreshTokenRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        authService.login(new LoginRequest("user@example.com", "correct"));

        assertThat(activeUser.getFailedLoginAttempts()).isZero();
    }

    @Test
    void login_wrongPassword_incrementsFailedAttempts() {
        when(userRepository.findByEmailIgnoreCase("user@example.com")).thenReturn(Optional.of(activeUser));
        when(passwordEncoder.matches(anyString(), anyString())).thenReturn(false);

        assertThatThrownBy(() -> authService.login(new LoginRequest("user@example.com", "wrong")));

        assertThat(activeUser.getFailedLoginAttempts()).isEqualTo(1);
        verify(userRepository).save(activeUser);
    }

    @Test
    void login_exceeds5FailedAttempts_locksAccount() {
        activeUser.setFailedLoginAttempts(4);
        when(userRepository.findByEmailIgnoreCase("user@example.com")).thenReturn(Optional.of(activeUser));
        when(passwordEncoder.matches(anyString(), anyString())).thenReturn(false);

        assertThatThrownBy(() -> authService.login(new LoginRequest("user@example.com", "wrong")));

        assertThat(activeUser.getLockedUntil()).isAfter(Instant.now());
    }

    // Refresh token rotation
    @Test
    void refresh_validToken_issuesNewPairAndRevokesOld() {
        String rawToken = "raw-token";
        String tokenHash = authService.hashToken(rawToken);

        RefreshToken token = new RefreshToken();
        token.setId(UUID.randomUUID());
        token.setFamilyId(UUID.randomUUID());
        token.setUserId(activeUser.getId());
        token.setTokenHash(tokenHash);
        token.setRevoked(false);
        token.setExpiresAt(Instant.now().plusSeconds(3600));

        when(refreshTokenRepository.findByTokenHashAndIsRevokedFalse(tokenHash))
                .thenReturn(Optional.of(token));
        when(userRepository.findById(activeUser.getId())).thenReturn(Optional.of(activeUser));
        when(jwtTokenProvider.generateAccessToken(any(), anyString())).thenReturn("new.access.token");
        when(jwtConfig.refreshTokenTtlDays()).thenReturn(7);
        when(refreshTokenRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        authService.refresh(rawToken);

        assertThat(token.isRevoked()).isTrue();
        verify(refreshTokenRepository, times(2)).save(any());
    }

    // Token reuse detection — family invalidation
    @Test
    void refresh_revokedToken_invalidatesEntireFamily() {
        String rawRevoked = "raw-revoked-token";
        String revokedHash = authService.hashToken(rawRevoked);

        RefreshToken revokedToken = new RefreshToken();
        UUID familyId = UUID.randomUUID();
        revokedToken.setFamilyId(familyId);
        revokedToken.setUserId(activeUser.getId());
        revokedToken.setRevoked(true);

        when(refreshTokenRepository.findByTokenHashAndIsRevokedFalse(revokedHash))
                .thenReturn(Optional.empty());
        when(refreshTokenRepository.findByTokenHash(revokedHash))
                .thenReturn(Optional.of(revokedToken));

        assertThatThrownBy(() -> authService.refresh(rawRevoked))
                .isInstanceOf(InvalidTokenException.class);

        verify(refreshTokenRepository).revokeAllByFamilyId(familyId);
    }

    @Test
    void logout_revokesToken() {
        String rawToken = "raw-token";
        String tokenHash = authService.hashToken(rawToken);

        RefreshToken token = new RefreshToken();
        token.setRevoked(false);

        when(refreshTokenRepository.findByTokenHashAndIsRevokedFalse(tokenHash))
                .thenReturn(Optional.of(token));

        authService.logout(rawToken);

        assertThat(token.isRevoked()).isTrue();
        verify(refreshTokenRepository).save(token);
    }

    @Test
    void logout_tokenNotFound_doesNothing() {
        String rawToken = "raw-no-token";
        String tokenHash = authService.hashToken(rawToken);
        when(refreshTokenRepository.findByTokenHashAndIsRevokedFalse(tokenHash))
                .thenReturn(Optional.empty());

        authService.logout(rawToken);

        verify(refreshTokenRepository, never()).save(any());
    }

    @Test
    void logoutAll_revokesAllUserTokens() {
        authService.logoutAll(activeUser.getId());

        verify(refreshTokenRepository).revokeAllByUserId(activeUser.getId());
    }

    @Test
    void login_inactiveAccount_throwsInvalidTokenException() {
        activeUser.setActive(false);
        when(userRepository.findByEmailIgnoreCase("user@example.com")).thenReturn(Optional.of(activeUser));
        when(passwordEncoder.matches(anyString(), anyString())).thenReturn(true);

        assertThatThrownBy(() -> authService.login(new LoginRequest("user@example.com", "correctPass")))
                .isInstanceOf(com.darkness.system.management.exception.InvalidTokenException.class);
    }

    @Test
    void refresh_expiredToken_revokesAndThrows() {
        String rawToken = "raw-expired-token";
        String tokenHash = authService.hashToken(rawToken);

        RefreshToken expiredToken = new RefreshToken();
        expiredToken.setId(UUID.randomUUID());
        expiredToken.setFamilyId(UUID.randomUUID());
        expiredToken.setUserId(activeUser.getId());
        expiredToken.setTokenHash(tokenHash);
        expiredToken.setRevoked(false);
        expiredToken.setExpiresAt(Instant.now().minusSeconds(3600));

        when(refreshTokenRepository.findByTokenHashAndIsRevokedFalse(tokenHash))
                .thenReturn(Optional.of(expiredToken));

        assertThatThrownBy(() -> authService.refresh(rawToken))
                .isInstanceOf(com.darkness.system.management.exception.InvalidTokenException.class);

        assertThat(expiredToken.isRevoked()).isTrue();
        verify(refreshTokenRepository).save(expiredToken);
    }

    @Test
    void refresh_tokenNotFound_noFamilyFound_throws() {
        String rawToken = "raw-unknown-token";
        String tokenHash = authService.hashToken(rawToken);

        when(refreshTokenRepository.findByTokenHashAndIsRevokedFalse(tokenHash))
                .thenReturn(Optional.empty());
        when(refreshTokenRepository.findByTokenHash(tokenHash))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.refresh(rawToken))
                .isInstanceOf(com.darkness.system.management.exception.InvalidTokenException.class);

        verify(refreshTokenRepository, never()).revokeAllByFamilyId(any());
    }

    @Test
    void refresh_userNotFound_throws() {
        String rawToken = "raw-orphan-token";
        String tokenHash = authService.hashToken(rawToken);

        RefreshToken token = new RefreshToken();
        token.setId(UUID.randomUUID());
        token.setFamilyId(UUID.randomUUID());
        token.setUserId(UUID.randomUUID());
        token.setTokenHash(tokenHash);
        token.setRevoked(false);
        token.setExpiresAt(Instant.now().plusSeconds(3600));

        when(refreshTokenRepository.findByTokenHashAndIsRevokedFalse(tokenHash))
                .thenReturn(Optional.of(token));
        when(userRepository.findById(token.getUserId())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.refresh(rawToken))
                .isInstanceOf(com.darkness.system.management.exception.ResourceNotFoundException.class);
    }

    @Test
    void refresh_inactiveUser_revokesAllAndThrows() {
        activeUser.setActive(false);
        String rawToken = "raw-inactive-user-token";
        String tokenHash = authService.hashToken(rawToken);

        RefreshToken token = new RefreshToken();
        token.setId(UUID.randomUUID());
        token.setFamilyId(UUID.randomUUID());
        token.setUserId(activeUser.getId());
        token.setTokenHash(tokenHash);
        token.setRevoked(false);
        token.setExpiresAt(Instant.now().plusSeconds(3600));

        when(refreshTokenRepository.findByTokenHashAndIsRevokedFalse(tokenHash))
                .thenReturn(Optional.of(token));
        when(userRepository.findById(activeUser.getId())).thenReturn(Optional.of(activeUser));

        assertThatThrownBy(() -> authService.refresh(rawToken))
                .isInstanceOf(com.darkness.system.management.exception.InvalidTokenException.class);

        verify(refreshTokenRepository).revokeAllByUserId(activeUser.getId());
    }
}
