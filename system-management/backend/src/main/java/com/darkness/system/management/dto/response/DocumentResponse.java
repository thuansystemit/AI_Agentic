package com.darkness.system.management.dto.response;

import com.darkness.system.management.domain.Document;

import java.time.Instant;
import java.util.UUID;

public record DocumentResponse(
        UUID id,
        String title,
        String content,
        UUID categoryId,
        UUID createdBy,
        Instant createdAt,
        Instant updatedAt
) {
    public static DocumentResponse from(Document doc) {
        return new DocumentResponse(
                doc.getId(),
                doc.getTitle(),
                doc.getContent(),
                doc.getCategoryId(),
                doc.getCreatedBy(),
                doc.getCreatedAt(),
                doc.getUpdatedAt()
        );
    }
}
