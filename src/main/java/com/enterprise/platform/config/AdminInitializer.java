package com.enterprise.platform.config;

import com.enterprise.platform.entity.User;
import com.enterprise.platform.repository.UserRepository;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

// 서버 시작 시 초기 관리자 계정이 없으면 생성
// 계정 정보는 application.properties 의 app.admin.* (환경변수로 변경 가능)
@Component
public class AdminInitializer implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(AdminInitializer.class);

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.admin.enabled:true}")
    private boolean enabled;

    @Value("${app.admin.name:관리자}")
    private String name;

    @Value("${app.admin.email}")
    private String email;

    @Value("${app.admin.password}")
    private String password;

    public AdminInitializer(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {

        if (!enabled) {
            return;
        }

        userRepository.findByEmail(email).ifPresentOrElse(
                existing -> {
                    // 이미 있는 계정의 권한을 몰래 올리지 않고 경고만 남김
                    if (!"ADMIN".equals(existing.getRole())) {
                        log.warn("초기 관리자 이메일({})이 일반 회원으로 이미 등록되어 있습니다. "
                                + "DB에서 role 을 ADMIN 으로 변경하거나 app.admin.email 을 바꾸세요.", email);
                    }
                },
                () -> {
                    User admin = new User();
                    admin.setName(name);
                    admin.setEmail(email);
                    admin.setPassword(passwordEncoder.encode(password));
                    admin.setRole("ADMIN");
                    userRepository.save(admin);

                    log.info("초기 관리자 계정을 생성했습니다: {}", email);
                }
        );
    }
}
