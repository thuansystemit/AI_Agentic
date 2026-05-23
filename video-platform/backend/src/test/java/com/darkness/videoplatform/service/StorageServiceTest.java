package com.darkness.videoplatform.service;

import com.darkness.videoplatform.exception.StorageException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.core.io.Resource;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class StorageServiceTest {

    @TempDir Path tempDir;

    StorageService storageService;

    @BeforeEach
    void setUp() {
        storageService = new StorageService();
        ReflectionTestUtils.setField(storageService, "uploadPath", tempDir);
    }

    @Test
    void store_validMp4WithExtension_storesFile() {
        MockMultipartFile file = new MockMultipartFile(
                "file", "video.mp4", "video/mp4", "content".getBytes());

        String stored = storageService.store(file);

        assertThat(stored).endsWith(".mp4");
        assertThat(tempDir.resolve(stored)).exists();
    }

    @Test
    void store_nullFilename_storesWithoutExtension() {
        MockMultipartFile file = new MockMultipartFile(
                "file", null, "video/mp4", "content".getBytes());

        String stored = storageService.store(file);

        assertThat(stored).doesNotContain(".");
        assertThat(tempDir.resolve(stored)).exists();
    }

    @Test
    void store_filenameWithoutDot_storesWithoutExtension() {
        MockMultipartFile file = new MockMultipartFile(
                "file", "nodotfile", "video/mp4", "content".getBytes());

        String stored = storageService.store(file);

        assertThat(stored).doesNotContain(".");
        assertThat(tempDir.resolve(stored)).exists();
    }

    @Test
    void store_emptyFile_throwsStorageException() {
        MockMultipartFile file = new MockMultipartFile(
                "file", "video.mp4", "video/mp4", new byte[0]);

        assertThatThrownBy(() -> storageService.store(file))
                .isInstanceOf(StorageException.class)
                .hasMessageContaining("empty");
    }

    @Test
    void store_nullContentType_throwsStorageException() {
        MockMultipartFile file = new MockMultipartFile(
                "file", "video.mp4", null, "content".getBytes());

        assertThatThrownBy(() -> storageService.store(file))
                .isInstanceOf(StorageException.class)
                .hasMessageContaining("Invalid file type");
    }

    @Test
    void store_invalidContentType_throwsStorageException() {
        MockMultipartFile file = new MockMultipartFile(
                "file", "doc.pdf", "application/pdf", "content".getBytes());

        assertThatThrownBy(() -> storageService.store(file))
                .isInstanceOf(StorageException.class)
                .hasMessageContaining("Invalid file type");
    }

    @Test
    void loadAsResource_existingFile_returnsResource() throws IOException {
        Path file = tempDir.resolve("test.mp4");
        Files.write(file, "data".getBytes());

        Resource resource = storageService.loadAsResource("test.mp4");

        assertThat(resource.exists()).isTrue();
        assertThat(resource.isReadable()).isTrue();
    }

    @Test
    void loadAsResource_nonExistentFile_throwsStorageException() {
        assertThatThrownBy(() -> storageService.loadAsResource("missing.mp4"))
                .isInstanceOf(StorageException.class)
                .hasMessageContaining("File not found");
    }

    @Test
    void delete_existingFile_deletesFile() throws IOException {
        Path file = tempDir.resolve("todelete.mp4");
        Files.write(file, "data".getBytes());

        storageService.delete("todelete.mp4");

        assertThat(file).doesNotExist();
    }

    @Test
    void delete_nonExistentFile_doesNotThrow() {
        storageService.delete("nonexistent.mp4");
    }

    @Test
    void init_createsUploadDirectory() {
        StorageService service = new StorageService();
        Path newDir = tempDir.resolve("uploads");
        ReflectionTestUtils.setField(service, "uploadDir", newDir.toString());
        service.init();
        assertThat(newDir).exists();
    }

    @Test
    void store_ioException_throwsStorageException() throws IOException {
        MultipartFile file = mock(MultipartFile.class);
        when(file.isEmpty()).thenReturn(false);
        when(file.getContentType()).thenReturn("video/mp4");
        when(file.getOriginalFilename()).thenReturn("video.mp4");
        when(file.getInputStream()).thenThrow(new IOException("disk full"));

        assertThatThrownBy(() -> storageService.store(file))
                .isInstanceOf(StorageException.class)
                .hasMessageContaining("Failed to store file");
    }

    @Test
    void delete_nonEmptyDirectory_throwsStorageException() throws IOException {
        Path dir = tempDir.resolve("non-empty-dir");
        Files.createDirectory(dir);
        Files.write(dir.resolve("file.txt"), "content".getBytes());

        assertThatThrownBy(() -> storageService.delete("non-empty-dir"))
                .isInstanceOf(StorageException.class)
                .hasMessageContaining("Failed to delete file");
    }
}
