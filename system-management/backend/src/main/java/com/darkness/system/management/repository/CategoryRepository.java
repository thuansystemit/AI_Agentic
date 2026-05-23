package com.darkness.system.management.repository;

import com.darkness.system.management.domain.Category;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface CategoryRepository extends JpaRepository<Category, UUID> {
    boolean existsByNameIgnoreCase(String name);

    /** Returns categories the viewer can see — those with at least one explicit group or direct permission. */
    @Query("""
        SELECT DISTINCT c FROM Category c
        WHERE EXISTS (
            SELECT 1 FROM CategoryUserPermission cup
            WHERE cup.id.categoryId = c.id AND cup.id.userId = :userId
        ) OR EXISTS (
            SELECT 1 FROM CategoryGroupPermission cgp
            WHERE cgp.id.categoryId = c.id AND cgp.id.groupId IN :groupIds
        )
    """)
    Page<Category> findAccessibleByViewer(@Param("userId") UUID userId,
                                          @Param("groupIds") List<UUID> groupIds,
                                          Pageable pageable);

    /** Same as findAccessibleByViewer but returns IDs only — used to get a flat list for batch permission resolution. */
    @Query("""
        SELECT DISTINCT c.id FROM Category c
        WHERE EXISTS (
            SELECT 1 FROM CategoryUserPermission cup
            WHERE cup.id.categoryId = c.id AND cup.id.userId = :userId
        ) OR EXISTS (
            SELECT 1 FROM CategoryGroupPermission cgp
            WHERE cgp.id.categoryId = c.id AND cgp.id.groupId IN :groupIds
        )
    """)
    List<UUID> findAccessibleIdsByViewer(@Param("userId") UUID userId,
                                         @Param("groupIds") List<UUID> groupIds);
}
