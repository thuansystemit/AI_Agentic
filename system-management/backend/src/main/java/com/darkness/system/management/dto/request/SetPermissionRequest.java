package com.darkness.system.management.dto.request;

import com.darkness.system.management.domain.enums.Permission;
import jakarta.validation.constraints.NotNull;

public record SetPermissionRequest(
        @NotNull Permission permission
) {}
