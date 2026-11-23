package com.enterprise.platform.config;

import com.enterprise.platform.entity.User;
import com.enterprise.platform.repository.UserRepository;
import com.enterprise.platform.service.JwtService;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final UserRepository userRepository;

    public JwtAuthenticationFilter(
            JwtService jwtService,
            UserRepository userRepository) {

        this.jwtService = jwtService;
        this.userRepository = userRepository;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {

        String authorization = request.getHeader("Authorization");

        if (authorization == null ||
                !authorization.startsWith("Bearer ")) {

            filterChain.doFilter(request, response);
            return;
        }

        String token = authorization.substring(7);

        try {
            // JWT 서명 및 만료 검증
            Claims claims = jwtService.validateToken(token);

            // JWT에서 이메일 추출
            String email = claims.getSubject();

            if (email == null || email.isBlank()) {
                throw new IllegalArgumentException("잘못된 토큰입니다.");
            }

            // DB에서 최신 회원 정보 조회
            User user = userRepository.findByEmail(email)
                    .orElseThrow(() ->
                            new IllegalArgumentException("회원 정보가 없습니다.")
                    );

            // DB에 저장된 역할 사용
            String role = user.getRole();

            if (!"USER".equals(role) && !"ADMIN".equals(role)) {
                throw new IllegalArgumentException("잘못된 회원 권한입니다.");
            }

            // Spring Security 인증 정보 생성
            UsernamePasswordAuthenticationToken authentication =
                    new UsernamePasswordAuthenticationToken(
                            user.getEmail(),
                            null,
                            List.of(
                                    new SimpleGrantedAuthority("ROLE_" + role)
                            )
                    );

            SecurityContextHolder.getContext()
                    .setAuthentication(authentication);

        } catch (JwtException | IllegalArgumentException e) {

            SecurityContextHolder.clearContext();

            response.sendError(
                    HttpServletResponse.SC_UNAUTHORIZED,
                    "유효하지 않거나 만료된 토큰입니다."
            );

            return;
        }

        filterChain.doFilter(request, response);
    }
}