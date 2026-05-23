package com.darkness.system.management.dto.response;

import com.darkness.system.management.domain.enums.Permission;

import java.time.Instant;
import java.util.UUID;

public record CategoryResponse(
        UUID id,
        String name,
        String description,
        Instant createdAt,
        Permission effectivePermission
) {}
