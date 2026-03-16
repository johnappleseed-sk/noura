package com.company.platform.auth.service;

import com.company.platform.auth.dto.CurrentUserResponse;
import com.company.platform.auth.dto.LoginRequest;
import com.company.platform.auth.dto.LoginResponse;

public interface AuthService {

    LoginResponse login(LoginRequest request);

    CurrentUserResponse currentUser();
}
