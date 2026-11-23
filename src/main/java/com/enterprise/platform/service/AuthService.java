package com.enterprise.platform.service;

import com.enterprise.platform.dto.LoginRequest;
import com.enterprise.platform.dto.LoginResponse;
import com.enterprise.platform.entity.User;
import com.enterprise.platform.repository.UserRepository;
import com.enterprise.platform.common.InvalidCredentialsException;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.security.crypto.password.PasswordEncoder;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public AuthService(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            JwtService jwtService) {

        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    @Transactional(readOnly = true)
    public LoginResponse login(LoginRequest request) {

        // 이메일로 회원 조회
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() ->
                        new InvalidCredentialsException(
                                "이메일 또는 비밀번호가 올바르지 않습니다."
                        )
                );

        // 기존 비밀번호가 없는 회원은 로그인 거부
        if (user.getPassword() == null ||
                !passwordEncoder.matches(
                        request.getPassword(),
                        user.getPassword()
                )) {

            throw new InvalidCredentialsException(
                    "이메일 또는 비밀번호가 올바르지 않습니다."
            );
        }

        // 로그인 성공 시 JWT 발급
        String token = jwtService.generateToken(user);

        return new LoginResponse(user, token);
    }
}