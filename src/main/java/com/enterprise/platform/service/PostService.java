package com.enterprise.platform.service;

import com.enterprise.platform.common.ForbiddenException;
import com.enterprise.platform.common.ResourceNotFoundException;
import com.enterprise.platform.dto.PageResponse;
import com.enterprise.platform.dto.PostRequest;
import com.enterprise.platform.dto.PostResponse;
import com.enterprise.platform.dto.PostSummaryResponse;
import com.enterprise.platform.entity.Post;
import com.enterprise.platform.entity.PostType;
import com.enterprise.platform.entity.User;
import com.enterprise.platform.repository.PostRepository;
import com.enterprise.platform.repository.UserRepository;

import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class PostService {

    private static final int MAX_PAGE_SIZE = 50;

    private final PostRepository postRepository;
    private final UserRepository userRepository;

    public PostService(
            PostRepository postRepository,
            UserRepository userRepository) {
        this.postRepository = postRepository;
        this.userRepository = userRepository;
    }

    // 게시글 등록
    public PostResponse createPost(PostRequest request, String currentEmail) {

        User author = findCurrentUser(currentEmail);

        // 공지사항은 관리자만 작성 가능
        if (request.getType() == PostType.NOTICE && !isAdmin(author)) {
            throw new ForbiddenException("공지사항은 관리자만 작성할 수 있습니다.");
        }

        Post post = new Post();
        post.setType(request.getType());
        post.setAuthor(author);
        applyRequest(post, request);

        return new PostResponse(postRepository.save(post));
    }

    // 게시글 목록 (페이지 단위)
    @Transactional(readOnly = true)
    public PageResponse<PostSummaryResponse> searchPosts(
            PostType type, String keyword, int page, int size) {

        int safePage = Math.max(page, 0);
        int safeSize = Math.min(Math.max(size, 1), MAX_PAGE_SIZE);

        return new PageResponse<>(
                postRepository.search(type, blankToNull(keyword),
                        PageRequest.of(safePage, safeSize)),
                PostSummaryResponse::new);
    }

    // 게시글 상세 (조회수 증가)
    public PostResponse getPost(Long id) {

        if (postRepository.increaseViewCount(id) == 0) {
            throw new ResourceNotFoundException("게시글을 찾을 수 없습니다.");
        }

        return new PostResponse(findPost(id));
    }

    // 게시글 수정 - 작성자 또는 관리자
    public PostResponse updatePost(
            Long id, PostRequest request, String currentEmail) {

        User currentUser = findCurrentUser(currentEmail);
        Post post = findPost(id);

        checkOwnerOrAdmin(post, currentUser);
        applyRequest(post, request);

        // 즉시 반영해야 @PreUpdate 로 갱신된 updatedAt 이 응답에 포함됨
        postRepository.flush();

        return new PostResponse(post);
    }

    // 게시글 삭제 - 작성자 또는 관리자
    public void deletePost(Long id, String currentEmail) {

        User currentUser = findCurrentUser(currentEmail);
        Post post = findPost(id);

        checkOwnerOrAdmin(post, currentUser);

        postRepository.delete(post);
    }

    // 제목, 내용, 상단 고정 반영 (종류는 바꾸지 않음)
    private void applyRequest(Post post, PostRequest request) {

        post.setTitle(request.getTitle());
        post.setContent(request.getContent());

        // 상단 고정은 공지사항에만 적용
        post.setPinned(post.getType() == PostType.NOTICE && request.isPinned());
    }

    private void checkOwnerOrAdmin(Post post, User currentUser) {

        if (isAdmin(currentUser)) {
            return;
        }

        if (!post.getAuthor().getId().equals(currentUser.getId())) {
            throw new ForbiddenException("작성자 또는 관리자만 게시글을 수정·삭제할 수 있습니다.");
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

    private Post findPost(Long id) {
        return postRepository.findById(id)
                .orElseThrow(() ->
                        new ResourceNotFoundException("게시글을 찾을 수 없습니다."));
    }

    private String blankToNull(String value) {
        return (value == null || value.isBlank()) ? null : value.trim();
    }
}