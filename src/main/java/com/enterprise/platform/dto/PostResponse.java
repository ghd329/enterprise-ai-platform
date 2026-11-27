package com.enterprise.platform.dto;

import com.enterprise.platform.entity.Post;
import com.enterprise.platform.entity.PostType;

import java.time.LocalDateTime;

// 게시글 상세 응답 (본문 포함)
public class PostResponse {

    private Long id;
    private PostType type;
    private String title;
    private String content;
    private boolean pinned;
    private long viewCount;
    private Long authorId;
    private String authorName;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public PostResponse(Post post) {
        this.id = post.getId();
        this.type = post.getType();
        this.title = post.getTitle();
        this.content = post.getContent();
        this.pinned = post.isPinned();
        this.viewCount = post.getViewCount();
        this.authorId = post.getAuthor().getId();
        this.authorName = post.getAuthor().getName();
        this.createdAt = post.getCreatedAt();
        this.updatedAt = post.getUpdatedAt();
    }

    public Long getId() { return id; }
    public PostType getType() { return type; }
    public String getTitle() { return title; }
    public String getContent() { return content; }
    public boolean isPinned() { return pinned; }
    public long getViewCount() { return viewCount; }
    public Long getAuthorId() { return authorId; }
    public String getAuthorName() { return authorName; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
}