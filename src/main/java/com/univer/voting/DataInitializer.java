package com.univer.voting;

import com.univer.voting.enums.RegistrationType;
import com.univer.voting.enums.UserRole;
import com.univer.voting.models.Users;
import com.univer.voting.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class DataInitializer implements ApplicationRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${admin.username}")
    private String adminUsername;

    @Value("${admin.pass}")
    private String password;

    @Value("${admin.mail}")
    private String adminMail;

    @Override
    public void run(ApplicationArguments args) {
        if (userRepository.existsByUsername(adminUsername)) {
            log.info("Admin user already exists, skipping seed.");
            return;
        }

        Users admin = Users.builder()
                .username(adminUsername)
                .email(adminMail)
                .passwordHash(passwordEncoder.encode(password))
                .fullName("Election Admin")
                .nationalId("ADMIN-001")
                .role(UserRole.ADMIN)
                .emailVerified(true)
                .accountActivated(true)
                .accountLocked(false)
                .registrationType(RegistrationType.SELF)
                .build();

        userRepository.save(admin);
        log.info("Default admin user created successfully.");
    }
}
