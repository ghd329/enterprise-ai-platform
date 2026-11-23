package com.enterprise.platform.service;

import com.enterprise.platform.dto.UserRequest;
import com.enterprise.platform.dto.UserResponse;
import com.enterprise.platform.entity.User;
import com.enterprise.platform.repository.UserRepository;
import com.enterprise.platform.common.ResourceNotFoundException;
import com.enterprise.platform.common.DuplicateResourceException;
import com.enterprise.platform.dto.UserUpdateRequest;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public UserService(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    // 회원 등록
    @Transactional
    public UserResponse createUser(UserRequest request) {

        if (request.getName() == null || request.getName().isBlank()
                || request.getEmail() == null || request.getEmail().isBlank()) {
            throw new IllegalArgumentException("이름과 이메일을 입력해야 합니다.");
        }

        if (userRepository.existsByEmail(request.getEmail())) {
            throw new DuplicateResourceException("이미 등록된 이메일입니다.");
        }

        User user = new User();
        user.setName(request.getName());
        user.setEmail(request.getEmail());

        // 비밀번호 BCrypt 암호화
        user.setPassword(passwordEncoder.encode(request.getPassword()));

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
                        new ResourceNotFoundException("회원을 찾을 수 없습니다."));

        return new UserResponse(user);
    }

    // 회원 정보 수정
    @Transactional
    public UserResponse updateUser(Long id, UserUpdateRequest request) {

        User user = userRepository.findById(id)
                .orElseThrow(() ->
                        new ResourceNotFoundException("회원을 찾을 수 없습니다."));

        if (userRepository.existsByEmail(request.getEmail())
                && !user.getEmail().equals(request.getEmail())) {
            throw new DuplicateResourceException("이미 등록된 이메일입니다.");
        }

        user.setName(request.getName());
        user.setEmail(request.getEmail());

        // 비밀번호를 입력한 경우에만 변경
        if (request.getPassword() != null
                && !request.getPassword().isBlank()) {
            user.setPassword(passwordEncoder.encode(request.getPassword()));
        }

        return new UserResponse(user);
    }

    // 회원 삭제
    @Transactional
    public void deleteUser(Long id) {

        User user = userRepository.findById(id)
                .orElseThrow(() ->
                        new ResourceNotFoundException("회원을 찾을 수 없습니다."));

        userRepository.delete(user);
    }
}
