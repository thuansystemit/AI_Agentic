package com.darkness.system.management.controller;

import com.darkness.system.management.domain.enums.GlobalRole;
import com.darkness.system.management.dto.request.CreateCategoryRequest;
import com.darkness.system.management.dto.request.SetPermissionRequest;
import com.darkness.system.management.dto.request.UpdateCategoryRequest;
import com.darkness.system.management.dto.response.CategoryResponse;
import com.darkness.system.management.dto.response.PageResponse;
import com.darkness.system.management.dto.response.PermissionEntryResponse;
import com.darkness.system.management.exception.AccessDeniedException;
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

import java.util.List;
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
    ResponseEntity<PageResponse<CategoryResponse>> listCategories(
            @AuthenticationPrincipal UserPrincipal principal,
            Pageable pageable) {
        return ResponseEntity.ok(categoryService.listCategories(principal.userId(), pageable));
    }

    @GetMapping("/{categoryId}")
    ResponseEntity<CategoryResponse> getCategory(@PathVariable UUID categoryId,
                                                  @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(categoryService.getCategory(categoryId, principal.userId()));
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
        return ResponseEntity.ok(categoryService.updateCategory(categoryId, request, principal.userId()));
    }

    @DeleteMapping("/{categoryId}")
    ResponseEntity<Void> deleteCategory(@PathVariable UUID categoryId,
                                        @AuthenticationPrincipal UserPrincipal principal) {
        requireAdmin(principal);
        categoryService.deleteCategory(categoryId);
        return ResponseEntity.noContent().build();
    }

    // ── Permission management (requires EDIT on the category) ────────────────

    @GetMapping("/{categoryId}/permissions/users")
    ResponseEntity<List<PermissionEntryResponse>> listUserPermissions(
            @PathVariable UUID categoryId,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(categoryService.listUserPermissions(categoryId, principal.userId()));
    }

    @PutMapping("/{categoryId}/permissions/users/{userId}")
    ResponseEntity<Void> setUserPermission(@PathVariable UUID categoryId,
                                           @PathVariable UUID userId,
                                           @Valid @RequestBody SetPermissionRequest request,
                                           @AuthenticationPrincipal UserPrincipal principal) {
        categoryService.setUserPermission(categoryId, userId, request, principal.userId());
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{categoryId}/permissions/users/{userId}")
    ResponseEntity<Void> removeUserPermission(@PathVariable UUID categoryId,
                                              @PathVariable UUID userId,
                                              @AuthenticationPrincipal UserPrincipal principal) {
        categoryService.removeUserPermission(categoryId, userId, principal.userId());
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{categoryId}/permissions/groups")
    ResponseEntity<List<PermissionEntryResponse>> listGroupPermissions(
            @PathVariable UUID categoryId,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(categoryService.listGroupPermissions(categoryId, principal.userId()));
    }

    @PutMapping("/{categoryId}/permissions/groups/{groupId}")
    ResponseEntity<Void> setGroupPermission(@PathVariable UUID categoryId,
                                            @PathVariable UUID groupId,
                                            @Valid @RequestBody SetPermissionRequest request,
                                            @AuthenticationPrincipal UserPrincipal principal) {
        categoryService.setGroupPermission(categoryId, groupId, request, principal.userId());
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{categoryId}/permissions/groups/{groupId}")
    ResponseEntity<Void> removeGroupPermission(@PathVariable UUID categoryId,
                                               @PathVariable UUID groupId,
                                               @AuthenticationPrincipal UserPrincipal principal) {
        categoryService.removeGroupPermission(categoryId, groupId, principal.userId());
        return ResponseEntity.noContent().build();
    }

    // ── Helper ───────────────────────────────────────────────────────────────

    private void requireAdmin(UserPrincipal principal) {
        GlobalRole role = userRepository.findRoleById(principal.userId());
        if (role == null || role != GlobalRole.ADMIN) {
            log.warn("requireAdmin: denied userId={} role={}", principal.userId(), role);
            throw new AccessDeniedException("Admin access required");
        }
    }
}
