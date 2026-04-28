package com.darkness.system.management.repository;

import com.darkness.system.management.domain.CategoryUserPermission;
import com.darkness.system.management.domain.CategoryUserPermission.CategoryUserPermissionId;
import com.darkness.system.management.domain.enums.Permission;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;
import java.util.UUID;

public interface CategoryUserPermissionRepository extends JpaRepository<CategoryUserPermission, CategoryUserPermissionId> {

    @Query("SELECT p.permission FROM CategoryUserPermission p WHERE p.id.categoryId = :categoryId AND p.id.userId = :userId")
    Optional<Permission> findPermissionByCategoryIdAndUserId(UUID categoryId, UUID userId);

    void deleteByIdCategoryIdAndIdUserId(UUID categoryId, UUID userId);
}
