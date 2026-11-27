package com.enterprise.platform.repository;

import com.enterprise.platform.entity.Post;
import com.enterprise.platform.entity.PostType;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PostRepository extends JpaRepository<Post, Long> {

    // 게시글 목록 (종류, 제목 키워드는 선택 조건)
    // 상단 고정 글 먼저, 그다음 최신순
    @Query(value = """
            select p from Post p
            join fetch p.author
            where (:type is null or p.type = :type)
              and (:keyword is null or p.title like concat('%', :keyword, '%'))
            order by p.pinned desc, p.createdAt desc
            """,
            countQuery = """
            select count(p) from Post p
            where (:type is null or p.type = :type)
              and (:keyword is null or p.title like concat('%', :keyword, '%'))
            """)
    Page<Post> search(
            @Param("type") PostType type,
            @Param("keyword") String keyword,
            Pageable pageable);

    // 조회수 1 증가 (DB에서 바로 +1 하므로 동시에 조회해도 숫자가 누락되지 않음)
    @Modifying(clearAutomatically = true)
    @Query("update Post p set p.viewCount = p.viewCount + 1 where p.id = :id")
    int increaseViewCount(@Param("id") Long id);

    boolean existsByAuthor_Id(Long authorId);
}