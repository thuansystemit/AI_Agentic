package com.darkness.system.management.service;

import com.darkness.system.management.domain.enums.GlobalRole;
import com.darkness.system.management.domain.enums.Permission;
import com.darkness.system.management.repository.CategoryGroupPermissionRepository;
import com.darkness.system.management.repository.CategoryUserPermissionRepository;
import com.darkness.system.management.repository.GroupMemberRepository;
import com.darkness.system.management.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PermissionServiceTest {

    @Mock UserRepository userRepository;
    @Mock GroupMemberRepository groupMemberRepository;
    @Mock CategoryUserPermissionRepository categoryUserPermissionRepository;
    @Mock CategoryGroupPermissionRepository categoryGroupPermissionRepository;

    @InjectMocks PermissionService permissionService;

    UUID userId;
    UUID categoryId;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        categoryId = UUID.randomUUID();
    }

    // ADMIN short-circuit
    @Test
    void admin_hasAnyPermission() {
        when(userRepository.findRoleById(userId)).thenReturn(GlobalRole.ADMIN);

        assertThat(permissionService.hasPermission(userId, categoryId, Permission.EDIT)).isTrue();
        assertThat(permissionService.hasPermission(userId, categoryId, Permission.WRITE)).isTrue();
        assertThat(permissionService.hasPermission(userId, categoryId, Permission.READ)).isTrue();
    }

    // Role baseline
    @Test
    void editor_hasWriteByDefault_noExplicitPermission() {
        when(userRepository.findRoleById(userId)).thenReturn(GlobalRole.EDITOR);
        when(groupMemberRepository.findGroupIdsByUserId(userId)).thenReturn(List.of());
        when(categoryUserPermissionRepository.findPermissionByCategoryIdAndUserId(categoryId, userId))
                .thenReturn(Optional.empty());
        when(categoryGroupPermissionRepository.findPermissionsByCategoryIdAndGroupIds(categoryId, List.of()))
                .thenReturn(List.of());

        assertThat(permissionService.hasPermission(userId, categoryId, Permission.WRITE)).isTrue();
        assertThat(permissionService.hasPermission(userId, categoryId, Permission.EDIT)).isFalse();
    }

    @Test
    void viewer_hasReadByDefault_noExplicitPermission() {
        when(userRepository.findRoleById(userId)).thenReturn(GlobalRole.VIEWER);
        when(groupMemberRepository.findGroupIdsByUserId(userId)).thenReturn(List.of());
        when(categoryUserPermissionRepository.findPermissionByCategoryIdAndUserId(categoryId, userId))
                .thenReturn(Optional.empty());
        when(categoryGroupPermissionRepository.findPermissionsByCategoryIdAndGroupIds(categoryId, List.of()))
                .thenReturn(List.of());

        // VIEWER with no explicit permission has no access (hidden-category requirement)
        assertThat(permissionService.hasPermission(userId, categoryId, Permission.READ)).isFalse();
        assertThat(permissionService.hasPermission(userId, categoryId, Permission.WRITE)).isFalse();
    }

    // Direct user override wins over role baseline
    @Test
    void viewer_withDirectEditPermission_canEdit() {
        when(userRepository.findRoleById(userId)).thenReturn(GlobalRole.VIEWER);
        when(groupMemberRepository.findGroupIdsByUserId(userId)).thenReturn(List.of());
        when(categoryUserPermissionRepository.findPermissionByCategoryIdAndUserId(categoryId, userId))
                .thenReturn(Optional.of(Permission.EDIT));
        when(categoryGroupPermissionRepository.findPermissionsByCategoryIdAndGroupIds(categoryId, List.of()))
                .thenReturn(List.of());

        assertThat(permissionService.hasPermission(userId, categoryId, Permission.EDIT)).isTrue();
    }

    // Group permission elevates access
    @Test
    void viewer_inGroupWithWrite_canWrite() {
        UUID groupId = UUID.randomUUID();
        when(userRepository.findRoleById(userId)).thenReturn(GlobalRole.VIEWER);
        when(groupMemberRepository.findGroupIdsByUserId(userId)).thenReturn(List.of(groupId));
        when(categoryUserPermissionRepository.findPermissionByCategoryIdAndUserId(categoryId, userId))
                .thenReturn(Optional.empty());
        when(categoryGroupPermissionRepository.findPermissionsByCategoryIdAndGroupIds(categoryId, List.of(groupId)))
                .thenReturn(List.of(Permission.WRITE));

        assertThat(permissionService.hasPermission(userId, categoryId, Permission.WRITE)).isTrue();
        assertThat(permissionService.hasPermission(userId, categoryId, Permission.EDIT)).isFalse();
    }

    // Most permissive wins — group gives EDIT, user direct gives READ
    @Test
    void mostPermissiveWins_groupEditOverridesDirectRead() {
        UUID groupId = UUID.randomUUID();
        when(userRepository.findRoleById(userId)).thenReturn(GlobalRole.VIEWER);
        when(groupMemberRepository.findGroupIdsByUserId(userId)).thenReturn(List.of(groupId));
        when(categoryUserPermissionRepository.findPermissionByCategoryIdAndUserId(categoryId, userId))
                .thenReturn(Optional.of(Permission.READ));
        when(categoryGroupPermissionRepository.findPermissionsByCategoryIdAndGroupIds(categoryId, List.of(groupId)))
                .thenReturn(List.of(Permission.EDIT));

        assertThat(permissionService.hasPermission(userId, categoryId, Permission.EDIT)).isTrue();
    }

    // resolvePermission returns effective permission level
    @Test
    void resolvePermission_returnsHighestOfAllLayers() {
        UUID groupId = UUID.randomUUID();
        when(userRepository.findRoleById(userId)).thenReturn(GlobalRole.VIEWER);
        when(groupMemberRepository.findGroupIdsByUserId(userId)).thenReturn(List.of(groupId));
        when(categoryUserPermissionRepository.findPermissionByCategoryIdAndUserId(categoryId, userId))
                .thenReturn(Optional.of(Permission.READ));
        when(categoryGroupPermissionRepository.findPermissionsByCategoryIdAndGroupIds(categoryId, List.of(groupId)))
                .thenReturn(List.of(Permission.WRITE));

        assertThat(permissionService.resolve(userId, categoryId)).contains(Permission.WRITE);
    }

    // EC-02: multiple groups — max wins
    @Test
    void multipleGroups_maxPermissionWins() {
        UUID g1 = UUID.randomUUID();
        UUID g2 = UUID.randomUUID();
        when(userRepository.findRoleById(userId)).thenReturn(GlobalRole.VIEWER);
        when(groupMemberRepository.findGroupIdsByUserId(userId)).thenReturn(List.of(g1, g2));
        when(categoryUserPermissionRepository.findPermissionByCategoryIdAndUserId(categoryId, userId))
                .thenReturn(Optional.empty());
        when(categoryGroupPermissionRepository.findPermissionsByCategoryIdAndGroupIds(categoryId, List.of(g1, g2)))
                .thenReturn(List.of(Permission.READ, Permission.EDIT));

        assertThat(permissionService.resolve(userId, categoryId)).contains(Permission.EDIT);
    }
}
