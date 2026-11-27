package com.enterprise.platform.dto;

import com.enterprise.platform.entity.Post;
import com.enterprise.platform.entity.PostType;

import java.time.LocalDateTime;

// 게시글 목록용 응답 (본문 제외)
public class PostSummaryResponse {

    private Long id;
    private PostType type;
    private String title;
    private boolean pinned;
    private long viewCount;
    private String authorName;
    private LocalDateTime createdAt;

    public PostSummaryResponse(Post post) {
        this.id = post.getId();
        this.type = post.getType();
        this.title = post.getTitle();
        this.pinned = post.isPinned();
        this.viewCount = post.getViewCount();
        this.authorName = post.getAuthor().getName();
        this.createdAt = post.getCreatedAt();
    }

    public Long getId() { return id; }
    public PostType getType() { return type; }
    public String getTitle() { return title; }
    public boolean isPinned() { return pinned; }
    public long getViewCount() { return viewCount; }
    public String getAuthorName() { return authorName; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}