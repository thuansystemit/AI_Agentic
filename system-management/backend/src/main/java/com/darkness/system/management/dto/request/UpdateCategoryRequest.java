package com.darkness.system.management.dto.request;

import jakarta.validation.constraints.Size;

public record UpdateCategoryRequest(
        @Size(min = 2, max = 100) String name,
        @Size(max = 500) String description
) {}
