package com.company.platform.auth.config;

import com.company.platform.auth.entity.Permission;
import com.company.platform.auth.entity.Role;
import com.company.platform.auth.entity.RolePermission;
import com.company.platform.auth.entity.User;
import com.company.platform.auth.entity.UserRole;
import com.company.platform.auth.entity.id.RolePermissionId;
import com.company.platform.auth.entity.id.UserRoleId;
import com.company.platform.auth.enums.UserStatus;
import com.company.platform.auth.repository.PermissionRepository;
import com.company.platform.auth.repository.RolePermissionRepository;
import com.company.platform.auth.repository.RoleRepository;
import com.company.platform.auth.repository.UserRepository;
import com.company.platform.auth.repository.UserRoleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Set;

@Slf4j
@Component
@Profile({"dev", "local-postgres", "local-mysql", "test"})
@RequiredArgsConstructor
public class IdentityRbacSeeder implements ApplicationRunner {

    public static final String DEFAULT_ADMIN_USERNAME = "admin";
    public static final String DEFAULT_ADMIN_EMAIL = "admin@noura.local";
    public static final String DEFAULT_ADMIN_PASSWORD = "Admin123!";

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PermissionRepository permissionRepository;
    private final UserRoleRepository userRoleRepository;
    private final RolePermissionRepository rolePermissionRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        Permission usersRead = ensurePermission("users", "read", "Users Read", "Read platform users.");
        Permission usersCreate = ensurePermission("users", "create", "Users Create", "Create platform users.");
        Role adminRole = ensureRole("ADMIN", "Admin", "Default bootstrap admin role.");

        ensureRolePermission(adminRole, usersRead);
        ensureRolePermission(adminRole, usersCreate);
        ensureAdminUser(adminRole);
    }

    private Permission ensurePermission(String scope, String action, String name, String description) {
        return permissionRepository.findByScopeIgnoreCaseAndActionIgnoreCase(scope, action)
                .map(existing -> {
                    existing.setName(name);
                    existing.setDescription(description);
                    return permissionRepository.save(existing);
                })
                .orElseGet(() -> {
                    Permission permission = new Permission();
                    permission.setScope(scope.toLowerCase(Locale.ROOT));
                    permission.setAction(action.toLowerCase(Locale.ROOT));
                    permission.setName(name);
                    permission.setDescription(description);
                    return permissionRepository.save(permission);
                });
    }

    private Role ensureRole(String code, String name, String description) {
        return roleRepository.findByCodeIgnoreCase(code)
                .map(existing -> {
                    existing.setName(name);
                    existing.setDescription(description);
                    existing.setActive(true);
                    return roleRepository.save(existing);
                })
                .orElseGet(() -> {
                    Role role = new Role();
                    role.setCode(code.toUpperCase(Locale.ROOT));
                    role.setName(name);
                    role.setDescription(description);
                    role.setActive(true);
                    return roleRepository.save(role);
                });
    }

    private void ensureRolePermission(Role role, Permission permission) {
        if (rolePermissionRepository.existsByIdRoleIdAndIdPermissionId(role.getId(), permission.getId())) {
            return;
        }
        RolePermission rolePermission = new RolePermission();
        rolePermission.setId(new RolePermissionId(role.getId(), permission.getId()));
        rolePermission.setRole(role);
        rolePermission.setPermission(permission);
        rolePermissionRepository.save(rolePermission);
    }

    private void ensureAdminUser(Role adminRole) {
        User admin = userRepository.findByEmailIgnoreCase(DEFAULT_ADMIN_EMAIL)
                .orElseGet(User::new);

        boolean isNew = admin.getId() == null;
        admin.setUsername(DEFAULT_ADMIN_USERNAME);
        admin.setEmail(DEFAULT_ADMIN_EMAIL);
        admin.setPasswordHash(isNew || admin.getPasswordHash() == null || admin.getPasswordHash().isBlank()
                ? passwordEncoder.encode(DEFAULT_ADMIN_PASSWORD)
                : admin.getPasswordHash());
        admin.setStatus(UserStatus.ACTIVE);
        admin.setEnabled(true);
        admin.setFullName("Platform Administrator");
        Set<String> baseRoles = new LinkedHashSet<>(admin.getBaseRoles());
        baseRoles.add("ADMIN");
        admin.setBaseRoles(baseRoles);

        User saved = userRepository.save(admin);

        if (!userRoleRepository.existsByIdUserIdAndIdRoleId(saved.getId(), adminRole.getId())) {
            UserRole userRole = new UserRole();
            userRole.setId(new UserRoleId(saved.getId(), adminRole.getId()));
            userRole.setUser(saved);
            userRole.setRole(adminRole);
            userRoleRepository.save(userRole);
        }

        log.info("Identity RBAC bootstrap admin ready: {} / {}", DEFAULT_ADMIN_EMAIL, DEFAULT_ADMIN_PASSWORD);
    }
}
