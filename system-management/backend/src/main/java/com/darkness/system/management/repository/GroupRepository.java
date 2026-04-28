package com.darkness.system.management.repository;

import com.darkness.system.management.domain.Group;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface GroupRepository extends JpaRepository<Group, UUID> {
    boolean existsByNameIgnoreCase(String name);
    Page<Group> findByNameContainingIgnoreCase(String name, Pageable pageable);
}
