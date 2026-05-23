package com.darkness.system.management.repository;

import com.darkness.system.management.domain.CategoryUserPermission;
import com.darkness.system.management.domain.CategoryUserPermission.CategoryUserPermissionId;
import com.darkness.system.management.domain.enums.Permission;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CategoryUserPermissionRepository extends JpaRepository<CategoryUserPermission, CategoryUserPermissionId> {

    @Query("SELECT p.permission FROM CategoryUserPermission p WHERE p.id.categoryId = :categoryId AND p.id.userId = :userId")
    Optional<Permission> findPermissionByCategoryIdAndUserId(@Param("categoryId") UUID categoryId, @Param("userId") UUID userId);

    @Query("SELECT p FROM CategoryUserPermission p WHERE p.id.categoryId IN :categoryIds AND p.id.userId = :userId")
    List<CategoryUserPermission> findByCategoryIdsAndUserId(@Param("categoryIds") List<UUID> categoryIds, @Param("userId") UUID userId);

    @Query("SELECT p FROM CategoryUserPermission p WHERE p.id.categoryId = :categoryId")
    List<CategoryUserPermission> findAllByCategoryId(@Param("categoryId") UUID categoryId);

    void deleteByIdCategoryIdAndIdUserId(UUID categoryId, UUID userId);
}
