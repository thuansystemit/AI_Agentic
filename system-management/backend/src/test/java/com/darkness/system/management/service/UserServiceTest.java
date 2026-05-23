package com.darkness.system.management.service;

import com.darkness.system.management.domain.User;
import com.darkness.system.management.domain.enums.GlobalRole;
import com.darkness.system.management.dto.request.CreateUserRequest;
import com.darkness.system.management.dto.request.ResetPasswordRequest;
import com.darkness.system.management.dto.request.UpdateUserRequest;
import com.darkness.system.management.dto.response.PageResponse;
import com.darkness.system.management.dto.response.UserResponse;
import com.darkness.system.management.exception.CannotModifySelfException;
import com.darkness.system.management.exception.EmailAlreadyExistsException;
import com.darkness.system.management.exception.ResourceNotFoundException;
import com.darkness.system.management.mapper.UserMapper;
import com.darkness.system.management.repository.RefreshTokenRepository;
import com.darkness.system.management.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.*;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock UserRepository userRepository;
    @Mock RefreshTokenRepository refreshTokenRepository;
    @Mock PasswordEncoder passwordEncoder;
    @Mock UserMapper userMapper;

    @InjectMocks UserService userService;

    UUID userId;
    UUID callerId;
    User user;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        callerId = UUID.randomUUID();
        user = new User();
        user.setId(userId);
        user.setEmail("user@test.com");
        user.setFullName("Test User");
        user.setGlobalRole(GlobalRole.VIEWER);
        user.setActive(true);
        lenient().when(userMapper.toResponse(any(User.class))).thenAnswer(inv -> {
            User u = inv.getArgument(0);
            return new UserResponse(u.getId(), u.getEmail(), u.getFullName(),
                    u.getGlobalRole(), u.isActive(), u.isLocked(), u.getCreatedAt());
        });
    }

    @Test
    void listUsers_noSearch_returnsAllUsers() {
        Page<User> page = new PageImpl<>(List.of(user));
        when(userRepository.findAll(any(Pageable.class))).thenReturn(page);

        PageResponse<UserResponse> result = userService.listUsers(null, PageRequest.of(0, 10));

        assertThat(result.content()).hasSize(1);
        assertThat(result.totalElements()).isEqualTo(1);
    }

    @Test
    void listUsers_emptySearch_returnsAllUsers() {
        Page<User> page = new PageImpl<>(List.of(user));
        when(userRepository.findAll(any(Pageable.class))).thenReturn(page);

        PageResponse<UserResponse> result = userService.listUsers("  ", PageRequest.of(0, 10));

        assertThat(result.content()).hasSize(1);
    }

    @Test
    void listUsers_withSearch_callsSearchQuery() {
        Page<User> page = new PageImpl<>(List.of(user));
        when(userRepository.findByFullNameContainingIgnoreCaseOrEmailContainingIgnoreCase(
                eq("test"), eq("test"), any(Pageable.class))).thenReturn(page);

        PageResponse<UserResponse> result = userService.listUsers("test", PageRequest.of(0, 10));

        assertThat(result.content()).hasSize(1);
        verify(userRepository).findByFullNameContainingIgnoreCaseOrEmailContainingIgnoreCase(
                eq("test"), eq("test"), any(Pageable.class));
    }

    @Test
    void getUser_found_returnsUserResponse() {
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));

        UserResponse result = userService.getUser(userId);

        assertThat(result.id()).isEqualTo(userId);
        assertThat(result.email()).isEqualTo("user@test.com");
    }

    @Test
    void getUser_notFound_throwsResourceNotFoundException() {
        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.getUser(userId))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void createUser_success_returnsCreatedUser() {
        CreateUserRequest req = new CreateUserRequest("new@test.com", "New User", "Password1!", GlobalRole.EDITOR);
        when(userRepository.existsByEmailIgnoreCase("new@test.com")).thenReturn(false);
        when(passwordEncoder.encode("Password1!")).thenReturn("$2a$hashed");
        when(userRepository.save(any(User.class))).thenAnswer(inv -> {
            User saved = inv.getArgument(0);
            saved.setId(UUID.randomUUID());
            return saved;
        });

        UserResponse result = userService.createUser(req);

        assertThat(result.email()).isEqualTo("new@test.com");
        assertThat(result.globalRole()).isEqualTo(GlobalRole.EDITOR);
    }

    @Test
    void createUser_emailExists_throwsEmailAlreadyExistsException() {
        CreateUserRequest req = new CreateUserRequest("exists@test.com", "Name", "Pass1234!", GlobalRole.VIEWER);
        when(userRepository.existsByEmailIgnoreCase("exists@test.com")).thenReturn(true);

        assertThatThrownBy(() -> userService.createUser(req))
                .isInstanceOf(EmailAlreadyExistsException.class);
        verify(userRepository, never()).save(any());
    }

    @Test
    void updateUser_selfModify_throwsCannotModifySelfException() {
        UpdateUserRequest req = new UpdateUserRequest("New Name", null, null);

        assertThatThrownBy(() -> userService.updateUser(userId, userId, req))
                .isInstanceOf(CannotModifySelfException.class);
    }

    @Test
    void updateUser_success_updatesFullName() {
        UpdateUserRequest req = new UpdateUserRequest("Updated Name", null, null);
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(userRepository.save(any(User.class))).thenReturn(user);

        userService.updateUser(userId, callerId, req);

        assertThat(user.getFullName()).isEqualTo("Updated Name");
    }

    @Test
    void updateUser_changeRole_updatesGlobalRole() {
        UpdateUserRequest req = new UpdateUserRequest(null, GlobalRole.EDITOR, null);
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(userRepository.save(any(User.class))).thenReturn(user);

        userService.updateUser(userId, callerId, req);

        assertThat(user.getGlobalRole()).isEqualTo(GlobalRole.EDITOR);
    }

    @Test
    void updateUser_deactivate_revokesRefreshTokens() {
        UpdateUserRequest req = new UpdateUserRequest(null, null, false);
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(userRepository.save(any(User.class))).thenReturn(user);

        userService.updateUser(userId, callerId, req);

        assertThat(user.isActive()).isFalse();
        verify(refreshTokenRepository).revokeAllByUserId(userId);
    }

    @Test
    void updateUser_activate_doesNotRevokeTokens() {
        UpdateUserRequest req = new UpdateUserRequest(null, null, true);
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(userRepository.save(any(User.class))).thenReturn(user);

        userService.updateUser(userId, callerId, req);

        assertThat(user.isActive()).isTrue();
        verify(refreshTokenRepository, never()).revokeAllByUserId(any());
    }

    @Test
    void resetPassword_selfModify_throwsCannotModifySelfException() {
        assertThatThrownBy(() -> userService.resetPassword(userId, userId, new ResetPasswordRequest("NewPass1!")))
                .isInstanceOf(CannotModifySelfException.class);
    }

    @Test
    void resetPassword_userNotFound_throwsResourceNotFoundException() {
        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.resetPassword(userId, callerId, new ResetPasswordRequest("NewPass1!")))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void resetPassword_success_encodesPasswordAndRevokesTokens() {
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(passwordEncoder.encode("NewPass1!")).thenReturn("$2a$hashed_new");
        when(userRepository.save(any(User.class))).thenReturn(user);

        userService.resetPassword(userId, callerId, new ResetPasswordRequest("NewPass1!"));

        assertThat(user.getPasswordHash()).isEqualTo("$2a$hashed_new");
        verify(refreshTokenRepository).revokeAllByUserId(userId);
    }

    @Test
    void deleteUser_selfModify_throwsCannotModifySelfException() {
        assertThatThrownBy(() -> userService.deleteUser(userId, userId))
                .isInstanceOf(CannotModifySelfException.class);
    }

    @Test
    void deleteUser_notFound_throwsResourceNotFoundException() {
        when(userRepository.existsById(userId)).thenReturn(false);

        assertThatThrownBy(() -> userService.deleteUser(userId, callerId))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void deleteUser_success_revokesTokensAndDeletes() {
        when(userRepository.existsById(userId)).thenReturn(true);

        userService.deleteUser(userId, callerId);

        verify(refreshTokenRepository).revokeAllByUserId(userId);
        verify(userRepository).deleteById(userId);
    }
}
