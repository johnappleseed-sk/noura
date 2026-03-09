package com.noura.platform.config;

import com.noura.platform.domain.entity.UserAccount;
import com.noura.platform.domain.enums.RoleType;
import com.noura.platform.repository.UserAccountRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.Set;

@Slf4j
@Component
@Profile("local-mysql")
@RequiredArgsConstructor
public class LocalDevelopmentDataSeeder implements ApplicationRunner {

    public static final String DEMO_ADMIN_EMAIL = "admin@noura.local";
    public static final String DEMO_ADMIN_PASSWORD = "Admin123!";

    private final UserAccountRepository userAccountRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(ApplicationArguments args) {
        if (userAccountRepository.findByEmailIgnoreCase(DEMO_ADMIN_EMAIL).isPresent()) {
            return;
        }

        UserAccount admin = new UserAccount();
        admin.setEmail(DEMO_ADMIN_EMAIL);
        admin.setFullName("Local Demo Admin");
        admin.setPasswordHash(passwordEncoder.encode(DEMO_ADMIN_PASSWORD));
        admin.setEnabled(true);
        admin.setRoles(Set.of(RoleType.ADMIN, RoleType.CUSTOMER));
        userAccountRepository.save(admin);

        log.info("Local demo admin ready: {} / {}", DEMO_ADMIN_EMAIL, DEMO_ADMIN_PASSWORD);
    }
}
