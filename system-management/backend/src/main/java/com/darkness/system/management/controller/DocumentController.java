package com.darkness.system.management.controller;

import com.darkness.system.management.dto.request.CreateDocumentRequest;
import com.darkness.system.management.dto.request.UpdateDocumentRequest;
import com.darkness.system.management.dto.response.DocumentResponse;
import com.darkness.system.management.dto.response.PageResponse;
import com.darkness.system.management.security.UserPrincipal;
import com.darkness.system.management.service.DocumentService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/documents")
public class DocumentController {

    private final DocumentService documentService;

    public DocumentController(DocumentService documentService) {
        this.documentService = documentService;
    }

    @GetMapping
    ResponseEntity<PageResponse<DocumentResponse>> listDocuments(
            @RequestParam UUID categoryId,
            @RequestParam(required = false) String search,
            @AuthenticationPrincipal UserPrincipal principal,
            Pageable pageable) {
        return ResponseEntity.ok(
                documentService.listDocuments(categoryId, search, principal.userId(), pageable));
    }

    @GetMapping("/{documentId}")
    ResponseEntity<DocumentResponse> getDocument(@PathVariable UUID documentId,
                                                  @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(documentService.getDocument(documentId, principal.userId()));
    }

    @PostMapping
    ResponseEntity<DocumentResponse> createDocument(@Valid @RequestBody CreateDocumentRequest request,
                                                     @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(documentService.createDocument(request, principal.userId()));
    }

    @PostMapping("/upload")
    ResponseEntity<DocumentResponse> uploadDocument(
            @RequestParam("file") MultipartFile file,
            @RequestParam("title") String title,
            @RequestParam("categoryId") UUID categoryId,
            @AuthenticationPrincipal UserPrincipal principal) throws IOException {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(documentService.uploadDocument(file, title, categoryId, principal.userId()));
    }

    @PatchMapping("/{documentId}")
    ResponseEntity<DocumentResponse> updateDocument(@PathVariable UUID documentId,
                                                     @Valid @RequestBody UpdateDocumentRequest request,
                                                     @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(documentService.updateDocument(documentId, request, principal.userId()));
    }

    @DeleteMapping("/{documentId}")
    ResponseEntity<Void> deleteDocument(@PathVariable UUID documentId,
                                        @AuthenticationPrincipal UserPrincipal principal) {
        documentService.deleteDocument(documentId, principal.userId());
        return ResponseEntity.noContent().build();
    }
}
