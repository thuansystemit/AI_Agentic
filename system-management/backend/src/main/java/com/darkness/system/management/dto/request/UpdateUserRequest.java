package com.darkness.system.management.dto.request;

import com.darkness.system.management.domain.enums.GlobalRole;
import jakarta.validation.constraints.Size;

public record UpdateUserRequest(
        @Size(min = 2, max = 100) String fullName,
        GlobalRole globalRole,
        Boolean isActive
) {}
