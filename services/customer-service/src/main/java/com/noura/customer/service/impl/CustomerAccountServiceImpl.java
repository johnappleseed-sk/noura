package com.noura.customer.service.impl;

import com.noura.customer.domain.entity.CustomerAddress;
import com.noura.customer.domain.entity.CustomerPaymentMethod;
import com.noura.customer.domain.entity.CustomerProfile;
import com.noura.customer.domain.enums.AddressValidationStatus;
import com.noura.customer.domain.enums.DefaultAddressType;
import com.noura.customer.dto.address.CustomerAddressRequest;
import com.noura.customer.dto.address.CustomerAddressResponse;
import com.noura.customer.dto.internal.CustomerLookupResponse;
import com.noura.customer.dto.payment.CustomerPaymentMethodRequest;
import com.noura.customer.dto.payment.CustomerPaymentMethodResponse;
import com.noura.customer.dto.profile.CustomerProfileResponse;
import com.noura.customer.dto.profile.UpdateCustomerProfileRequest;
import com.noura.customer.exception.CustomerOperationException;
import com.noura.customer.exception.NotFoundException;
import com.noura.customer.repository.CustomerAddressRepository;
import com.noura.customer.repository.CustomerPaymentMethodRepository;
import com.noura.customer.repository.CustomerProfileRepository;
import com.noura.customer.service.CustomerAccountService;
import com.noura.customer.service.model.CustomerIdentity;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

/**
 * Default implementation of {@link CustomerAccountService}.
 */
@Service
@RequiredArgsConstructor
public class CustomerAccountServiceImpl implements CustomerAccountService {

