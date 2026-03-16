package com.noura.platform.inventory.security;

import com.noura.platform.common.exception.ApiException;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@Component
@RequiredArgsConstructor
public class InventoryJwtAuthenticationFilter extends OncePerRequestFilter {

    private final InventoryTokenService inventoryTokenService;
    private final InventoryIdentityService inventoryIdentityService;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        if (SecurityContextHolder.getContext().getAuthentication() == null) {
            String header = request.getHeader(HttpHeaders.AUTHORIZATION);
            if (StringUtils.hasText(header) && header.startsWith("Bearer ")) {
                String token = header.substring(7).trim();
                if (StringUtils.hasText(token)) {
                    try {
                        Claims claims = inventoryTokenService.parseAccessToken(token);
                        String userId = claims.getSubject();
                        InventoryUserPrincipal principal = inventoryIdentityService.loadPrincipalByUserId(userId);
                        List<SimpleGrantedAuthority> authorities = new ArrayList<>();
                        principal.roles().forEach(role -> authorities.add(new SimpleGrantedAuthority("ROLE_" + role)));
                        principal.permissions().forEach(permission -> authorities.add(new SimpleGrantedAuthority(permission)));
                        UsernamePasswordAuthenticationToken authentication = UsernamePasswordAuthenticationToken.authenticated(
                                principal,
                                token,
                                authorities
                        );
                        authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                        SecurityContextHolder.getContext().setAuthentication(authentication);
                    } catch (JwtException | IllegalArgumentException | ApiException ex) {
                        // Token is not a valid inventory JWT. Do not fail the request here because the monolith
                        // can also authenticate the same request using the platform JWT filter.
                        SecurityContextHolder.clearContext();
                    }
                }
            }
        }
        filterChain.doFilter(request, response);
    }
}
