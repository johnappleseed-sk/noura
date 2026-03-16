package com.noura.platform.service;

import com.noura.platform.mapper.StoreMapper;
import com.noura.platform.repository.MerchantRepository;
import com.noura.platform.repository.StoreRepository;
import com.noura.platform.repository.StoreTenantRepository;
import com.noura.platform.repository.UserAccountRepository;
import com.noura.platform.service.recovery.RecoveryGovernanceService;
import com.noura.platform.service.impl.StoreServiceImpl;
import com.noura.platform.domain.enums.StoreStatus;
import com.noura.platform.dto.store.UpdateStoreStatusRequest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
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

    @Test
    @WithMockUser(roles = "CUSTOMER")
    void listAdminStores_shouldDenyNonAdminRole() {
        assertThrows(AccessDeniedException.class, () -> storeService.listAdminStores(null, null, null, null, org.springframework.data.domain.Pageable.unpaged()));
    }

    @Test
    @WithMockUser(roles = "CUSTOMER")
    void getAdminStore_shouldDenyNonAdminRole() {
        assertThrows(AccessDeniedException.class, () -> storeService.getAdminStore(java.util.UUID.randomUUID()));
    }

    @Test
    @WithMockUser(roles = "CUSTOMER")
    void updateStoreStatus_shouldDenyNonAdminRole() {
        assertThrows(AccessDeniedException.class, () -> storeService.updateStoreStatus(java.util.UUID.randomUUID(), new UpdateStoreStatusRequest(StoreStatus.ACTIVE)));
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
        RecoveryGovernanceService recoveryGovernanceService() {
            return mock(RecoveryGovernanceService.class);
        }

        @Bean
        StoreTenantRepository storeTenantRepository() {
            return mock(StoreTenantRepository.class);
        }

        @Bean
        MerchantRepository merchantRepository() {
            return mock(MerchantRepository.class);
        }

        @Bean
        StoreService storeService(
                StoreRepository storeRepository,
                UserAccountRepository userAccountRepository,
                StoreMapper storeMapper,
                RecoveryGovernanceService recoveryGovernanceService,
                StoreTenantRepository storeTenantRepository,
                MerchantRepository merchantRepository
        ) {
            return new StoreServiceImpl(
                    storeRepository,
                    merchantRepository,
                    userAccountRepository,
                    storeMapper,
                    recoveryGovernanceService,
                    storeTenantRepository
            );
        }
    }
}
