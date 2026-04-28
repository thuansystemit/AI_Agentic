package com.darkness.system.management.service;

import com.darkness.system.management.domain.enums.GlobalRole;
import com.darkness.system.management.domain.enums.Permission;
import com.darkness.system.management.repository.CategoryGroupPermissionRepository;
import com.darkness.system.management.repository.CategoryUserPermissionRepository;
import com.darkness.system.management.repository.GroupMemberRepository;
import com.darkness.system.management.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

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
        return resolvePermission(userId, categoryId).isAtLeast(required);
    }

    public Permission resolvePermission(UUID userId, UUID categoryId) {
        GlobalRole role = userRepository.findRoleById(userId);

        // Layer 1: ADMIN short-circuit
        if (role == GlobalRole.ADMIN) {
            return Permission.EDIT;
        }

        // Layer 2: role baseline
        Permission baseline = switch (role) {
            case EDITOR -> Permission.WRITE;
            case VIEWER -> Permission.READ;
            default -> Permission.READ;
        };

        // Layer 3: group permissions
        List<UUID> groupIds = groupMemberRepository.findGroupIdsByUserId(userId);
        Optional<Permission> groupMax = categoryGroupPermissionRepository
                .findPermissionsByCategoryIdAndGroupIds(categoryId, groupIds)
                .stream()
                .max(Comparator.comparingInt(Permission::getLevel));

        // Layer 4: direct user override
        Optional<Permission> directUser = categoryUserPermissionRepository
                .findPermissionByCategoryIdAndUserId(categoryId, userId);

        // Most permissive wins across all layers
        Permission effective = baseline;
        if (groupMax.isPresent() && groupMax.get().getLevel() > effective.getLevel()) {
            effective = groupMax.get();
        }
        if (directUser.isPresent() && directUser.get().getLevel() > effective.getLevel()) {
            effective = directUser.get();
        }
        return effective;
    }
}
