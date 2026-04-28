package com.darkness.system.management.controller;

import com.darkness.system.management.domain.enums.GlobalRole;
import com.darkness.system.management.dto.request.CreateGroupRequest;
import com.darkness.system.management.dto.request.UpdateGroupRequest;
import com.darkness.system.management.dto.response.GroupResponse;
import com.darkness.system.management.dto.response.PageResponse;
import com.darkness.system.management.repository.UserRepository;
import com.darkness.system.management.security.UserPrincipal;
import com.darkness.system.management.service.GroupService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/groups")
public class GroupController {

    private final GroupService groupService;
    private final UserRepository userRepository;

    public GroupController(GroupService groupService, UserRepository userRepository) {
        this.groupService = groupService;
        this.userRepository = userRepository;
    }

    @GetMapping
    ResponseEntity<PageResponse<GroupResponse>> listGroups(
            @RequestParam(required = false) String search,
            Pageable pageable) {
        return ResponseEntity.ok(groupService.listGroups(search, pageable));
    }

    @GetMapping("/{groupId}")
    ResponseEntity<GroupResponse> getGroup(@PathVariable UUID groupId) {
        return ResponseEntity.ok(groupService.getGroup(groupId));
    }

    @PostMapping
    ResponseEntity<GroupResponse> createGroup(@Valid @RequestBody CreateGroupRequest request,
                                               @AuthenticationPrincipal UserPrincipal principal) {
        requireAdmin(principal);
        return ResponseEntity.status(HttpStatus.CREATED).body(groupService.createGroup(request));
    }

    @PatchMapping("/{groupId}")
    ResponseEntity<GroupResponse> updateGroup(@PathVariable UUID groupId,
                                               @Valid @RequestBody UpdateGroupRequest request,
                                               @AuthenticationPrincipal UserPrincipal principal) {
        requireAdmin(principal);
        return ResponseEntity.ok(groupService.updateGroup(groupId, request));
    }

    @DeleteMapping("/{groupId}")
    ResponseEntity<Void> deleteGroup(@PathVariable UUID groupId,
                                     @AuthenticationPrincipal UserPrincipal principal) {
        requireAdmin(principal);
        groupService.deleteGroup(groupId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{groupId}/members/{userId}")
    ResponseEntity<Void> addMember(@PathVariable UUID groupId,
                                   @PathVariable UUID userId,
                                   @AuthenticationPrincipal UserPrincipal principal) {
        requireAdmin(principal);
        groupService.addMember(groupId, userId);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{groupId}/members/{userId}")
    ResponseEntity<Void> removeMember(@PathVariable UUID groupId,
                                      @PathVariable UUID userId,
                                      @AuthenticationPrincipal UserPrincipal principal) {
        requireAdmin(principal);
        groupService.removeMember(groupId, userId);
        return ResponseEntity.noContent().build();
    }

    private void requireAdmin(UserPrincipal principal) {
        GlobalRole role = userRepository.findRoleById(principal.userId());
        if (role != GlobalRole.ADMIN) {
            throw new com.darkness.system.management.exception.AccessDeniedException("Admin access required");
        }
    }
}
