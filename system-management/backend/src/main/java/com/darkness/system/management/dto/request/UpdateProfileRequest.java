package com.darkness.system.management.dto.request;

import jakarta.validation.constraints.Size;

public record UpdateProfileRequest(
        @Size(min = 2, max = 100, message = "Full name must be between 2 and 100 characters")
        String fullName,

        @Size(max = 500, message = "Bio must be 500 characters or fewer")
        String bio
) {}
