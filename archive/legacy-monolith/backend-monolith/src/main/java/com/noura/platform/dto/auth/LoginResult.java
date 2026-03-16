package com.noura.platform.dto.auth;

public record LoginResult(LoginStatus status,
                          Long userId,
                          String challengeToken,
                          boolean firstTimeSetup,
                          String otpauthUrl,
                          String qrDataUrl,
                          Long lockedMinutes) {

    public static LoginResult invalidCredentials() {
        return new LoginResult(LoginStatus.INVALID_CREDENTIALS, null, null, false, null, null, null);
    }

    public static LoginResult disabled() {
        return new LoginResult(LoginStatus.DISABLED, null, null, false, null, null, null);
    }

    public static LoginResult locked(long lockedMinutes) {
        return new LoginResult(LoginStatus.LOCKED, null, null, false, null, null, lockedMinutes);
    }

    public static LoginResult otpRequired(Long userId,
                                          String challengeToken,
                                          boolean firstTimeSetup,
                                          String otpauthUrl,
                                          String qrDataUrl) {
        return new LoginResult(LoginStatus.OTP_REQUIRED, userId, challengeToken, firstTimeSetup, otpauthUrl, qrDataUrl, null);
    }
}
