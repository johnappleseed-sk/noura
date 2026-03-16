package com.noura.platform.service;

import com.noura.platform.dto.auth.AuthTokensResponse;
import com.noura.platform.dto.auth.LoginRequest;
import com.noura.platform.dto.auth.LoginResult;
import com.noura.platform.dto.auth.OtpResult;
import com.noura.platform.dto.auth.PasswordResetConfirmRequest;
import com.noura.platform.dto.auth.PasswordResetRequest;
import com.noura.platform.dto.auth.RefreshTokenRequest;
import com.noura.platform.dto.auth.RegisterRequest;
import com.noura.platform.dto.auth.RegisterResult;
import jakarta.servlet.http.HttpServletRequest;

public interface UnifiedAuthService {
    AuthTokensResponse register(RegisterRequest request);

    AuthTokensResponse login(LoginRequest request);

    AuthTokensResponse refresh(RefreshTokenRequest request);

    void requestPasswordReset(PasswordResetRequest request);

    void resetPassword(PasswordResetConfirmRequest request);

    RegisterResult registerCommerce(String email, String password);

    LoginResult loginCommerceWithPassword(
            String email,
            String password,
            HttpServletRequest request
    );

    LoginResult loginCommerceWithIdentifier(
            String identifier,
            String password,
            HttpServletRequest request
    );

    OtpResult verifyCommerceOtp(
            String challengeToken,
            String otpCode,
            HttpServletRequest request
    );

    OtpResult verifyCommerceOtpForIdentifier(
            String identifier,
            String otpCode,
            HttpServletRequest request
    );

    OtpResult verifyCommerceOtpForUserId(
            Long userId,
            String otpCode,
            HttpServletRequest request
    );
}
