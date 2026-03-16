package com.noura.platform.service.impl;

import com.noura.platform.common.exception.BadRequestException;
import com.noura.platform.domain.entity.Merchant;
import com.noura.platform.domain.enums.MerchantStatus;
import com.noura.platform.dto.merchant.CreateMerchantRequest;
import com.noura.platform.dto.merchant.UpdateMerchantStatusRequest;
import com.noura.platform.repository.MerchantRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MerchantServiceImplTest {

    @Test
    void createMerchant_throwsOnDuplicateMerchantCode() {
        MerchantRepository repository = mock(MerchantRepository.class);
        when(repository.existsByMerchantCodeIgnoreCase(anyString())).thenReturn(true);
        MerchantServiceImpl service = new MerchantServiceImpl(repository);

        CreateMerchantRequest request = new CreateMerchantRequest(
                "MER-ABC",
                "Acme Legal",
                "Acme Display",
                null,
                null,
                null,
                null,
                null,
                null,
                MerchantStatus.ACTIVE
        );

        BadRequestException exception = assertThrows(BadRequestException.class, () -> service.createMerchant(request));
        assertEquals("MERCHANT_CODE_DUPLICATE", exception.getCode());
        verify(repository, never()).save(any());
    }

    @Test
    void createMerchant_persistsNormalizedData() {
        MerchantRepository repository = mock(MerchantRepository.class);
        when(repository.existsByMerchantCodeIgnoreCase("MER-ABC")).thenReturn(false);
        when(repository.save(any(Merchant.class))).thenAnswer(invocation -> {
            Merchant merchant = invocation.getArgument(0);
            merchant.setId(UUID.randomUUID());
            return merchant;
        });

        MerchantServiceImpl service = new MerchantServiceImpl(repository);
        CreateMerchantRequest request = new CreateMerchantRequest(
                "mer-abc",
                "Acme Legal",
                "Acme Display",
                " sales@acme.com ",
                " +12025550199 ",
                "us",
                LocalDate.now(),
                LocalDate.now().plusDays(30),
                "notes",
                MerchantStatus.ACTIVE
        );

        service.createMerchant(request);

        ArgumentCaptor<Merchant> captor = ArgumentCaptor.forClass(Merchant.class);
        verify(repository).save(captor.capture());
        Merchant saved = captor.getValue();
        assertThat(saved.getMerchantCode()).isEqualTo("MER-ABC");
        assertThat(saved.getDisplayName()).isEqualTo("Acme Display");
        assertThat(saved.getEmail()).isEqualTo("sales@acme.com");
        assertThat(saved.getPhone()).isEqualTo("+12025550199");
        assertThat(saved.getCountryCode()).isEqualTo("US");
    }

    @Test
    void listMerchants_usesSearchAcrossLegalAndDisplayName() {
        MerchantRepository repository = mock(MerchantRepository.class);
        when(repository.findAll(
                any(org.springframework.data.jpa.domain.Specification.class),
                any(org.springframework.data.domain.Pageable.class)
        )).thenReturn(new PageImpl<>(List.of()));
        MerchantServiceImpl service = new MerchantServiceImpl(repository);

        service.listMerchants("acme", MerchantStatus.ACTIVE, PageRequest.of(0, 10));

        verify(repository).findAll(
                any(org.springframework.data.jpa.domain.Specification.class),
                any(org.springframework.data.domain.Pageable.class)
        );
    }

    @Test
    void updateMerchantStatus_updatesAndReturnsStatus() {
        UUID merchantId = UUID.randomUUID();
        MerchantRepository repository = mock(MerchantRepository.class);
        Merchant existing = new Merchant();
        existing.setId(merchantId);
        existing.setStatus(MerchantStatus.DRAFT);
        when(repository.findById(merchantId)).thenReturn(Optional.of(existing));
        when(repository.save(existing)).thenReturn(existing);

        MerchantServiceImpl service = new MerchantServiceImpl(repository);
        var response = service.updateMerchantStatus(merchantId, new UpdateMerchantStatusRequest(MerchantStatus.ACTIVE));

        assertEquals(MerchantStatus.ACTIVE, response.status());
        assertEquals(merchantId, response.id());
    }

    @Test
    void createMerchant_invalidDateWindowRejected() {
        MerchantRepository repository = mock(MerchantRepository.class);
        when(repository.existsByMerchantCodeIgnoreCase(anyString())).thenReturn(false);

        MerchantServiceImpl service = new MerchantServiceImpl(repository);
        CreateMerchantRequest request = new CreateMerchantRequest(
                "MER-RETRY",
                "Acme Legal",
                "Acme Display",
                null,
                null,
                null,
                LocalDate.now().plusDays(30),
                LocalDate.now().minusDays(1),
                null,
                MerchantStatus.ACTIVE
        );

        BadRequestException exception = assertThrows(BadRequestException.class, () -> service.createMerchant(request));
        assertEquals("MERCHANT_CONTRACT_DATES_INVALID", exception.getCode());
        verify(repository, never()).save(any());
    }
}
