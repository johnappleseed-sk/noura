package com.noura.platform.service;

import com.noura.platform.repository.MerchantContractActionRepository;
import com.noura.platform.repository.MerchantContractRepository;
import com.noura.platform.repository.MerchantRepository;
import com.noura.platform.repository.StoreRepository;
import com.noura.platform.repository.StoreTenantRepository;
import com.noura.platform.repository.UserAccountRepository;
import com.noura.platform.repository.UserStoreAssignmentRepository;
import com.noura.platform.service.impl.MerchantContractServiceImpl;
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
@ContextConfiguration(classes = MerchantContractServiceMethodSecurityTest.Config.class)
class MerchantContractServiceMethodSecurityTest {

    @jakarta.annotation.Resource
    private MerchantContractService merchantContractService;

    @Test
    @WithMockUser(roles = "CUSTOMER")
    void approveContract_shouldDenyNonAdminRole() {
        assertThrows(AccessDeniedException.class, () -> merchantContractService.approveContract(java.util.UUID.randomUUID(), null));
    }

    @TestConfiguration
    @EnableMethodSecurity
    static class Config {
        @Bean MerchantRepository merchantRepository() { return mock(MerchantRepository.class); }
        @Bean MerchantContractRepository merchantContractRepository() { return mock(MerchantContractRepository.class); }
        @Bean MerchantContractActionRepository merchantContractActionRepository() { return mock(MerchantContractActionRepository.class); }
        @Bean StoreTenantRepository storeTenantRepository() { return mock(StoreTenantRepository.class); }
        @Bean StoreRepository storeRepository() { return mock(StoreRepository.class); }
        @Bean UserAccountRepository userAccountRepository() { return mock(UserAccountRepository.class); }
        @Bean UserStoreAssignmentRepository userStoreAssignmentRepository() { return mock(UserStoreAssignmentRepository.class); }
        @Bean StoreService storeService() { return mock(StoreService.class); }

        @Bean
        MerchantContractService merchantContractService(
                MerchantRepository merchantRepository,
                MerchantContractRepository merchantContractRepository,
                MerchantContractActionRepository merchantContractActionRepository,
                StoreTenantRepository storeTenantRepository,
                StoreRepository storeRepository,
                UserAccountRepository userAccountRepository,
                UserStoreAssignmentRepository userStoreAssignmentRepository,
                StoreService storeService
        ) {
            return new MerchantContractServiceImpl(
                    merchantRepository,
                    merchantContractRepository,
                    merchantContractActionRepository,
                    storeTenantRepository,
                    storeRepository,
                    userAccountRepository,
                    userStoreAssignmentRepository,
                    storeService
            );
        }
    }
}

