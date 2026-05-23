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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class VideoServiceTest {

    @Mock private VideoRepository videoRepository;
    @Mock private StorageService storageService;
    @Mock private CurrentUser currentUser;
    @Mock private VideoMapper videoMapper;

    @InjectMocks
    private VideoService videoService;

    private User testUser;
    private User otherUser;
    private Video testVideo;
    private VideoResponse testResponse;

    @BeforeEach
    void setUp() {
        testUser = User.builder().id(1L).email("test@example.com").username("testuser").build();
        otherUser = User.builder().id(2L).email("other@example.com").username("otheruser").build();

        testVideo = Video.builder()
                .id(1L).title("Test Video").description("A test video")
                .filePath("abc-123.mp4").fileName("testvideo.mp4")
                .fileSize(1024L).contentType("video/mp4").user(testUser)
                .build();

        testResponse = VideoResponse.builder()
                .id(1L).title("Test Video").description("A test video")
                .fileName("testvideo.mp4").fileSize(1024L).contentType("video/mp4")
                .streamUrl("/api/videos/1/stream")
                .owner(VideoResponse.VideoOwner.builder().id(1L).username("testuser").build())
                .build();
    }

    @Test
    void upload_shouldStoreFileAndSaveVideo() {
        MultipartFile file = mock(MultipartFile.class);
        when(file.getOriginalFilename()).thenReturn("video.mp4");
        when(file.getSize()).thenReturn(1024L);
        when(file.getContentType()).thenReturn("video/mp4");
        when(currentUser.require()).thenReturn(testUser);
        when(storageService.store(file)).thenReturn("stored-file.mp4");
        when(videoRepository.save(any(Video.class))).thenReturn(testVideo);
        when(videoMapper.toResponse(testVideo)).thenReturn(testResponse);

        VideoResponse response = videoService.upload(file, "My Video", "Description");

        assertThat(response.getTitle()).isEqualTo("Test Video");
        assertThat(response.getOwner().getUsername()).isEqualTo("testuser");
        verify(storageService).store(file);
        verify(videoRepository).save(any(Video.class));
    }

    @Test
    void upload_shouldThrowWhenTitleIsBlank() {
        when(currentUser.require()).thenReturn(testUser);
        MultipartFile file = mock(MultipartFile.class);

        assertThatThrownBy(() -> videoService.upload(file, "  ", "desc"))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Title is required");
    }

    @Test
    void upload_shouldThrowWhenTitleIsNull() {
        when(currentUser.require()).thenReturn(testUser);
        MultipartFile file = mock(MultipartFile.class);

        assertThatThrownBy(() -> videoService.upload(file, null, "desc"))
                .isInstanceOf(BadRequestException.class);
    }

    @Test
    void listVideos_shouldReturnPaginatedResults() {
        Page<Video> page = new PageImpl<>(List.of(testVideo));
        when(videoRepository.findAllOrderByCreatedAtDesc(any(Pageable.class))).thenReturn(page);
        when(videoMapper.toResponse(testVideo)).thenReturn(testResponse);

        PageResponse<VideoResponse> response = videoService.listVideos(0, 12, null);

        assertThat(response.getContent()).hasSize(1);
        assertThat(response.getTotalElements()).isEqualTo(1);
    }

    @Test
    void listVideos_shouldSearchByTitle() {
        Page<Video> page = new PageImpl<>(List.of(testVideo));
        when(videoRepository.searchByTitle(anyString(), any(Pageable.class))).thenReturn(page);
        when(videoMapper.toResponse(testVideo)).thenReturn(testResponse);

        PageResponse<VideoResponse> response = videoService.listVideos(0, 12, "Test");

        assertThat(response.getContent()).hasSize(1);
        verify(videoRepository).searchByTitle(eq("Test"), any(Pageable.class));
    }

    @Test
    void listMyVideos_shouldReturnUserVideos() {
        when(currentUser.require()).thenReturn(testUser);
        Page<Video> page = new PageImpl<>(List.of(testVideo));
        when(videoRepository.findByUserId(eq(1L), any(Pageable.class))).thenReturn(page);
        when(videoMapper.toResponse(testVideo)).thenReturn(testResponse);

        PageResponse<VideoResponse> response = videoService.listMyVideos(0, 12);

        assertThat(response.getContent()).hasSize(1);
    }

    @Test
    void getVideo_shouldReturnVideoResponse() {
        when(videoRepository.findById(1L)).thenReturn(Optional.of(testVideo));
        when(videoMapper.toResponse(testVideo)).thenReturn(testResponse);

        VideoResponse response = videoService.getVideo(1L);

        assertThat(response.getId()).isEqualTo(1L);
        assertThat(response.getTitle()).isEqualTo("Test Video");
    }

    @Test
    void getVideo_shouldThrowWhenNotFound() {
        when(videoRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> videoService.getVideo(999L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void updateVideo_shouldUpdateMetadata() {
        when(currentUser.require()).thenReturn(testUser);
        when(videoRepository.findById(1L)).thenReturn(Optional.of(testVideo));
        when(videoRepository.save(any(Video.class))).thenReturn(testVideo);
        when(videoMapper.toResponse(testVideo)).thenReturn(testResponse);

        VideoUpdateRequest request = VideoUpdateRequest.builder()
                .title("Updated Title").description("Updated description").build();

        VideoResponse response = videoService.updateVideo(1L, request);

        assertThat(response).isNotNull();
        verify(videoRepository).save(any(Video.class));
    }

    @Test
    void updateVideo_shouldThrowWhenNotOwner() {
        when(currentUser.require()).thenReturn(otherUser);
        when(videoRepository.findById(1L)).thenReturn(Optional.of(testVideo));

        assertThatThrownBy(() -> videoService.updateVideo(1L, VideoUpdateRequest.builder().title("x").build()))
                .isInstanceOf(UnauthorizedException.class)
                .hasMessageContaining("only edit your own videos");
    }

    @Test
    void updateVideo_shouldThrowWhenNotFound() {
        when(currentUser.require()).thenReturn(testUser);
        when(videoRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> videoService.updateVideo(999L, VideoUpdateRequest.builder().title("x").build()))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void deleteVideo_shouldDeleteFileAndRecord() {
        when(currentUser.require()).thenReturn(testUser);
        when(videoRepository.findById(1L)).thenReturn(Optional.of(testVideo));

        videoService.deleteVideo(1L);

        verify(storageService).delete("abc-123.mp4");
        verify(videoRepository).delete(testVideo);
    }

    @Test
    void deleteVideo_shouldThrowWhenNotOwner() {
        when(currentUser.require()).thenReturn(otherUser);
        when(videoRepository.findById(1L)).thenReturn(Optional.of(testVideo));

        assertThatThrownBy(() -> videoService.deleteVideo(1L))
                .isInstanceOf(UnauthorizedException.class)
                .hasMessageContaining("only delete your own videos");
    }

    @Test
    void deleteVideo_shouldThrowWhenNotFound() {
        when(currentUser.require()).thenReturn(testUser);
        when(videoRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> videoService.deleteVideo(999L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void getVideoStream_shouldReturnResource() {
        when(videoRepository.findById(1L)).thenReturn(Optional.of(testVideo));
        when(storageService.loadAsResource("abc-123.mp4")).thenReturn(mock(org.springframework.core.io.Resource.class));

        assertThat(videoService.getVideoStream(1L)).isNotNull();
    }

    @Test
    void getVideoContentType_shouldReturnContentType() {
        when(videoRepository.findById(1L)).thenReturn(Optional.of(testVideo));

        assertThat(videoService.getVideoContentType(1L)).isEqualTo("video/mp4");
    }

    @Test
    void upload_nullDescription_setsNullDescription() {
        MultipartFile file = mock(MultipartFile.class);
        when(file.getOriginalFilename()).thenReturn("video.mp4");
        when(file.getSize()).thenReturn(1024L);
        when(file.getContentType()).thenReturn("video/mp4");
        when(currentUser.require()).thenReturn(testUser);
        when(storageService.store(file)).thenReturn("stored.mp4");
        when(videoRepository.save(any(Video.class))).thenReturn(testVideo);
        when(videoMapper.toResponse(testVideo)).thenReturn(testResponse);

        VideoResponse response = videoService.upload(file, "Title", null);

        assertThat(response).isNotNull();
    }

    @Test
    void listVideos_blankSearch_returnsAll() {
        Page<Video> page = new PageImpl<>(List.of(testVideo));
        when(videoRepository.findAllOrderByCreatedAtDesc(any(Pageable.class))).thenReturn(page);
        when(videoMapper.toResponse(testVideo)).thenReturn(testResponse);

        PageResponse<VideoResponse> response = videoService.listVideos(0, 12, "   ");

        assertThat(response.getContent()).hasSize(1);
        verify(videoRepository).findAllOrderByCreatedAtDesc(any(Pageable.class));
    }

    @Test
    void updateVideo_nullTitle_doesNotChangeTitle() {
        when(currentUser.require()).thenReturn(testUser);
        when(videoRepository.findById(1L)).thenReturn(Optional.of(testVideo));
        when(videoRepository.save(any(Video.class))).thenReturn(testVideo);
        when(videoMapper.toResponse(testVideo)).thenReturn(testResponse);

        videoService.updateVideo(1L, VideoUpdateRequest.builder().title(null).description("desc").build());

        assertThat(testVideo.getTitle()).isEqualTo("Test Video");
    }

    @Test
    void updateVideo_nullDescription_doesNotChangeDescription() {
        when(currentUser.require()).thenReturn(testUser);
        when(videoRepository.findById(1L)).thenReturn(Optional.of(testVideo));
        when(videoRepository.save(any(Video.class))).thenReturn(testVideo);
        when(videoMapper.toResponse(testVideo)).thenReturn(testResponse);

        videoService.updateVideo(1L, VideoUpdateRequest.builder().title("New Title").description(null).build());

        assertThat(testVideo.getDescription()).isEqualTo("A test video");
    }
}
