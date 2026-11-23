package com.enterprise.platform.controller;

import com.enterprise.platform.dto.DocumentRequest;
import com.enterprise.platform.dto.DocumentResponse;
import com.enterprise.platform.service.DocumentService;

import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/documents")
public class DocumentController {

    private final DocumentService documentService;

    public DocumentController(DocumentService documentService) {
        this.documentService = documentService;
    }

    // 문서 등록 - 로그인한 회원
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public DocumentResponse createDocument(
            @Valid @RequestBody DocumentRequest request,
            Authentication authentication) {

        return documentService.createDocument(request, authentication.getName());
    }

    // 문서 목록 조회 - 예) /api/documents?departmentId=1&category=인사규정&keyword=휴가
    @GetMapping
    public List<DocumentResponse> searchDocuments(
            @RequestParam(required = false) Long departmentId,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String keyword) {

        return documentService.searchDocuments(departmentId, category, keyword);
    }

    // 문서 상세 조회
    @GetMapping("/{id}")
    public DocumentResponse getDocumentById(@PathVariable Long id) {

        return documentService.getDocumentById(id);
    }

    // 문서 수정 - 작성자 또는 관리자
    @PutMapping("/{id}")
    public DocumentResponse updateDocument(
            @PathVariable Long id,
            @Valid @RequestBody DocumentRequest request,
            Authentication authentication) {

        return documentService.updateDocument(id, request, authentication.getName());
    }

    // 문서 삭제 - 작성자 또는 관리자
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteDocument(
            @PathVariable Long id,
            Authentication authentication) {

        documentService.deleteDocument(id, authentication.getName());
    }
}
