package com.darkness.videoplatform.service;

import com.darkness.videoplatform.dto.PageResponse;
import com.darkness.videoplatform.dto.VideoResponse;
import com.darkness.videoplatform.dto.VideoUpdateRequest;
import com.darkness.videoplatform.entity.User;
import com.darkness.videoplatform.entity.Video;
import com.darkness.videoplatform.exception.BadRequestException;
import com.darkness.videoplatform.exception.ResourceNotFoundException;
import com.darkness.videoplatform.exception.UnauthorizedException;
import com.darkness.videoplatform.mapper.VideoMapper;
import com.darkness.videoplatform.repository.VideoRepository;
import com.darkness.videoplatform.security.CurrentUser;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
@RequiredArgsConstructor
public class VideoService {

    private final VideoRepository videoRepository;
    private final StorageService storageService;
    private final CurrentUser currentUser;
    private final VideoMapper videoMapper;

    @Transactional
    public VideoResponse upload(MultipartFile file, String title, String description) {
        User user = currentUser.require();
        if (title == null || title.isBlank()) {
            throw new BadRequestException("Title is required");
        }
        String storedFilename = storageService.store(file);
        Video video = Video.builder()
                .title(title.trim())
                .description(description != null ? description.trim() : null)
                .filePath(storedFilename)
                .fileName(file.getOriginalFilename())
                .fileSize(file.getSize())
                .contentType(file.getContentType())
                .user(user)
                .build();
        return videoMapper.toResponse(videoRepository.save(video));
    }

    @Transactional(readOnly = true)
    public PageResponse<VideoResponse> listVideos(int page, int size, String search) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<Video> videoPage = (search != null && !search.isBlank())
                ? videoRepository.searchByTitle(search.trim(), pageable)
                : videoRepository.findAllOrderByCreatedAtDesc(pageable);
        return buildPageResponse(videoPage);
    }

    @Transactional(readOnly = true)
    public PageResponse<VideoResponse> listMyVideos(int page, int size) {
        User user = currentUser.require();
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        return buildPageResponse(videoRepository.findByUserId(user.getId(), pageable));
    }

    @Transactional(readOnly = true)
    public VideoResponse getVideo(Long id) {
        return videoMapper.toResponse(findOrThrow(id));
    }

    @Transactional
    public VideoResponse updateVideo(Long id, VideoUpdateRequest request) {
        User user = currentUser.require();
        Video video = findOrThrow(id);
        if (!video.getUser().getId().equals(user.getId())) {
            throw new UnauthorizedException("You can only edit your own videos");
        }
        if (request.getTitle() != null) video.setTitle(request.getTitle().trim());
        if (request.getDescription() != null) video.setDescription(request.getDescription().trim());
        return videoMapper.toResponse(videoRepository.save(video));
    }

    @Transactional
    public void deleteVideo(Long id) {
        User user = currentUser.require();
        Video video = findOrThrow(id);
        if (!video.getUser().getId().equals(user.getId())) {
            throw new UnauthorizedException("You can only delete your own videos");
        }
        storageService.delete(video.getFilePath());
        videoRepository.delete(video);
    }

    public Resource getVideoStream(Long id) {
        return storageService.loadAsResource(findOrThrow(id).getFilePath());
    }

    public String getVideoContentType(Long id) {
        return findOrThrow(id).getContentType();
    }

    private Video findOrThrow(Long id) {
        return videoRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Video", "id", id));
    }

    private PageResponse<VideoResponse> buildPageResponse(Page<Video> videoPage) {
        return PageResponse.<VideoResponse>builder()
                .content(videoPage.getContent().stream().map(videoMapper::toResponse).toList())
                .page(videoPage.getNumber())
                .size(videoPage.getSize())
                .totalElements(videoPage.getTotalElements())
                .totalPages(videoPage.getTotalPages())
                .last(videoPage.isLast())
                .build();
    }
}
