package com.darkness.videoplatform.security;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class JwtTokenProviderTest {

    private static final long ACCESS_MS = 3_600_000L;
    private static final long REFRESH_MS = 86_400_000L;

    private JwtTokenProvider provider(String secret) {
        return new JwtTokenProvider(secret, ACCESS_MS, REFRESH_MS);
    }

    @Test
    void constructor_shortKey_getsPadded() {
        JwtTokenProvider p = provider("short");
        assertThat(p.generateAccessToken(1L, "a@b.com")).isNotEmpty();
    }

    @Test
    void constructor_longBase64Key_noPadding() {
        // 32-byte base64 = MTIzNDU2Nzg5MDEyMzQ1Njc4OTAxMjM0NTY3ODkwMTI=
        JwtTokenProvider p = provider("MTIzNDU2Nzg5MDEyMzQ1Njc4OTAxMjM0NTY3ODkwMTI=");
        assertThat(p.generateAccessToken(1L, "a@b.com")).isNotEmpty();
    }

    @Test
    void validateToken_validToken_returnsTrue() {
        JwtTokenProvider p = provider("short");
        String token = p.generateAccessToken(1L, "a@b.com");
        assertThat(p.validateToken(token)).isTrue();
    }

    @Test
    void validateToken_invalidToken_returnsFalse() {
        JwtTokenProvider p = provider("short");
        assertThat(p.validateToken("not-a-jwt")).isFalse();
    }

    @Test
    void getUserIdFromToken_returnsCorrectId() {
        JwtTokenProvider p = provider("short");
        String token = p.generateAccessToken(42L, "a@b.com");
        assertThat(p.getUserIdFromToken(token)).isEqualTo(42L);
    }

    @Test
    void getTokenType_accessToken_returnsAccess() {
        JwtTokenProvider p = provider("short");
        String token = p.generateAccessToken(1L, "a@b.com");
        assertThat(p.getTokenType(token)).isEqualTo("access");
    }

    @Test
    void getTokenType_refreshToken_returnsRefresh() {
        JwtTokenProvider p = provider("short");
        String token = p.generateRefreshToken(1L);
        assertThat(p.getTokenType(token)).isEqualTo("refresh");
    }

    @Test
    void getAccessTokenExpirationMs_returnsConfiguredValue() {
        JwtTokenProvider p = provider("short");
        assertThat(p.getAccessTokenExpirationMs()).isEqualTo(ACCESS_MS);
    }
}
