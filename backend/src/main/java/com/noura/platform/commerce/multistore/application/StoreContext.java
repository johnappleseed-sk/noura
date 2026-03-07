package com.noura.platform.commerce.multistore.application;

/**
 * Thread-local store context for multi-store operations.
 * Set by filter/interceptor based on request headers, domain, or user session.
 */
public class StoreContext {
    private static final ThreadLocal<Long> currentStoreId = new ThreadLocal<>();

    public static void setStoreId(Long storeId) {
        currentStoreId.set(storeId);
    }

    public static Long getStoreId() {
        return currentStoreId.get();
    }

    public static void clear() {
        currentStoreId.remove();
    }

    public static boolean hasStore() {
        return currentStoreId.get() != null;
    }
}
