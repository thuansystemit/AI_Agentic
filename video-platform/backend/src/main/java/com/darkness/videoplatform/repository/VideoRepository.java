package com.darkness.videoplatform.repository;

import com.darkness.videoplatform.entity.Video;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface VideoRepository extends JpaRepository<Video, Long> {

    Page<Video> findByUserId(Long userId, Pageable pageable);

    @Query("SELECT v FROM Video v WHERE LOWER(v.title) LIKE LOWER(CONCAT('%', :query, '%'))")
    Page<Video> searchByTitle(@Param("query") String query, Pageable pageable);

    @Query("SELECT v FROM Video v ORDER BY v.createdAt DESC")
    Page<Video> findAllOrderByCreatedAtDesc(Pageable pageable);
}
