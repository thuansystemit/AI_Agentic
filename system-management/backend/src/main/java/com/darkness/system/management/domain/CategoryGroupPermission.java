package com.darkness.system.management.domain;

import com.darkness.system.management.domain.enums.Permission;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.ColumnTransformer;

import java.io.Serializable;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "category_group_permissions")
@Getter
@Setter
public class CategoryGroupPermission {

    @EmbeddedId
    private CategoryGroupPermissionId id = new CategoryGroupPermissionId();

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, columnDefinition = "permission_level")
    @ColumnTransformer(write = "?::permission_level")
    private Permission permission;

    @Column(name = "granted_at", nullable = false, updatable = false)
    private Instant grantedAt = Instant.now();

    @Embeddable
    @Getter
    @Setter
    public static class CategoryGroupPermissionId implements Serializable {
        @Column(name = "category_id")
        private UUID categoryId;

        @Column(name = "group_id")
        private UUID groupId;
    }
}
