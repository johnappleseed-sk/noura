package com.noura.platform.service.impl;

import com.noura.platform.dto.auth.AuthTokensResponse;
import com.noura.platform.dto.auth.LoginRequest;
import com.noura.platform.dto.auth.LoginResult;
import com.noura.platform.dto.auth.OtpResult;
import com.noura.platform.dto.auth.PasswordResetConfirmRequest;
import com.noura.platform.dto.auth.PasswordResetRequest;
import com.noura.platform.dto.auth.RefreshTokenRequest;
import com.noura.platform.dto.auth.RegisterRequest;
import com.noura.platform.dto.auth.RegisterResult;
import com.noura.platform.service.AuthService;
import com.noura.platform.service.UnifiedAuthService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UnifiedAuthServiceImpl implements UnifiedAuthService {

    private final AuthService platformAuthService;
    private final ObjectProvider<com.noura.platform.commerce.service.AuthService> commerceAuthServiceProvider;

    @Override
    public AuthTokensResponse register(RegisterRequest request) {
        return platformAuthService.register(request);
    }

    @Override
    public AuthTokensResponse login(LoginRequest request) {
        return platformAuthService.login(request);
    }

    @Override
    public AuthTokensResponse refresh(RefreshTokenRequest request) {
        return platformAuthService.refresh(request);
    }

    @Override
    public void requestPasswordReset(PasswordResetRequest request) {
        platformAuthService.requestPasswordReset(request);
    }

    @Override
    public void resetPassword(PasswordResetConfirmRequest request) {
        platformAuthService.resetPassword(request);
    }

    @Override
    public RegisterResult registerCommerce(String email, String password) {
        return commerceAuthService().register(email, password);
    }

    @Override
    public LoginResult loginCommerceWithPassword(
            String email,
            String password,
            HttpServletRequest request
    ) {
        return commerceAuthService().loginWithPassword(email, password, request);
    }

    @Override
    public LoginResult loginCommerceWithIdentifier(
            String identifier,
            String password,
            HttpServletRequest request
    ) {
        return commerceAuthService().loginWithIdentifier(identifier, password, request);
    }

    @Override
    public OtpResult verifyCommerceOtp(
            String challengeToken,
            String otpCode,
            HttpServletRequest request
    ) {
        return commerceAuthService().verifyOtp(challengeToken, otpCode, request);
    }

    @Override
    public OtpResult verifyCommerceOtpForIdentifier(
            String identifier,
            String otpCode,
            HttpServletRequest request
    ) {
        return commerceAuthService().verifyOtpForIdentifier(identifier, otpCode, request);
    }

    @Override
    public OtpResult verifyCommerceOtpForUserId(
            Long userId,
            String otpCode,
            HttpServletRequest request
    ) {
        return commerceAuthService().verifyOtpForUserId(userId, otpCode, request);
    }

    private com.noura.platform.commerce.service.AuthService commerceAuthService() {
        com.noura.platform.commerce.service.AuthService service = commerceAuthServiceProvider.getIfAvailable();
        if (service == null) {
            throw new IllegalStateException("Legacy commerce auth service is not active in the current runtime profile.");
        }
        return service;
    }
}
