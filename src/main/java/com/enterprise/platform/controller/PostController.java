package com.enterprise.platform.controller;

import com.enterprise.platform.dto.PageResponse;
import com.enterprise.platform.dto.PostRequest;
import com.enterprise.platform.dto.PostResponse;
import com.enterprise.platform.dto.PostSummaryResponse;
import com.enterprise.platform.entity.PostType;
import com.enterprise.platform.service.PostService;

import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/posts")
public class PostController {

    private final PostService postService;

    public PostController(PostService postService) {
        this.postService = postService;
    }

    // 게시글 등록 - 공지사항(NOTICE)은 관리자, 자유게시판(BOARD)은 로그인한 회원
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public PostResponse createPost(
            @Valid @RequestBody PostRequest request,
            Authentication authentication) {

        return postService.createPost(request, authentication.getName());
    }

    // 게시글 목록 - 예) /api/posts?type=NOTICE&keyword=휴가&page=0&size=10
    @GetMapping
    public PageResponse<PostSummaryResponse> searchPosts(
            @RequestParam(required = false) PostType type,
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        return postService.searchPosts(type, keyword, page, size);
    }

    // 게시글 상세 (조회수 1 증가)
    @GetMapping("/{id}")
    public PostResponse getPost(@PathVariable Long id) {

        return postService.getPost(id);
    }

    // 게시글 수정 - 작성자 또는 관리자
    @PutMapping("/{id}")
    public PostResponse updatePost(
            @PathVariable Long id,
            @Valid @RequestBody PostRequest request,
            Authentication authentication) {

        return postService.updatePost(id, request, authentication.getName());
    }

    // 게시글 삭제 - 작성자 또는 관리자
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deletePost(
            @PathVariable Long id,
            Authentication authentication) {

        postService.deletePost(id, authentication.getName());
    }
}