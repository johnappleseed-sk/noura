package com.noura.platform.inventory.security;

import com.noura.platform.common.exception.ApiException;
import com.noura.platform.common.exception.NotFoundException;
import com.noura.platform.inventory.domain.IamRolePermission;
import com.noura.platform.inventory.domain.IamUser;
import com.noura.platform.inventory.domain.IamUserRole;
import com.noura.platform.inventory.repository.IamRolePermissionRepository;
import com.noura.platform.inventory.repository.IamUserRepository;
import com.noura.platform.inventory.repository.IamUserRoleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class InventoryIdentityService {

    private final IamUserRepository iamUserRepository;
    private final IamUserRoleRepository iamUserRoleRepository;
    private final IamRolePermissionRepository iamRolePermissionRepository;

    public InventoryUserPrincipal loadPrincipalByUserId(String userId) {
        IamUser user = iamUserRepository.findByIdAndDeletedAtIsNull(userId)
                .orElseThrow(() -> new NotFoundException("USER_NOT_FOUND", "Inventory user not found"));
        validateUserStatus(user);
        return buildPrincipal(user);
    }

    public InventoryUserPrincipal loadPrincipalByLogin(String login) {
        IamUser user = iamUserRepository.findByUsernameIgnoreCaseAndDeletedAtIsNull(login)
                .or(() -> iamUserRepository.findByEmailIgnoreCaseAndDeletedAtIsNull(login))
                .orElseThrow(() -> new NotFoundException("USER_NOT_FOUND", "Inventory user not found"));
        validateUserStatus(user);
        return buildPrincipal(user);
    }

    public InventoryCurrentUserSnapshot getCurrentUserSnapshot() {
        return InventorySecurityContext.currentPrincipal()
                .map(principal -> new InventoryCurrentUserSnapshot(principal.userId(), principal.username(), principal.email()))
                .orElse(null);
    }

    public InventoryUserPrincipal buildPrincipal(IamUser user) {
        List<IamUserRole> userRoles = iamUserRoleRepository.findDetailedByUserId(user.getId());
        List<String> roleIds = userRoles.stream().map(userRole -> userRole.getRole().getId()).toList();
        List<String> roles = userRoles.stream().map(userRole -> userRole.getRole().getCode()).distinct().sorted().toList();
        List<String> permissions = roleIds.isEmpty()
                ? List.of()
                : iamRolePermissionRepository.findDetailedByRoleIds(roleIds).stream()
                .map(IamRolePermission::getPermission)
                .map(permission -> permission.getCode())
                .distinct()
                .sorted()
                .toList();
        return new InventoryUserPrincipal(
                user.getId(),
                user.getUsername(),
                user.getEmail(),
                user.getFullName(),
                roles,
                permissions
        );
    }

    private void validateUserStatus(IamUser user) {
        if (!"ACTIVE".equalsIgnoreCase(user.getStatus())) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "USER_INACTIVE", "Inventory user is not active");
        }
    }

    public record InventoryCurrentUserSnapshot(String userId, String username, String email) {
    }
}
