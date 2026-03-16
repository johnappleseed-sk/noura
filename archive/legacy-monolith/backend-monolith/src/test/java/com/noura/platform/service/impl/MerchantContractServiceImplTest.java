package com.noura.platform.service.impl;

import com.noura.platform.domain.entity.Merchant;
import com.noura.platform.domain.entity.MerchantContract;
import com.noura.platform.domain.entity.Store;
import com.noura.platform.domain.entity.StoreTenant;
import com.noura.platform.domain.entity.UserAccount;
import com.noura.platform.domain.enums.MerchantContractStatus;
import com.noura.platform.domain.enums.MerchantStatus;
import com.noura.platform.domain.enums.StoreTenantStatus;
import com.noura.platform.dto.contract.ContractStoreRegistrationRequest;
import com.noura.platform.dto.contract.MerchantContractDecisionRequest;
import com.noura.platform.dto.store.StoreRequest;
import com.noura.platform.domain.enums.StoreServiceType;
import com.noura.platform.repository.MerchantContractActionRepository;
import com.noura.platform.repository.MerchantContractRepository;
import com.noura.platform.repository.MerchantRepository;
import com.noura.platform.repository.StoreRepository;
import com.noura.platform.repository.StoreTenantRepository;
import com.noura.platform.repository.UserAccountRepository;
import com.noura.platform.repository.UserStoreAssignmentRepository;
import com.noura.platform.service.StoreService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class MerchantContractServiceImplTest {

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void approveThenRegisterStore_createsActiveTenant() {
        MerchantRepository merchantRepository = mock(MerchantRepository.class);
        MerchantContractRepository contractRepository = mock(MerchantContractRepository.class);
        MerchantContractActionRepository actionRepository = mock(MerchantContractActionRepository.class);
        StoreTenantRepository storeTenantRepository = mock(StoreTenantRepository.class);
        StoreRepository storeRepository = mock(StoreRepository.class);
        UserAccountRepository userAccountRepository = mock(UserAccountRepository.class);
        UserStoreAssignmentRepository userStoreAssignmentRepository = mock(UserStoreAssignmentRepository.class);
        StoreService storeService = mock(StoreService.class);

        MerchantContractServiceImpl service = new MerchantContractServiceImpl(
                merchantRepository,
                contractRepository,
                actionRepository,
                storeTenantRepository,
                storeRepository,
                userAccountRepository,
                userStoreAssignmentRepository,
                storeService
        );

        String adminEmail = "admin@noura.local";
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                adminEmail,
                "n/a",
                Set.of(new SimpleGrantedAuthority("ROLE_ADMIN"))
        ));

        UserAccount adminUser = new UserAccount();
        adminUser.setId(UUID.randomUUID());
        adminUser.setEmail(adminEmail);
        when(userAccountRepository.findByEmailIgnoreCase(adminEmail)).thenReturn(Optional.of(adminUser));

        Merchant merchant = new Merchant();
        merchant.setId(UUID.randomUUID());
        merchant.setName("Partner A");
        merchant.setStatus(MerchantStatus.ACTIVE);

        UUID contractId = UUID.randomUUID();
        MerchantContract contract = new MerchantContract();
        contract.setId(contractId);
        contract.setMerchant(merchant);
        contract.setContractNumber("C-1001");
        contract.setStatus(MerchantContractStatus.PENDING_APPROVAL);
        contract.setStartDate(LocalDate.now().minusDays(1));
        contract.setEndDate(LocalDate.now().plusDays(30));

        when(contractRepository.findById(contractId)).thenReturn(Optional.of(contract));
        when(contractRepository.save(any(MerchantContract.class))).thenAnswer(invocation -> invocation.getArgument(0));

        service.approveContract(contractId, new MerchantContractDecisionRequest("ok"));
        assertThat(contract.getStatus()).isEqualTo(MerchantContractStatus.APPROVED);

        UUID storeId = UUID.randomUUID();
        when(storeService.createStore(any(StoreRequest.class))).thenReturn(new com.noura.platform.dto.store.StoreDto(
                storeId,
                "Partner Store",
                "Line 1",
                "City",
                "State",
                "Zip",
                "Country",
                "Region",
                BigDecimal.ONE,
                BigDecimal.ONE,
                null,
                LocalTime.of(8, 0),
                LocalTime.of(18, 0),
                true,
                Set.of(StoreServiceType.DELIVERY),
                new BigDecimal("1.00"),
                new BigDecimal("10.00"),
                0D,
                true
        ));

        Store store = new Store();
        store.setId(storeId);
        store.setName("Partner Store");
        store.setActive(true);
        when(storeRepository.findById(storeId)).thenReturn(Optional.of(store));
        when(storeTenantRepository.findByStoreId(storeId)).thenReturn(Optional.empty());
        when(storeTenantRepository.save(any(StoreTenant.class))).thenAnswer(invocation -> {
            StoreTenant saved = invocation.getArgument(0);
            saved.setId(UUID.randomUUID());
            return saved;
        });

        StoreRequest storeRequest = new StoreRequest(
                "Partner Store",
                "Line 1",
                "City",
                "State",
                "Zip",
                "Country",
                "Region",
                BigDecimal.ONE,
                BigDecimal.ONE,
                null,
                LocalTime.of(8, 0),
                LocalTime.of(18, 0),
                true,
                Set.of(StoreServiceType.DELIVERY),
                new BigDecimal("1.00"),
                new BigDecimal("10.00")
        );

        service.registerStore(contractId, new ContractStoreRegistrationRequest(storeRequest, null));

        ArgumentCaptor<StoreTenant> tenantCaptor = ArgumentCaptor.forClass(StoreTenant.class);
        verify(storeTenantRepository).save(tenantCaptor.capture());
        StoreTenant tenant = tenantCaptor.getValue();
        assertThat(tenant.getStore().getId()).isEqualTo(storeId);
        assertThat(tenant.getContract().getId()).isEqualTo(contractId);
        assertThat(tenant.getMerchant().getId()).isEqualTo(merchant.getId());
        assertThat(tenant.getStatus()).isEqualTo(StoreTenantStatus.ACTIVE);
        assertThat(tenant.getActivatedAt()).isNotNull();
    }
}

