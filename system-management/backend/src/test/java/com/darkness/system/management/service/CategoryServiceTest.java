package com.darkness.system.management.service;

import com.darkness.system.management.domain.Category;
import com.darkness.system.management.domain.enums.GlobalRole;
import com.darkness.system.management.domain.enums.Permission;
import com.darkness.system.management.dto.request.CreateCategoryRequest;
import com.darkness.system.management.dto.request.SetPermissionRequest;
import com.darkness.system.management.dto.request.UpdateCategoryRequest;
import com.darkness.system.management.dto.response.CategoryResponse;
import com.darkness.system.management.dto.response.PageResponse;
import com.darkness.system.management.exception.AccessDeniedException;
import com.darkness.system.management.exception.DuplicateNameException;
import com.darkness.system.management.exception.ResourceNotFoundException;
import com.darkness.system.management.mapper.CategoryMapper;
import com.darkness.system.management.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.*;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CategoryServiceTest {

    @Mock CategoryRepository categoryRepository;
    @Mock CategoryUserPermissionRepository categoryUserPermissionRepository;
    @Mock CategoryGroupPermissionRepository categoryGroupPermissionRepository;
    @Mock UserRepository userRepository;
    @Mock GroupRepository groupRepository;
    @Mock GroupMemberRepository groupMemberRepository;
    @Mock CategoryMapper categoryMapper;
    @Mock PermissionService permissionService;

    @InjectMocks CategoryService categoryService;

    UUID categoryId;
    UUID userId;
    UUID groupId;
    Category category;

    @BeforeEach
    void setUp() {
        categoryId = UUID.randomUUID();
        userId = UUID.randomUUID();
        groupId = UUID.randomUUID();
        category = new Category();
        category.setId(categoryId);
        category.setName("Engineering");
        category.setDescription("Engineering docs");
        lenient().when(categoryMapper.toResponse(any(Category.class), any(Permission.class))).thenAnswer(inv -> {
            Category c = inv.getArgument(0);
            Permission p = inv.getArgument(1);
            return new CategoryResponse(c.getId(), c.getName(), c.getDescription(), c.getCreatedAt(), p);
        });
    }

    @Test
    void listCategories_returnsPaginatedCategories() {
        Page<Category> page = new PageImpl<>(List.of(category));
        when(userRepository.findRoleById(userId)).thenReturn(GlobalRole.ADMIN);
        when(categoryRepository.findAll(any(Pageable.class))).thenReturn(page);
        when(permissionService.resolveBatch(eq(userId), anyList())).thenReturn(Map.of(categoryId, Permission.EDIT));

        PageResponse<CategoryResponse> result = categoryService.listCategories(userId, PageRequest.of(0, 10));

        assertThat(result.content()).hasSize(1);
        assertThat(result.content().get(0).name()).isEqualTo("Engineering");
    }

    @Test
    void listCategories_viewer_usesAccessibleQuery() {
        Page<Category> page = new PageImpl<>(List.of(category));
        when(userRepository.findRoleById(userId)).thenReturn(GlobalRole.VIEWER);
        when(groupMemberRepository.findGroupIdsByUserId(userId)).thenReturn(List.of());
        when(categoryRepository.findAccessibleByViewer(eq(userId), anyList(), any(Pageable.class))).thenReturn(page);
        when(permissionService.resolveBatch(eq(userId), anyList())).thenReturn(Map.of(categoryId, Permission.READ));

        PageResponse<CategoryResponse> result = categoryService.listCategories(userId, PageRequest.of(0, 10));

        assertThat(result.content()).hasSize(1);
        verify(categoryRepository).findAccessibleByViewer(eq(userId), anyList(), any(Pageable.class));
    }

    @Test
    void getCategory_found_returnsCategoryResponse() {
        when(categoryRepository.findById(categoryId)).thenReturn(Optional.of(category));
        when(permissionService.resolve(userId, categoryId)).thenReturn(Optional.of(Permission.READ));

        CategoryResponse result = categoryService.getCategory(categoryId, userId);

        assertThat(result.id()).isEqualTo(categoryId);
    }

    @Test
    void getCategory_notFound_throws() {
        when(categoryRepository.findById(categoryId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> categoryService.getCategory(categoryId, userId))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void getCategory_noPermission_throws() {
        when(categoryRepository.findById(categoryId)).thenReturn(Optional.of(category));
        when(permissionService.resolve(userId, categoryId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> categoryService.getCategory(categoryId, userId))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void createCategory_success() {
        CreateCategoryRequest req = new CreateCategoryRequest("Design", "Design docs");
        when(categoryRepository.existsByNameIgnoreCase("Design")).thenReturn(false);
        when(categoryRepository.save(any(Category.class))).thenAnswer(inv -> {
            Category c = inv.getArgument(0);
            c.setId(UUID.randomUUID());
            return c;
        });

        CategoryResponse result = categoryService.createCategory(req);

        assertThat(result.name()).isEqualTo("Design");
    }

    @Test
    void createCategory_duplicateName_throws() {
        CreateCategoryRequest req = new CreateCategoryRequest("Engineering", null);
        when(categoryRepository.existsByNameIgnoreCase("Engineering")).thenReturn(true);

        assertThatThrownBy(() -> categoryService.createCategory(req))
                .isInstanceOf(DuplicateNameException.class);
    }

    @Test
    void updateCategory_success_updatesName() {
        UpdateCategoryRequest req = new UpdateCategoryRequest("Platform", null);
        when(categoryRepository.findById(categoryId)).thenReturn(Optional.of(category));
        when(categoryRepository.existsByNameIgnoreCase("Platform")).thenReturn(false);
        when(categoryRepository.save(any(Category.class))).thenReturn(category);

        categoryService.updateCategory(categoryId, req, userId);

        assertThat(category.getName()).isEqualTo("Platform");
    }

    @Test
    void updateCategory_sameName_doesNotCheckDuplicate() {
        UpdateCategoryRequest req = new UpdateCategoryRequest("engineering", null);
        when(categoryRepository.findById(categoryId)).thenReturn(Optional.of(category));
        when(categoryRepository.save(any(Category.class))).thenReturn(category);

        categoryService.updateCategory(categoryId, req, userId);

        verify(categoryRepository, never()).existsByNameIgnoreCase(any());
    }

    @Test
    void updateCategory_duplicateName_throws() {
        UpdateCategoryRequest req = new UpdateCategoryRequest("Conflict", null);
        when(categoryRepository.findById(categoryId)).thenReturn(Optional.of(category));
        when(categoryRepository.existsByNameIgnoreCase("Conflict")).thenReturn(true);

        assertThatThrownBy(() -> categoryService.updateCategory(categoryId, req, userId))
                .isInstanceOf(DuplicateNameException.class);
    }

    @Test
    void updateCategory_nullName_updatesDescriptionOnly() {
        UpdateCategoryRequest req = new UpdateCategoryRequest(null, "New description");
        when(categoryRepository.findById(categoryId)).thenReturn(Optional.of(category));
        when(categoryRepository.save(any(Category.class))).thenReturn(category);

        categoryService.updateCategory(categoryId, req, userId);

        assertThat(category.getName()).isEqualTo("Engineering");
        assertThat(category.getDescription()).isEqualTo("New description");
    }

    @Test
    void deleteCategory_notFound_throws() {
        when(categoryRepository.existsById(categoryId)).thenReturn(false);

        assertThatThrownBy(() -> categoryService.deleteCategory(categoryId))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void deleteCategory_success() {
        when(categoryRepository.existsById(categoryId)).thenReturn(true);

        categoryService.deleteCategory(categoryId);

        verify(categoryRepository).deleteById(categoryId);
    }

    @Test
    void setUserPermission_categoryNotFound_throws() {
        when(categoryRepository.findById(categoryId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> categoryService.setUserPermission(
                categoryId, userId, new SetPermissionRequest(Permission.READ), userId))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void setUserPermission_userNotFound_throws() {
        when(categoryRepository.findById(categoryId)).thenReturn(Optional.of(category));
        when(permissionService.hasPermission(userId, categoryId, Permission.EDIT)).thenReturn(true);
        when(userRepository.existsById(userId)).thenReturn(false);

        assertThatThrownBy(() -> categoryService.setUserPermission(
                categoryId, userId, new SetPermissionRequest(Permission.READ), userId))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void setUserPermission_success_deletesAndSavesNew() {
        when(categoryRepository.findById(categoryId)).thenReturn(Optional.of(category));
        when(permissionService.hasPermission(userId, categoryId, Permission.EDIT)).thenReturn(true);
        when(userRepository.existsById(userId)).thenReturn(true);

        categoryService.setUserPermission(categoryId, userId, new SetPermissionRequest(Permission.WRITE), userId);

        verify(categoryUserPermissionRepository).deleteByIdCategoryIdAndIdUserId(categoryId, userId);
        verify(categoryUserPermissionRepository).save(any());
    }

    @Test
    void removeUserPermission_callsDelete() {
        when(permissionService.hasPermission(userId, categoryId, Permission.EDIT)).thenReturn(true);

        categoryService.removeUserPermission(categoryId, userId, userId);

        verify(categoryUserPermissionRepository).deleteByIdCategoryIdAndIdUserId(categoryId, userId);
    }

    @Test
    void setGroupPermission_categoryNotFound_throws() {
        when(categoryRepository.findById(categoryId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> categoryService.setGroupPermission(
                categoryId, groupId, new SetPermissionRequest(Permission.READ), userId))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void setGroupPermission_groupNotFound_throws() {
        when(categoryRepository.findById(categoryId)).thenReturn(Optional.of(category));
        when(permissionService.hasPermission(userId, categoryId, Permission.EDIT)).thenReturn(true);
        when(groupRepository.existsById(groupId)).thenReturn(false);

        assertThatThrownBy(() -> categoryService.setGroupPermission(
                categoryId, groupId, new SetPermissionRequest(Permission.READ), userId))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void setGroupPermission_success_deletesAndSavesNew() {
        when(categoryRepository.findById(categoryId)).thenReturn(Optional.of(category));
        when(permissionService.hasPermission(userId, categoryId, Permission.EDIT)).thenReturn(true);
        when(groupRepository.existsById(groupId)).thenReturn(true);

        categoryService.setGroupPermission(categoryId, groupId, new SetPermissionRequest(Permission.EDIT), userId);

        verify(categoryGroupPermissionRepository).deleteByIdCategoryIdAndIdGroupId(categoryId, groupId);
        verify(categoryGroupPermissionRepository).save(any());
    }

    @Test
    void removeGroupPermission_callsDelete() {
        when(permissionService.hasPermission(userId, categoryId, Permission.EDIT)).thenReturn(true);

        categoryService.removeGroupPermission(categoryId, groupId, userId);

        verify(categoryGroupPermissionRepository).deleteByIdCategoryIdAndIdGroupId(categoryId, groupId);
    }
}
