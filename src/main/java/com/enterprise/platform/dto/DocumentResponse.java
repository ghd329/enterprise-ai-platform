package com.enterprise.platform.dto;

import com.enterprise.platform.entity.Document;

import java.time.LocalDateTime;

public class DocumentResponse {

    private Long id;
    private String title;
    private String content;
    private String category;
    private Long authorId;
    private String authorName;
    private Long departmentId;
    private String departmentName;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public DocumentResponse(Document document) {
        this.id = document.getId();
        this.title = document.getTitle();
        this.content = document.getContent();
        this.category = document.getCategory();
        this.authorId = document.getAuthor().getId();
        this.authorName = document.getAuthor().getName();
        this.createdAt = document.getCreatedAt();
        this.updatedAt = document.getUpdatedAt();

        if (document.getDepartment() != null) {
            this.departmentId = document.getDepartment().getId();
            this.departmentName = document.getDepartment().getName();
        }
    }

    public Long getId() { return id; }
    public String getTitle() { return title; }
    public String getContent() { return content; }
    public String getCategory() { return category; }
    public Long getAuthorId() { return authorId; }
    public String getAuthorName() { return authorName; }
    public Long getDepartmentId() { return departmentId; }
    public String getDepartmentName() { return departmentName; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
}
