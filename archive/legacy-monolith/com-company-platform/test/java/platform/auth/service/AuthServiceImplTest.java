package com.company.platform.auth.service;

import com.company.platform.auth.dto.CurrentUserResponse;
import com.company.platform.auth.dto.LoginRequest;
import com.company.platform.auth.dto.LoginResponse;
import com.company.platform.auth.entity.Permission;
import com.company.platform.auth.entity.Role;
import com.company.platform.auth.entity.RolePermission;
import com.company.platform.auth.entity.User;
import com.company.platform.auth.entity.UserRole;
import com.company.platform.auth.entity.id.RolePermissionId;
import com.company.platform.auth.entity.id.UserRoleId;
import com.company.platform.auth.enums.UserStatus;
import com.company.platform.auth.repository.UserRepository;
import com.company.platform.auth.service.impl.AuthServiceImpl;
import com.company.platform.exception.PlatformException;
import com.noura.platform.config.AppProperties;
import com.noura.platform.domain.enums.RoleType;
import com.noura.platform.security.JwtTokenProvider;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AuthServiceImplTest {

    @Test
    void login_shouldReturnJwtAndCurrentUser() {
        AuthenticationManager authenticationManager = mock(AuthenticationManager.class);
        UserRepository userRepository = mock(UserRepository.class);
        AppProperties properties = new AppProperties();
        properties.getJwt().setSecret("0123456789abcdef0123456789abcdef");
        properties.getJwt().setIssuer("noura-test");
        properties.getJwt().setAccessTokenValidityMinutes(30);
        JwtTokenProvider jwtTokenProvider = new JwtTokenProvider(properties);
        AuthServiceImpl authService = new AuthServiceImpl(authenticationManager, userRepository, jwtTokenProvider, properties);

        User user = userWithAdminAuthority();
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenReturn(new UsernamePasswordAuthenticationToken(user.getEmail(), null, Set.of()));
        when(userRepository.findDetailedByEmail("admin@noura.local")).thenReturn(Optional.of(user));

        LoginResponse response = authService.login(new LoginRequest("admin@noura.local", "Admin123!"));

        assertEquals("Bearer", response.tokenType());
        assertEquals("admin@noura.local", response.user().email());
        assertEquals(Set.of("ADMIN"), response.user().roles());
    }

    @Test
    void currentUser_shouldRejectAnonymousAccess() {
        AuthenticationManager authenticationManager = mock(AuthenticationManager.class);
        UserRepository userRepository = mock(UserRepository.class);
        AppProperties properties = new AppProperties();
        properties.getJwt().setSecret("0123456789abcdef0123456789abcdef");
        properties.getJwt().setIssuer("noura-test");
        JwtTokenProvider jwtTokenProvider = new JwtTokenProvider(properties);
        AuthServiceImpl authService = new AuthServiceImpl(authenticationManager, userRepository, jwtTokenProvider, properties);

        SecurityContextHolder.clearContext();

        assertThrows(PlatformException.class, authService::currentUser);
    }

    private User userWithAdminAuthority() {
        User user = new User();
        user.setId(UUID.randomUUID());
        user.setUsername("admin");
        user.setEmail("admin@noura.local");
        user.setStatus(UserStatus.ACTIVE);
        user.setEnabled(true);
        user.setBaseRoles(Set.of(RoleType.ADMIN.name()));

        Role role = new Role();
        role.setId(UUID.randomUUID());
        role.setCode("ADMIN");
        role.setName("Admin");
        role.setActive(true);

        Permission permission = new Permission();
        permission.setId(UUID.randomUUID());
        permission.setScope("users");
        permission.setAction("read");
        permission.setName("Users Read");

        RolePermission rolePermission = new RolePermission();
        rolePermission.setId(new RolePermissionId(role.getId(), permission.getId()));
        rolePermission.setRole(role);
        rolePermission.setPermission(permission);
        role.setRolePermissions(Set.of(rolePermission));

        UserRole userRole = new UserRole();
        userRole.setId(new UserRoleId(user.getId(), role.getId()));
        userRole.setUser(user);
        userRole.setRole(role);
        user.setUserRoles(Set.of(userRole));
        return user;
    }
}
