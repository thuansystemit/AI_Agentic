package com.darkness.system.management.service;

import com.darkness.system.management.domain.Document;
import com.darkness.system.management.domain.enums.Permission;
import com.darkness.system.management.dto.request.CreateDocumentRequest;
import com.darkness.system.management.dto.request.UpdateDocumentRequest;
import com.darkness.system.management.dto.response.DocumentResponse;
import com.darkness.system.management.dto.response.PageResponse;
import com.darkness.system.management.exception.AccessDeniedException;
import com.darkness.system.management.exception.ResourceNotFoundException;
import com.darkness.system.management.mapper.DocumentMapper;
import com.darkness.system.management.repository.CategoryRepository;
import com.darkness.system.management.repository.DocumentRepository;
import org.springframework.web.multipart.MultipartFile;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.*;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DocumentServiceTest {

    @Mock DocumentRepository documentRepository;
    @Mock CategoryRepository categoryRepository;
    @Mock PermissionService permissionService;
    @Mock DocumentMapper documentMapper;

    @InjectMocks DocumentService documentService;

    UUID docId;
    UUID categoryId;
    UUID callerId;
    Document document;

    @BeforeEach
    void setUp() {
        docId = UUID.randomUUID();
        categoryId = UUID.randomUUID();
        callerId = UUID.randomUUID();

        document = new Document();
        document.setId(docId);
        document.setTitle("Test Doc");
        document.setContent("Hello");
        document.setCategoryId(categoryId);
        document.setCreatedBy(callerId);
        lenient().when(documentMapper.toResponse(any(Document.class))).thenAnswer(inv -> {
            Document d = inv.getArgument(0);
            return new DocumentResponse(d.getId(), d.getTitle(), d.getContent(),
                    d.getCategoryId(), d.getCreatedBy(), d.getCreatedAt(), d.getUpdatedAt());
        });
    }

    // List documents
    @Test
    void listDocuments_noPermission_throwsAccessDenied() {
        when(permissionService.hasPermission(callerId, categoryId, Permission.READ)).thenReturn(false);

        assertThatThrownBy(() ->
                documentService.listDocuments(categoryId, null, callerId, PageRequest.of(0, 10)))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void listDocuments_noSearch_returnsDocs() {
        when(permissionService.hasPermission(callerId, categoryId, Permission.READ)).thenReturn(true);
        Page<Document> page = new PageImpl<>(List.of(document));
        when(documentRepository.findByCategoryId(eq(categoryId), any(Pageable.class))).thenReturn(page);

        PageResponse<DocumentResponse> result =
                documentService.listDocuments(categoryId, null, callerId, PageRequest.of(0, 10));

        assertThat(result.content()).hasSize(1);
    }

    @Test
    void listDocuments_emptySearch_returnsDocs() {
        when(permissionService.hasPermission(callerId, categoryId, Permission.READ)).thenReturn(true);
        Page<Document> page = new PageImpl<>(List.of(document));
        when(documentRepository.findByCategoryId(eq(categoryId), any(Pageable.class))).thenReturn(page);

        PageResponse<DocumentResponse> result =
                documentService.listDocuments(categoryId, "  ", callerId, PageRequest.of(0, 10));

        assertThat(result.content()).hasSize(1);
    }

    @Test
    void listDocuments_withSearch_callsSearchQuery() {
        when(permissionService.hasPermission(callerId, categoryId, Permission.READ)).thenReturn(true);
        Page<Document> page = new PageImpl<>(List.of(document));
        when(documentRepository.findByTitleContainingIgnoreCaseAndCategoryId(
                eq("test"), eq(categoryId), any(Pageable.class))).thenReturn(page);

        PageResponse<DocumentResponse> result =
                documentService.listDocuments(categoryId, "test", callerId, PageRequest.of(0, 10));

        assertThat(result.content()).hasSize(1);
        verify(documentRepository).findByTitleContainingIgnoreCaseAndCategoryId(
                eq("test"), eq(categoryId), any(Pageable.class));
    }

    // Get document
    @Test
    void getDocument_noPermission_throwsAccessDenied() {
        when(documentRepository.findById(docId)).thenReturn(Optional.of(document));
        when(permissionService.hasPermission(callerId, categoryId, Permission.READ)).thenReturn(false);

        assertThatThrownBy(() -> documentService.getDocument(docId, callerId))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void getDocument_notFound_throws() {
        when(documentRepository.findById(docId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> documentService.getDocument(docId, callerId))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void getDocument_success_returnsDoc() {
        when(documentRepository.findById(docId)).thenReturn(Optional.of(document));
        when(permissionService.hasPermission(callerId, categoryId, Permission.READ)).thenReturn(true);

        DocumentResponse result = documentService.getDocument(docId, callerId);

        assertThat(result.id()).isEqualTo(docId);
        assertThat(result.title()).isEqualTo("Test Doc");
    }

    // Create document
    @Test
    void createDocument_categoryNotFound_throws() {
        CreateDocumentRequest req = new CreateDocumentRequest("Title", "Content", categoryId);
        when(categoryRepository.existsById(categoryId)).thenReturn(false);

        assertThatThrownBy(() -> documentService.createDocument(req, callerId))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void createDocument_noPermission_throwsAccessDenied() {
        CreateDocumentRequest req = new CreateDocumentRequest("Title", "Content", categoryId);
        when(categoryRepository.existsById(categoryId)).thenReturn(true);
        when(permissionService.hasPermission(callerId, categoryId, Permission.WRITE)).thenReturn(false);

        assertThatThrownBy(() -> documentService.createDocument(req, callerId))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void createDocument_success_returnsCreatedDoc() {
        CreateDocumentRequest req = new CreateDocumentRequest("New Title", "Content", categoryId);
        when(categoryRepository.existsById(categoryId)).thenReturn(true);
        when(permissionService.hasPermission(callerId, categoryId, Permission.WRITE)).thenReturn(true);
        when(documentRepository.save(any(Document.class))).thenAnswer(inv -> {
            Document d = inv.getArgument(0);
            d.setId(UUID.randomUUID());
            return d;
        });

        DocumentResponse result = documentService.createDocument(req, callerId);

        assertThat(result.title()).isEqualTo("New Title");
        assertThat(result.content()).isEqualTo("Content");
        assertThat(result.createdBy()).isEqualTo(callerId);
    }

    @Test
    void createDocument_nullContent_defaultsToEmpty() {
        CreateDocumentRequest req = new CreateDocumentRequest("Title", null, categoryId);
        when(categoryRepository.existsById(categoryId)).thenReturn(true);
        when(permissionService.hasPermission(callerId, categoryId, Permission.WRITE)).thenReturn(true);
        when(documentRepository.save(any(Document.class))).thenAnswer(inv -> inv.getArgument(0));

        DocumentResponse result = documentService.createDocument(req, callerId);

        assertThat(result.content()).isEqualTo("");
    }

    // Update document
    @Test
    void updateDocument_noPermission_throwsAccessDenied() {
        when(documentRepository.findById(docId)).thenReturn(Optional.of(document));
        when(permissionService.hasPermission(callerId, categoryId, Permission.WRITE)).thenReturn(false);

        assertThatThrownBy(() -> documentService.updateDocument(
                docId, new UpdateDocumentRequest("New Title", null), callerId))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void updateDocument_success_updatesFields() {
        when(documentRepository.findById(docId)).thenReturn(Optional.of(document));
        when(permissionService.hasPermission(callerId, categoryId, Permission.WRITE)).thenReturn(true);
        when(documentRepository.save(any(Document.class))).thenReturn(document);

        documentService.updateDocument(docId, new UpdateDocumentRequest("Updated", "New Content"), callerId);

        assertThat(document.getTitle()).isEqualTo("Updated");
        assertThat(document.getContent()).isEqualTo("New Content");
    }

    @Test
    void updateDocument_nullFields_doesNotUpdate() {
        when(documentRepository.findById(docId)).thenReturn(Optional.of(document));
        when(permissionService.hasPermission(callerId, categoryId, Permission.WRITE)).thenReturn(true);
        when(documentRepository.save(any(Document.class))).thenReturn(document);

        documentService.updateDocument(docId, new UpdateDocumentRequest(null, null), callerId);

        assertThat(document.getTitle()).isEqualTo("Test Doc");
        assertThat(document.getContent()).isEqualTo("Hello");
    }

    // Delete document
    @Test
    void deleteDocument_noEditPermission_throwsAccessDenied() {
        when(documentRepository.findById(docId)).thenReturn(Optional.of(document));
        when(permissionService.hasPermission(callerId, categoryId, Permission.EDIT)).thenReturn(false);

        assertThatThrownBy(() -> documentService.deleteDocument(docId, callerId))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void deleteDocument_success() {
        when(documentRepository.findById(docId)).thenReturn(Optional.of(document));
        when(permissionService.hasPermission(callerId, categoryId, Permission.EDIT)).thenReturn(true);

        documentService.deleteDocument(docId, callerId);

        verify(documentRepository).deleteById(docId);
    }

    @Test
    void deleteDocument_notFound_throws() {
        when(documentRepository.findById(docId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> documentService.deleteDocument(docId, callerId))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    // Upload document
    @Test
    void uploadDocument_categoryNotFound_throws() throws Exception {
        MultipartFile file = mock(MultipartFile.class);
        when(categoryRepository.existsById(categoryId)).thenReturn(false);

        assertThatThrownBy(() -> documentService.uploadDocument(file, "Title", categoryId, callerId))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void uploadDocument_noPermission_throws() throws Exception {
        MultipartFile file = mock(MultipartFile.class);
        when(categoryRepository.existsById(categoryId)).thenReturn(true);
        when(permissionService.hasPermission(callerId, categoryId, Permission.WRITE)).thenReturn(false);

        assertThatThrownBy(() -> documentService.uploadDocument(file, "Title", categoryId, callerId))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void uploadDocument_success_withExplicitTitle() throws Exception {
        MultipartFile file = mock(MultipartFile.class);
        when(file.getBytes()).thenReturn("file content".getBytes());
        when(categoryRepository.existsById(categoryId)).thenReturn(true);
        when(permissionService.hasPermission(callerId, categoryId, Permission.WRITE)).thenReturn(true);
        when(documentRepository.save(any(Document.class))).thenAnswer(inv -> {
            Document d = inv.getArgument(0);
            d.setId(UUID.randomUUID());
            return d;
        });

        DocumentResponse result = documentService.uploadDocument(file, "My Title", categoryId, callerId);

        assertThat(result.title()).isEqualTo("My Title");
        assertThat(result.createdBy()).isEqualTo(callerId);
    }

    @Test
    void uploadDocument_blankTitle_stripsExtension() throws Exception {
        MultipartFile file = mock(MultipartFile.class);
        when(file.getBytes()).thenReturn("content".getBytes());
        when(file.getOriginalFilename()).thenReturn("report.txt");
        when(categoryRepository.existsById(categoryId)).thenReturn(true);
        when(permissionService.hasPermission(callerId, categoryId, Permission.WRITE)).thenReturn(true);
        when(documentRepository.save(any(Document.class))).thenAnswer(inv -> inv.getArgument(0));

        DocumentResponse result = documentService.uploadDocument(file, "  ", categoryId, callerId);

        assertThat(result.title()).isEqualTo("report");
    }

    @Test
    void uploadDocument_blankTitle_nullFilename_usesUntitled() throws Exception {
        MultipartFile file = mock(MultipartFile.class);
        when(file.getBytes()).thenReturn("content".getBytes());
        when(file.getOriginalFilename()).thenReturn(null);
        when(categoryRepository.existsById(categoryId)).thenReturn(true);
        when(permissionService.hasPermission(callerId, categoryId, Permission.WRITE)).thenReturn(true);
        when(documentRepository.save(any(Document.class))).thenAnswer(inv -> inv.getArgument(0));

        DocumentResponse result = documentService.uploadDocument(file, "", categoryId, callerId);

        assertThat(result.title()).isEqualTo("Untitled");
    }

    @Test
    void uploadDocument_blankTitle_blankFilename_usesUntitled() throws Exception {
        MultipartFile file = mock(MultipartFile.class);
        when(file.getBytes()).thenReturn("content".getBytes());
        when(file.getOriginalFilename()).thenReturn("  ");
        when(categoryRepository.existsById(categoryId)).thenReturn(true);
        when(permissionService.hasPermission(callerId, categoryId, Permission.WRITE)).thenReturn(true);
        when(documentRepository.save(any(Document.class))).thenAnswer(inv -> inv.getArgument(0));

        DocumentResponse result = documentService.uploadDocument(file, "", categoryId, callerId);

        assertThat(result.title()).isEqualTo("Untitled");
    }

    @Test
    void uploadDocument_blankTitle_noExtension_usesFilename() throws Exception {
        MultipartFile file = mock(MultipartFile.class);
        when(file.getBytes()).thenReturn("content".getBytes());
        when(file.getOriginalFilename()).thenReturn("nodot");
        when(categoryRepository.existsById(categoryId)).thenReturn(true);
        when(permissionService.hasPermission(callerId, categoryId, Permission.WRITE)).thenReturn(true);
        when(documentRepository.save(any(Document.class))).thenAnswer(inv -> inv.getArgument(0));

        DocumentResponse result = documentService.uploadDocument(file, "", categoryId, callerId);

        assertThat(result.title()).isEqualTo("nodot");
    }
}
