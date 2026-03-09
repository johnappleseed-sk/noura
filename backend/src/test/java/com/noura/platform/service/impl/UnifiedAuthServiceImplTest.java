package com.noura.platform.service.impl;

import com.noura.platform.domain.enums.RoleType;
import com.noura.platform.dto.auth.AuthTokensResponse;
import com.noura.platform.dto.auth.LoginRequest;
import com.noura.platform.dto.auth.LoginResult;
import com.noura.platform.dto.auth.LoginStatus;
import com.noura.platform.service.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.ObjectProvider;

import java.util.Iterator;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UnifiedAuthServiceImplTest {

    @Mock
    private AuthService platformAuthService;

    private UnifiedAuthServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new UnifiedAuthServiceImpl(platformAuthService, provider(null));
    }

    @Test
    void loginDelegatesToPlatformAuthService() {
        LoginRequest request = new LoginRequest("admin@noura.local", "Admin123!");
        AuthTokensResponse expected = new AuthTokensResponse(
                UUID.randomUUID(),
                "admin@noura.local",
                "Local Demo Admin",
                Set.of(RoleType.ADMIN),
                "access-token",
                "refresh-token"
        );
        when(platformAuthService.login(request)).thenReturn(expected);

        AuthTokensResponse actual = service.login(request);

        assertThat(actual).isSameAs(expected);
        verify(platformAuthService).login(request);
    }

    @Test
    void loginCommerceWithIdentifierDelegatesWhenLegacyServiceIsAvailable() {
        HttpServletRequest request = mock(HttpServletRequest.class);
        var expected = new LoginResult(
                LoginStatus.OTP_REQUIRED,
                42L,
                "challenge-token",
                false,
                null,
                null,
                null
        );
        var commerceAuthService = legacyCommerceAuthService(expected);
        service = new UnifiedAuthServiceImpl(platformAuthService, provider(commerceAuthService));

        var actual = service.loginCommerceWithIdentifier("cashier", "Secret123!", request);

        assertThat(actual).isEqualTo(expected);
    }

    @Test
    void loginCommerceWithIdentifierFailsFastWhenLegacyServiceIsInactive() {
        assertThatThrownBy(() -> service.loginCommerceWithIdentifier("cashier", "Secret123!", mock(HttpServletRequest.class)))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Legacy commerce auth service is not active");
    }

    private ObjectProvider<com.noura.platform.commerce.service.AuthService> provider(
            com.noura.platform.commerce.service.AuthService authService
    ) {
        return new ObjectProvider<>() {
            @Override
            public com.noura.platform.commerce.service.AuthService getObject(Object... args) {
                return getIfAvailable();
            }

            @Override
            public com.noura.platform.commerce.service.AuthService getIfAvailable() {
                return authService;
            }

            @Override
            public com.noura.platform.commerce.service.AuthService getIfUnique() {
                return authService;
            }

            @Override
            public com.noura.platform.commerce.service.AuthService getObject() {
                if (authService == null) {
                    throw new IllegalStateException("Legacy commerce auth service is not active in the current runtime profile.");
                }
                return authService;
            }

            @Override
            public Iterator<com.noura.platform.commerce.service.AuthService> iterator() {
                return authService == null ? java.util.List.<com.noura.platform.commerce.service.AuthService>of().iterator()
                        : java.util.List.of(authService).iterator();
            }

            @Override
            public Stream<com.noura.platform.commerce.service.AuthService> stream() {
                return authService == null ? Stream.empty() : Stream.of(authService);
            }
        };
    }

    private com.noura.platform.commerce.service.AuthService legacyCommerceAuthService(
            LoginResult loginResult
    ) {
        return new com.noura.platform.commerce.service.AuthService(null, null, null, null, null, null, null) {
            @Override
            public LoginResult loginWithIdentifier(String identifier, String password, HttpServletRequest request) {
                return loginResult;
            }
        };
    }
}
