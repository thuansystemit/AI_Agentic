package com.darkness.system.management.mapper;

import com.darkness.system.management.domain.Document;
import com.darkness.system.management.dto.response.DocumentResponse;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface DocumentMapper {
    DocumentResponse toResponse(Document document);
}
