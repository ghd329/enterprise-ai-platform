package com.enterprise.platform.dto;

import com.enterprise.platform.entity.PostType;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

// 게시글 등록/수정 요청
public class PostRequest {

    // 등록 시에만 사용 (수정 시에는 무시되어 종류를 바꿀 수 없음)
    @NotNull(message = "게시글 종류(NOTICE 또는 BOARD)는 필수 입력값입니다.")
    private PostType type;

    @NotBlank(message = "제목은 필수 입력값입니다.")
    @Size(max = 200, message = "제목은 200자 이하여야 합니다.")
    private String title;

    @NotBlank(message = "내용은 필수 입력값입니다.")
    @Size(max = 60000, message = "내용은 60000자 이하여야 합니다.")
    private String content;

    // 공지사항 상단 고정 여부 (자유게시판은 무시)
    private boolean pinned;

    public PostRequest() {}

    public PostType getType() { return type; }
    public String getTitle() { return title; }
    public String getContent() { return content; }
    public boolean isPinned() { return pinned; }

    public void setType(PostType type) { this.type = type; }
    public void setTitle(String title) { this.title = title; }
    public void setContent(String content) { this.content = content; }
    public void setPinned(boolean pinned) { this.pinned = pinned; }
}