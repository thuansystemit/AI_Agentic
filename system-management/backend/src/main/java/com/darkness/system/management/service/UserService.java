package com.darkness.system.management.service;

import com.darkness.system.management.domain.User;
import com.darkness.system.management.domain.enums.GlobalRole;
import com.darkness.system.management.dto.request.CreateUserRequest;
import com.darkness.system.management.dto.request.UpdateUserRequest;
import com.darkness.system.management.dto.response.PageResponse;
import com.darkness.system.management.dto.response.UserResponse;
import com.darkness.system.management.exception.CannotModifySelfException;
import com.darkness.system.management.exception.EmailAlreadyExistsException;
import com.darkness.system.management.exception.ResourceNotFoundException;
import com.darkness.system.management.repository.RefreshTokenRepository;
import com.darkness.system.management.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class UserService {

    private static final Logger log = LoggerFactory.getLogger(UserService.class);

    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;

    public UserService(UserRepository userRepository,
                       RefreshTokenRepository refreshTokenRepository,
                       PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.refreshTokenRepository = refreshTokenRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional(readOnly = true)
    public PageResponse<UserResponse> listUsers(String search, Pageable pageable) {
        var page = (search == null || search.isBlank())
                ? userRepository.findAll(pageable).map(UserResponse::from)
                : userRepository.findByFullNameContainingIgnoreCaseOrEmailContainingIgnoreCase(
                        search, search, pageable).map(UserResponse::from);
        return PageResponse.from(page);
    }

    @Transactional(readOnly = true)
    public UserResponse getUser(UUID userId) {
        return UserResponse.from(findOrThrow(userId));
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
        UserResponse saved = UserResponse.from(userRepository.save(user));
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
        UserResponse saved = UserResponse.from(userRepository.save(user));
        log.info("updateUser: updated userId={}", targetId);
        return saved;
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
