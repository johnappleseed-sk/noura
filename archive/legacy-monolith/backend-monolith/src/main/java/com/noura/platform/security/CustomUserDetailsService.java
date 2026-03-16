package com.noura.platform.security;

import com.noura.platform.domain.entity.AdminUserRole;
import com.noura.platform.domain.entity.UserAccount;
import com.noura.platform.repository.AdminUserRoleRepository;
import com.noura.platform.repository.UserAccountRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    private final UserAccountRepository userAccountRepository;
    private final AdminUserRoleRepository adminUserRoleRepository;

    /**
     * Executes load user by username.
     *
     * @param email The email value.
     * @return The result of load user by username.
     * @throws UsernameNotFoundException If the operation cannot be completed.
     */
    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        UserAccount account = userAccountRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));

        Set<GrantedAuthority> authorities = account.getRoles()
                .stream()
                .map(roleType -> new SimpleGrantedAuthority("ROLE_" + roleType.name()))
                .collect(Collectors.toSet());

        Set<AdminUserRole> assignedAdminRoles = new LinkedHashSet<>(adminUserRoleRepository.findDetailedByUserId(account.getId()));
        assignedAdminRoles.stream()
                .map(AdminUserRole::getRole)
                .filter(role -> role != null && role.isActive())
                .map(role -> role.getCode())
                .filter(code -> code != null && !code.isBlank())
                .map(code -> new SimpleGrantedAuthority("ROLE_" + code.trim().toUpperCase(Locale.ROOT)))
                .forEach(authorities::add);

        assignedAdminRoles.stream()
                .map(AdminUserRole::getRole)
                .filter(role -> role != null && role.getRolePermissions() != null)
                .flatMap(role -> role.getRolePermissions().stream())
                .map(rolePermission -> rolePermission.getPermission())
                .filter(permission -> permission != null)
                .map(permission -> toPermissionAuthority(permission.getScope(), permission.getAction()))
                .filter(authority -> authority != null && !authority.isBlank())
                .map(SimpleGrantedAuthority::new)
                .forEach(authorities::add);

        return User.withUsername(account.getEmail())
                .password(account.getPasswordHash())
                .authorities(authorities)
                .disabled(!account.isEnabled())
                .build();
    }

    private String toPermissionAuthority(String scope, String action) {
        if (scope == null || action == null || scope.isBlank() || action.isBlank()) {
            return null;
        }
        String normalizedScope = scope.trim().toUpperCase(Locale.ROOT).replaceAll("[^A-Z0-9]+", "_");
        String normalizedAction = action.trim().toUpperCase(Locale.ROOT).replaceAll("[^A-Z0-9]+", "_");
        return "PERM_" + normalizedScope + "_" + normalizedAction;
    }
}
