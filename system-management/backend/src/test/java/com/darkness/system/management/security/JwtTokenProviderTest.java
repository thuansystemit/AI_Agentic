package com.darkness.system.management.security;

import com.darkness.system.management.config.JwtConfig;
import com.darkness.system.management.exception.InvalidTokenException;
import io.jsonwebtoken.Claims;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.*;

class JwtTokenProviderTest {

    // 64-char secret → HS512-safe key
    private static final String SECRET = "test-secret-key-that-is-long-enough-for-hmac-sha256-algorithm!!";

    JwtTokenProvider provider;

    @BeforeEach
    void setUp() {
        JwtConfig config = new JwtConfig(SECRET, 15, 7);
        provider = new JwtTokenProvider(config);
    }

    @Test
    void generateAccessToken_returnsNonNullToken() {
        UUID userId = UUID.randomUUID();
        String token = provider.generateAccessToken(userId, "user@test.com");

        assertThat(token).isNotBlank();
        assertThat(token.split("\\.")).hasSize(3); // header.payload.signature
    }

    @Test
    void parseAndValidate_validToken_returnsClaims() {
        UUID userId = UUID.randomUUID();
        String token = provider.generateAccessToken(userId, "user@test.com");

        Claims claims = provider.parseAndValidate(token);

        assertThat(claims).isNotNull();
        assertThat(claims.getSubject()).isEqualTo(userId.toString());
    }

    @Test
    void extractUserId_returnsCorrectUUID() {
        UUID userId = UUID.randomUUID();
        String token = provider.generateAccessToken(userId, "user@test.com");
        Claims claims = provider.parseAndValidate(token);

        UUID extracted = provider.extractUserId(claims);

        assertThat(extracted).isEqualTo(userId);
    }

    @Test
    void extractEmail_returnsCorrectEmail() {
        UUID userId = UUID.randomUUID();
        String token = provider.generateAccessToken(userId, "user@test.com");
        Claims claims = provider.parseAndValidate(token);

        String email = provider.extractEmail(claims);

        assertThat(email).isEqualTo("user@test.com");
    }

    @Test
    void parseAndValidate_invalidToken_throwsInvalidTokenException() {
        assertThatThrownBy(() -> provider.parseAndValidate("not.a.valid.jwt"))
                .isInstanceOf(InvalidTokenException.class);
    }

    @Test
    void parseAndValidate_tamperedToken_throwsInvalidTokenException() {
        UUID userId = UUID.randomUUID();
        String token = provider.generateAccessToken(userId, "user@test.com");
        String tampered = token.substring(0, token.length() - 5) + "XXXXX";

        assertThatThrownBy(() -> provider.parseAndValidate(tampered))
                .isInstanceOf(InvalidTokenException.class);
    }

    @Test
    void parseAndValidate_wrongSecretToken_throwsInvalidTokenException() {
        JwtConfig otherConfig = new JwtConfig("completely-different-secret-key-also-long-enough!!", 15, 7);
        JwtTokenProvider otherProvider = new JwtTokenProvider(otherConfig);
        String token = otherProvider.generateAccessToken(UUID.randomUUID(), "user@test.com");

        assertThatThrownBy(() -> provider.parseAndValidate(token))
                .isInstanceOf(InvalidTokenException.class);
    }
}
