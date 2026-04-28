package com.darkness.videoplatform.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VideoResponse {

    private Long id;
    private String title;
    private String description;
    private String fileName;
    private Long fileSize;
    private String contentType;
    private String streamUrl;
    private VideoOwner owner;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class VideoOwner {
        private Long id;
        private String username;
    }
}
