package com.noura.platform.testsupport.inventory.api;

import com.noura.platform.config.AppProperties;
import com.noura.platform.inventory.config.InventoryHeaderAuthenticationFilter;
import com.noura.platform.inventory.security.InventoryIdentityService;
import com.noura.platform.inventory.security.InventoryJwtAuthenticationFilter;
import com.noura.platform.inventory.security.InventorySecurityProperties;
import com.noura.platform.inventory.security.InventoryTokenService;
import com.noura.platform.inventory.security.InventoryUserPrincipal;
import com.noura.platform.security.CustomUserDetailsService;
import com.noura.platform.security.JwtAuthenticationFilter;
import com.noura.platform.security.JwtTokenProvider;
import com.noura.platform.repository.UserAccountRepository;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;

import static org.mockito.Mockito.mock;

@TestConfiguration
@EnableMethodSecurity
public class InventoryWebMvcSecurityTestConfig {

    @Bean
    InventoryJwtAuthenticationFilter inventoryJwtAuthenticationFilter(InventorySecurityProperties securityProperties) {
        return new InventoryJwtAuthenticationFilter(
                new InventoryTokenService(securityProperties),
                new InventoryIdentityService(null, null, null) {
                    @Override
                    public InventoryUserPrincipal loadPrincipalByUserId(String userId) {
                        throw new UnsupportedOperationException("JWT authentication is not exercised in WebMvc controller tests");
                    }
                }
        );
    }

    @Bean
    JwtAuthenticationFilter platformJwtAuthenticationFilter() {
        AppProperties appProperties = new AppProperties();
        appProperties.getJwt().setSecret("12345678901234567890123456789012");
        appProperties.getJwt().setIssuer("inventory-webmvc-test");
        JwtTokenProvider jwtTokenProvider = new JwtTokenProvider(appProperties);
        CustomUserDetailsService userDetailsService = new CustomUserDetailsService(null) {
            @Override
            public UserDetails loadUserByUsername(String email) {
                throw new UnsupportedOperationException("Platform JWT authentication is not exercised in inventory WebMvc controller tests");
            }
        };
        return new JwtAuthenticationFilter(jwtTokenProvider, userDetailsService) {
            @Override
            protected void doFilterInternal(
                    HttpServletRequest request,
                    HttpServletResponse response,
                    FilterChain filterChain
            ) throws ServletException, IOException {
                filterChain.doFilter(request, response);
            }
        };
    }

    @Bean
    InventoryHeaderAuthenticationFilter inventoryHeaderAuthenticationFilter(InventorySecurityProperties securityProperties) {
        return new InventoryHeaderAuthenticationFilter(securityProperties);
    }

    @Bean
    JwtAuthenticationFilter jwtAuthenticationFilter() {
        AppProperties appProperties = new AppProperties();
        appProperties.getJwt().setSecret("12345678901234567890123456789012");
        appProperties.getJwt().setIssuer("inventory-webmvc-test");
        return new JwtAuthenticationFilter(
                new JwtTokenProvider(appProperties),
                new CustomUserDetailsService(mock(UserAccountRepository.class))
        );
    }
}
