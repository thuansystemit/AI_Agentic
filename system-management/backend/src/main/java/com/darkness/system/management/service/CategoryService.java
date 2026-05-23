package com.darkness.system.management.service;

import com.darkness.system.management.domain.Category;
import com.darkness.system.management.domain.CategoryGroupPermission;
import com.darkness.system.management.domain.CategoryUserPermission;
import com.darkness.system.management.domain.enums.GlobalRole;
import com.darkness.system.management.domain.enums.Permission;
import com.darkness.system.management.dto.request.CreateCategoryRequest;
import com.darkness.system.management.dto.request.SetPermissionRequest;
import com.darkness.system.management.dto.request.UpdateCategoryRequest;
import com.darkness.system.management.dto.response.CategoryResponse;
import com.darkness.system.management.dto.response.PageResponse;
import com.darkness.system.management.dto.response.PermissionEntryResponse;
import com.darkness.system.management.exception.AccessDeniedException;
import com.darkness.system.management.exception.DuplicateNameException;
import com.darkness.system.management.exception.ResourceNotFoundException;
import com.darkness.system.management.mapper.CategoryMapper;
import com.darkness.system.management.repository.CategoryGroupPermissionRepository;
import com.darkness.system.management.repository.CategoryRepository;
import com.darkness.system.management.repository.CategoryUserPermissionRepository;
import com.darkness.system.management.repository.GroupRepository;
import com.darkness.system.management.repository.GroupMemberRepository;
import com.darkness.system.management.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class CategoryService {

    private final CategoryRepository categoryRepository;
    private final CategoryUserPermissionRepository categoryUserPermissionRepository;
    private final CategoryGroupPermissionRepository categoryGroupPermissionRepository;
    private final UserRepository userRepository;
    private final GroupRepository groupRepository;
    private final GroupMemberRepository groupMemberRepository;
    private final CategoryMapper categoryMapper;
    private final PermissionService permissionService;

    @Transactional(readOnly = true)
    public PageResponse<CategoryResponse> listCategories(UUID callerId, Pageable pageable) {
        GlobalRole role = userRepository.findRoleById(callerId);

        Page<Category> page;
        if (role == GlobalRole.ADMIN || role == GlobalRole.EDITOR) {
            // ADMIN and EDITOR see all categories
            page = categoryRepository.findAll(pageable);
        } else {
            // VIEWER sees only categories with explicit group or direct permission
            List<UUID> groupIds = groupMemberRepository.findGroupIdsByUserId(callerId);
            page = categoryRepository.findAccessibleByViewer(callerId, groupIds, pageable);
        }

        // Resolve effective permissions in batch for the current page
        List<UUID> categoryIds = page.stream().map(Category::getId).toList();
        Map<UUID, Permission> permMap = permissionService.resolveBatch(callerId, categoryIds);

        return PageResponse.from(page.map(cat ->
                categoryMapper.toResponse(cat, permMap.getOrDefault(cat.getId(), Permission.READ))));
    }

    @Transactional(readOnly = true)
    public CategoryResponse getCategory(UUID categoryId, UUID callerId) {
        Category category = findOrThrow(categoryId);
        Permission effective = permissionService.resolve(callerId, categoryId)
                .orElseThrow(() -> new AccessDeniedException("Access denied to category: " + categoryId));
        return categoryMapper.toResponse(category, effective);
    }

    @Transactional
    public CategoryResponse createCategory(CreateCategoryRequest request) {
        if (categoryRepository.existsByNameIgnoreCase(request.name())) {
            log.warn("createCategory: duplicate name '{}'", request.name());
            throw new DuplicateNameException("Category name already exists: " + request.name());
        }
        Category category = new Category();
        category.setName(request.name());
        category.setDescription(request.description());
        Category saved = categoryRepository.save(category);
        log.info("createCategory: created categoryId={} name='{}'", saved.getId(), saved.getName());
        return categoryMapper.toResponse(saved, Permission.EDIT);
    }

    @Transactional
    public CategoryResponse updateCategory(UUID categoryId, UpdateCategoryRequest request, UUID callerId) {
        Category category = findOrThrow(categoryId);
        if (request.name() != null) {
            if (!request.name().equalsIgnoreCase(category.getName())
                    && categoryRepository.existsByNameIgnoreCase(request.name())) {
                log.warn("updateCategory: duplicate name '{}' for categoryId={}", request.name(), categoryId);
                throw new DuplicateNameException("Category name already exists: " + request.name());
            }
            category.setName(request.name());
        }
        if (request.description() != null) category.setDescription(request.description());
        Category saved = categoryRepository.save(category);
        log.info("updateCategory: updated categoryId={}", categoryId);
        return categoryMapper.toResponse(saved, Permission.EDIT);
    }

    @Transactional
    public void deleteCategory(UUID categoryId) {
        if (!categoryRepository.existsById(categoryId)) {
            throw new ResourceNotFoundException("Category not found: " + categoryId);
        }
        categoryRepository.deleteById(categoryId);
        log.info("deleteCategory: deleted categoryId={}", categoryId);
    }

    // ── Permission management ────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public List<PermissionEntryResponse> listUserPermissions(UUID categoryId, UUID callerId) {
        findOrThrow(categoryId);
        requireEdit(callerId, categoryId);
        List<CategoryUserPermission> entries = categoryUserPermissionRepository.findAllByCategoryId(categoryId);
        List<UUID> userIds = entries.stream().map(e -> e.getId().getUserId()).toList();
        Map<UUID, String> nameMap = userRepository.findAllById(userIds).stream()
                .collect(java.util.stream.Collectors.toMap(u -> u.getId(), u -> u.getFullName()));
        return entries.stream()
                .map(e -> new PermissionEntryResponse(
                        e.getId().getUserId(),
                        nameMap.getOrDefault(e.getId().getUserId(), "Unknown"),
                        e.getPermission()))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<PermissionEntryResponse> listGroupPermissions(UUID categoryId, UUID callerId) {
        findOrThrow(categoryId);
        requireEdit(callerId, categoryId);
        List<CategoryGroupPermission> entries = categoryGroupPermissionRepository.findAllByCategoryId(categoryId);
        List<UUID> groupIds = entries.stream().map(e -> e.getId().getGroupId()).toList();
        Map<UUID, String> nameMap = groupRepository.findAllById(groupIds).stream()
                .collect(java.util.stream.Collectors.toMap(g -> g.getId(), g -> g.getName()));
        return entries.stream()
                .map(e -> new PermissionEntryResponse(
                        e.getId().getGroupId(),
                        nameMap.getOrDefault(e.getId().getGroupId(), "Unknown"),
                        e.getPermission()))
                .toList();
    }

    @Transactional
    public void setUserPermission(UUID categoryId, UUID userId, SetPermissionRequest request, UUID callerId) {
        findOrThrow(categoryId);
        requireEdit(callerId, categoryId);
        if (!userRepository.existsById(userId)) {
            throw new ResourceNotFoundException("User not found: " + userId);
        }
        categoryUserPermissionRepository.deleteByIdCategoryIdAndIdUserId(categoryId, userId);
        CategoryUserPermission perm = new CategoryUserPermission();
        CategoryUserPermission.CategoryUserPermissionId id = new CategoryUserPermission.CategoryUserPermissionId();
        id.setCategoryId(categoryId);
        id.setUserId(userId);
        perm.setId(id);
        perm.setPermission(request.permission());
        categoryUserPermissionRepository.save(perm);
        log.info("setUserPermission: categoryId={} userId={} permission={}", categoryId, userId, request.permission());
    }

    @Transactional
    public void removeUserPermission(UUID categoryId, UUID userId, UUID callerId) {
        requireEdit(callerId, categoryId);
        categoryUserPermissionRepository.deleteByIdCategoryIdAndIdUserId(categoryId, userId);
        log.info("removeUserPermission: categoryId={} userId={}", categoryId, userId);
    }

    @Transactional
    public void setGroupPermission(UUID categoryId, UUID groupId, SetPermissionRequest request, UUID callerId) {
        findOrThrow(categoryId);
        requireEdit(callerId, categoryId);
        if (!groupRepository.existsById(groupId)) {
            throw new ResourceNotFoundException("Group not found: " + groupId);
        }
        categoryGroupPermissionRepository.deleteByIdCategoryIdAndIdGroupId(categoryId, groupId);
        CategoryGroupPermission perm = new CategoryGroupPermission();
        CategoryGroupPermission.CategoryGroupPermissionId id = new CategoryGroupPermission.CategoryGroupPermissionId();
        id.setCategoryId(categoryId);
        id.setGroupId(groupId);
        perm.setId(id);
        perm.setPermission(request.permission());
        categoryGroupPermissionRepository.save(perm);
        log.info("setGroupPermission: categoryId={} groupId={} permission={}", categoryId, groupId, request.permission());
    }

    @Transactional
    public void removeGroupPermission(UUID categoryId, UUID groupId, UUID callerId) {
        requireEdit(callerId, categoryId);
        categoryGroupPermissionRepository.deleteByIdCategoryIdAndIdGroupId(categoryId, groupId);
        log.info("removeGroupPermission: categoryId={} groupId={}", categoryId, groupId);
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    private void requireEdit(UUID callerId, UUID categoryId) {
        if (!permissionService.hasPermission(callerId, categoryId, Permission.EDIT)) {
            throw new AccessDeniedException("EDIT permission required on category: " + categoryId);
        }
    }

    private Category findOrThrow(UUID categoryId) {
        return categoryRepository.findById(categoryId)
                .orElseThrow(() -> new ResourceNotFoundException("Category not found: " + categoryId));
    }
}
