package com.darkness.system.management.service;

import com.darkness.system.management.domain.enums.GlobalRole;
import com.darkness.system.management.domain.enums.Permission;
import com.darkness.system.management.repository.CategoryGroupPermissionRepository;
import com.darkness.system.management.repository.CategoryUserPermissionRepository;
import com.darkness.system.management.repository.GroupMemberRepository;
import com.darkness.system.management.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Service
@Transactional(readOnly = true)
public class PermissionService {

    private final UserRepository userRepository;
    private final GroupMemberRepository groupMemberRepository;
    private final CategoryUserPermissionRepository categoryUserPermissionRepository;
    private final CategoryGroupPermissionRepository categoryGroupPermissionRepository;

    public PermissionService(UserRepository userRepository,
                             GroupMemberRepository groupMemberRepository,
                             CategoryUserPermissionRepository categoryUserPermissionRepository,
                             CategoryGroupPermissionRepository categoryGroupPermissionRepository) {
        this.userRepository = userRepository;
        this.groupMemberRepository = groupMemberRepository;
        this.categoryUserPermissionRepository = categoryUserPermissionRepository;
        this.categoryGroupPermissionRepository = categoryGroupPermissionRepository;
    }

    public boolean hasPermission(UUID userId, UUID categoryId, Permission required) {
        return resolve(userId, categoryId).map(p -> p.isAtLeast(required)).orElse(false);
    }

    /**
     * Resolves the effective permission for a user on a category.
     * Returns empty if the user has no access at all (VIEWER with no explicit grants).
     *
     * Resolution order (most permissive wins):
     *   ADMIN → always EDIT
     *   EDITOR → WRITE baseline, elevated by group/direct if higher
     *   VIEWER → no baseline; requires explicit group or direct grant
     */
    public Optional<Permission> resolve(UUID userId, UUID categoryId) {
        GlobalRole role = userRepository.findRoleById(userId);
        if (role == GlobalRole.ADMIN) return Optional.of(Permission.EDIT);

        List<UUID> groupIds = groupMemberRepository.findGroupIdsByUserId(userId);

        Optional<Permission> groupMax = categoryGroupPermissionRepository
                .findPermissionsByCategoryIdAndGroupIds(categoryId, groupIds)
                .stream()
                .max(Comparator.comparingInt(Permission::getLevel));

        Optional<Permission> direct = categoryUserPermissionRepository
                .findPermissionByCategoryIdAndUserId(categoryId, userId);

        if (role == GlobalRole.VIEWER && groupMax.isEmpty() && direct.isEmpty()) {
            return Optional.empty();
        }

        Permission effective = (role == GlobalRole.EDITOR) ? Permission.WRITE : Permission.READ;
        if (groupMax.isPresent() && groupMax.get().getLevel() > effective.getLevel()) effective = groupMax.get();
        if (direct.isPresent() && direct.get().getLevel() > effective.getLevel()) effective = direct.get();
        return Optional.of(effective);
    }

    /**
     * Batch-resolves permissions for many categories in 4 queries regardless of N.
     * Returns a map of categoryId → effective Permission (absent means no access).
     */
    public Map<UUID, Permission> resolveBatch(UUID userId, List<UUID> categoryIds) {
        if (categoryIds.isEmpty()) return Map.of();

        GlobalRole role = userRepository.findRoleById(userId);
        if (role == GlobalRole.ADMIN) {
            Map<UUID, Permission> result = new LinkedHashMap<>();
            categoryIds.forEach(id -> result.put(id, Permission.EDIT));
            return result;
        }

        List<UUID> groupIds = groupMemberRepository.findGroupIdsByUserId(userId);

        // group permissions: map categoryId → max group permission
        Map<UUID, Permission> groupMaxMap = new HashMap<>();
        if (!groupIds.isEmpty()) {
            categoryGroupPermissionRepository
                    .findByCategoryIdsAndGroupIds(categoryIds, groupIds)
                    .forEach(p -> groupMaxMap.merge(p.getId().getCategoryId(), p.getPermission(),
                            (a, b) -> a.getLevel() >= b.getLevel() ? a : b));
        }

        // direct user permissions: map categoryId → permission
        Map<UUID, Permission> directMap = new HashMap<>();
        categoryUserPermissionRepository
                .findByCategoryIdsAndUserId(categoryIds, userId)
                .forEach(p -> directMap.put(p.getId().getCategoryId(), p.getPermission()));

        Map<UUID, Permission> result = new LinkedHashMap<>();
        for (UUID catId : categoryIds) {
            Optional<Permission> groupMax = Optional.ofNullable(groupMaxMap.get(catId));
            Optional<Permission> direct   = Optional.ofNullable(directMap.get(catId));

            if (role == GlobalRole.VIEWER && groupMax.isEmpty() && direct.isEmpty()) {
                continue; // no access
            }

            Permission effective = (role == GlobalRole.EDITOR) ? Permission.WRITE : Permission.READ;
            if (groupMax.isPresent() && groupMax.get().getLevel() > effective.getLevel()) effective = groupMax.get();
            if (direct.isPresent() && direct.get().getLevel() > effective.getLevel()) effective = direct.get();
            result.put(catId, effective);
        }
        return result;
    }
}
