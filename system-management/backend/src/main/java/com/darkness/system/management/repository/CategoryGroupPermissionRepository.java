package com.darkness.system.management.repository;

import com.darkness.system.management.domain.CategoryGroupPermission;
import com.darkness.system.management.domain.CategoryGroupPermission.CategoryGroupPermissionId;
import com.darkness.system.management.domain.enums.Permission;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface CategoryGroupPermissionRepository extends JpaRepository<CategoryGroupPermission, CategoryGroupPermissionId> {

    @Query("SELECT p.permission FROM CategoryGroupPermission p WHERE p.id.categoryId = :categoryId AND p.id.groupId IN :groupIds")
    List<Permission> findPermissionsByCategoryIdAndGroupIds(@Param("categoryId") UUID categoryId, @Param("groupIds") List<UUID> groupIds);

    @Query("SELECT p FROM CategoryGroupPermission p WHERE p.id.categoryId IN :categoryIds AND p.id.groupId IN :groupIds")
    List<CategoryGroupPermission> findByCategoryIdsAndGroupIds(@Param("categoryIds") List<UUID> categoryIds, @Param("groupIds") List<UUID> groupIds);

    @Query("SELECT p FROM CategoryGroupPermission p WHERE p.id.categoryId = :categoryId")
    List<CategoryGroupPermission> findAllByCategoryId(@Param("categoryId") UUID categoryId);

    void deleteByIdCategoryIdAndIdGroupId(UUID categoryId, UUID groupId);
}
