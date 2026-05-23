package com.darkness.system.management.dto.response;

import java.time.Instant;
import java.util.UUID;

public record GroupResponse(
        UUID id,
        String name,
        String description,
        Instant createdAt
) {}
