package com.darkness.system.management.security;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class UserPrincipalTest {

    @Test
    void getUsername_returnsEmail() {
        UserPrincipal principal = new UserPrincipal(UUID.randomUUID(), "user@test.com");
        assertThat(principal.getUsername()).isEqualTo("user@test.com");
    }

    @Test
    void getPassword_returnsNull() {
        UserPrincipal principal = new UserPrincipal(UUID.randomUUID(), "user@test.com");
        assertThat(principal.getPassword()).isNull();
    }

    @Test
    void getAuthorities_containsRoleUser() {
        UserPrincipal principal = new UserPrincipal(UUID.randomUUID(), "user@test.com");
        assertThat(principal.getAuthorities())
                .extracting(a -> a.getAuthority())
                .containsExactly("ROLE_USER");
    }

    @Test
    void isAccountNonExpired_returnsTrue() {
        UserPrincipal principal = new UserPrincipal(UUID.randomUUID(), "user@test.com");
        assertThat(principal.isAccountNonExpired()).isTrue();
    }

    @Test
    void isAccountNonLocked_returnsTrue() {
        UserPrincipal principal = new UserPrincipal(UUID.randomUUID(), "user@test.com");
        assertThat(principal.isAccountNonLocked()).isTrue();
    }

    @Test
    void isCredentialsNonExpired_returnsTrue() {
        UserPrincipal principal = new UserPrincipal(UUID.randomUUID(), "user@test.com");
        assertThat(principal.isCredentialsNonExpired()).isTrue();
    }

    @Test
    void isEnabled_returnsTrue() {
        UserPrincipal principal = new UserPrincipal(UUID.randomUUID(), "user@test.com");
        assertThat(principal.isEnabled()).isTrue();
    }

    @Test
    void userId_returnsCorrectId() {
        UUID id = UUID.randomUUID();
        UserPrincipal principal = new UserPrincipal(id, "user@test.com");
        assertThat(principal.userId()).isEqualTo(id);
    }
}
