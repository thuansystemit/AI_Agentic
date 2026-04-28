package com.darkness.system.management.controller;

import com.darkness.system.management.domain.enums.GlobalRole;
import com.darkness.system.management.dto.request.CreateCategoryRequest;
import com.darkness.system.management.dto.request.SetPermissionRequest;
import com.darkness.system.management.dto.request.UpdateCategoryRequest;
import com.darkness.system.management.dto.response.CategoryResponse;
import com.darkness.system.management.dto.response.PageResponse;
import com.darkness.system.management.repository.UserRepository;
import com.darkness.system.management.security.UserPrincipal;
import com.darkness.system.management.service.CategoryService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/categories")
public class CategoryController {

    private static final Logger log = LoggerFactory.getLogger(CategoryController.class);

    private final CategoryService categoryService;
    private final UserRepository userRepository;

    public CategoryController(CategoryService categoryService, UserRepository userRepository) {
        this.categoryService = categoryService;
        this.userRepository = userRepository;
    }

    @GetMapping
    ResponseEntity<PageResponse<CategoryResponse>> listCategories(Pageable pageable) {
        return ResponseEntity.ok(categoryService.listCategories(pageable));
    }

    @GetMapping("/{categoryId}")
    ResponseEntity<CategoryResponse> getCategory(@PathVariable UUID categoryId) {
        return ResponseEntity.ok(categoryService.getCategory(categoryId));
    }

    @PostMapping
    ResponseEntity<CategoryResponse> createCategory(@Valid @RequestBody CreateCategoryRequest request,
                                                     @AuthenticationPrincipal UserPrincipal principal) {
        requireAdmin(principal);
        return ResponseEntity.status(HttpStatus.CREATED).body(categoryService.createCategory(request));
    }

    @PatchMapping("/{categoryId}")
    ResponseEntity<CategoryResponse> updateCategory(@PathVariable UUID categoryId,
                                                     @Valid @RequestBody UpdateCategoryRequest request,
                                                     @AuthenticationPrincipal UserPrincipal principal) {
        requireAdmin(principal);
        return ResponseEntity.ok(categoryService.updateCategory(categoryId, request));
    }

    @DeleteMapping("/{categoryId}")
    ResponseEntity<Void> deleteCategory(@PathVariable UUID categoryId,
                                        @AuthenticationPrincipal UserPrincipal principal) {
        requireAdmin(principal);
        categoryService.deleteCategory(categoryId);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/{categoryId}/permissions/users/{userId}")
    ResponseEntity<Void> setUserPermission(@PathVariable UUID categoryId,
                                           @PathVariable UUID userId,
                                           @Valid @RequestBody SetPermissionRequest request,
                                           @AuthenticationPrincipal UserPrincipal principal) {
        requireAdmin(principal);
        categoryService.setUserPermission(categoryId, userId, request);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{categoryId}/permissions/users/{userId}")
    ResponseEntity<Void> removeUserPermission(@PathVariable UUID categoryId,
                                              @PathVariable UUID userId,
                                              @AuthenticationPrincipal UserPrincipal principal) {
        requireAdmin(principal);
        categoryService.removeUserPermission(categoryId, userId);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/{categoryId}/permissions/groups/{groupId}")
    ResponseEntity<Void> setGroupPermission(@PathVariable UUID categoryId,
                                            @PathVariable UUID groupId,
                                            @Valid @RequestBody SetPermissionRequest request,
                                            @AuthenticationPrincipal UserPrincipal principal) {
        requireAdmin(principal);
        categoryService.setGroupPermission(categoryId, groupId, request);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{categoryId}/permissions/groups/{groupId}")
    ResponseEntity<Void> removeGroupPermission(@PathVariable UUID categoryId,
                                               @PathVariable UUID groupId,
                                               @AuthenticationPrincipal UserPrincipal principal) {
        requireAdmin(principal);
        categoryService.removeGroupPermission(categoryId, groupId);
        return ResponseEntity.noContent().build();
    }

    private void requireAdmin(UserPrincipal principal) {
        GlobalRole role = userRepository.findRoleById(principal.userId());
        if (role == null) {
            log.warn("requireAdmin: no role found for userId={} — user may have been deleted", principal.userId());
            throw new com.darkness.system.management.exception.AccessDeniedException("Admin access required");
        }
        if (role != GlobalRole.ADMIN) {
            log.warn("requireAdmin: access denied userId={} email={} role={}", principal.userId(), principal.email(), role);
            throw new com.darkness.system.management.exception.AccessDeniedException("Admin access required");
        }
        log.debug("requireAdmin: granted userId={} email={}", principal.userId(), principal.email());
    }
}
