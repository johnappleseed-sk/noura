package com.company.platform.auth.service;

import com.company.platform.auth.repository.RoleRepository;
import com.company.platform.auth.repository.UserRepository;
import com.company.platform.auth.repository.UserRoleRepository;
import com.company.platform.auth.service.impl.UserServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;

@ExtendWith(SpringExtension.class)
@ActiveProfiles("method-security-test")
@ContextConfiguration(classes = UserServiceMethodSecurityTest.Config.class)
class UserServiceMethodSecurityTest {

    @jakarta.annotation.Resource
    private UserService userService;

    @Test
    @WithMockUser(roles = "CUSTOMER")
    void createUser_shouldDenyNonAdminRole() {
        assertThrows(AccessDeniedException.class, () -> userService.createUser(null));
    }

    @Test
    @WithMockUser(roles = "CUSTOMER")
    void listUsers_shouldDenyNonAdminRole() {
        assertThrows(AccessDeniedException.class, () -> userService.listUsers());
    }

    @TestConfiguration
    @EnableMethodSecurity
    static class Config {
        @Bean UserRepository userRepository() { return mock(UserRepository.class); }
        @Bean RoleRepository roleRepository() { return mock(RoleRepository.class); }
        @Bean UserRoleRepository userRoleRepository() { return mock(UserRoleRepository.class); }
        @Bean PasswordEncoder passwordEncoder() { return PasswordEncoderFactories.createDelegatingPasswordEncoder(); }

        @Bean
        UserService userService(
                UserRepository userRepository,
                RoleRepository roleRepository,
                UserRoleRepository userRoleRepository,
                PasswordEncoder passwordEncoder
        ) {
            return new UserServiceImpl(userRepository, roleRepository, userRoleRepository, passwordEncoder);
        }
    }
}
