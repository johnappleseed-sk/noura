package com.noura.platform.inventory.config;

import com.noura.platform.inventory.domain.IamRole;
import com.noura.platform.inventory.domain.IamUser;
import com.noura.platform.inventory.domain.IamUserRole;
import com.noura.platform.inventory.domain.id.IamUserRoleId;
import com.noura.platform.inventory.repository.IamRoleRepository;
import com.noura.platform.inventory.repository.IamUserRepository;
import com.noura.platform.inventory.repository.IamUserRoleRepository;
import com.noura.platform.inventory.security.InventorySecurityProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.util.StringUtils;

@Configuration
@RequiredArgsConstructor
public class InventoryLocalAdminSeeder {

    private final InventorySecurityProperties securityProperties;

    @Bean
    ApplicationRunner inventoryLocalAdminRunner(IamUserRepository iamUserRepository,
                                                IamRoleRepository iamRoleRepository,
                                                IamUserRoleRepository iamUserRoleRepository,
                                                PasswordEncoder passwordEncoder) {
        return args -> {
            InventorySecurityProperties.SeedAdmin seedAdmin = securityProperties.getSeedAdmin();
            if (seedAdmin == null || !seedAdmin.isEnabled() || !StringUtils.hasText(seedAdmin.getEmail())) {
                return;
            }
            IamUser user = iamUserRepository.findByEmailIgnoreCaseAndDeletedAtIsNull(seedAdmin.getEmail())
                    .or(() -> iamUserRepository.findByUsernameIgnoreCaseAndDeletedAtIsNull(seedAdmin.getUsername()))
                    .orElseGet(() -> {
                        IamUser admin = new IamUser();
                        admin.setUsername(seedAdmin.getUsername().trim());
                        admin.setEmail(seedAdmin.getEmail().trim().toLowerCase());
                        admin.setFullName(seedAdmin.getFullName().trim());
                        admin.setPasswordHash(passwordEncoder.encode(seedAdmin.getPassword()));
                        admin.setStatus("ACTIVE");
                        return iamUserRepository.save(admin);
                    });
            IamRole adminRole = iamRoleRepository.findByCodeIgnoreCase("ADMIN")
                    .orElseThrow(() -> new IllegalStateException("ADMIN role is not seeded"));
            IamUserRoleId userRoleId = new IamUserRoleId(user.getId(), adminRole.getId());
            if (!iamUserRoleRepository.existsById(userRoleId)) {
                IamUserRole link = new IamUserRole();
                link.setId(userRoleId);
                link.setUser(user);
                link.setRole(adminRole);
                iamUserRoleRepository.save(link);
            }
        };
    }
}
