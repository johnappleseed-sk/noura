package com.noura.platform.service;

import com.noura.platform.dto.auth.*;

public interface AuthService {
    /**
     * Executes register.
     *
     * @param request The request payload for this operation.
     * @return The result of register.
     */
    AuthTokensResponse register(RegisterRequest request);

    /**
     * Executes login.
     *
     * @param request The request payload for this operation.
     * @return The result of login.
     */
    AuthTokensResponse login(LoginRequest request);

    /**
     * Executes refresh.
     *
     * @param request The request payload for this operation.
     * @return The result of refresh.
     */
    AuthTokensResponse refresh(RefreshTokenRequest request);

    /**
     * Executes request password reset.
     *
     * @param request The request payload for this operation.
     */
    void requestPasswordReset(PasswordResetRequest request);

    /**
     * Executes reset password.
     *
     * @param request The request payload for this operation.
     */
    void resetPassword(PasswordResetConfirmRequest request);
}
