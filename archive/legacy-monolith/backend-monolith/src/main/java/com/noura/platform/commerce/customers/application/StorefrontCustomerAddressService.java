package com.noura.platform.commerce.customers.application;

import com.noura.platform.commerce.customers.domain.CustomerAccount;
import com.noura.platform.commerce.customers.domain.CustomerAddress;
import com.noura.platform.commerce.customers.infrastructure.CustomerAccountRepo;
import com.noura.platform.commerce.customers.infrastructure.CustomerAddressRepo;
import com.noura.platform.dto.storefront.CustomerAddressDto;
import com.noura.platform.dto.storefront.StorefrontCustomerAddressRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;

@Service
@Transactional
public class StorefrontCustomerAddressService {
    private final CustomerAccountRepo customerAccountRepo;
    private final CustomerAddressRepo customerAddressRepo;

    public StorefrontCustomerAddressService(CustomerAccountRepo customerAccountRepo,
                                           CustomerAddressRepo customerAddressRepo) {
        this.customerAccountRepo = customerAccountRepo;
        this.customerAddressRepo = customerAddressRepo;
    }

    public List<CustomerAddressDto> listAddresses(Long customerId) {
        CustomerAccount customer = resolveCustomer(customerId);
        return customerAddressRepo.findByCustomerAccount_IdOrderByIdAsc(customer.getId())
                .stream()
                .map(this::toDto)
                .toList();
    }

    public CustomerAddressDto addAddress(Long customerId, StorefrontCustomerAddressRequest request) {
        CustomerAccount customer = resolveCustomer(customerId);
        List<CustomerAddress> existing = customerAddressRepo.findByCustomerAccount_IdOrderByIdAsc(customer.getId());

        boolean hasDefaultShipping = existing.stream().anyMatch(item -> Boolean.TRUE.equals(item.getDefaultShipping()));
        boolean hasDefaultBilling = existing.stream().anyMatch(item -> Boolean.TRUE.equals(item.getDefaultBilling()));

        boolean defaultShipping = request.defaultShipping();
        boolean defaultBilling = request.defaultBilling();
        if (!hasDefaultShipping && !defaultShipping) {
            defaultShipping = true;
        }
        if (!hasDefaultBilling && !defaultBilling) {
            defaultBilling = true;
        }

        if (defaultShipping) {
            clearDefaultShipping(existing);
        }
        if (defaultBilling) {
            clearDefaultBilling(existing);
        }

        CustomerAddress address = new CustomerAddress();
        address.setCustomerAccount(customer);
        address.setLabel(normalize(request.label()));
        address.setRecipientName(requireText("recipientName", request.recipientName(), 120));
        address.setPhone(normalize(request.phone()));
        address.setLine1(requireText("line1", request.line1(), 255));
        address.setLine2(normalize(request.line2()));
        address.setDistrict(normalize(request.district()));
        address.setCity(requireText("city", request.city(), 120));
        address.setStateProvince(normalize(request.stateProvince()));
        address.setPostalCode(normalize(request.postalCode()));
        address.setCountryCode(normalizeCountryCode(request.countryCode()));
        address.setDefaultShipping(defaultShipping);
        address.setDefaultBilling(defaultBilling);
        address.setCreatedAt(LocalDateTime.now());
        address.setUpdatedAt(LocalDateTime.now());

        CustomerAddress saved = customerAddressRepo.save(address);
        return toDto(saved);
    }

    public void deleteAddress(Long customerId, Long addressId) {
        if (!customerAddressRepo.existsByIdAndCustomerAccount_Id(addressId, customerId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Address not found.");
        }
        customerAddressRepo.deleteById(addressId);
    }

    private void clearDefaultShipping(List<CustomerAddress> existing) {
        for (CustomerAddress item : existing) {
            if (Boolean.TRUE.equals(item.getDefaultShipping())) {
                item.setDefaultShipping(Boolean.FALSE);
            }
        }
        if (!existing.isEmpty()) {
            customerAddressRepo.saveAll(existing);
        }
    }

    private void clearDefaultBilling(List<CustomerAddress> existing) {
        for (CustomerAddress item : existing) {
            if (Boolean.TRUE.equals(item.getDefaultBilling())) {
                item.setDefaultBilling(Boolean.FALSE);
            }
        }
        if (!existing.isEmpty()) {
            customerAddressRepo.saveAll(existing);
        }
    }

    private CustomerAccount resolveCustomer(Long customerId) {
        return customerAccountRepo.findById(customerId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Customer not found."));
    }

    private String normalize(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isBlank() ? null : trimmed;
    }

    private String requireText(String field, String value, int maxLength) {
        String normalized = normalize(value);
        if (normalized == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, field + " is required.");
        }
        if (normalized.length() > maxLength) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, field + " exceeds max length " + maxLength + ".");
        }
        return normalized;
    }

    private String normalizeCountryCode(String value) {
        String normalized = requireText("countryCode", normalize(value), 2).toUpperCase();
        if (normalized.length() != 2) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "countryCode must be two characters.");
        }
        return normalized;
    }

    private CustomerAddressDto toDto(CustomerAddress address) {
        return new CustomerAddressDto(
                address.getId(),
                address.getLabel(),
                address.getRecipientName(),
                address.getPhone(),
                address.getLine1(),
                address.getLine2(),
                address.getDistrict(),
                address.getCity(),
                address.getStateProvince(),
                address.getPostalCode(),
                address.getCountryCode(),
                Boolean.TRUE.equals(address.getDefaultShipping()),
                Boolean.TRUE.equals(address.getDefaultBilling()),
                address.getCreatedAt(),
                address.getUpdatedAt()
        );
    }
}
