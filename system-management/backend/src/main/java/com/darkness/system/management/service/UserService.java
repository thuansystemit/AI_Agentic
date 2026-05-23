package com.darkness.system.management.service;

import com.darkness.system.management.domain.User;
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
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserService {

    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final UserMapper userMapper;

    @Transactional(readOnly = true)
    public PageResponse<UserResponse> listUsers(String search, Pageable pageable) {
        var page = (search == null || search.isBlank())
                ? userRepository.findAll(pageable).map(userMapper::toResponse)
                : userRepository.findByFullNameContainingIgnoreCaseOrEmailContainingIgnoreCase(
                        search, search, pageable).map(userMapper::toResponse);
        return PageResponse.from(page);
    }

    @Transactional(readOnly = true)
    public UserResponse getUser(UUID userId) {
        return userMapper.toResponse(findOrThrow(userId));
    }

    @Transactional
    public UserResponse createUser(CreateUserRequest request) {
        if (userRepository.existsByEmailIgnoreCase(request.email())) {
            log.warn("createUser: email already exists email={}", request.email());
            throw new EmailAlreadyExistsException(request.email());
        }
        User user = new User();
        user.setEmail(request.email().toLowerCase());
        user.setFullName(request.fullName());
        user.setPasswordHash(passwordEncoder.encode(request.password()));
        user.setGlobalRole(request.globalRole());
        user.setActive(true);
        UserResponse saved = userMapper.toResponse(userRepository.save(user));
        log.info("createUser: created userId={} email={} role={}", saved.id(), saved.email(), saved.globalRole());
        return saved;
    }

    @Transactional
    public UserResponse updateUser(UUID targetId, UUID callerId, UpdateUserRequest request) {
        if (targetId.equals(callerId)) {
            throw new CannotModifySelfException();
        }
        User user = findOrThrow(targetId);
        if (request.fullName() != null) user.setFullName(request.fullName());
        if (request.globalRole() != null) user.setGlobalRole(request.globalRole());
        if (request.isActive() != null) {
            user.setActive(request.isActive());
            if (!request.isActive()) {
                refreshTokenRepository.revokeAllByUserId(targetId);
                log.info("updateUser: revoked all tokens for deactivated userId={}", targetId);
            }
        }
        UserResponse saved = userMapper.toResponse(userRepository.save(user));
        log.info("updateUser: updated userId={}", targetId);
        return saved;
    }

    @Transactional
    public void resetPassword(UUID targetId, UUID callerId, ResetPasswordRequest request) {
        if (targetId.equals(callerId)) {
            throw new CannotModifySelfException();
        }
        User user = findOrThrow(targetId);
        user.setPasswordHash(passwordEncoder.encode(request.newPassword()));
        userRepository.save(user);
        refreshTokenRepository.revokeAllByUserId(targetId);
        log.info("resetPassword: password reset for userId={} by adminId={}", targetId, callerId);
    }

    @Transactional
    public void deleteUser(UUID targetId, UUID callerId) {
        if (targetId.equals(callerId)) {
            throw new CannotModifySelfException();
        }
        if (!userRepository.existsById(targetId)) {
            throw new ResourceNotFoundException("User not found: " + targetId);
        }
        refreshTokenRepository.revokeAllByUserId(targetId);
        userRepository.deleteById(targetId);
        log.info("deleteUser: deleted userId={}", targetId);
    }

    private User findOrThrow(UUID userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + userId));
    }
}
