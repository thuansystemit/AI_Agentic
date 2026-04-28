package com.darkness.system.management.controller;

import com.darkness.system.management.domain.enums.GlobalRole;
import com.darkness.system.management.dto.request.CreateUserRequest;
import com.darkness.system.management.dto.request.UpdateUserRequest;
import com.darkness.system.management.dto.response.PageResponse;
import com.darkness.system.management.dto.response.UserResponse;
import com.darkness.system.management.repository.UserRepository;
import com.darkness.system.management.security.UserPrincipal;
import com.darkness.system.management.service.UserService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/users")
public class UserController {

    private static final Logger log = LoggerFactory.getLogger(UserController.class);

    private final UserService userService;
    private final UserRepository userRepository;

    public UserController(UserService userService, UserRepository userRepository) {
        this.userService = userService;
        this.userRepository = userRepository;
    }

    @GetMapping
    ResponseEntity<PageResponse<UserResponse>> listUsers(
            @RequestParam(required = false) String search,
            @AuthenticationPrincipal UserPrincipal principal,
            Pageable pageable) {
        requireAdmin(principal);
        return ResponseEntity.ok(userService.listUsers(search, pageable));
    }

    @GetMapping("/{userId}")
    ResponseEntity<UserResponse> getUser(@PathVariable UUID userId,
                                         @AuthenticationPrincipal UserPrincipal principal) {
        requireAdmin(principal);
        return ResponseEntity.ok(userService.getUser(userId));
    }

    @PostMapping
    ResponseEntity<UserResponse> createUser(@Valid @RequestBody CreateUserRequest request,
                                            @AuthenticationPrincipal UserPrincipal principal) {
        requireAdmin(principal);
        return ResponseEntity.status(HttpStatus.CREATED).body(userService.createUser(request));
    }

    @PatchMapping("/{userId}")
    ResponseEntity<UserResponse> updateUser(@PathVariable UUID userId,
                                            @Valid @RequestBody UpdateUserRequest request,
                                            @AuthenticationPrincipal UserPrincipal principal) {
        requireAdmin(principal);
        return ResponseEntity.ok(userService.updateUser(userId, principal.userId(), request));
    }

    @DeleteMapping("/{userId}")
    ResponseEntity<Void> deleteUser(@PathVariable UUID userId,
                                    @AuthenticationPrincipal UserPrincipal principal) {
        requireAdmin(principal);
        userService.deleteUser(userId, principal.userId());
        return ResponseEntity.noContent().build();
    }

    private void requireAdmin(UserPrincipal principal) {
        GlobalRole role = userRepository.findRoleById(principal.userId());
        if (role == null) {
            log.warn("requireAdmin: no role found for userId={} — user may have been deleted", principal.userId());
            throw new com.darkness.system.management.exception.AccessDeniedException("Admin access required");
        }
        if (role != GlobalRole.ADMIN) {
            log.warn("requireAdmin: access denied userId={} email={} role={}", principal.userId(), principal.email(), role);
            throw new com.darkness.system.management.exception.AccessDeniedException("Admin access required");
        }
        log.debug("requireAdmin: granted userId={} email={}", principal.userId(), principal.email());
    }
}
