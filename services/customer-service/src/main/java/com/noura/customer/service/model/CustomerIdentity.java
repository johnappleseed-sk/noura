package com.noura.customer.service.model;

/**
 * Resolved customer identity used by account APIs.
 *
 * @param externalSubject stable external subject key
 * @param emailHint optional email hint from gateway-forwarded claim
 */
public record CustomerIdentity(
        String externalSubject,
        String emailHint
) {
}
