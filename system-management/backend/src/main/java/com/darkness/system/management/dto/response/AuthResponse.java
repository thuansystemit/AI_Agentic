package com.darkness.system.management.dto.response;

import com.darkness.system.management.domain.enums.GlobalRole;
import com.fasterxml.jackson.annotation.JsonIgnore;

import java.util.UUID;

public record AuthResponse(
        UUID userId,
        String email,
        String fullName,
        GlobalRole globalRole,
        @JsonIgnore String accessToken   // delivered via HttpOnly cookie — never in the JSON body
) {}
