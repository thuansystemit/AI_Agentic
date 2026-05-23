package com.darkness.videoplatform.security;

import com.darkness.videoplatform.entity.User;
import com.darkness.videoplatform.exception.UnauthorizedException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CurrentUserTest {

    private final CurrentUser currentUser = new CurrentUser();

    @AfterEach
    void clearContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void get_noAuthentication_returnsNull() {
        assertThat(currentUser.get()).isNull();
    }

    @Test
    void get_authenticatedUser_returnsUser() {
        User user = User.builder().id(1L).email("a@b.com").build();
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(user, null, Collections.emptyList()));

        assertThat(currentUser.get()).isEqualTo(user);
    }

    @Test
    void get_principalNotUser_returnsNull() {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("not-a-user", null, Collections.emptyList()));

        assertThat(currentUser.get()).isNull();
    }

    @Test
    void require_noAuthentication_throws() {
        assertThatThrownBy(() -> currentUser.require())
                .isInstanceOf(UnauthorizedException.class);
    }

    @Test
    void require_authenticated_returnsUser() {
        User user = User.builder().id(1L).email("a@b.com").build();
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(user, null, Collections.emptyList()));

        assertThat(currentUser.require()).isEqualTo(user);
    }
}
