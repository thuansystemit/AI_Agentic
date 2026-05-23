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
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class DocumentService {

    private final DocumentRepository documentRepository;
    private final CategoryRepository categoryRepository;
    private final PermissionService permissionService;
    private final DocumentMapper documentMapper;

    @Transactional(readOnly = true)
    public PageResponse<DocumentResponse> listDocuments(UUID categoryId, String search, UUID callerId, Pageable pageable) {
        requirePermission(callerId, categoryId, Permission.READ);
        var page = (search == null || search.isBlank())
                ? documentRepository.findByCategoryId(categoryId, pageable).map(documentMapper::toResponse)
                : documentRepository.findByTitleContainingIgnoreCaseAndCategoryId(search, categoryId, pageable)
                        .map(documentMapper::toResponse);
        return PageResponse.from(page);
    }

    @Transactional(readOnly = true)
    public DocumentResponse getDocument(UUID documentId, UUID callerId) {
        Document doc = findOrThrow(documentId);
        requirePermission(callerId, doc.getCategoryId(), Permission.READ);
        return documentMapper.toResponse(doc);
    }

    @Transactional
    public DocumentResponse createDocument(CreateDocumentRequest request, UUID callerId) {
        if (!categoryRepository.existsById(request.categoryId())) {
            throw new ResourceNotFoundException("Category not found: " + request.categoryId());
        }
        requirePermission(callerId, request.categoryId(), Permission.WRITE);
        Document doc = new Document();
        doc.setTitle(request.title());
        doc.setContent(request.content() != null ? request.content() : "");
        doc.setCategoryId(request.categoryId());
        doc.setCreatedBy(callerId);
        return documentMapper.toResponse(documentRepository.save(doc));
    }

    @Transactional
    public DocumentResponse uploadDocument(MultipartFile file, String title, UUID categoryId, UUID callerId) throws IOException {
        if (!categoryRepository.existsById(categoryId)) {
            throw new ResourceNotFoundException("Category not found: " + categoryId);
        }
        requirePermission(callerId, categoryId, Permission.WRITE);
        String content = new String(file.getBytes(), StandardCharsets.UTF_8);
        Document doc = new Document();
        doc.setTitle(title.isBlank() ? stripExtension(file.getOriginalFilename()) : title);
        doc.setContent(content);
        doc.setCategoryId(categoryId);
        doc.setCreatedBy(callerId);
        return documentMapper.toResponse(documentRepository.save(doc));
    }

    @Transactional
    public DocumentResponse updateDocument(UUID documentId, UpdateDocumentRequest request, UUID callerId) {
        Document doc = findOrThrow(documentId);
        requirePermission(callerId, doc.getCategoryId(), Permission.WRITE);
        if (request.title() != null) doc.setTitle(request.title());
        if (request.content() != null) doc.setContent(request.content());
        return documentMapper.toResponse(documentRepository.save(doc));
    }

    @Transactional
    public void deleteDocument(UUID documentId, UUID callerId) {
        Document doc = findOrThrow(documentId);
        requirePermission(callerId, doc.getCategoryId(), Permission.EDIT);
        documentRepository.deleteById(documentId);
    }

    private void requirePermission(UUID userId, UUID categoryId, Permission required) {
        if (!permissionService.hasPermission(userId, categoryId, required)) {
            throw new AccessDeniedException("Insufficient permission for category: " + categoryId);
        }
    }

    private Document findOrThrow(UUID documentId) {
        return documentRepository.findById(documentId)
                .orElseThrow(() -> new ResourceNotFoundException("Document not found: " + documentId));
    }

    private String stripExtension(String filename) {
        if (filename == null || filename.isBlank()) return "Untitled";
        int dot = filename.lastIndexOf('.');
        return dot > 0 ? filename.substring(0, dot) : filename;
    }
}
