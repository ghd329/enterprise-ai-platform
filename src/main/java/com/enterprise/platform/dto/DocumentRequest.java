package com.enterprise.platform.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

// 문서 등록/수정 요청
public class DocumentRequest {

    @NotBlank(message = "문서 제목은 필수 입력값입니다.")
    @Size(max = 200, message = "문서 제목은 200자 이하여야 합니다.")
    private String title;

    @Size(max = 60000, message = "문서 본문은 60000자 이하여야 합니다.")
    private String content;

    @Size(max = 50, message = "문서 분류는 50자 이하여야 합니다.")
    private String category;

    // 등록 시 생략하면 작성자의 소속 부서, 수정 시 생략하면 기존 부서 유지
    // 일반 회원은 자신의 부서만, 관리자는 모든 부서 지정 가능
    private Long departmentId;

    public DocumentRequest() {}

    public String getTitle() { return title; }
    public String getContent() { return content; }
    public String getCategory() { return category; }
    public Long getDepartmentId() { return departmentId; }

    public void setTitle(String title) { this.title = title; }
    public void setContent(String content) { this.content = content; }
    public void setCategory(String category) { this.category = category; }
    public void setDepartmentId(Long departmentId) { this.departmentId = departmentId; }
}
