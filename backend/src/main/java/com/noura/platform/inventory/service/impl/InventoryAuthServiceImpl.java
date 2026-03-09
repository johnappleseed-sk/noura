package com.noura.platform.inventory.service.impl;

import com.noura.platform.common.exception.ApiException;
import com.noura.platform.inventory.domain.IamRole;
import com.noura.platform.inventory.domain.IamUser;
import com.noura.platform.inventory.domain.IamUserRole;
import com.noura.platform.inventory.domain.id.IamUserRoleId;
import com.noura.platform.inventory.dto.auth.InventoryAuthResponse;
import com.noura.platform.inventory.dto.auth.InventoryCurrentUserResponse;
import com.noura.platform.inventory.dto.auth.InventoryLoginRequest;
import com.noura.platform.inventory.dto.auth.InventoryRegisterRequest;
import com.noura.platform.inventory.repository.IamRoleRepository;
import com.noura.platform.inventory.repository.IamUserRepository;
import com.noura.platform.inventory.repository.IamUserRoleRepository;
import com.noura.platform.inventory.security.InventoryIdentityService;
import com.noura.platform.inventory.security.InventorySecurityContext;
import com.noura.platform.inventory.security.InventoryTokenService;
import com.noura.platform.inventory.security.InventoryUserPrincipal;
import com.noura.platform.inventory.service.InventoryAuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.Instant;

@Service
@RequiredArgsConstructor
public class InventoryAuthServiceImpl implements InventoryAuthService {

    private static final String VIEWER_ROLE = "VIEWER";

    private final IamUserRepository iamUserRepository;
    private final IamRoleRepository iamRoleRepository;
    private final IamUserRoleRepository iamUserRoleRepository;
    private final PasswordEncoder passwordEncoder;
    private final InventoryIdentityService inventoryIdentityService;
    private final InventoryTokenService inventoryTokenService;

    @Override
    @Transactional(transactionManager = "inventoryTransactionManager")
    public InventoryAuthResponse register(InventoryRegisterRequest request) {
        validateUniqueUser(request.username(), request.email());
        IamUser user = new IamUser();
        user.setUsername(request.username().trim());
        user.setEmail(request.email().trim().toLowerCase());
        user.setFullName(request.fullName().trim());
        user.setPasswordHash(passwordEncoder.encode(request.password()));
        user.setStatus("ACTIVE");
        user = iamUserRepository.save(user);

        IamRole viewerRole = iamRoleRepository.findByCodeIgnoreCase(VIEWER_ROLE)
                .orElseThrow(() -> new IllegalStateException("VIEWER role is not seeded"));
        IamUserRole link = new IamUserRole();
        link.setId(new IamUserRoleId(user.getId(), viewerRole.getId()));
        link.setUser(user);
        link.setRole(viewerRole);
        iamUserRoleRepository.save(link);

        return issueAuthResponse(user);
    }

    @Override
    @Transactional(transactionManager = "inventoryTransactionManager")
    public InventoryAuthResponse login(InventoryLoginRequest request) {
        String login = request.login().trim();
        IamUser user = iamUserRepository.findByUsernameIgnoreCaseAndDeletedAtIsNull(login)
                .or(() -> iamUserRepository.findByEmailIgnoreCaseAndDeletedAtIsNull(login))
                .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "AUTH_INVALID", "Invalid credentials"));
        if (!"ACTIVE".equalsIgnoreCase(user.getStatus())) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "AUTH_INVALID", "Invalid credentials");
        }
        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "AUTH_INVALID", "Invalid credentials");
        }
        user.setLastLoginAt(Instant.now());
        iamUserRepository.save(user);
        return issueAuthResponse(user);
    }

    @Override
    @Transactional(transactionManager = "inventoryTransactionManager", readOnly = true)
    public InventoryCurrentUserResponse currentUser() {
        InventoryUserPrincipal principal = InventorySecurityContext.currentPrincipal()
                .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "AUTH_REQUIRED", "Authentication required"));
        return toCurrentUserResponse(principal);
    }

    private InventoryAuthResponse issueAuthResponse(IamUser user) {
        InventoryUserPrincipal principal = inventoryIdentityService.buildPrincipal(user);
        InventoryTokenService.InventoryTokenPayload tokenPayload = inventoryTokenService.issueAccessToken(principal);
        return new InventoryAuthResponse(
                tokenPayload.token(),
                "Bearer",
                tokenPayload.expiresAt(),
                toCurrentUserResponse(principal)
        );
    }

    private InventoryCurrentUserResponse toCurrentUserResponse(InventoryUserPrincipal principal) {
        return new InventoryCurrentUserResponse(
                principal.userId(),
                principal.username(),
                principal.email(),
                principal.fullName(),
                "ACTIVE",
                principal.roles(),
                principal.permissions()
        );
    }

    private void validateUniqueUser(String username, String email) {
        if (!StringUtils.hasText(username) || !StringUtils.hasText(email)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "USER_INVALID", "Username and email are required");
        }
        if (iamUserRepository.existsByUsernameIgnoreCaseAndDeletedAtIsNull(username.trim())) {
            throw new ApiException(HttpStatus.CONFLICT, "USERNAME_EXISTS", "Username already exists");
        }
        if (iamUserRepository.existsByEmailIgnoreCaseAndDeletedAtIsNull(email.trim())) {
            throw new ApiException(HttpStatus.CONFLICT, "EMAIL_EXISTS", "Email already exists");
        }
    }
}
