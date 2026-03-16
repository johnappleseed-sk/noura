package com.company.platform.auth.web;

import com.company.platform.auth.dto.CurrentUserResponse;
import com.company.platform.auth.dto.LoginRequest;
import com.company.platform.auth.dto.LoginResponse;
import com.company.platform.auth.service.AuthService;
import com.company.platform.common.api.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<LoginResponse>> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(ApiResponse.ok("Logged in", authService.login(request)));
    }

    @GetMapping("/me")
    public ResponseEntity<ApiResponse<CurrentUserResponse>> me() {
        return ResponseEntity.ok(ApiResponse.ok("Current user", authService.currentUser()));
    }
}
