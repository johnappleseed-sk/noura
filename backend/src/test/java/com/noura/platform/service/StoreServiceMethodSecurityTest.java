package com.noura.platform.service;

import com.noura.platform.mapper.StoreMapper;
import com.noura.platform.repository.StoreRepository;
import com.noura.platform.repository.UserAccountRepository;
import com.noura.platform.service.impl.StoreServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;

@ExtendWith(SpringExtension.class)
@ActiveProfiles("method-security-test")
@ContextConfiguration(classes = StoreServiceMethodSecurityTest.Config.class)
class StoreServiceMethodSecurityTest {

    @jakarta.annotation.Resource
    private StoreService storeService;

    @Test
    @WithMockUser(roles = "CUSTOMER")
    void createStore_shouldDenyNonAdminRole() {
        assertThrows(AccessDeniedException.class, () -> storeService.createStore(null));
    }

    @TestConfiguration
    @EnableMethodSecurity
    static class Config {

        @Bean
        StoreRepository storeRepository() {
            return mock(StoreRepository.class);
        }

        @Bean
        UserAccountRepository userAccountRepository() {
            return mock(UserAccountRepository.class);
        }

        @Bean
        StoreMapper storeMapper() {
            return mock(StoreMapper.class);
        }

        @Bean
        StoreService storeService(
                StoreRepository storeRepository,
                UserAccountRepository userAccountRepository,
                StoreMapper storeMapper
        ) {
            return new StoreServiceImpl(storeRepository, userAccountRepository, storeMapper);
        }
    }
}
