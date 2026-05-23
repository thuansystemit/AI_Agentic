package com.darkness.videoplatform.security;

import com.darkness.videoplatform.entity.User;
import com.darkness.videoplatform.repository.UserRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class JwtAuthenticationFilterTest {

    @Mock JwtTokenProvider tokenProvider;
    @Mock UserRepository userRepository;

    @InjectMocks JwtAuthenticationFilter filter;

    @AfterEach
    void clearContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void doFilter_noCookies_proceedsWithNoAuth() throws Exception {
        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse response = mock(HttpServletResponse.class);
        FilterChain chain = mock(FilterChain.class);
        when(request.getCookies()).thenReturn(null);

        filter.doFilterInternal(request, response, chain);

        verify(chain).doFilter(request, response);
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }

    @Test
    void doFilter_cookiesWithoutAccessToken_proceedsWithNoAuth() throws Exception {
        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse response = mock(HttpServletResponse.class);
        FilterChain chain = mock(FilterChain.class);
        when(request.getCookies()).thenReturn(new Cookie[]{new Cookie("other_cookie", "value")});

        filter.doFilterInternal(request, response, chain);

        verify(chain).doFilter(request, response);
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }

    @Test
    void doFilter_invalidJwt_proceedsWithNoAuth() throws Exception {
        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse response = mock(HttpServletResponse.class);
        FilterChain chain = mock(FilterChain.class);
        when(request.getCookies()).thenReturn(new Cookie[]{new Cookie("access_token", "bad.token")});
        when(tokenProvider.validateToken("bad.token")).thenReturn(false);

        filter.doFilterInternal(request, response, chain);

        verify(chain).doFilter(request, response);
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }

    @Test
    void doFilter_validAccessJwt_userFound_setsAuthentication() throws Exception {
        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse response = mock(HttpServletResponse.class);
        FilterChain chain = mock(FilterChain.class);
        User user = User.builder().id(1L).email("a@b.com").build();
        when(request.getCookies()).thenReturn(new Cookie[]{new Cookie("access_token", "valid.jwt")});
        when(tokenProvider.validateToken("valid.jwt")).thenReturn(true);
        when(tokenProvider.getTokenType("valid.jwt")).thenReturn("access");
        when(tokenProvider.getUserIdFromToken("valid.jwt")).thenReturn(1L);
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        filter.doFilterInternal(request, response, chain);

        verify(chain).doFilter(request, response);
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNotNull();
        assertThat(SecurityContextHolder.getContext().getAuthentication().getPrincipal()).isEqualTo(user);
    }

    @Test
    void doFilter_validAccessJwt_userNotFound_proceedsWithNoAuth() throws Exception {
        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse response = mock(HttpServletResponse.class);
        FilterChain chain = mock(FilterChain.class);
        when(request.getCookies()).thenReturn(new Cookie[]{new Cookie("access_token", "valid.jwt")});
        when(tokenProvider.validateToken("valid.jwt")).thenReturn(true);
        when(tokenProvider.getTokenType("valid.jwt")).thenReturn("access");
        when(tokenProvider.getUserIdFromToken("valid.jwt")).thenReturn(99L);
        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        filter.doFilterInternal(request, response, chain);

        verify(chain).doFilter(request, response);
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }

    @Test
    void doFilter_validRefreshJwt_proceedsWithNoAuth() throws Exception {
        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse response = mock(HttpServletResponse.class);
        FilterChain chain = mock(FilterChain.class);
        when(request.getCookies()).thenReturn(new Cookie[]{new Cookie("access_token", "refresh.jwt")});
        when(tokenProvider.validateToken("refresh.jwt")).thenReturn(true);
        when(tokenProvider.getTokenType("refresh.jwt")).thenReturn("refresh");

        filter.doFilterInternal(request, response, chain);

        verify(chain).doFilter(request, response);
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }

    @Test
    void doFilter_exceptionDuringProcessing_proceedsWithNoAuth() throws Exception {
        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse response = mock(HttpServletResponse.class);
        FilterChain chain = mock(FilterChain.class);
        when(request.getCookies()).thenReturn(new Cookie[]{new Cookie("access_token", "token")});
        when(tokenProvider.validateToken("token")).thenThrow(new RuntimeException("unexpected"));

        filter.doFilterInternal(request, response, chain);

        verify(chain).doFilter(request, response);
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }
}
