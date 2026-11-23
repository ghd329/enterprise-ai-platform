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

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
public class DocumentService {

    private final DocumentRepository documentRepository;
    private final UserRepository userRepository;
    private final DepartmentRepository departmentRepository;

    public DocumentService(
            DocumentRepository documentRepository,
            UserRepository userRepository,
            DepartmentRepository departmentRepository) {
        this.documentRepository = documentRepository;
        this.userRepository = userRepository;
        this.departmentRepository = departmentRepository;
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
