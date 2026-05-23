package com.darkness.system.management.dto.response;

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
) {}
