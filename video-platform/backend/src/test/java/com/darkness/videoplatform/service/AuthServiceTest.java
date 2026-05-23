package com.darkness.videoplatform.service;

import com.darkness.videoplatform.dto.LoginRequest;
import com.darkness.videoplatform.dto.RegisterRequest;
import com.darkness.videoplatform.entity.User;
import com.darkness.videoplatform.exception.BadRequestException;
import com.darkness.videoplatform.exception.UnauthorizedException;
import com.darkness.videoplatform.repository.UserRepository;
import com.darkness.videoplatform.security.JwtTokenProvider;
import com.darkness.videoplatform.service.AuthService.TokenPair;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtTokenProvider tokenProvider;

    @InjectMocks
    private AuthService authService;

    private User testUser;

    @BeforeEach
    void setUp() {
        testUser = User.builder()
                .id(1L)
                .email("test@example.com")
                .username("testuser")
                .password("encoded-password")
                .build();
    }

    @Test
    void register_shouldCreateUserAndReturnTokenPair() {
        RegisterRequest request = RegisterRequest.builder()
                .email("new@example.com")
                .username("newuser")
                .password("password123")
                .build();

        when(userRepository.existsByEmail(anyString())).thenReturn(false);
        when(userRepository.existsByUsername(anyString())).thenReturn(false);
        when(passwordEncoder.encode(anyString())).thenReturn("encoded");
        when(userRepository.save(any(User.class))).thenReturn(testUser);
        when(tokenProvider.generateAccessToken(any(), anyString())).thenReturn("access-token");
        when(tokenProvider.generateRefreshToken(any())).thenReturn("refresh-token");

        TokenPair pair = authService.register(request);

        assertThat(pair.accessToken()).isEqualTo("access-token");
        assertThat(pair.refreshToken()).isEqualTo("refresh-token");
        assertThat(pair.user().getEmail()).isEqualTo("test@example.com");
    }

    @Test
    void register_shouldThrowWhenEmailExists() {
        RegisterRequest request = RegisterRequest.builder()
                .email("existing@example.com")
                .username("newuser")
                .password("password123")
                .build();

        when(userRepository.existsByEmail("existing@example.com")).thenReturn(true);

        assertThatThrownBy(() -> authService.register(request))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Email is already registered");
    }

    @Test
    void register_shouldThrowWhenUsernameExists() {
        RegisterRequest request = RegisterRequest.builder()
                .email("new@example.com")
                .username("existinguser")
                .password("password123")
                .build();

        when(userRepository.existsByEmail(anyString())).thenReturn(false);
        when(userRepository.existsByUsername("existinguser")).thenReturn(true);

        assertThatThrownBy(() -> authService.register(request))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Username is already taken");
    }

    @Test
    void login_shouldReturnTokenPairWhenCredentialsValid() {
        LoginRequest request = LoginRequest.builder()
                .email("test@example.com")
                .password("password123")
                .build();

        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(testUser));
        when(passwordEncoder.matches("password123", "encoded-password")).thenReturn(true);
        when(tokenProvider.generateAccessToken(any(), anyString())).thenReturn("access-token");
        when(tokenProvider.generateRefreshToken(any())).thenReturn("refresh-token");

        TokenPair pair = authService.login(request);

        assertThat(pair.accessToken()).isEqualTo("access-token");
        assertThat(pair.user().getUsername()).isEqualTo("testuser");
    }

    @Test
    void login_shouldThrowWhenEmailNotFound() {
        LoginRequest request = LoginRequest.builder()
                .email("notfound@example.com")
                .password("password123")
                .build();

        when(userRepository.findByEmail("notfound@example.com")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.login(request))
                .isInstanceOf(BadCredentialsException.class);
    }

    @Test
    void login_shouldThrowWhenPasswordIncorrect() {
        LoginRequest request = LoginRequest.builder()
                .email("test@example.com")
                .password("wrongpassword")
                .build();

        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(testUser));
        when(passwordEncoder.matches("wrongpassword", "encoded-password")).thenReturn(false);

        assertThatThrownBy(() -> authService.login(request))
                .isInstanceOf(BadCredentialsException.class);
    }

    @Test
    void refresh_shouldThrowWhenTokenInvalid() {
        when(tokenProvider.validateToken("invalid-token")).thenReturn(false);

        assertThatThrownBy(() -> authService.refresh("invalid-token"))
                .isInstanceOf(UnauthorizedException.class)
                .hasMessageContaining("Invalid or expired refresh token");
    }

    @Test
    void refresh_shouldThrowWhenNotRefreshToken() {
        when(tokenProvider.validateToken("access-token")).thenReturn(true);
        when(tokenProvider.getTokenType("access-token")).thenReturn("access");

        assertThatThrownBy(() -> authService.refresh("access-token"))
                .isInstanceOf(UnauthorizedException.class)
                .hasMessageContaining("not a refresh token");
    }

    @Test
    void refresh_shouldThrowWhenUserNotFound() {
        when(tokenProvider.validateToken("refresh-token")).thenReturn(true);
        when(tokenProvider.getTokenType("refresh-token")).thenReturn("refresh");
        when(tokenProvider.getUserIdFromToken("refresh-token")).thenReturn(99L);
        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.refresh("refresh-token"))
                .isInstanceOf(UnauthorizedException.class)
                .hasMessageContaining("User not found");
    }

    @Test
    void refresh_validToken_returnsNewTokenPair() {
        when(tokenProvider.validateToken("refresh-token")).thenReturn(true);
        when(tokenProvider.getTokenType("refresh-token")).thenReturn("refresh");
        when(tokenProvider.getUserIdFromToken("refresh-token")).thenReturn(1L);
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(tokenProvider.generateAccessToken(any(), anyString())).thenReturn("new-access");
        when(tokenProvider.generateRefreshToken(any())).thenReturn("new-refresh");

        TokenPair pair = authService.refresh("refresh-token");

        assertThat(pair.accessToken()).isEqualTo("new-access");
        assertThat(pair.refreshToken()).isEqualTo("new-refresh");
    }

    @Test
    void toResponse_mapsUserToAuthResponse() {
        var response = authService.toResponse(testUser);

        assertThat(response.getUser().getId()).isEqualTo(testUser.getId());
        assertThat(response.getUser().getEmail()).isEqualTo(testUser.getEmail());
        assertThat(response.getUser().getUsername()).isEqualTo(testUser.getUsername());
    }
}
