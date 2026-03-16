package com.noura.platform.service;

import com.noura.platform.config.AppProperties;
import com.noura.platform.domain.entity.PasswordResetToken;
import com.noura.platform.domain.entity.UserAccount;
import com.noura.platform.dto.auth.PasswordResetConfirmRequest;
import com.noura.platform.dto.auth.PasswordResetRequest;
import com.noura.platform.repository.PasswordResetTokenRepository;
import com.noura.platform.repository.RefreshTokenRepository;
import com.noura.platform.repository.UserAccountRepository;
import com.noura.platform.security.JwtTokenProvider;
import com.noura.platform.service.impl.AuthServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.HexFormat;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceImplPasswordResetTest {

    @Mock
    private UserAccountRepository userAccountRepository;

    @Mock
    private RefreshTokenRepository refreshTokenRepository;

    @Mock
    private PasswordResetTokenRepository passwordResetTokenRepository;

    @Mock
    private AuthenticationManager authenticationManager;

    @Mock
    private PasswordEncoder passwordEncoder;

    private AuthServiceImpl authService;

    @BeforeEach
    void setUp() {
        AppProperties appProperties = new AppProperties();
        appProperties.getAuth().setB2bEmailPattern(".*");
        JwtTokenProvider jwtTokenProvider = new JwtTokenProvider(appProperties);
        authService = new AuthServiceImpl(
                userAccountRepository,
                refreshTokenRepository,
                passwordResetTokenRepository,
                authenticationManager,
                passwordEncoder,
                jwtTokenProvider,
                appProperties
        );
    }

    @Test
    void requestPasswordReset_shouldStoreHashedToken() {
        UserAccount user = new UserAccount();
        user.setEmail("customer@noura.test");
        when(userAccountRepository.findByEmailIgnoreCase("customer@noura.test")).thenReturn(Optional.of(user));
        when(passwordResetTokenRepository.save(any(PasswordResetToken.class))).thenAnswer(invocation -> invocation.getArgument(0));

        authService.requestPasswordReset(new PasswordResetRequest("customer@noura.test"));

        ArgumentCaptor<PasswordResetToken> captor = ArgumentCaptor.forClass(PasswordResetToken.class);
        verify(passwordResetTokenRepository).save(captor.capture());
        String tokenHash = captor.getValue().getTokenHash();

        assertNotNull(tokenHash);
        assertEquals(64, tokenHash.length());
        assertTrue(tokenHash.matches("^[0-9a-f]{64}$"));
        assertFalse(tokenHash.contains("-"));
    }

    @Test
    void resetPassword_shouldValidateUsingTokenHash() {
        String rawToken = "reset-token-value";
        String expectedHash = sha256(rawToken);
        PasswordResetToken token = new PasswordResetToken();
        UserAccount user = new UserAccount();
        token.setUser(user);
        token.setUsed(false);
        token.setExpiresAt(Instant.now().plusSeconds(600));

        when(passwordResetTokenRepository.findByTokenHashAndUsedFalse(expectedHash)).thenReturn(Optional.of(token));
        when(passwordEncoder.encode("new-password-123")).thenReturn("encoded-password");

        authService.resetPassword(new PasswordResetConfirmRequest(rawToken, "new-password-123"));

        verify(passwordResetTokenRepository).findByTokenHashAndUsedFalse(expectedHash);
        verify(userAccountRepository).save(user);
        verify(passwordResetTokenRepository).save(token);
        assertEquals("encoded-password", user.getPasswordHash());
        assertTrue(token.isUsed());
    }

    private String sha256(String value) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException(ex);
        }
    }
}
