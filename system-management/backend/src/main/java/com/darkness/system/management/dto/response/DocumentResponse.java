package com.darkness.system.management.dto.response;

import java.time.Instant;
import java.util.UUID;

public record DocumentResponse(
        UUID id,
        String title,
        String content,
        UUID categoryId,
        UUID createdBy,
        Instant createdAt,
        Instant updatedAt
) {}
