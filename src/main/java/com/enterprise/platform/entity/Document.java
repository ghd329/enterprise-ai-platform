package com.enterprise.platform.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "documents")
public class Document {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 200)
    private String title;

    // 문서 본문 (5단계에서 PDF 추출 텍스트도 이 필드 또는 별도 테이블에 저장 예정)
    // MySQL TEXT(64KB)는 한글 약 2만 자라서 MEDIUMTEXT(16MB) 사용
    @Column(columnDefinition = "MEDIUMTEXT")
    private String content;

    // 문서 분류 (예: 인사규정, 업무매뉴얼, 재무)
    @Column(length = 50)
    private String category;

    // 문서 작성자
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "author_id", nullable = false)
    private User author;

    // 문서 소속 부서 (null 이면 전사 공통 문서)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "department_id")
    private Department department;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    public Document() {}

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = createdAt;
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public String getTitle() { return title; }
    public String getContent() { return content; }
    public String getCategory() { return category; }
    public User getAuthor() { return author; }
    public Department getDepartment() { return department; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }

    public void setTitle(String title) { this.title = title; }
    public void setContent(String content) { this.content = content; }
    public void setCategory(String category) { this.category = category; }
    public void setAuthor(User author) { this.author = author; }
    public void setDepartment(Department department) { this.department = department; }
}
