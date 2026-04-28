package com.darkness.system.management.security;

import com.darkness.system.management.config.JwtConfig;
import com.darkness.system.management.exception.InvalidTokenException;
import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class JwtAuthenticationFilterTest {

    @Mock JwtTokenProvider jwtTokenProvider;
    @Mock FilterChain filterChain;

    JwtAuthenticationFilter filter;

    UUID userId;

    @BeforeEach
    void setUp() {
        filter = new JwtAuthenticationFilter(jwtTokenProvider);
        userId = UUID.randomUUID();
        SecurityContextHolder.clearContext();
    }

    @Test
    void doFilter_noCookiesNoHeader_proceedsUnauthenticated() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilterInternal(request, response, filterChain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        verify(filterChain).doFilter(request, response);
    }

    @Test
    void doFilter_validBearerToken_setsAuthentication() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer valid.token.here");
        MockHttpServletResponse response = new MockHttpServletResponse();

        Claims mockClaims = mock(Claims.class);
        when(jwtTokenProvider.parseAndValidate("valid.token.here")).thenReturn(mockClaims);
        when(jwtTokenProvider.extractUserId(mockClaims)).thenReturn(userId);
        when(jwtTokenProvider.extractEmail(mockClaims)).thenReturn("user@test.com");

        filter.doFilterInternal(request, response, filterChain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNotNull();
        UserPrincipal principal = (UserPrincipal) SecurityContextHolder.getContext()
                .getAuthentication().getPrincipal();
        assertThat(principal.userId()).isEqualTo(userId);
        assertThat(principal.email()).isEqualTo("user@test.com");
        verify(filterChain).doFilter(request, response);
    }

    @Test
    void doFilter_invalidBearerToken_proceedsUnauthenticated() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer invalid.token");
        MockHttpServletResponse response = new MockHttpServletResponse();

        when(jwtTokenProvider.parseAndValidate("invalid.token"))
                .thenThrow(new InvalidTokenException("Invalid JWT"));

        filter.doFilterInternal(request, response, filterChain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        verify(filterChain).doFilter(request, response);
    }

    @Test
    void doFilter_validCookieToken_setsAuthentication() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        Cookie cookie = new Cookie("access_token", "cookie.token.value");
        request.setCookies(cookie);
        MockHttpServletResponse response = new MockHttpServletResponse();

        Claims mockClaims = mock(Claims.class);
        when(jwtTokenProvider.parseAndValidate("cookie.token.value")).thenReturn(mockClaims);
        when(jwtTokenProvider.extractUserId(mockClaims)).thenReturn(userId);
        when(jwtTokenProvider.extractEmail(mockClaims)).thenReturn("cookie@test.com");

        filter.doFilterInternal(request, response, filterChain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNotNull();
        verify(filterChain).doFilter(request, response);
    }

    @Test
    void doFilter_nonMatchingCookie_proceedsUnauthenticated() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        Cookie cookie = new Cookie("other_cookie", "some_value");
        request.setCookies(cookie);
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilterInternal(request, response, filterChain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        verify(jwtTokenProvider, never()).parseAndValidate(anyString());
        verify(filterChain).doFilter(request, response);
    }

    @Test
    void doFilter_bearerPrefixOnly_proceedsUnauthenticated() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer ");
        MockHttpServletResponse response = new MockHttpServletResponse();

        Claims mockClaims = mock(Claims.class);
        when(jwtTokenProvider.parseAndValidate("")).thenThrow(new InvalidTokenException("empty"));

        filter.doFilterInternal(request, response, filterChain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }
}
