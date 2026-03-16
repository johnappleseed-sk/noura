package com.noura.platform.inventory.config;

import com.noura.platform.inventory.security.InventorySecurityProperties;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@Component
@RequiredArgsConstructor
public class InventoryHeaderAuthenticationFilter extends OncePerRequestFilter {

    private static final String USER_HEADER = "X-Inventory-User";
    private static final String ROLES_HEADER = "X-Inventory-Roles";
    private static final String PERMISSIONS_HEADER = "X-Inventory-Permissions";

    private final InventorySecurityProperties securityProperties;

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !securityProperties.isDevHeaderAuthEnabled();
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        if (SecurityContextHolder.getContext().getAuthentication() == null) {
            String username = request.getHeader(USER_HEADER);
            if (StringUtils.hasText(username)) {
                List<GrantedAuthority> authorities = new ArrayList<>();
                authorities.addAll(parseRoles(request.getHeader(ROLES_HEADER)));
                authorities.addAll(parsePermissions(request.getHeader(PERMISSIONS_HEADER)));
                if (authorities.isEmpty()) {
                    authorities.add(new SimpleGrantedAuthority("ROLE_VIEWER"));
                }
                UsernamePasswordAuthenticationToken authentication = UsernamePasswordAuthenticationToken.authenticated(
                        username,
                        "N/A",
                        authorities
                );
                authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                SecurityContextHolder.getContext().setAuthentication(authentication);
            }
        }
        filterChain.doFilter(request, response);
    }

    private List<GrantedAuthority> parseRoles(String rolesHeader) {
        return splitHeader(rolesHeader)
                .stream()
                .map(role -> role.startsWith("ROLE_") ? role : "ROLE_" + role.toUpperCase(Locale.ROOT))
                .map(SimpleGrantedAuthority::new)
                .map(GrantedAuthority.class::cast)
                .toList();
    }

    private List<GrantedAuthority> parsePermissions(String permissionsHeader) {
        return splitHeader(permissionsHeader)
                .stream()
                .map(SimpleGrantedAuthority::new)
                .map(GrantedAuthority.class::cast)
                .toList();
    }

    private List<String> splitHeader(String rawHeader) {
        if (!StringUtils.hasText(rawHeader)) {
            return List.of();
        }
        return List.of(rawHeader.split(","))
                .stream()
                .map(String::trim)
                .filter(StringUtils::hasText)
                .toList();
    }
}
