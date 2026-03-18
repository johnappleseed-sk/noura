package com.noura.customer.service.impl;

import com.noura.customer.domain.entity.CustomerProfile;
import com.noura.customer.dto.profile.CustomerProfileResponse;
import com.noura.customer.exception.CustomerOperationException;
import com.noura.customer.repository.CustomerAddressRepository;
import com.noura.customer.repository.CustomerProfileRepository;
import com.noura.customer.service.model.CustomerIdentity;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link CustomerAccountServiceImpl}.
 */
@ExtendWith(MockitoExtension.class)
class CustomerAccountServiceImplTest {

    @Mock
    private CustomerProfileRepository customerProfileRepository;

    @Mock
    private CustomerAddressRepository customerAddressRepository;

    @InjectMocks
    private CustomerAccountServiceImpl customerAccountService;

    /**
     * Verifies profile lookup lazily creates a profile when one does not yet exist.
     */
    @Test
    void shouldCreateProfileWhenMissing() {
        when(customerProfileRepository.findByExternalSubject(eq("subject-001"))).thenReturn(Optional.empty());
        when(customerProfileRepository.save(any(CustomerProfile.class))).thenAnswer(invocation -> {
            CustomerProfile profile = invocation.getArgument(0, CustomerProfile.class);
            profile.setId(UUID.fromString("11111111-1111-1111-1111-111111111111"));
            return profile;
        });

        CustomerProfileResponse response = customerAccountService.getOrCreateCurrentProfile(
                new CustomerIdentity("subject-001", "alice@example.com")
        );

        verify(customerProfileRepository).save(any(CustomerProfile.class));
        Assertions.assertEquals("subject-001", response.externalSubject());
        Assertions.assertEquals("alice@example.com", response.email());
        Assertions.assertEquals("alice", response.fullName());
    }

    /**
     * Verifies blank external subject lookups are rejected as invalid requests.
     */
    @Test
    void shouldRejectBlankExternalSubjectLookup() {
        CustomerOperationException exception = Assertions.assertThrows(
                CustomerOperationException.class,
                () -> customerAccountService.lookupByExternalSubject(" ")
        );

        Assertions.assertEquals("EXTERNAL_SUBJECT_REQUIRED", exception.getCode());
        Assertions.assertEquals(400, exception.getStatus().value());
    }
}

