package com.darkness.system.management.dto.response;

import com.darkness.system.management.domain.User;
import com.darkness.system.management.domain.enums.GlobalRole;

import java.time.Instant;
import java.util.UUID;

public record UserResponse(
        UUID id,
        String email,
        String fullName,
        GlobalRole globalRole,
        boolean isActive,
        boolean isLocked,
        Instant createdAt
) {
    public static UserResponse from(User user) {
        return new UserResponse(
                user.getId(),
                user.getEmail(),
                user.getFullName(),
                user.getGlobalRole(),
                user.isActive(),
                user.isLocked(),
                user.getCreatedAt()
        );
    }
}
