package com.enterprise.platform.repository;

import com.enterprise.platform.entity.Document;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface DocumentRepository extends JpaRepository<Document, Long> {

    // 문서 목록 검색 (부서, 분류, 제목 키워드는 모두 선택 조건)
    // 작성자/부서를 함께 조회해 N+1 문제 방지
    @Query("""
            select d from Document d
            join fetch d.author
            left join fetch d.department dep
            where (:departmentId is null or dep.id = :departmentId)
              and (:category is null or d.category = :category)
              and (:keyword is null or d.title like concat('%', :keyword, '%'))
            order by d.createdAt desc
            """)
    List<Document> search(
            @Param("departmentId") Long departmentId,
            @Param("category") String category,
            @Param("keyword") String keyword);

    boolean existsByAuthor_Id(Long authorId);

    boolean existsByDepartment_Id(Long departmentId);
}
