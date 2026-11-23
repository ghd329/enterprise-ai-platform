package com.enterprise.platform.config;

import com.enterprise.platform.entity.User;
import com.enterprise.platform.repository.UserRepository;

import org.springframework.stereotype.Component;

@Component
public class UserAuthorization {

    private final UserRepository userRepository;

    public UserAuthorization(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    // 본인 또는 관리자 여부 확인
    public boolean isOwnerOrAdmin(Long userId, String email) {

        User currentUser = userRepository.findByEmail(email)
                .orElse(null);

        if (currentUser == null) {
            return false;
        }

        // 관리자라면 모든 회원에 접근 가능
        if ("ADMIN".equals(currentUser.getRole())) {
            return true;
        }

        // 일반 사용자는 자신의 회원 정보만 접근 가능
        return currentUser.getId().equals(userId);
    }
}