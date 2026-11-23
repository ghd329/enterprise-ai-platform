package com.enterprise.platform.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;

import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    public SecurityConfig(
            JwtAuthenticationFilter jwtAuthenticationFilter) {
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http)
            throws Exception {

        http
                // CSRF 비활성화
                .csrf(csrf -> csrf.disable())

                // 기본 로그인 화면 비활성화
                .formLogin(form -> form.disable())

                // HTTP Basic 인증 비활성화
                .httpBasic(basic -> basic.disable())

                // 세션을 사용하지 않는 JWT 방식
                .sessionManagement(session ->
                        session.sessionCreationPolicy(
                                SessionCreationPolicy.STATELESS
                        )
                )

                // 토큰 없이 보호된 API 접근 시 403 대신 401 JSON 응답
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint((request, response, e) -> {
                            response.setStatus(401);
                            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
                            response.setCharacterEncoding("UTF-8");
                            response.getWriter().write(
                                    "{\"status\":401,\"message\":\"로그인이 필요합니다.\"}"
                            );
                        })
                )

                // URL별 접근 권한 설정
                .authorizeHttpRequests(auth -> auth

                        // 누구나 접근 가능
                        .requestMatchers(
                                HttpMethod.GET, "/"
                        ).permitAll()

                        .requestMatchers(
                                HttpMethod.POST, "/api/auth/login"
                        ).permitAll()

                        .requestMatchers(
                                HttpMethod.POST, "/api/users"
                        ).permitAll()

                        .requestMatchers("/error").permitAll()

                        // 나머지 API는 인증 필요
                        .anyRequest().authenticated()
                )

                // JWT 필터를 Spring Security 인증 필터 앞에 등록
                .addFilterBefore(
                        jwtAuthenticationFilter,
                        UsernamePasswordAuthenticationFilter.class
                );

        return http.build();
    }
}

