package com.company.platform.auth.service;

import com.company.platform.auth.dto.CreateUserRequest;
import com.company.platform.auth.dto.UserResponse;
import org.springframework.security.access.prepost.PreAuthorize;

import java.util.List;

public interface UserService {

    @PreAuthorize("hasRole('ADMIN') or hasRole('SUPER_ADMIN') or hasAuthority('PERM_USERS_CREATE')")
    UserResponse createUser(CreateUserRequest request);

    @PreAuthorize("hasRole('ADMIN') or hasRole('SUPER_ADMIN') or hasAuthority('PERM_USERS_READ')")
    List<UserResponse> listUsers();
}
