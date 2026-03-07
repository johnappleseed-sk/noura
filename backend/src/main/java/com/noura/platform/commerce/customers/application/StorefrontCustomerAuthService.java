package com.noura.platform.commerce.customers.application;

import com.noura.platform.commerce.customers.domain.CustomerAccount;
import com.noura.platform.commerce.customers.domain.CustomerAccountStatus;
import com.noura.platform.commerce.customers.infrastructure.CustomerAccountRepo;
import com.noura.platform.commerce.service.JwtTokenService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Duration;
import java.util.Locale;


@Service
@Transactional
public class StorefrontCustomerAuthService {
    private final CustomerAccountRepo customerAccountRepo;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenService jwtTokenService;

    @Value("${app.security.jwt.access-token-minutes:720}")
    private long accessTokenMinutes;

    public StorefrontCustomerAuthService(CustomerAccountRepo customerAccountRepo,
                                         PasswordEncoder passwordEncoder,
                                         JwtTokenService jwtTokenService) {
        this.customerAccountRepo = customerAccountRepo;
        this.passwordEncoder = passwordEncoder;
        this.jwtTokenService = jwtTokenService;
    }

    public CustomerAuthResult register(String email, String password, String firstName, String lastName, String phone) {
        String normalizedEmail = normalizeEmail(email);
        validatePassword(password);

        if (customerAccountRepo.existsByEmailIgnoreCase(normalizedEmail)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Email already registered.");
        }

        CustomerAccount customer = new CustomerAccount();
        customer.setEmail(normalizedEmail);
        customer.setPasswordHash(passwordEncoder.encode(password));
        customer.setFirstName(trimOrNull(firstName));
        customer.setLastName(trimOrNull(lastName));
        customer.setPhone(trimOrNull(phone));
        customer.setStatus(CustomerAccountStatus.ACTIVE);
        customer.setEmailVerified(Boolean.TRUE);
        customer.setMarketingOptIn(Boolean.FALSE);
        CustomerAccount saved = customerAccountRepo.save(customer);

        return new CustomerAuthResult(
                saved.getId(),
                saved.getEmail(),
                saved.getFirstName(),
                saved.getLastName(),
                saved.getPhone(),
                saved.getStatus().name()
        );
    }

    public CustomerLoginResult login(String email, String password) {
        String normalizedEmail = normalizeEmail(email);
        CustomerAccount customer = customerAccountRepo.findByEmailIgnoreCase(normalizedEmail)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid credentials."));

        if (customer.getStatus() != CustomerAccountStatus.ACTIVE) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Account is not active.");
        }
        if (customer.getPasswordHash() == null || customer.getPasswordHash().isBlank()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid credentials.");
        }
        if (!passwordEncoder.matches(password == null ? "" : password, customer.getPasswordHash())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid credentials.");
        }

        String token = jwtTokenService.issueCustomerAccessToken(customer);
        long expiresInSeconds = Duration.ofMinutes(normalizeAccessMinutes(accessTokenMinutes)).toSeconds();
        return new CustomerLoginResult(
                token,
                expiresInSeconds,
                customer.getId(),
                customer.getEmail(),
                customer.getFirstName(),
                customer.getLastName(),
                customer.getPhone()
        );
    }

    public CustomerMeResult getCustomer(Long customerId) {
        CustomerAccount customer = customerAccountRepo.findById(customerId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Customer not found."));
        if (customer.getStatus() != CustomerAccountStatus.ACTIVE) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Customer is not active.");
        }

        return new CustomerMeResult(
                customer.getId(),
                customer.getEmail(),
                customer.getFirstName(),
                customer.getLastName(),
                customer.getPhone(),
                customer.getStatus().name(),
                customer.getEmailVerified(),
                customer.getMarketingOptIn()
        );
    }

    private String normalizeEmail(String email) {
        String normalized = email == null ? "" : email.trim().toLowerCase(Locale.ROOT);
        if (!normalized.matches("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "A valid email is required.");
        }
        return normalized;
    }

    private String trimOrNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isBlank() ? null : trimmed;
    }

    private void validatePassword(String password) {
        if (password == null || password.length() < 8) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Password must be at least 8 characters.");
        }
    }

    private long normalizeAccessMinutes(long configured) {
        return configured <= 0 ? 5 : configured;
    }

    public record CustomerAuthResult(Long id,
                                    String email,
                                    String firstName,
                                    String lastName,
                                    String phone,
                                    String status) {
    }

    public record CustomerLoginResult(String accessToken,
                                     long expiresInSeconds,
                                     Long id,
                                     String email,
                                     String firstName,
                                     String lastName,
                                     String phone) {
    }

    public record CustomerMeResult(Long id,
                                  String email,
                                  String firstName,
                                  String lastName,
                                  String phone,
                                  String status,
                                  Boolean emailVerified,
                                  Boolean marketingOptIn) {
    }
}
