package com.noura.platform.service.impl;

import com.noura.platform.common.exception.NotFoundException;
import com.noura.platform.domain.entity.Store;
import com.noura.platform.domain.enums.StoreStatus;
import com.noura.platform.domain.enums.StoreType;
import com.noura.platform.dto.store.CreateStoreRequest;
import com.noura.platform.dto.store.StoreResponse;
import com.noura.platform.dto.store.UpdateStoreStatusRequest;
import com.noura.platform.mapper.StoreMapper;
import com.noura.platform.repository.MerchantRepository;
import com.noura.platform.repository.StoreRepository;
import com.noura.platform.repository.StoreTenantRepository;
import com.noura.platform.repository.UserAccountRepository;
import com.noura.platform.service.recovery.RecoveryGovernanceService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class StoreServiceImplTest {

    @Test
    void createAdminStore_shouldRejectWhenMerchantMissing() {
        StoreRepository storeRepository = mock(StoreRepository.class);
        MerchantRepository merchantRepository = mock(MerchantRepository.class);
        UserAccountRepository userAccountRepository = mock(UserAccountRepository.class);
        StoreMapper storeMapper = mock(StoreMapper.class);
        RecoveryGovernanceService recoveryGovernanceService = mock(RecoveryGovernanceService.class);
        StoreTenantRepository storeTenantRepository = mock(StoreTenantRepository.class);

        when(merchantRepository.existsById(UUID.randomUUID())).thenReturn(false);

        StoreServiceImpl service = new StoreServiceImpl(
                storeRepository,
                merchantRepository,
                userAccountRepository,
                storeMapper,
                recoveryGovernanceService,
                storeTenantRepository
        );

        UUID missingMerchantId = UUID.randomUUID();
        CreateStoreRequest request = new CreateStoreRequest(
                "STORE-ACME",
                "Main",
                "main-store",
                missingMerchantId,
                StoreType.MERCHANT,
                StoreStatus.ACTIVE,
                "ops@acme.com",
                "+12025550099",
                "US",
                "San Diego",
                "100 First Ave",
                "Ste 200"
        );

        assertThrows(NotFoundException.class, () -> service.createAdminStore(request));
        verify(storeRepository, never()).save(any());
    }

    @Test
    void createAdminStore_shouldPersistNormalizedData() {
        StoreRepository storeRepository = mock(StoreRepository.class);
        MerchantRepository merchantRepository = mock(MerchantRepository.class);
        UserAccountRepository userAccountRepository = mock(UserAccountRepository.class);
        StoreMapper storeMapper = mock(StoreMapper.class);
        RecoveryGovernanceService recoveryGovernanceService = mock(RecoveryGovernanceService.class);
        StoreTenantRepository storeTenantRepository = mock(StoreTenantRepository.class);

        when(merchantRepository.existsById(any())).thenReturn(true);
        when(storeRepository.existsByStoreCodeIgnoreCase(anyString())).thenReturn(false);
        when(storeRepository.existsBySlugIgnoreCase(anyString())).thenReturn(false);

        when(storeRepository.save(any(Store.class))).thenAnswer(invocation -> {
            Store entity = invocation.getArgument(0);
            entity.setId(UUID.randomUUID());
            return entity;
        });

        StoreServiceImpl service = new StoreServiceImpl(
                storeRepository,
                merchantRepository,
                userAccountRepository,
                storeMapper,
                recoveryGovernanceService,
                storeTenantRepository
        );

        CreateStoreRequest request = new CreateStoreRequest(
                " store-acme-01 ",
                " Main Store ",
                " Main Store ! ",
                UUID.randomUUID(),
                StoreType.MERCHANT,
                StoreStatus.ACTIVE,
                " Ops@Acme.Com ",
                " +12025550100 ",
                " us ",
                "San Diego",
                "100 first ave",
                null
        );

        StoreResponse response = service.createAdminStore(request);

        ArgumentCaptor<Store> capture = ArgumentCaptor.forClass(Store.class);
        verify(storeRepository).save(capture.capture());
        Store saved = capture.getValue();

        assertEquals("STORE-ACME-01", saved.getStoreCode());
        assertEquals("main-store", saved.getSlug());
        assertEquals("ops@acme.com", saved.getContactEmail());
        assertEquals("+12025550100", saved.getContactPhone());
        assertEquals("US", saved.getCountryCode());
        assertNotNull(response.id());
        assertEquals("Main Store", saved.getName());
    }

    @Test
    void listAdminStores_shouldUseRepositoryFindAll() {
        StoreRepository storeRepository = mock(StoreRepository.class);
        MerchantRepository merchantRepository = mock(MerchantRepository.class);
        UserAccountRepository userAccountRepository = mock(UserAccountRepository.class);
        StoreMapper storeMapper = mock(StoreMapper.class);
        RecoveryGovernanceService recoveryGovernanceService = mock(RecoveryGovernanceService.class);
        StoreTenantRepository storeTenantRepository = mock(StoreTenantRepository.class);

        when(storeRepository.findAll(any(org.springframework.data.jpa.domain.Specification.class), any(org.springframework.data.domain.Pageable.class)))
                .thenReturn(new PageImpl<>(List.of()));

        StoreServiceImpl service = new StoreServiceImpl(
                storeRepository,
                merchantRepository,
                userAccountRepository,
                storeMapper,
                recoveryGovernanceService,
                storeTenantRepository
        );

        Page<StoreResponse> response = service.listAdminStores("acme", UUID.randomUUID(), StoreType.BRANCH, StoreStatus.ACTIVE, PageRequest.of(0, 10));

        assertThat(response).isNotNull();
        verify(storeRepository).findAll(any(org.springframework.data.jpa.domain.Specification.class), any(org.springframework.data.domain.Pageable.class));
    }

    @Test
    void updateStoreStatus_shouldPersistAndReturnUpdatedStatus() {
        StoreRepository storeRepository = mock(StoreRepository.class);
        MerchantRepository merchantRepository = mock(MerchantRepository.class);
        UserAccountRepository userAccountRepository = mock(UserAccountRepository.class);
        StoreMapper storeMapper = mock(StoreMapper.class);
        RecoveryGovernanceService recoveryGovernanceService = mock(RecoveryGovernanceService.class);
        StoreTenantRepository storeTenantRepository = mock(StoreTenantRepository.class);

        UUID storeId = UUID.randomUUID();
        Store existing = new Store();
        existing.setId(storeId);
        existing.setStatus(StoreStatus.DRAFT);

        when(storeRepository.findById(storeId)).thenReturn(Optional.of(existing));
        when(storeRepository.save(any(Store.class))).thenAnswer(invocation -> invocation.getArgument(0));

        StoreServiceImpl service = new StoreServiceImpl(
                storeRepository,
                merchantRepository,
                userAccountRepository,
                storeMapper,
                recoveryGovernanceService,
                storeTenantRepository
        );

        StoreResponse response = service.updateStoreStatus(storeId, new UpdateStoreStatusRequest(StoreStatus.SUSPENDED));

        assertEquals(StoreStatus.SUSPENDED, response.status());
        assertEquals(storeId, response.id());
        assertThat(response.slug()).isNull();
    }

    @Test
    void createAdminStore_shouldRejectWhenStoreCodeCannotBeUniqued() {
        StoreRepository storeRepository = mock(StoreRepository.class);
        MerchantRepository merchantRepository = mock(MerchantRepository.class);
        UserAccountRepository userAccountRepository = mock(UserAccountRepository.class);
        StoreMapper storeMapper = mock(StoreMapper.class);
        RecoveryGovernanceService recoveryGovernanceService = mock(RecoveryGovernanceService.class);
        StoreTenantRepository storeTenantRepository = mock(StoreTenantRepository.class);

        when(merchantRepository.existsById(any())).thenReturn(true);
        when(storeRepository.existsByStoreCodeIgnoreCase(anyString())).thenReturn(true);
        when(storeRepository.existsByStoreCodeIgnoreCaseAndIdNot(anyString(), any())).thenReturn(true);
        when(storeRepository.existsBySlugIgnoreCase(anyString())).thenReturn(false);

        StoreServiceImpl service = new StoreServiceImpl(
                storeRepository,
                merchantRepository,
                userAccountRepository,
                storeMapper,
                recoveryGovernanceService,
                storeTenantRepository
        );

        UUID merchantId = UUID.randomUUID();
        CreateStoreRequest request = new CreateStoreRequest(
                "DUP-STORE",
                "Main Store",
                "main-store",
                merchantId,
                StoreType.MERCHANT,
                StoreStatus.ACTIVE,
                "ops@acme.com",
                "+12025550101",
                "US",
                "San Diego",
                "100 First Ave",
                "Suite 1"
        );

        assertThrows(com.noura.platform.common.exception.BadRequestException.class, () -> service.createAdminStore(request));
    }
}
