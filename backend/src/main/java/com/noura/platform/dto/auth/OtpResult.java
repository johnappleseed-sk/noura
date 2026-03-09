package com.noura.platform.dto.auth;

import com.noura.platform.commerce.entity.UserRole;

import java.util.List;

public record OtpResult(OtpStatus status,
                        String accessToken,
                        long expiresInSeconds,
                        UserRole role,
                        List<String> permissions,
                        Long lockedMinutes) {

    public static OtpResult invalidOtp() {
        return new OtpResult(OtpStatus.INVALID_OTP, null, 0, null, List.of(), null);
    }

    public static OtpResult disabled() {
        return new OtpResult(OtpStatus.DISABLED, null, 0, null, List.of(), null);
    }

    public static OtpResult locked(long lockedMinutes) {
        return new OtpResult(OtpStatus.LOCKED, null, 0, null, List.of(), lockedMinutes);
    }

    public static OtpResult success(String accessToken,
                                    long expiresInSeconds,
                                    UserRole role,
                                    List<String> permissions) {
        return new OtpResult(OtpStatus.SUCCESS, accessToken, expiresInSeconds, role, permissions, null);
    }
}
