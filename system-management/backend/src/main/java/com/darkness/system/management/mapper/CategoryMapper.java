package com.darkness.system.management.mapper;

import com.darkness.system.management.domain.Category;
import com.darkness.system.management.domain.enums.Permission;
import com.darkness.system.management.dto.response.CategoryResponse;
import org.springframework.stereotype.Component;

@Component
public class CategoryMapper {

    public CategoryResponse toResponse(Category category, Permission effectivePermission) {
        return new CategoryResponse(
                category.getId(),
                category.getName(),
                category.getDescription(),
                category.getCreatedAt(),
                effectivePermission
        );
    }
}
