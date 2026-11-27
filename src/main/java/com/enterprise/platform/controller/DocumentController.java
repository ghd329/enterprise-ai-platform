package com.enterprise.platform.controller;

import com.enterprise.platform.dto.DocumentRequest;
import com.enterprise.platform.dto.DocumentResponse;
import com.enterprise.platform.service.DocumentService;
import com.enterprise.platform.service.DocumentChunkService;
import com.enterprise.platform.dto.RagChatResponse;
import com.enterprise.platform.dto.RagQuestionRequest;
import com.enterprise.platform.service.AiChatService;
import com.enterprise.platform.dto.MultiDocumentRagQuestionRequest;
import com.enterprise.platform.dto.MultiDocumentRagChatResponse;
import com.enterprise.platform.dto.DocumentSearchRequest;
import com.enterprise.platform.dto.DocumentSearchResult;
import java.util.Map;

import jakarta.validation.Valid;

import org.springframework.http.MediaType;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/documents")
public class DocumentController {

    private final DocumentService documentService;
    private final DocumentChunkService documentChunkService;
    private final AiChatService aiChatService;

    public DocumentController(
            DocumentService documentService,
            DocumentChunkService documentChunkService,
            AiChatService aiChatService) {

        this.documentService = documentService;
        this.documentChunkService = documentChunkService;
        this.aiChatService = aiChatService;
    }

    // PDF 업로드
    @PostMapping(
            value = "/upload",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE
    )
    @ResponseStatus(HttpStatus.CREATED)
    public DocumentResponse uploadPdf(

            @RequestParam("file") MultipartFile file,

            @RequestParam(required = false) String title,

            @RequestParam(required = false) String category,

            @RequestParam(required = false) Long departmentId,

            Authentication authentication) {

        return documentService.uploadPdf(
                file,
                title,
                category,
                departmentId,
                authentication.getName()
        );
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

    // 문서 청크 임베딩 생성 및 저장
    @PostMapping("/{id}/embeddings")
    public Map<String, Object> createEmbeddings(@PathVariable Long id) {

        int count = documentChunkService.createEmbeddings(id);

        return Map.of(
                "message", "문서 임베딩 생성 및 저장이 완료되었습니다.",
                "documentId", id,
                "chunkCount", count
        );
    }

    // 문서 유사도 검색
    @PostMapping("/{id}/search")
    public List<DocumentSearchResult> searchSimilarChunks(
            @PathVariable Long id,
            @RequestBody DocumentSearchRequest request) {

        int topK = request.topK() == null ? 3 : request.topK();

        return documentChunkService.searchSimilarChunksWithScore(
                id,
                request.question(),
                topK
        );
    }

    // 문서 기반 RAG 질문 및 답변
    @PostMapping("/{id}/rag-chat")
    public RagChatResponse ragChat(
            @PathVariable Long id,
            @RequestBody RagQuestionRequest request) {

        int topK = request.topK() == null
                ? 3
                : request.topK();

        return documentChunkService.generateRagAnswer(
                id,
                request.question(),
                topK
        );
    }

    @PostMapping("/rag-chat")
    public MultiDocumentRagChatResponse multiDocumentRagChat(
            @RequestBody MultiDocumentRagQuestionRequest request) {

        int topK = request.topK() == null
                ? 5
                : request.topK();

        return documentChunkService.generateMultiDocumentRagAnswer(
                request.documentIds(),
                request.question(),
                topK
        );
    }

}
