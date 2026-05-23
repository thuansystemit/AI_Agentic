package com.darkness.system.management.service;

import com.darkness.system.management.domain.Group;
import com.darkness.system.management.domain.GroupMember;
import com.darkness.system.management.dto.request.CreateGroupRequest;
import com.darkness.system.management.dto.request.UpdateGroupRequest;
import com.darkness.system.management.dto.response.GroupResponse;
import com.darkness.system.management.dto.response.PageResponse;
import com.darkness.system.management.exception.DuplicateNameException;
import com.darkness.system.management.exception.ResourceNotFoundException;
import com.darkness.system.management.mapper.GroupMapper;
import com.darkness.system.management.repository.GroupMemberRepository;
import com.darkness.system.management.repository.GroupRepository;
import com.darkness.system.management.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class GroupService {

    private final GroupRepository groupRepository;
    private final GroupMemberRepository groupMemberRepository;
    private final UserRepository userRepository;
    private final GroupMapper groupMapper;

    @Transactional(readOnly = true)
    public PageResponse<GroupResponse> listGroups(String search, Pageable pageable) {
        var page = (search == null || search.isBlank())
                ? groupRepository.findAll(pageable).map(groupMapper::toResponse)
                : groupRepository.findByNameContainingIgnoreCase(search, pageable).map(groupMapper::toResponse);
        return PageResponse.from(page);
    }

    @Transactional(readOnly = true)
    public GroupResponse getGroup(UUID groupId) {
        return groupMapper.toResponse(findOrThrow(groupId));
    }

    @Transactional
    public GroupResponse createGroup(CreateGroupRequest request) {
        if (groupRepository.existsByNameIgnoreCase(request.name())) {
            throw new DuplicateNameException("Group name already exists: " + request.name());
        }
        Group group = new Group();
        group.setName(request.name());
        group.setDescription(request.description());
        return groupMapper.toResponse(groupRepository.save(group));
    }

    @Transactional
    public GroupResponse updateGroup(UUID groupId, UpdateGroupRequest request) {
        Group group = findOrThrow(groupId);
        if (request.name() != null) {
            if (!request.name().equalsIgnoreCase(group.getName())
                    && groupRepository.existsByNameIgnoreCase(request.name())) {
                throw new DuplicateNameException("Group name already exists: " + request.name());
            }
            group.setName(request.name());
        }
        if (request.description() != null) group.setDescription(request.description());
        return groupMapper.toResponse(groupRepository.save(group));
    }

    @Transactional
    public void deleteGroup(UUID groupId) {
        if (!groupRepository.existsById(groupId)) {
            throw new ResourceNotFoundException("Group not found: " + groupId);
        }
        groupRepository.deleteById(groupId);
    }

    @Transactional
    public void addMember(UUID groupId, UUID userId) {
        findOrThrow(groupId);
        if (!userRepository.existsById(userId)) {
            throw new ResourceNotFoundException("User not found: " + userId);
        }
        if (groupMemberRepository.existsByIdUserIdAndIdGroupId(userId, groupId)) {
            return;
        }
        GroupMember member = new GroupMember();
        GroupMember.GroupMemberId id = new GroupMember.GroupMemberId();
        id.setUserId(userId);
        id.setGroupId(groupId);
        member.setId(id);
        groupMemberRepository.save(member);
    }

    @Transactional
    public void removeMember(UUID groupId, UUID userId) {
        findOrThrow(groupId);
        groupMemberRepository.deleteByIdUserIdAndIdGroupId(userId, groupId);
    }

    private Group findOrThrow(UUID groupId) {
        return groupRepository.findById(groupId)
                .orElseThrow(() -> new ResourceNotFoundException("Group not found: " + groupId));
    }
}
