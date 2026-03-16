package com.noura.platform.service;

import com.noura.platform.domain.enums.MerchantStatus;
import com.noura.platform.dto.merchant.CreateMerchantRequest;
import com.noura.platform.dto.merchant.UpdateMerchantStatusRequest;
import com.noura.platform.repository.MerchantRepository;
import com.noura.platform.service.impl.MerchantServiceImpl;
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

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;

@ExtendWith(SpringExtension.class)
@ActiveProfiles("method-security-test")
@ContextConfiguration(classes = MerchantServiceMethodSecurityTest.Config.class)
class MerchantServiceMethodSecurityTest {

    @jakarta.annotation.Resource
    private MerchantService merchantService;

    @Test
    @WithMockUser(roles = "CUSTOMER")
    void createMerchant_shouldDenyNonAdminRole() {
        CreateMerchantRequest request = new CreateMerchantRequest(
                "M001",
                "Acme Co",
                "Acme Marketplace",
                "ops@acme.com",
                "+12025550199",
                "US",
                null,
                null,
                null,
                MerchantStatus.ACTIVE
        );
        assertThrows(AccessDeniedException.class, () -> merchantService.createMerchant(request));
    }

    @Test
    @WithMockUser(roles = "CUSTOMER")
    void listMerchants_shouldDenyNonAdminRole() {
        assertThrows(AccessDeniedException.class, () -> merchantService.listMerchants(null, null, null));
    }

    @Test
    @WithMockUser(roles = "CUSTOMER")
    void getMerchant_shouldDenyNonAdminRole() {
        assertThrows(AccessDeniedException.class, () -> merchantService.getMerchant(UUID.randomUUID()));
    }

    @Test
    @WithMockUser(roles = "CUSTOMER")
    void updateMerchantStatus_shouldDenyNonAdminRole() {
        assertThrows(
                AccessDeniedException.class,
                () -> merchantService.updateMerchantStatus(UUID.randomUUID(), new UpdateMerchantStatusRequest(MerchantStatus.ACTIVE))
        );
    }

    @TestConfiguration
    @EnableMethodSecurity
    static class Config {
        @Bean
        MerchantRepository merchantRepository() {
            return mock(MerchantRepository.class);
        }

        @Bean
        MerchantService merchantService(MerchantRepository merchantRepository) {
            return new MerchantServiceImpl(merchantRepository);
        }
    }
}
