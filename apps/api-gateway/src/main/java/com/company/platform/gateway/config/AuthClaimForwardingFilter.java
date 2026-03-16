package com.company.platform.gateway.config;

import org.springframework.core.Ordered;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import reactor.core.publisher.Mono;

import java.util.List;

@Component
public class AuthClaimForwardingFilter implements GlobalFilter, Ordered {

    private final GatewayAuthProperties gatewayAuthProperties;

    public AuthClaimForwardingFilter(GatewayAuthProperties gatewayAuthProperties) {
        this.gatewayAuthProperties = gatewayAuthProperties;
    }

    @Override
    public int getOrder() {
        return Ordered.LOWEST_PRECEDENCE - 100;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        if (!gatewayAuthProperties.isForwardClaims()) {
            return chain.filter(exchange);
        }

        return exchange.getPrincipal()
                .cast(Authentication.class)
                .filter(this::isAuthenticatedUser)
                .map(authentication -> enrichExchange(exchange, authentication))
                .defaultIfEmpty(exchange)
                .flatMap(chain::filter);
    }

    private boolean isAuthenticatedUser(Authentication authentication) {
        return authentication != null
                && authentication.isAuthenticated()
                && !(authentication instanceof AnonymousAuthenticationToken);
    }

    private ServerWebExchange enrichExchange(ServerWebExchange exchange, Authentication authentication) {
        String subject = resolveSubject(authentication);
        String username = resolveUsername(authentication);
        String roles = resolveRoles(authentication);

        ServerHttpRequest.Builder requestBuilder = exchange.getRequest().mutate();
        if (StringUtils.hasText(subject)) {
            requestBuilder.header(gatewayAuthProperties.getSubjectHeader(), subject);
        }
        if (StringUtils.hasText(username)) {
            requestBuilder.header(gatewayAuthProperties.getUsernameHeader(), username);
        }
        if (StringUtils.hasText(roles)) {
            requestBuilder.header(gatewayAuthProperties.getRolesHeader(), roles);
        }
        return exchange.mutate().request(requestBuilder.build()).build();
    }

    private String resolveSubject(Authentication authentication) {
        if (authentication instanceof JwtAuthenticationToken jwtAuthenticationToken) {
            return jwtAuthenticationToken.getToken().getSubject();
        }
        if (authentication.getPrincipal() instanceof Jwt jwt) {
            return jwt.getSubject();
        }
        return authentication.getName();
    }

    private String resolveUsername(Authentication authentication) {
        if (authentication instanceof JwtAuthenticationToken jwtAuthenticationToken) {
            Object preferredUsername = jwtAuthenticationToken.getToken().getClaims().get("preferred_username");
            if (preferredUsername instanceof String preferredUsernameValue && StringUtils.hasText(preferredUsernameValue)) {
                return preferredUsernameValue;
            }
        }
        return authentication.getName();
    }

    private String resolveRoles(Authentication authentication) {
        List<String> roles = authentication.getAuthorities().stream()
                .map(grantedAuthority -> grantedAuthority.getAuthority())
                .filter(StringUtils::hasText)
                .toList();
        return String.join(",", roles);
    }
}
