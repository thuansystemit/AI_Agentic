package com.darkness.system.management.dto.response;

import com.darkness.system.management.domain.enums.GlobalRole;

import java.util.UUID;

public record ProfileResponse(
        UUID userId,
        String email,
        String fullName,
        String bio,
        GlobalRole globalRole
) {}
