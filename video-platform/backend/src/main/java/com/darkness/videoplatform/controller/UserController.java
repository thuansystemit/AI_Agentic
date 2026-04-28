package com.darkness.videoplatform.controller;

import com.darkness.videoplatform.dto.AuthResponse;
import com.darkness.videoplatform.dto.PageResponse;
import com.darkness.videoplatform.dto.VideoResponse;
import com.darkness.videoplatform.entity.User;
import com.darkness.videoplatform.security.CurrentUser;
import com.darkness.videoplatform.service.VideoService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final CurrentUser currentUser;
    private final VideoService videoService;

    @GetMapping("/me")
    public ResponseEntity<AuthResponse.UserResponse> getCurrentUser() {
        User user = currentUser.require();
        AuthResponse.UserResponse response = AuthResponse.UserResponse.builder()
                .id(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .build();
        return ResponseEntity.ok(response);
    }

    @GetMapping("/me/videos")
    public ResponseEntity<PageResponse<VideoResponse>> getMyVideos(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "12") int size) {
        return ResponseEntity.ok(videoService.listMyVideos(page, size));
    }
}
