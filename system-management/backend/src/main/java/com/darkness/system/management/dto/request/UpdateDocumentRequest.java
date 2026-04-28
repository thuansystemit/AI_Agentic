package com.darkness.system.management.dto.request;

import jakarta.validation.constraints.Size;

public record UpdateDocumentRequest(
        @Size(min = 1, max = 255) String title,
        String content
) {}
