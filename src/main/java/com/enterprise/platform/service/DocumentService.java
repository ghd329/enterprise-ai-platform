package com.enterprise.platform.service;

import com.enterprise.platform.common.ForbiddenException;
import com.enterprise.platform.common.ResourceNotFoundException;
import com.enterprise.platform.dto.DocumentRequest;
import com.enterprise.platform.dto.DocumentResponse;
import com.enterprise.platform.entity.Department;
import com.enterprise.platform.entity.Document;
import com.enterprise.platform.entity.User;
import com.enterprise.platform.repository.DepartmentRepository;
import com.enterprise.platform.repository.DocumentRepository;
import com.enterprise.platform.repository.UserRepository;

import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
public class DocumentService {

    private final DocumentRepository documentRepository;
    private final UserRepository userRepository;
    private final DepartmentRepository departmentRepository;
    private final PdfTextExtractor pdfTextExtractor;
    private final DocumentChunkService documentChunkService;

    public DocumentService(
            DocumentRepository documentRepository,
            UserRepository userRepository,
            DepartmentRepository departmentRepository,
            PdfTextExtractor pdfTextExtractor,
            DocumentChunkService documentChunkService) {

        this.documentRepository = documentRepository;
        this.userRepository = userRepository;
        this.departmentRepository = departmentRepository;
        this.pdfTextExtractor = pdfTextExtractor;
        this.documentChunkService = documentChunkService;
    }

    // 문서 등록 - 로그인한 회원이 작성자가 됨
    public DocumentResponse createDocument(
            DocumentRequest request, String currentEmail) {

        User author = findCurrentUser(currentEmail);

        Document document = new Document();
        document.setAuthor(author);
        applyRequest(document, request);

        // 부서를 생략하면 작성자의 소속 부서로 등록
        document.setDepartment(request.getDepartmentId() == null
                ? author.getDepartment()
                : resolveDepartment(request.getDepartmentId(), author));

        return new DocumentResponse(documentRepository.save(document));
    }

    // PDF 업로드 및 텍스트 추출 후 문서 저장
    public DocumentResponse uploadPdf(
            MultipartFile file,
            String title,
            String category,
            Long departmentId,
            String currentEmail) {

        User author = findCurrentUser(currentEmail);

        // PDF 텍스트 추출
        String extractedText = pdfTextExtractor.extractText(file);

        // 문서 생성
        Document document = new Document();

        document.setAuthor(author);

        // 제목을 입력하지 않으면 PDF 파일명 사용
        String documentTitle = blankToNull(title);

        if (documentTitle == null) {
            documentTitle = file.getOriginalFilename();
        }

        if (documentTitle == null || documentTitle.isBlank()) {
            documentTitle = "업로드 문서";
        }

        // 제목 길이 제한
        if (documentTitle.length() > 200) {
            throw new ResponseStatusException(
                    org.springframework.http.HttpStatus.BAD_REQUEST,
                    "문서 제목은 200자 이내로 입력해주세요."
            );
        }

        document.setTitle(documentTitle);
        document.setContent(extractedText);
        document.setCategory(blankToNull(category));

        // 부서 미지정 시 작성자의 소속 부서 사용
        document.setDepartment(
                departmentId == null
                        ? author.getDepartment()
                        : resolveDepartment(departmentId, author)
        );

        // 원본 문서를 먼저 DB에 저장
        Document savedDocument = documentRepository.save(document);

        // 저장된 문서 내용을 청크로 분할하고 DB에 저장
        documentChunkService.createChunks(savedDocument);

        // 응답 반환
        return new DocumentResponse(savedDocument);
    }

    // 문서 목록 조회 (부서/분류/제목 검색)
    @Transactional(readOnly = true)
    public List<DocumentResponse> searchDocuments(
            Long departmentId, String category, String keyword) {

        return documentRepository.search(
                        departmentId,
                        blankToNull(category),
                        blankToNull(keyword))
                .stream()
                .map(DocumentResponse::new)
                .toList();
    }

    // 문서 상세 조회
    @Transactional(readOnly = true)
    public DocumentResponse getDocumentById(Long id) {

        return new DocumentResponse(findDocument(id));
    }

    // 문서 수정 - 작성자 또는 관리자
    public DocumentResponse updateDocument(
            Long id, DocumentRequest request, String currentEmail) {

        User currentUser = findCurrentUser(currentEmail);
        Document document = findDocument(id);

        checkOwnerOrAdmin(document, currentUser);
        applyRequest(document, request);

        // 부서를 생략하면 기존 소속 부서 유지
        if (request.getDepartmentId() != null) {
            document.setDepartment(
                    resolveDepartment(request.getDepartmentId(), currentUser));
        }

        // 즉시 반영해야 @PreUpdate 로 갱신된 updatedAt 이 응답에 포함됨
        documentRepository.flush();

        return new DocumentResponse(document);
    }

    // 문서 삭제 - 작성자 또는 관리자
    public void deleteDocument(Long id, String currentEmail) {

        User currentUser = findCurrentUser(currentEmail);
        Document document = findDocument(id);

        checkOwnerOrAdmin(document, currentUser);

        documentRepository.delete(document);
    }

    // 요청 값(제목, 본문, 분류)을 문서에 반영
    private void applyRequest(Document document, DocumentRequest request) {

        document.setTitle(request.getTitle());
        document.setContent(request.getContent());
        document.setCategory(blankToNull(request.getCategory()));
    }

    // 지정한 부서로 문서를 둘 수 있는지 확인
    // - 관리자: 모든 부서 지정 가능
    // - 일반 회원: 자신의 소속 부서만 지정 가능
    private Department resolveDepartment(Long departmentId, User currentUser) {

        Department department = departmentRepository.findById(departmentId)
                .orElseThrow(() ->
                        new ResourceNotFoundException("부서를 찾을 수 없습니다."));

        if (!isAdmin(currentUser)
                && (currentUser.getDepartment() == null
                || !currentUser.getDepartment().getId().equals(departmentId))) {
            throw new ForbiddenException("소속 부서의 문서만 등록할 수 있습니다.");
        }

        return department;
    }

    private void checkOwnerOrAdmin(Document document, User currentUser) {

        if (isAdmin(currentUser)) {
            return;
        }

        if (!document.getAuthor().getId().equals(currentUser.getId())) {
            throw new ForbiddenException("작성자 또는 관리자만 문서를 수정·삭제할 수 있습니다.");
        }
    }

    private boolean isAdmin(User user) {
        return "ADMIN".equals(user.getRole());
    }

    private User findCurrentUser(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() ->
                        new ResourceNotFoundException("회원을 찾을 수 없습니다."));
    }

    private Document findDocument(Long id) {
        return documentRepository.findById(id)
                .orElseThrow(() ->
                        new ResourceNotFoundException("문서를 찾을 수 없습니다."));
    }

    private String blankToNull(String value) {
        return (value == null || value.isBlank()) ? null : value.trim();
    }
}
