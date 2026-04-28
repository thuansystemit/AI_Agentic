package com.darkness.system.management.service;

import com.darkness.system.management.domain.Category;
import com.darkness.system.management.domain.CategoryGroupPermission;
import com.darkness.system.management.domain.CategoryUserPermission;
import com.darkness.system.management.domain.enums.Permission;
import com.darkness.system.management.dto.request.CreateCategoryRequest;
import com.darkness.system.management.dto.request.SetPermissionRequest;
import com.darkness.system.management.dto.request.UpdateCategoryRequest;
import com.darkness.system.management.dto.response.CategoryResponse;
import com.darkness.system.management.dto.response.PageResponse;
import com.darkness.system.management.exception.DuplicateNameException;
import com.darkness.system.management.exception.ResourceNotFoundException;
import com.darkness.system.management.repository.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class CategoryService {

    private static final Logger log = LoggerFactory.getLogger(CategoryService.class);

    private final CategoryRepository categoryRepository;
    private final CategoryUserPermissionRepository categoryUserPermissionRepository;
    private final CategoryGroupPermissionRepository categoryGroupPermissionRepository;
    private final UserRepository userRepository;
    private final GroupRepository groupRepository;

    public CategoryService(CategoryRepository categoryRepository,
                           CategoryUserPermissionRepository categoryUserPermissionRepository,
                           CategoryGroupPermissionRepository categoryGroupPermissionRepository,
                           UserRepository userRepository,
                           GroupRepository groupRepository) {
        this.categoryRepository = categoryRepository;
        this.categoryUserPermissionRepository = categoryUserPermissionRepository;
        this.categoryGroupPermissionRepository = categoryGroupPermissionRepository;
        this.userRepository = userRepository;
        this.groupRepository = groupRepository;
    }

    @Transactional(readOnly = true)
    public PageResponse<CategoryResponse> listCategories(Pageable pageable) {
        return PageResponse.from(categoryRepository.findAll(pageable).map(CategoryResponse::from));
    }

    @Transactional(readOnly = true)
    public CategoryResponse getCategory(UUID categoryId) {
        return CategoryResponse.from(findOrThrow(categoryId));
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
        CategoryResponse saved = CategoryResponse.from(categoryRepository.save(category));
        log.info("createCategory: created categoryId={} name='{}'", saved.id(), saved.name());
        return saved;
    }

    @Transactional
    public CategoryResponse updateCategory(UUID categoryId, UpdateCategoryRequest request) {
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
        CategoryResponse saved = CategoryResponse.from(categoryRepository.save(category));
        log.info("updateCategory: updated categoryId={}", categoryId);
        return saved;
    }

    @Transactional
    public void deleteCategory(UUID categoryId) {
        if (!categoryRepository.existsById(categoryId)) {
            throw new ResourceNotFoundException("Category not found: " + categoryId);
        }
        categoryRepository.deleteById(categoryId);
        log.info("deleteCategory: deleted categoryId={}", categoryId);
    }

    @Transactional
    public void setUserPermission(UUID categoryId, UUID userId, SetPermissionRequest request) {
        findOrThrow(categoryId);
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
    }

    @Transactional
    public void removeUserPermission(UUID categoryId, UUID userId) {
        categoryUserPermissionRepository.deleteByIdCategoryIdAndIdUserId(categoryId, userId);
    }

    @Transactional
    public void setGroupPermission(UUID categoryId, UUID groupId, SetPermissionRequest request) {
        findOrThrow(categoryId);
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
    }

    @Transactional
    public void removeGroupPermission(UUID categoryId, UUID groupId) {
        categoryGroupPermissionRepository.deleteByIdCategoryIdAndIdGroupId(categoryId, groupId);
    }

    private Category findOrThrow(UUID categoryId) {
        return categoryRepository.findById(categoryId)
                .orElseThrow(() -> new ResourceNotFoundException("Category not found: " + categoryId));
    }
}
