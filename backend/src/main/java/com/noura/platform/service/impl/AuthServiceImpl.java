package com.noura.platform.service.impl;

import com.noura.platform.common.exception.UnauthorizedException;
import com.noura.platform.config.AppProperties;
import com.noura.platform.domain.entity.PasswordResetToken;
import com.noura.platform.domain.entity.RefreshToken;
import com.noura.platform.domain.entity.UserAccount;
import com.noura.platform.domain.enums.RoleType;
import com.noura.platform.dto.auth.*;
import com.noura.platform.repository.PasswordResetTokenRepository;
import com.noura.platform.repository.RefreshTokenRepository;
import com.noura.platform.repository.UserAccountRepository;
import com.noura.platform.security.JwtTokenProvider;
import com.noura.platform.service.AuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.HexFormat;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final UserAccountRepository userAccountRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordResetTokenRepository passwordResetTokenRepository;
    private final AuthenticationManager authenticationManager;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    private final AppProperties appProperties;

    /**
     * Executes register.
     *
     * @param request The request payload for this operation.
     * @return The result of register.
     */
    @Override
    @Transactional
    public AuthTokensResponse register(RegisterRequest request) {
        userAccountRepository.findByEmailIgnoreCase(request.email()).ifPresent(existing -> {
            throw new UnauthorizedException("EMAIL_EXISTS", "Email already registered");
        });
        UserAccount user = new UserAccount();
        user.setEmail(request.email().toLowerCase());
        user.setFullName(request.fullName());
        user.setPasswordHash(passwordEncoder.encode(request.password()));
        user.setRoles(resolveRoles(request.email()));
        user = userAccountRepository.save(user);
        return issueTokens(user);
    }

    /**
     * Executes login.
     *
     * @param request The request payload for this operation.
     * @return The result of login.
     */
    @Override
    public AuthTokensResponse login(LoginRequest request) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.email(), request.password())
        );
        UserAccount user = userAccountRepository.findByEmailIgnoreCase(request.email())
                .orElseThrow(() -> new UnauthorizedException("AUTH_INVALID", "Invalid credentials"));
        return issueTokens(user);
    }

    /**
     * Executes refresh.
     *
     * @param request The request payload for this operation.
     * @return The result of refresh.
     */
    @Override
    @Transactional
    public AuthTokensResponse refresh(RefreshTokenRequest request) {
        RefreshToken refreshToken = refreshTokenRepository.findByTokenAndRevokedFalse(request.refreshToken())
                .orElseThrow(() -> new UnauthorizedException("REFRESH_INVALID", "Invalid refresh token"));
        if (refreshToken.getExpiresAt().isBefore(Instant.now())) {
            throw new UnauthorizedException("REFRESH_EXPIRED", "Refresh token expired");
        }
        refreshToken.setRevoked(true);
        refreshTokenRepository.save(refreshToken);
        return issueTokens(refreshToken.getUser());
    }

    /**
     * Executes request password reset.
     *
     * @param request The request payload for this operation.
     */
    @Override
    @Transactional
    public void requestPasswordReset(PasswordResetRequest request) {
        userAccountRepository.findByEmailIgnoreCase(request.email()).ifPresent(user -> {
            String rawToken = UUID.randomUUID().toString();
            PasswordResetToken token = new PasswordResetToken();
            token.setUser(user);
            token.setTokenHash(hashResetToken(rawToken));
            token.setExpiresAt(Instant.now().plus(1, ChronoUnit.HOURS));
            token.setUsed(false);
            passwordResetTokenRepository.save(token);
        });
    }

    /**
     * Executes reset password.
     *
     * @param request The request payload for this operation.
     */
    @Override
    @Transactional
    public void resetPassword(PasswordResetConfirmRequest request) {
        PasswordResetToken token = passwordResetTokenRepository.findByTokenHashAndUsedFalse(hashResetToken(request.token()))
                .orElseThrow(() -> new UnauthorizedException("RESET_INVALID", "Invalid reset token"));
        if (token.getExpiresAt().isBefore(Instant.now())) {
            throw new UnauthorizedException("RESET_EXPIRED", "Password reset token expired");
        }
        UserAccount user = token.getUser();
        user.setPasswordHash(passwordEncoder.encode(request.newPassword()));
        userAccountRepository.save(user);
        token.setUsed(true);
        passwordResetTokenRepository.save(token);
    }

    /**
     * Executes hash reset token.
     *
     * @param token The token value.
     * @return The result of hash reset token.
     */
    private String hashResetToken(String token) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(token.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 algorithm unavailable", ex);
        }
    }

    /**
     * Determines whether issue tokens.
     *
     * @param user The user context for this operation.
     * @return True when the condition is satisfied; otherwise false.
     */
    private AuthTokensResponse issueTokens(UserAccount user) {
        String accessToken = jwtTokenProvider.generateAccessToken(user.getId(), user.getEmail(), user.getRoles());
        RefreshToken refresh = new RefreshToken();
        refresh.setUser(user);
        refresh.setToken(UUID.randomUUID().toString());
        refresh.setExpiresAt(Instant.now().plus(appProperties.getJwt().getRefreshTokenValidityDays(), ChronoUnit.DAYS));
        refresh.setRevoked(false);
        refresh = refreshTokenRepository.save(refresh);
        return new AuthTokensResponse(
                user.getId(),
                user.getEmail(),
                user.getFullName(),
                user.getRoles(),
                accessToken,
                refresh.getToken()
        );
    }

    /**
     * Executes resolve roles.
     *
     * @param email The email value.
     * @return The result of resolve roles.
     */
    private Set<RoleType> resolveRoles(String email) {
        Set<RoleType> roles = new HashSet<>();
        roles.add(RoleType.CUSTOMER);
        if (Pattern.compile(appProperties.getAuth().getB2bEmailPattern(), Pattern.CASE_INSENSITIVE).matcher(email).matches()) {
            roles.add(RoleType.B2B);
        }
        return roles;
    }
}
