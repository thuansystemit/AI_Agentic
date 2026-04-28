package com.darkness.videoplatform.service;

import com.darkness.videoplatform.dto.*;
import com.darkness.videoplatform.entity.User;
import com.darkness.videoplatform.exception.BadRequestException;
import com.darkness.videoplatform.exception.UnauthorizedException;
import com.darkness.videoplatform.repository.UserRepository;
import com.darkness.videoplatform.security.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider tokenProvider;

    @Transactional
    public TokenPair register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new BadRequestException("Email is already registered");
        }
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new BadRequestException("Username is already taken");
        }

        User user = User.builder()
                .email(request.getEmail())
                .username(request.getUsername())
                .password(passwordEncoder.encode(request.getPassword()))
                .build();

        return buildTokenPair(userRepository.save(user));
    }

    public TokenPair login(LoginRequest request) {
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new BadCredentialsException("Invalid email or password"));

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new BadCredentialsException("Invalid email or password");
        }

        return buildTokenPair(user);
    }

    public TokenPair refresh(String rawRefreshToken) {
        if (!tokenProvider.validateToken(rawRefreshToken)) {
            throw new UnauthorizedException("Invalid or expired refresh token");
        }
        if (!"refresh".equals(tokenProvider.getTokenType(rawRefreshToken))) {
            throw new UnauthorizedException("Token is not a refresh token");
        }

        Long userId = tokenProvider.getUserIdFromToken(rawRefreshToken);
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UnauthorizedException("User not found"));

        return buildTokenPair(user);
    }

    public AuthResponse toResponse(User user) {
        return AuthResponse.builder()
                .user(AuthResponse.UserResponse.builder()
                        .id(user.getId())
                        .username(user.getUsername())
                        .email(user.getEmail())
                        .build())
                .build();
    }

    private TokenPair buildTokenPair(User user) {
        return new TokenPair(
                tokenProvider.generateAccessToken(user.getId(), user.getEmail()),
                tokenProvider.generateRefreshToken(user.getId()),
                user
        );
    }

    public record TokenPair(String accessToken, String refreshToken, User user) {}
}
