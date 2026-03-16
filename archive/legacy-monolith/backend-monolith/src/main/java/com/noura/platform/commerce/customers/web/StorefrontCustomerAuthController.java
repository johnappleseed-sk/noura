package com.noura.platform.commerce.customers.web;

import com.noura.platform.commerce.api.v1.dto.common.ApiEnvelope;
import com.noura.platform.commerce.api.v1.support.ApiTrace;
import com.noura.platform.commerce.customers.application.StorefrontCustomerAuthService;
import com.noura.platform.commerce.customers.domain.StorefrontCustomerPrincipal;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.context.annotation.Profile;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import static org.springframework.http.HttpStatus.UNAUTHORIZED;

import org.springframework.web.server.ResponseStatusException;

@Profile("legacy-storefront")
@RestController
@RequestMapping("/api/storefront/v1/customers")
public class StorefrontCustomerAuthController {
    private final StorefrontCustomerAuthService authService;

    public StorefrontCustomerAuthController(StorefrontCustomerAuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/register")
    public ApiEnvelope<StorefrontCustomerRegistrationResponse> register(@Valid @RequestBody StorefrontCustomerRegisterRequest request,
                                                                     HttpServletRequest requestContext) {
        var result = authService.register(
                request.email(),
                request.password(),
                request.firstName(),
                request.lastName(),
                request.phone()
        );
        return ApiEnvelope.success(
                "CUSTOMER_REGISTER_OK",
                "Customer registered successfully.",
                new StorefrontCustomerRegistrationResponse(
                        result.id(),
                        result.email(),
                        result.firstName(),
                        result.lastName(),
                        result.phone(),
                        result.status()
                ),
                ApiTrace.resolve(requestContext)
        );
    }

    @PostMapping("/login")
    public ApiEnvelope<StorefrontCustomerLoginResponse> login(@Valid @RequestBody StorefrontCustomerLoginRequest request,
                                                             HttpServletRequest requestContext) {
        var result = authService.login(request.email(), request.password());
        return ApiEnvelope.success(
                "CUSTOMER_LOGIN_OK",
                "Customer login successful.",
                new StorefrontCustomerLoginResponse(
                        result.accessToken(),
                        result.expiresInSeconds(),
                        new StorefrontCustomerMeResponse(
                                result.id(),
                                result.email(),
                                result.firstName(),
                                result.lastName(),
                                result.phone()
                        )
                ),
                ApiTrace.resolve(requestContext)
        );
    }

    @GetMapping("/me")
    public ApiEnvelope<StorefrontCustomerMeResponse> me(Authentication authentication, HttpServletRequest requestContext) {
        StorefrontCustomerPrincipal principal = resolveCustomerPrincipal(authentication);
        var result = authService.getCustomer(principal.id());
        return ApiEnvelope.success(
                "CUSTOMER_ME_OK",
                "Customer profile fetched successfully.",
                new StorefrontCustomerMeResponse(
                        result.id(),
                        result.email(),
                        result.firstName(),
                        result.lastName(),
                        result.phone()
                ),
                ApiTrace.resolve(requestContext)
        );
    }

    private StorefrontCustomerPrincipal resolveCustomerPrincipal(Authentication authentication) {
        if (authentication == null || authentication.getPrincipal() == null) {
            throw new ResponseStatusException(UNAUTHORIZED, "Authentication required.");
        }
        Object principal = authentication.getPrincipal();
        if (!(principal instanceof StorefrontCustomerPrincipal customerPrincipal)) {
            throw new ResponseStatusException(UNAUTHORIZED, "Customer authentication required.");
        }
        if (customerPrincipal.id() == null || customerPrincipal.id() <= 0) {
            throw new ResponseStatusException(UNAUTHORIZED, "Invalid customer token.");
        }
        return customerPrincipal;
    }

    public record StorefrontCustomerRegisterRequest(
            @Email(message = "email is invalid")
            @NotBlank(message = "email is required")
            @Size(max = 160, message = "email length must be <= 160")
            String email,

            @NotBlank(message = "password is required")
            @Size(min = 8, max = 128, message = "password length must be between 8 and 128")
            String password,

            String firstName,
            String lastName,
            String phone
    ) {
    }

    public record StorefrontCustomerLoginRequest(
            @Email(message = "email is invalid")
            @NotBlank(message = "email is required")
            @Size(max = 160, message = "email length must be <= 160")
            String email,

            @NotBlank(message = "password is required")
            @Size(min = 8, max = 128, message = "password length must be between 8 and 128")
            String password
    ) {
    }

    public record StorefrontCustomerRegistrationResponse(Long id,
                                                        String email,
                                                        String firstName,
                                                        String lastName,
                                                        String phone,
                                                        String status) {
    }

    public record StorefrontCustomerMeResponse(Long id,
                                             String email,
                                             String firstName,
                                             String lastName,
                                             String phone) {
    }

    public record StorefrontCustomerLoginResponse(String accessToken,
                                                  long expiresInSeconds,
                                                  StorefrontCustomerMeResponse customer) {
    }
}
