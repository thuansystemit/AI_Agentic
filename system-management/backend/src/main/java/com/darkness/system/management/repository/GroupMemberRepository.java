package com.darkness.system.management.repository;

import com.darkness.system.management.domain.GroupMember;
import com.darkness.system.management.domain.GroupMember.GroupMemberId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.UUID;

public interface GroupMemberRepository extends JpaRepository<GroupMember, GroupMemberId> {

    @Query("SELECT gm.id.groupId FROM GroupMember gm WHERE gm.id.userId = :userId")
    List<UUID> findGroupIdsByUserId(UUID userId);

    boolean existsByIdUserIdAndIdGroupId(UUID userId, UUID groupId);

    void deleteByIdUserIdAndIdGroupId(UUID userId, UUID groupId);
}
