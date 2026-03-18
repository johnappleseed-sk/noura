package com.noura.customer.service;

import com.noura.customer.domain.enums.DefaultAddressType;
import com.noura.customer.dto.address.CustomerAddressRequest;
import com.noura.customer.dto.address.CustomerAddressResponse;
import com.noura.customer.dto.internal.CustomerLookupResponse;
import com.noura.customer.dto.payment.CustomerPaymentMethodRequest;
import com.noura.customer.dto.payment.CustomerPaymentMethodResponse;
import com.noura.customer.dto.profile.CustomerProfileResponse;
import com.noura.customer.dto.profile.UpdateCustomerProfileRequest;
import com.noura.customer.service.model.CustomerIdentity;

import java.util.List;
import java.util.UUID;

/**
 * Customer account service contract for profile and address book operations.
 */
public interface CustomerAccountService {

    /**
     * Loads or lazily creates current customer profile for provided identity.
     *
     * @param identity resolved customer identity
     * @return customer profile response
     */
    CustomerProfileResponse getOrCreateCurrentProfile(CustomerIdentity identity);

    /**
     * Updates current customer profile.
     *
     * @param identity resolved customer identity
     * @param request profile update payload
     * @return updated profile response
     */
    CustomerProfileResponse updateCurrentProfile(CustomerIdentity identity, UpdateCustomerProfileRequest request);

    /**
     * Lists current customer addresses.
     *
     * @param identity resolved customer identity
     * @return address list
     */
    List<CustomerAddressResponse> listAddresses(CustomerIdentity identity);

    /**
     * Retrieves one current-customer address.
     *
     * @param identity resolved customer identity
     * @param addressId address identifier
     * @return address response
     */
    CustomerAddressResponse getAddress(CustomerIdentity identity, UUID addressId);

    /**
     * Adds address for current customer.
     *
     * @param identity resolved customer identity
     * @param request address payload
     * @return created address response
     */
    CustomerAddressResponse addAddress(CustomerIdentity identity, CustomerAddressRequest request);

    /**
     * Updates one current-customer address.
     *
     * @param identity resolved customer identity
     * @param addressId address identifier
     * @param request address payload
     * @return updated address response
     */
    CustomerAddressResponse updateAddress(CustomerIdentity identity, UUID addressId, CustomerAddressRequest request);

    /**
     * Deletes one current-customer address.
     *
     * @param identity resolved customer identity
     * @param addressId address identifier
     */
    void deleteAddress(CustomerIdentity identity, UUID addressId);

    /**
     * Sets default address flags for one address.
     *
     * @param identity resolved customer identity
     * @param addressId address identifier
     * @param defaultType default flag selector
     * @return updated address response
     */
    CustomerAddressResponse setDefaultAddress(CustomerIdentity identity, UUID addressId, DefaultAddressType defaultType);

    /**
     * Lists current customer payment methods.
     *
     * @param identity resolved customer identity
     * @return payment methods
     */
    List<CustomerPaymentMethodResponse> listPaymentMethods(CustomerIdentity identity);

    /**
     * Adds one payment method for current customer.
     *
     * @param identity resolved customer identity
     * @param request payment method payload
     * @return created payment method
     */
    CustomerPaymentMethodResponse addPaymentMethod(CustomerIdentity identity, CustomerPaymentMethodRequest request);

    /**
     * Updates one payment method for current customer.
     *
     * @param identity resolved customer identity
     * @param paymentMethodId payment method identifier
     * @param request payment method payload
     * @return updated payment method
     */
    CustomerPaymentMethodResponse updatePaymentMethod(
            CustomerIdentity identity,
            UUID paymentMethodId,
            CustomerPaymentMethodRequest request
    );

    /**
     * Deletes one payment method for current customer.
     *
     * @param identity resolved customer identity
     * @param paymentMethodId payment method identifier
     */
    void deletePaymentMethod(CustomerIdentity identity, UUID paymentMethodId);

    /**
     * Internal lookup by customer profile ID.
     *
     * @param customerId customer profile identifier
     * @return lookup response
     */
    CustomerLookupResponse lookupById(UUID customerId);

    /**
     * Internal lookup by external subject.
     *
     * @param externalSubject external subject key
     * @return lookup response
     */
    CustomerLookupResponse lookupByExternalSubject(String externalSubject);
}
