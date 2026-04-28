package com.darkness.system.management.repository;

import com.darkness.system.management.domain.Document;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface DocumentRepository extends JpaRepository<Document, UUID> {
    Page<Document> findByCategoryId(UUID categoryId, Pageable pageable);
    Page<Document> findByTitleContainingIgnoreCaseAndCategoryId(String title, UUID categoryId, Pageable pageable);
}
