package com.darkness.system.management.repository;

import com.darkness.system.management.domain.CategoryGroupPermission;
import com.darkness.system.management.domain.CategoryGroupPermission.CategoryGroupPermissionId;
import com.darkness.system.management.domain.enums.Permission;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.UUID;

public interface CategoryGroupPermissionRepository extends JpaRepository<CategoryGroupPermission, CategoryGroupPermissionId> {

    @Query("SELECT p.permission FROM CategoryGroupPermission p WHERE p.id.categoryId = :categoryId AND p.id.groupId IN :groupIds")
    List<Permission> findPermissionsByCategoryIdAndGroupIds(UUID categoryId, List<UUID> groupIds);

    void deleteByIdCategoryIdAndIdGroupId(UUID categoryId, UUID groupId);
}
