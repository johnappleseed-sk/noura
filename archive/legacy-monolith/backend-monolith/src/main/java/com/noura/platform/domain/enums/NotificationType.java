package com.noura.platform.domain.enums;

public enum NotificationType {
    ORDER,
    PAYMENT,
    INVENTORY,
    MERCHANT,
    STORE,
    SECURITY,
    SYSTEM,
    AI,
    PROMOTION,
    GENERAL;

    public static NotificationType fromCategory(NotificationCategory category) {
        if (category == null) {
            return GENERAL;
        }
        return switch (category) {
            case ORDER -> ORDER;
            case STORE -> STORE;
            case SECURITY -> SECURITY;
            case SYSTEM -> SYSTEM;
            case AI -> AI;
        };
    }
}
