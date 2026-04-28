package com.darkness.system.management.service;

import com.darkness.system.management.domain.Group;
import com.darkness.system.management.dto.request.CreateGroupRequest;
import com.darkness.system.management.dto.request.UpdateGroupRequest;
import com.darkness.system.management.dto.response.GroupResponse;
import com.darkness.system.management.dto.response.PageResponse;
import com.darkness.system.management.exception.DuplicateNameException;
import com.darkness.system.management.exception.ResourceNotFoundException;
import com.darkness.system.management.repository.GroupMemberRepository;
import com.darkness.system.management.repository.GroupRepository;
import com.darkness.system.management.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.*;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class GroupServiceTest {

    @Mock GroupRepository groupRepository;
    @Mock GroupMemberRepository groupMemberRepository;
    @Mock UserRepository userRepository;

    @InjectMocks GroupService groupService;

    UUID groupId;
    UUID userId;
    Group group;

    @BeforeEach
    void setUp() {
        groupId = UUID.randomUUID();
        userId = UUID.randomUUID();
        group = new Group();
        group.setId(groupId);
        group.setName("Developers");
        group.setDescription("Dev team");
    }

    @Test
    void listGroups_noSearch_returnsAllGroups() {
        Page<Group> page = new PageImpl<>(List.of(group));
        when(groupRepository.findAll(any(Pageable.class))).thenReturn(page);

        PageResponse<GroupResponse> result = groupService.listGroups(null, PageRequest.of(0, 10));

        assertThat(result.content()).hasSize(1);
    }

    @Test
    void listGroups_emptySearch_returnsAllGroups() {
        Page<Group> page = new PageImpl<>(List.of(group));
        when(groupRepository.findAll(any(Pageable.class))).thenReturn(page);

        PageResponse<GroupResponse> result = groupService.listGroups("  ", PageRequest.of(0, 10));

        assertThat(result.content()).hasSize(1);
    }

    @Test
    void listGroups_withSearch_callsSearchQuery() {
        Page<Group> page = new PageImpl<>(List.of(group));
        when(groupRepository.findByNameContainingIgnoreCase(eq("dev"), any(Pageable.class))).thenReturn(page);

        PageResponse<GroupResponse> result = groupService.listGroups("dev", PageRequest.of(0, 10));

        assertThat(result.content()).hasSize(1);
        verify(groupRepository).findByNameContainingIgnoreCase(eq("dev"), any(Pageable.class));
    }

    @Test
    void getGroup_found_returnsGroupResponse() {
        when(groupRepository.findById(groupId)).thenReturn(Optional.of(group));

        GroupResponse result = groupService.getGroup(groupId);

        assertThat(result.id()).isEqualTo(groupId);
        assertThat(result.name()).isEqualTo("Developers");
    }

    @Test
    void getGroup_notFound_throws() {
        when(groupRepository.findById(groupId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> groupService.getGroup(groupId))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void createGroup_success_returnsGroupResponse() {
        CreateGroupRequest req = new CreateGroupRequest("QA", "QA team");
        when(groupRepository.existsByNameIgnoreCase("QA")).thenReturn(false);
        when(groupRepository.save(any(Group.class))).thenAnswer(inv -> {
            Group g = inv.getArgument(0);
            g.setId(UUID.randomUUID());
            return g;
        });

        GroupResponse result = groupService.createGroup(req);

        assertThat(result.name()).isEqualTo("QA");
    }

    @Test
    void createGroup_duplicateName_throws() {
        CreateGroupRequest req = new CreateGroupRequest("Developers", null);
        when(groupRepository.existsByNameIgnoreCase("Developers")).thenReturn(true);

        assertThatThrownBy(() -> groupService.createGroup(req))
                .isInstanceOf(DuplicateNameException.class);
    }

    @Test
    void updateGroup_success_updatesName() {
        UpdateGroupRequest req = new UpdateGroupRequest("Platform", null);
        when(groupRepository.findById(groupId)).thenReturn(Optional.of(group));
        when(groupRepository.existsByNameIgnoreCase("Platform")).thenReturn(false);
        when(groupRepository.save(any(Group.class))).thenReturn(group);

        groupService.updateGroup(groupId, req);

        assertThat(group.getName()).isEqualTo("Platform");
    }

    @Test
    void updateGroup_sameName_doesNotCheckDuplicate() {
        UpdateGroupRequest req = new UpdateGroupRequest("developers", null);
        when(groupRepository.findById(groupId)).thenReturn(Optional.of(group));
        when(groupRepository.save(any(Group.class))).thenReturn(group);

        groupService.updateGroup(groupId, req);

        verify(groupRepository, never()).existsByNameIgnoreCase(any());
    }

    @Test
    void updateGroup_duplicateName_throws() {
        UpdateGroupRequest req = new UpdateGroupRequest("ExistingName", null);
        when(groupRepository.findById(groupId)).thenReturn(Optional.of(group));
        when(groupRepository.existsByNameIgnoreCase("ExistingName")).thenReturn(true);

        assertThatThrownBy(() -> groupService.updateGroup(groupId, req))
                .isInstanceOf(DuplicateNameException.class);
    }

    @Test
    void updateGroup_nullName_updatesDescriptionOnly() {
        UpdateGroupRequest req = new UpdateGroupRequest(null, "Updated description");
        when(groupRepository.findById(groupId)).thenReturn(Optional.of(group));
        when(groupRepository.save(any(Group.class))).thenReturn(group);

        groupService.updateGroup(groupId, req);

        assertThat(group.getName()).isEqualTo("Developers");
        assertThat(group.getDescription()).isEqualTo("Updated description");
    }

    @Test
    void deleteGroup_notFound_throws() {
        when(groupRepository.existsById(groupId)).thenReturn(false);

        assertThatThrownBy(() -> groupService.deleteGroup(groupId))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void deleteGroup_success() {
        when(groupRepository.existsById(groupId)).thenReturn(true);

        groupService.deleteGroup(groupId);

        verify(groupRepository).deleteById(groupId);
    }

    @Test
    void addMember_success_savesMembership() {
        when(groupRepository.findById(groupId)).thenReturn(Optional.of(group));
        when(userRepository.existsById(userId)).thenReturn(true);
        when(groupMemberRepository.existsByIdUserIdAndIdGroupId(userId, groupId)).thenReturn(false);

        groupService.addMember(groupId, userId);

        verify(groupMemberRepository).save(any());
    }

    @Test
    void addMember_alreadyMember_isIdempotent() {
        when(groupRepository.findById(groupId)).thenReturn(Optional.of(group));
        when(userRepository.existsById(userId)).thenReturn(true);
        when(groupMemberRepository.existsByIdUserIdAndIdGroupId(userId, groupId)).thenReturn(true);

        groupService.addMember(groupId, userId);

        verify(groupMemberRepository, never()).save(any());
    }

    @Test
    void addMember_userNotFound_throws() {
        when(groupRepository.findById(groupId)).thenReturn(Optional.of(group));
        when(userRepository.existsById(userId)).thenReturn(false);

        assertThatThrownBy(() -> groupService.addMember(groupId, userId))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void removeMember_callsDelete() {
        when(groupRepository.findById(groupId)).thenReturn(Optional.of(group));

        groupService.removeMember(groupId, userId);

        verify(groupMemberRepository).deleteByIdUserIdAndIdGroupId(userId, groupId);
    }
}
