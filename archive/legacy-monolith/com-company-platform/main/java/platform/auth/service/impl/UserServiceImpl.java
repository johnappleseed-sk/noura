package com.company.platform.auth.service.impl;

import com.company.platform.auth.dto.CreateUserRequest;
import com.company.platform.auth.dto.UserResponse;
import com.company.platform.auth.entity.Permission;
import com.company.platform.auth.entity.Role;
import com.company.platform.auth.entity.User;
import com.company.platform.auth.entity.UserRole;
import com.company.platform.auth.entity.id.UserRoleId;
import com.company.platform.auth.enums.UserStatus;
import com.company.platform.auth.repository.RoleRepository;
import com.company.platform.auth.repository.UserRepository;
import com.company.platform.auth.repository.UserRoleRepository;
import com.company.platform.auth.service.UserService;
import com.company.platform.exception.PlatformException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class UserServiceImpl implements UserService {

    private static final Set<String> COMPATIBILITY_BASE_ROLES = Set.of("ADMIN", "CUSTOMER", "B2B");

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final UserRoleRepository userRoleRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public UserResponse createUser(CreateUserRequest request) {
        String email = normalize(request.email());
        String username = normalize(request.username());

        if (userRepository.existsByEmailIgnoreCase(email)) {
            throw new PlatformException(HttpStatus.CONFLICT, "EMAIL_ALREADY_EXISTS", "Email is already in use.");
        }
        if (userRepository.existsByUsernameIgnoreCase(username)) {
            throw new PlatformException(HttpStatus.CONFLICT, "USERNAME_ALREADY_EXISTS", "Username is already in use.");
        }

        Set<String> normalizedRoleCodes = normalizeRoleCodes(request.roleCodes());
        final Set<String> requestedRoleCodes = normalizedRoleCodes.isEmpty()
                ? Set.of("CUSTOMER")
                : normalizedRoleCodes;

        List<Role> roles = roleRepository.findAll().stream()
                .filter(role -> role.getCode() != null && requestedRoleCodes.contains(role.getCode().trim().toUpperCase(Locale.ROOT)))
                .toList();

        if (roles.size() != requestedRoleCodes.size()) {
            Set<String> foundCodes = roles.stream()
                    .map(Role::getCode)
                    .map(code -> code.toUpperCase(Locale.ROOT))
                    .collect(Collectors.toSet());
            Set<String> missingCodes = requestedRoleCodes.stream()
                    .filter(code -> !foundCodes.contains(code))
                    .collect(Collectors.toCollection(LinkedHashSet::new));
            throw new PlatformException(HttpStatus.BAD_REQUEST, "ROLE_NOT_FOUND", "Unknown role codes: " + String.join(", ", missingCodes));
        }

        User user = new User();
        user.setUsername(username);
        user.setEmail(email);
        user.setPasswordHash(passwordEncoder.encode(request.password()));
        user.setStatus(UserStatus.ACTIVE);
        user.setEnabled(true);
        user.setFullName(request.username().trim());
        user.setBaseRoles(roles.stream()
                .map(Role::getCode)
                .map(code -> code.toUpperCase(Locale.ROOT))
                .filter(COMPATIBILITY_BASE_ROLES::contains)
                .collect(Collectors.toCollection(LinkedHashSet::new)));

        User saved = userRepository.save(user);

        for (Role role : roles) {
            UserRole userRole = new UserRole();
            userRole.setId(new UserRoleId(saved.getId(), role.getId()));
            userRole.setUser(saved);
            userRole.setRole(role);
            userRoleRepository.save(userRole);
        }

        User hydrated = userRepository.findDetailedById(saved.getId())
                .orElseThrow(() -> new PlatformException(HttpStatus.INTERNAL_SERVER_ERROR, "USER_CREATE_FAILED", "Created user could not be reloaded."));

        return toUserResponse(hydrated);
    }

    @Override
    @Transactional(readOnly = true)
    public List<UserResponse> listUsers() {
        return userRepository.findAllDetailed().stream()
                .map(this::toUserResponse)
                .toList();
    }

    private UserResponse toUserResponse(User user) {
        Set<String> roleCodes = new LinkedHashSet<>(user.getBaseRoles());
        user.getUserRoles().stream()
                .map(UserRole::getRole)
                .filter(role -> role != null && role.getCode() != null)
                .map(Role::getCode)
                .map(code -> code.toUpperCase(Locale.ROOT))
                .forEach(roleCodes::add);

        Set<String> permissionCodes = user.getUserRoles().stream()
                .map(UserRole::getRole)
                .filter(role -> role != null && role.isActive())
                .flatMap(role -> role.getRolePermissions().stream())
                .map(rolePermission -> rolePermission.getPermission())
                .filter(permission -> permission != null)
                .map(Permission::getCode)
                .map(code -> "PERM_" + code)
                .sorted(Comparator.naturalOrder())
                .collect(Collectors.toCollection(LinkedHashSet::new));

        return new UserResponse(
                user.getId(),
                user.getUsername(),
                user.getEmail(),
                user.getStatus(),
                roleCodes,
                permissionCodes
        );
    }

    private Set<String> normalizeRoleCodes(Collection<String> roleCodes) {
        if (roleCodes == null) {
            return new LinkedHashSet<>();
        }
        return roleCodes.stream()
                .filter(code -> code != null && !code.isBlank())
                .map(code -> code.trim().toUpperCase(Locale.ROOT))
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    private String normalize(String value) {
        return value == null ? null : value.trim().toLowerCase(Locale.ROOT);
    }
}