    private final CustomerProfileRepository customerProfileRepository;
    private final CustomerAddressRepository customerAddressRepository;
    private final CustomerPaymentMethodRepository customerPaymentMethodRepository;

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional
    public CustomerProfileResponse getOrCreateCurrentProfile(CustomerIdentity identity) {
        CustomerProfile profile = getOrCreateProfile(identity);
        return toProfileResponse(profile);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional
    public CustomerProfileResponse updateCurrentProfile(CustomerIdentity identity, UpdateCustomerProfileRequest request) {
        CustomerProfile profile = getOrCreateProfile(identity);
        profile.setFullName(request.fullName().trim());
        profile.setPhone(trimToNull(request.phone()));
        if (trimToNull(request.email()) != null) {
            profile.setEmail(trimToNull(request.email()).toLowerCase(Locale.ROOT));
        } else if (trimToNull(identity.emailHint()) != null) {
            profile.setEmail(trimToNull(identity.emailHint()).toLowerCase(Locale.ROOT));
        }
        profile.setUpdatedBy(identity.externalSubject());
        CustomerProfile saved = customerProfileRepository.save(profile);
        return toProfileResponse(saved);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional
    public List<CustomerAddressResponse> listAddresses(CustomerIdentity identity) {
        CustomerProfile profile = getOrCreateProfile(identity);
        return customerAddressRepository.findByCustomerOrderByUpdatedAtDesc(profile).stream()
                .map(this::toAddressResponse)
                .toList();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional
    public CustomerAddressResponse getAddress(CustomerIdentity identity, UUID addressId) {
        CustomerProfile profile = getOrCreateProfile(identity);
        return toAddressResponse(requireAddress(profile, addressId));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional
    public CustomerAddressResponse addAddress(CustomerIdentity identity, CustomerAddressRequest request) {
        CustomerProfile profile = getOrCreateProfile(identity);
        validateCoordinatePair(request.latitude(), request.longitude());

        CustomerAddress address = new CustomerAddress();
        address.setCustomer(profile);
        applyAddressFields(address, request);
        address.setCreatedBy(identity.externalSubject());
        address.setUpdatedBy(identity.externalSubject());

        CustomerAddress saved = customerAddressRepository.save(address);
        applyDefaultFlags(profile, saved, request.defaultShipping(), request.defaultBilling(), identity.externalSubject());
        return toAddressResponse(saved);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional
    public CustomerAddressResponse updateAddress(CustomerIdentity identity, UUID addressId, CustomerAddressRequest request) {
        CustomerProfile profile = getOrCreateProfile(identity);
        CustomerAddress address = requireAddress(profile, addressId);
        validateCoordinatePair(request.latitude(), request.longitude());

        applyAddressFields(address, request);
        address.setUpdatedBy(identity.externalSubject());
        CustomerAddress saved = customerAddressRepository.save(address);
        applyDefaultFlags(profile, saved, request.defaultShipping(), request.defaultBilling(), identity.externalSubject());
        return toAddressResponse(saved);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional
    public void deleteAddress(CustomerIdentity identity, UUID addressId) {
        CustomerProfile profile = getOrCreateProfile(identity);
        CustomerAddress address = requireAddress(profile, addressId);
        customerAddressRepository.delete(address);

        if (addressId.equals(profile.getDefaultShippingAddressId())) {
            profile.setDefaultShippingAddressId(null);
        }
        if (addressId.equals(profile.getDefaultBillingAddressId())) {
            profile.setDefaultBillingAddressId(null);
        }
        profile.setUpdatedBy(identity.externalSubject());
        customerProfileRepository.save(profile);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional
    public CustomerAddressResponse setDefaultAddress(CustomerIdentity identity, UUID addressId, DefaultAddressType defaultType) {
        CustomerProfile profile = getOrCreateProfile(identity);
        CustomerAddress address = requireAddress(profile, addressId);

        clearDefaults(profile, defaultType);
        if (defaultType == DefaultAddressType.SHIPPING || defaultType == DefaultAddressType.BOTH) {
            address.setDefaultShipping(true);
            profile.setDefaultShippingAddressId(address.getId());
        }
        if (defaultType == DefaultAddressType.BILLING || defaultType == DefaultAddressType.BOTH) {
            address.setDefaultBilling(true);
            profile.setDefaultBillingAddressId(address.getId());
        }

        address.setUpdatedBy(identity.externalSubject());
        profile.setUpdatedBy(identity.externalSubject());
        customerAddressRepository.save(address);
        customerProfileRepository.save(profile);
        return toAddressResponse(address);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional
    public List<CustomerPaymentMethodResponse> listPaymentMethods(CustomerIdentity identity) {
        CustomerProfile profile = getOrCreateProfile(identity);
        return customerPaymentMethodRepository.findByCustomerOrderByUpdatedAtDesc(profile).stream()
                .map(this::toPaymentMethodResponse)
                .toList();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional
    public CustomerPaymentMethodResponse addPaymentMethod(CustomerIdentity identity, CustomerPaymentMethodRequest request) {
        CustomerProfile profile = getOrCreateProfile(identity);
        if (Boolean.TRUE.equals(request.defaultMethod())) {
            clearDefaultPaymentMethod(profile);
        }

        CustomerPaymentMethod paymentMethod = new CustomerPaymentMethod();
        paymentMethod.setCustomer(profile);
        applyPaymentMethodFields(paymentMethod, request);
        paymentMethod.setCreatedBy(identity.externalSubject());
        paymentMethod.setUpdatedBy(identity.externalSubject());
        CustomerPaymentMethod saved = customerPaymentMethodRepository.save(paymentMethod);
        return toPaymentMethodResponse(saved);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional
    public CustomerPaymentMethodResponse updatePaymentMethod(
            CustomerIdentity identity,
            UUID paymentMethodId,
            CustomerPaymentMethodRequest request
    ) {
        CustomerProfile profile = getOrCreateProfile(identity);
        CustomerPaymentMethod paymentMethod = requirePaymentMethod(profile, paymentMethodId);
        if (Boolean.TRUE.equals(request.defaultMethod())) {
            clearDefaultPaymentMethod(profile);
        }

        applyPaymentMethodFields(paymentMethod, request);
        paymentMethod.setUpdatedBy(identity.externalSubject());
        CustomerPaymentMethod saved = customerPaymentMethodRepository.save(paymentMethod);
        return toPaymentMethodResponse(saved);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional
    public void deletePaymentMethod(CustomerIdentity identity, UUID paymentMethodId) {
        CustomerProfile profile = getOrCreateProfile(identity);
        CustomerPaymentMethod paymentMethod = requirePaymentMethod(profile, paymentMethodId);
        customerPaymentMethodRepository.delete(paymentMethod);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional(readOnly = true)
    public CustomerLookupResponse lookupById(UUID customerId) {
        CustomerProfile profile = customerProfileRepository.findById(customerId)
                .orElseThrow(() -> new NotFoundException("CUSTOMER_NOT_FOUND", "Customer not found"));
        return toLookupResponse(profile);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional(readOnly = true)
    public CustomerLookupResponse lookupByExternalSubject(String externalSubject) {
        String normalizedSubject = trimToNull(externalSubject);
        if (normalizedSubject == null) {
            throw new CustomerOperationException(HttpStatus.BAD_REQUEST, "EXTERNAL_SUBJECT_REQUIRED", "externalSubject is required");
        }
        CustomerProfile profile = customerProfileRepository.findByExternalSubject(normalizedSubject)
                .orElseThrow(() -> new NotFoundException("CUSTOMER_NOT_FOUND", "Customer not found"));
        return toLookupResponse(profile);
    }

    /**
     * Loads or creates profile from external identity.
     *
     * @param identity customer identity context
     * @return profile aggregate
     */
    private CustomerProfile getOrCreateProfile(CustomerIdentity identity) {
        return customerProfileRepository.findByExternalSubject(identity.externalSubject())
                .orElseGet(() -> {
                    CustomerProfile created = new CustomerProfile();
                    created.setExternalSubject(identity.externalSubject());
                    created.setEmail(trimToNull(identity.emailHint()) == null ? null : identity.emailHint().toLowerCase(Locale.ROOT));
                    created.setFullName(deriveDefaultName(identity));
                    created.setPhone(null);
                    created.setEnabled(true);
                    created.setCreatedBy(identity.externalSubject());
                    created.setUpdatedBy(identity.externalSubject());
                    return customerProfileRepository.save(created);
                });
    }

    /**
     * Derives default display name for newly created profile records.
     *
     * @param identity customer identity context
     * @return derived display name
     */
    private String deriveDefaultName(CustomerIdentity identity) {
        String emailHint = trimToNull(identity.emailHint());
        if (emailHint == null) {
            return "Customer";
        }
        String localPart = emailHint.contains("@") ? emailHint.substring(0, emailHint.indexOf('@')) : emailHint;
        if (localPart.isBlank()) {
            return "Customer";
        }
        return localPart;
    }

    /**
     * Loads one address constrained to customer ownership.
     *
     * @param profile customer profile owner
     * @param addressId address ID
     * @return address entity
     */
    private CustomerAddress requireAddress(CustomerProfile profile, UUID addressId) {
        return customerAddressRepository.findByIdAndCustomer(addressId, profile)
                .orElseThrow(() -> new NotFoundException("ADDRESS_NOT_FOUND", "Address not found"));
    }

    /**
     * Loads one payment method constrained to customer ownership.
     *
     * @param profile customer profile owner
     * @param paymentMethodId payment method identifier
     * @return payment method entity
     */
    private CustomerPaymentMethod requirePaymentMethod(CustomerProfile profile, UUID paymentMethodId) {
        return customerPaymentMethodRepository.findByIdAndCustomer(paymentMethodId, profile)
                .orElseThrow(() -> new NotFoundException("PAYMENT_METHOD_NOT_FOUND", "Payment method not found"));
    }

    /**
     * Applies mutable fields from request to address entity.
     *
     * @param address target entity
     * @param request source payload
     */
    private void applyAddressFields(CustomerAddress address, CustomerAddressRequest request) {
        address.setLabel(trimToNull(request.label()));
        address.setFullName(request.fullName().trim());
        address.setPhone(trimToNull(request.phone()));
        address.setLine1(request.line1().trim());
        address.setLine2(trimToNull(request.line2()));
        address.setDistrict(trimToNull(request.district()));
        address.setCity(request.city().trim());
        address.setStateProvince(request.stateProvince().trim());
        address.setPostalCode(request.postalCode().trim());
        address.setCountryCode(request.countryCode().trim().toUpperCase(Locale.ROOT));
        address.setLatitude(request.latitude());
        address.setLongitude(request.longitude());
        address.setAccuracyMeters(request.accuracyMeters());
        address.setPlaceId(trimToNull(request.placeId()));
        address.setFormattedAddress(trimToNull(request.formattedAddress()));
        address.setDeliveryInstructions(trimToNull(request.deliveryInstructions()));
        address.setValidationStatus(request.latitude() == null || request.longitude() == null
                ? AddressValidationStatus.UNVERIFIED
                : AddressValidationStatus.VALID);
    }

    /**
     * Applies mutable fields from request to payment-method entity.
     *
     * @param paymentMethod target entity
     * @param request source payload
     */
    private void applyPaymentMethodFields(CustomerPaymentMethod paymentMethod, CustomerPaymentMethodRequest request) {
        paymentMethod.setMethodType(request.methodType().trim().toUpperCase(Locale.ROOT));
        paymentMethod.setProvider(request.provider().trim());
        paymentMethod.setTokenizedReference(request.tokenizedReference().trim());
        paymentMethod.setDefaultMethod(Boolean.TRUE.equals(request.defaultMethod()));
    }

    /**
     * Applies requested default flag changes after address save.
     *
     * @param profile profile owner
     * @param address target address
     * @param defaultShipping requested shipping default flag
     * @param defaultBilling requested billing default flag
     * @param actor actor identifier for audit fields
     */
    private void applyDefaultFlags(
            CustomerProfile profile,
            CustomerAddress address,
            Boolean defaultShipping,
            Boolean defaultBilling,
            String actor
    ) {
        boolean markShipping = Boolean.TRUE.equals(defaultShipping);
        boolean markBilling = Boolean.TRUE.equals(defaultBilling);
        if (!markShipping && !markBilling) {
            customerAddressRepository.save(address);
            return;
        }

        if (markShipping) {
            clearDefaults(profile, DefaultAddressType.SHIPPING);
            address.setDefaultShipping(true);
            profile.setDefaultShippingAddressId(address.getId());
        }
        if (markBilling) {
            clearDefaults(profile, DefaultAddressType.BILLING);
            address.setDefaultBilling(true);
            profile.setDefaultBillingAddressId(address.getId());
        }
        profile.setUpdatedBy(actor);
        address.setUpdatedBy(actor);
        customerAddressRepository.save(address);
        customerProfileRepository.save(profile);
    }

    /**
     * Clears default shipping/billing flags depending on requested type.
     *
     * @param profile profile owner
     * @param defaultType default type selector
     */
    private void clearDefaults(CustomerProfile profile, DefaultAddressType defaultType) {
        List<CustomerAddress> addresses = customerAddressRepository.findByCustomerOrderByUpdatedAtDesc(profile);
        for (CustomerAddress address : addresses) {
            boolean changed = false;
            if (defaultType == DefaultAddressType.SHIPPING || defaultType == DefaultAddressType.BOTH) {
                if (address.isDefaultShipping()) {
                    address.setDefaultShipping(false);
                    changed = true;
                }
            }
            if (defaultType == DefaultAddressType.BILLING || defaultType == DefaultAddressType.BOTH) {
                if (address.isDefaultBilling()) {
                    address.setDefaultBilling(false);
                    changed = true;
                }
            }
            if (changed) {
                customerAddressRepository.save(address);
            }
        }
        if (defaultType == DefaultAddressType.SHIPPING || defaultType == DefaultAddressType.BOTH) {
            profile.setDefaultShippingAddressId(null);
        }
        if (defaultType == DefaultAddressType.BILLING || defaultType == DefaultAddressType.BOTH) {
            profile.setDefaultBillingAddressId(null);
        }
    }

    /**
     * Clears the current default payment method for the profile.
     *
     * @param profile profile owner
     */
    private void clearDefaultPaymentMethod(CustomerProfile profile) {
        List<CustomerPaymentMethod> paymentMethods = customerPaymentMethodRepository.findByCustomerOrderByUpdatedAtDesc(profile);
        for (CustomerPaymentMethod paymentMethod : paymentMethods) {
            if (paymentMethod.isDefaultMethod()) {
                paymentMethod.setDefaultMethod(false);
                customerPaymentMethodRepository.save(paymentMethod);
            }
        }
    }

    /**
     * Validates latitude/longitude pair constraints.
     *
     * @param latitude latitude
     * @param longitude longitude
     */
    private void validateCoordinatePair(BigDecimal latitude, BigDecimal longitude) {
        boolean hasLat = latitude != null;
        boolean hasLng = longitude != null;
        if (hasLat != hasLng) {
            throw new CustomerOperationException(
                    HttpStatus.BAD_REQUEST,
                    "ADDRESS_COORDINATES_INVALID",
                    "Latitude and longitude must be provided together"
            );
        }
    }

    /**
     * Maps profile entity to account response DTO.
     *
     * @param profile profile entity
     * @return profile response DTO
     */
    private CustomerProfileResponse toProfileResponse(CustomerProfile profile) {
        return new CustomerProfileResponse(
                profile.getId(),
                profile.getExternalSubject(),
                profile.getFullName(),
                profile.getEmail(),
                profile.getPhone(),
                profile.isEnabled(),
                profile.getDefaultShippingAddressId(),
                profile.getDefaultBillingAddressId()
        );
    }

    /**
     * Maps address entity to response DTO.
     *
     * @param address address entity
     * @return address response DTO
     */
    private CustomerAddressResponse toAddressResponse(CustomerAddress address) {
        return new CustomerAddressResponse(
                address.getId(),
                address.getLabel(),
                address.getFullName(),
                address.getPhone(),
                address.getLine1(),
                address.getLine2(),
                address.getDistrict(),
                address.getCity(),
                address.getStateProvince(),
                address.getPostalCode(),
                address.getCountryCode(),
                address.getLatitude(),
                address.getLongitude(),
                address.getAccuracyMeters(),
                address.getPlaceId(),
                address.getFormattedAddress(),
                address.getDeliveryInstructions(),
                address.getValidationStatus(),
                address.isDefaultShipping(),
                address.isDefaultBilling(),
                address.isDefaultShipping() || address.isDefaultBilling()
        );
    }

    /**
     * Maps profile entity to internal lookup response.
     *
     * @param profile profile entity
     * @return internal lookup response
     */
    private CustomerLookupResponse toLookupResponse(CustomerProfile profile) {
        return new CustomerLookupResponse(
                profile.getId(),
                profile.getExternalSubject(),
                profile.getFullName(),
                profile.getEmail(),
                profile.getPhone(),
                profile.isEnabled(),
                profile.getDefaultShippingAddressId(),
                profile.getDefaultBillingAddressId()
        );
    }

    /**
     * Maps payment-method entity to response DTO.
     *
     * @param paymentMethod payment-method entity
     * @return payment-method response DTO
     */
    private CustomerPaymentMethodResponse toPaymentMethodResponse(CustomerPaymentMethod paymentMethod) {
        return new CustomerPaymentMethodResponse(
                paymentMethod.getId(),
                paymentMethod.getMethodType(),
                paymentMethod.getProvider(),
                paymentMethod.getTokenizedReference(),
                paymentMethod.isDefaultMethod()
        );
    }

    /**
     * Trims a string and normalizes blanks to null.
     *
     * @param value source value
     * @return normalized value or null
     */
    private String trimToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
