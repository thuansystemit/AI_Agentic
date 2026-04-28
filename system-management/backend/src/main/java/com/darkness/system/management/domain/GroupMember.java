package com.darkness.system.management.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "group_members")
@Getter
@Setter
public class GroupMember {

    @EmbeddedId
    private GroupMemberId id = new GroupMemberId();

    @Column(name = "joined_at", nullable = false, updatable = false)
    private Instant joinedAt = Instant.now();

    @Embeddable
    @Getter
    @Setter
    public static class GroupMemberId implements Serializable {
        @Column(name = "user_id")
        private UUID userId;

        @Column(name = "group_id")
        private UUID groupId;
    }
}
