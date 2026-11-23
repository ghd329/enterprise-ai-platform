package com.enterprise.platform.repository;

import com.enterprise.platform.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByEmail(String email);

    boolean existsByEmail(String email);

    boolean existsByDepartment_Id(Long departmentId);

    // 특정 부서에 소속된 회원 목록
    List<User> findByDepartment_IdOrderByNameAsc(Long departmentId);
}
