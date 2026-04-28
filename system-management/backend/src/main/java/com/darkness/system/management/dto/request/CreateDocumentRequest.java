package com.darkness.system.management.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.UUID;

public record CreateDocumentRequest(
        @NotBlank @Size(min = 1, max = 255) String title,
        String content,
        @NotNull UUID categoryId
) {}
