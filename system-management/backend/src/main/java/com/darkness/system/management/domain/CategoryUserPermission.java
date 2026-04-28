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
@Table(name = "category_user_permissions")
@Getter
@Setter
public class CategoryUserPermission {

    @EmbeddedId
    private CategoryUserPermissionId id = new CategoryUserPermissionId();

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, columnDefinition = "permission_level")
    @ColumnTransformer(write = "?::permission_level")
    private Permission permission;

    @Column(name = "granted_at", nullable = false, updatable = false)
    private Instant grantedAt = Instant.now();

    @Embeddable
    @Getter
    @Setter
    public static class CategoryUserPermissionId implements Serializable {
        @Column(name = "category_id")
        private UUID categoryId;

        @Column(name = "user_id")
        private UUID userId;
    }
}
