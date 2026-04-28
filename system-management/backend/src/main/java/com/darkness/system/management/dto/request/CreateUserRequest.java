package com.darkness.system.management.dto.request;

import com.darkness.system.management.domain.enums.GlobalRole;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreateUserRequest(
        @NotBlank @Email String email,
        @NotBlank @Size(min = 2, max = 100) String fullName,
        @NotBlank @Size(min = 8, max = 128) String password,
        @NotNull GlobalRole globalRole
) {}
