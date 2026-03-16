package com.company.platform.auth.service.impl;

import com.company.platform.auth.dto.CurrentUserResponse;
import com.company.platform.auth.dto.LoginRequest;
import com.company.platform.auth.dto.LoginResponse;
import com.company.platform.auth.entity.Permission;
import com.company.platform.auth.entity.User;
import com.company.platform.auth.repository.UserRepository;
import com.company.platform.auth.service.AuthService;
import com.company.platform.exception.PlatformException;
import com.noura.platform.config.AppProperties;
import com.noura.platform.domain.enums.RoleType;
import com.noura.platform.security.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AuthServiceImpl implements AuthService {

    private final AuthenticationManager authenticationManager;
    private final UserRepository userRepository;
    private final JwtTokenProvider jwtTokenProvider;
    private final AppProperties appProperties;

    @Override
    public LoginResponse login(LoginRequest request) {
        String email = normalize(request.email());

        try {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(email, request.password())
            );
        } catch (BadCredentialsException | DisabledException ex) {
            throw new PlatformException(HttpStatus.UNAUTHORIZED, "INVALID_CREDENTIALS", "Invalid email or password.");
        }

        User user = userRepository.findDetailedByEmail(email)
                .orElseThrow(() -> new PlatformException(HttpStatus.UNAUTHORIZED, "USER_NOT_FOUND", "Authenticated user could not be loaded."));

        Set<RoleType> baseRoles = user.getBaseRoles().stream()
                .map(role -> {
                    try {
                        return RoleType.valueOf(role.toUpperCase(Locale.ROOT));
                    } catch (IllegalArgumentException ignored) {
                        return null;
                    }
                })
                .filter(role -> role != null)
                .collect(java.util.stream.Collectors.toCollection(LinkedHashSet::new));

        if (baseRoles.isEmpty()) {
            baseRoles.add(RoleType.CUSTOMER);
        }

        String token = jwtTokenProvider.generateAccessToken(user.getId(), user.getEmail(), baseRoles);

        return new LoginResponse(
                token,
                "Bearer",
                appProperties.getJwt().getAccessTokenValidityMinutes() * 60,
                toCurrentUserResponse(user)
        );
    }

    @Override
    public CurrentUserResponse currentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || authentication.getName() == null || "anonymousUser".equals(authentication.getName())) {
            throw new PlatformException(HttpStatus.UNAUTHORIZED, "AUTH_REQUIRED", "Authentication is required.");
        }

        User user = userRepository.findDetailedByEmail(authentication.getName())
                .orElseThrow(() -> new PlatformException(HttpStatus.UNAUTHORIZED, "USER_NOT_FOUND", "Authenticated user could not be loaded."));

        return toCurrentUserResponse(user);
    }

    public CurrentUserResponse toCurrentUserResponse(User user) {
        return new CurrentUserResponse(
                user.getId(),
                user.getUsername(),
                user.getEmail(),
                user.getStatus(),
                resolveRoleCodes(user),
                resolvePermissionCodes(user)
        );
    }

    private Set<String> resolveRoleCodes(User user) {
        Set<String> roles = new LinkedHashSet<>(user.getBaseRoles());
        user.getUserRoles().stream()
                .map(userRole -> userRole.getRole())
                .filter(role -> role != null && role.isActive())
                .map(role -> role.getCode())
                .filter(code -> code != null && !code.isBlank())
                .map(code -> code.toUpperCase(Locale.ROOT))
                .forEach(roles::add);
        return roles;
    }

    private Set<String> resolvePermissionCodes(User user) {
        return user.getUserRoles().stream()
                .map(userRole -> userRole.getRole())
                .filter(role -> role != null && role.isActive())
                .flatMap(role -> role.getRolePermissions().stream())
                .map(rolePermission -> rolePermission.getPermission())
                .filter(permission -> permission != null)
                .map(Permission::getCode)
                .filter(code -> code != null && !code.isBlank())
                .map(code -> "PERM_" + code)
                .sorted(Comparator.naturalOrder())
                .collect(java.util.stream.Collectors.toCollection(LinkedHashSet::new));
    }

    private String normalize(String value) {
        return value == null ? null : value.trim().toLowerCase(Locale.ROOT);
    }
}
