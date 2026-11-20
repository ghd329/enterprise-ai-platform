package com.enterprise.platform.service;

import com.enterprise.platform.dto.UserRequest;
import com.enterprise.platform.dto.UserResponse;
import com.enterprise.platform.entity.User;
import com.enterprise.platform.repository.UserRepository;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class UserService {

    private final UserRepository userRepository;

    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    // 회원 등록
    @Transactional
    public UserResponse createUser(UserRequest request) {

        if (request.getName() == null || request.getName().isBlank()
                || request.getEmail() == null || request.getEmail().isBlank()) {
            throw new IllegalArgumentException("이름과 이메일을 입력해야 합니다.");
        }

        if (userRepository.existsByEmail(request.getEmail())) {
            throw new IllegalArgumentException("이미 등록된 이메일입니다.");
        }

        User user = new User();
        user.setName(request.getName());
        user.setEmail(request.getEmail());
        user.setRole("USER");

        User savedUser = userRepository.save(user);

        return new UserResponse(savedUser);
    }

    // 전체 회원 조회
    @Transactional(readOnly = true)
    public List<UserResponse> getAllUsers() {

        return userRepository.findAll()
                .stream()
                .map(UserResponse::new)
                .toList();
    }

    // 회원 상세 조회
    @Transactional(readOnly = true)
    public UserResponse getUserById(Long id) {

        User user = userRepository.findById(id)
                .orElseThrow(() ->
                        new IllegalArgumentException("회원을 찾을 수 없습니다."));

        return new UserResponse(user);
    }

    // 회원 정보 수정
    @Transactional
    public UserResponse updateUser(Long id, UserRequest request) {

        User user = userRepository.findById(id)
                .orElseThrow(() ->
                        new IllegalArgumentException("회원을 찾을 수 없습니다."));

        if (request.getName() == null || request.getName().isBlank()
                || request.getEmail() == null || request.getEmail().isBlank()) {
            throw new IllegalArgumentException("이름과 이메일을 입력해야 합니다.");
        }

        if (!user.getEmail().equals(request.getEmail())
                && userRepository.existsByEmail(request.getEmail())) {
            throw new IllegalArgumentException("이미 등록된 이메일입니다.");
        }

        user.setName(request.getName());
        user.setEmail(request.getEmail());

        return new UserResponse(user);
    }

    // 회원 삭제
    @Transactional
    public void deleteUser(Long id) {

        User user = userRepository.findById(id)
                .orElseThrow(() ->
                        new IllegalArgumentException("회원을 찾을 수 없습니다."));

        userRepository.delete(user);
    }
}
