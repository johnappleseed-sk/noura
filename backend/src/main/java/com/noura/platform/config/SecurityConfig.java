package com.noura.platform.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.noura.platform.common.api.ApiResponse;
import com.noura.platform.security.CustomUserDetailsService;
import com.noura.platform.security.JwtAuthenticationFilter;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

@Configuration
@RequiredArgsConstructor
@EnableMethodSecurity
@EnableConfigurationProperties(AppProperties.class)
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final CustomUserDetailsService userDetailsService;
    private final RequestCorrelationFilter requestCorrelationFilter;
    private final RateLimitFilter rateLimitFilter;
    private final AppProperties appProperties;
    private final ObjectMapper objectMapper;

    /**
     * Executes security filter chain.
     *
     * @param http The current HTTP request used to populate response metadata.
     * @return The result of security filter chain.
     * @throws Exception If the operation cannot be completed.
     */
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http.csrf(csrf -> csrf.disable())
                .cors(Customizer.withDefaults())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .exceptionHandling(exceptions -> exceptions
                        .authenticationEntryPoint((request, response, authException) -> writeAuthError(
                                response,
                                HttpStatus.UNAUTHORIZED,
                                ApiResponse.fail(
                                        "Authentication required",
                                        "AUTH_REQUIRED",
                                        "Authentication is required to access this resource",
                                        request.getRequestURI()
                                )
                        ))
                        .accessDeniedHandler((request, response, accessDeniedException) -> writeAuthError(
                                response,
                                HttpStatus.FORBIDDEN,
                                ApiResponse.fail(
                                        "Access denied",
                                        "ACCESS_DENIED",
                                        "You do not have permission to access this resource",
                                        request.getRequestURI()
                                )
                        ))
                )
                .headers(headers -> headers
                        .httpStrictTransportSecurity(hsts -> hsts
                                .includeSubDomains(true)
                                .maxAgeInSeconds(31_536_000)
                                .requestMatcher(request -> true)
                        )
                        .frameOptions(frame -> frame.deny())
                        .contentTypeOptions(Customizer.withDefaults())
                        .contentSecurityPolicy(csp -> csp
                                .policyDirectives("default-src 'self'; frame-ancestors 'none'; object-src 'none'; base-uri 'self'")
                        )
                        .referrerPolicy(referrer -> referrer
                                .policy(ReferrerPolicyHeaderWriter.ReferrerPolicy.NO_REFERRER)
                        )
                )
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/swagger-ui/**", "/v3/api-docs/**", "/actuator/health/**", "/actuator/info").permitAll()
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        .requestMatchers(HttpMethod.POST, appProperties.getApi().getVersionPrefix() + "/auth/**").permitAll()
                        .requestMatchers(HttpMethod.GET, appProperties.getApi().getVersionPrefix() + "/products/**").permitAll()
                        .requestMatchers(HttpMethod.GET, appProperties.getApi().getVersionPrefix() + "/categories/**").permitAll()
                        .requestMatchers(HttpMethod.GET, appProperties.getApi().getVersionPrefix() + "/stores/**").permitAll()
                        .requestMatchers(HttpMethod.GET, appProperties.getApi().getVersionPrefix() + "/search/**").permitAll()
                        .requestMatchers(HttpMethod.GET, appProperties.getApi().getVersionPrefix() + "/merchandising/products").permitAll()
                        .requestMatchers(HttpMethod.GET, appProperties.getApi().getVersionPrefix() + "/recommendations/product/**").permitAll()
                        .requestMatchers(HttpMethod.GET, appProperties.getApi().getVersionPrefix() + "/recommendations/trending").permitAll()
                        .requestMatchers(HttpMethod.GET, appProperties.getApi().getVersionPrefix() + "/recommendations/best-sellers").permitAll()
                        .requestMatchers(HttpMethod.GET, appProperties.getApi().getVersionPrefix() + "/recommendations/deals").permitAll()
                        .requestMatchers(HttpMethod.GET, appProperties.getApi().getVersionPrefix() + "/price-lists").permitAll()
                        .requestMatchers(HttpMethod.GET, appProperties.getApi().getVersionPrefix() + "/prices/**").permitAll()
                        .requestMatchers(HttpMethod.GET, appProperties.getApi().getVersionPrefix() + "/promotions/active").permitAll()
                        .requestMatchers(HttpMethod.GET, appProperties.getApi().getVersionPrefix() + "/runtime/features").permitAll()
                        .requestMatchers(HttpMethod.POST, appProperties.getApi().getVersionPrefix() + "/analytics/events").permitAll()
                        .requestMatchers(HttpMethod.GET, appProperties.getApi().getVersionPrefix() + "/carousels/hero").permitAll()
                        .requestMatchers(HttpMethod.POST, appProperties.getApi().getVersionPrefix() + "/location/reverse-geocode").permitAll()
                        .requestMatchers(HttpMethod.POST, appProperties.getApi().getVersionPrefix() + "/location/forward-geocode").permitAll()
                        .requestMatchers(HttpMethod.POST, appProperties.getApi().getVersionPrefix() + "/location/validate-service-area").permitAll()
                        .requestMatchers(HttpMethod.GET, appProperties.getApi().getVersionPrefix() + "/location/nearby-stores").permitAll()
                        .requestMatchers("/ws/**").permitAll()
                        .anyRequest().authenticated()
                )
                .authenticationProvider(authenticationProvider())
                .addFilterBefore(rateLimitFilter, UsernamePasswordAuthenticationFilter.class)
                .addFilterBefore(requestCorrelationFilter, RateLimitFilter.class)
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }

    /**
     * Executes authentication provider.
     *
     * @return The result of authentication provider.
     */
    @Bean
    public DaoAuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider();
        provider.setUserDetailsService(userDetailsService);
        provider.setPasswordEncoder(passwordEncoder());
        return provider;
    }

    /**
     * Executes authentication manager.
     *
     * @param configuration The configuration value.
     * @return The result of authentication manager.
     * @throws Exception If the operation cannot be completed.
     */
    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration configuration) throws Exception {
        return configuration.getAuthenticationManager();
    }

    /**
     * Executes password encoder.
     *
     * @return The result of password encoder.
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /**
     * Executes cors configuration source.
     *
     * @return The result of cors configuration source.
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration cors = new CorsConfiguration();
        List<String> allowedOrigins = Arrays.stream(
                        Optional.ofNullable(appProperties.getCors().getAllowedOrigins()).orElse("").split(",")
                )
                .map(String::trim)
                .filter(origin -> !origin.isEmpty())
                .toList();
        cors.setAllowedOrigins(allowedOrigins);
        cors.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        cors.setAllowedHeaders(List.of("*"));
        cors.setExposedHeaders(List.of("Authorization"));
        cors.setAllowCredentials(true);
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", cors);
        return source;
    }

    private void writeAuthError(HttpServletResponse response, HttpStatus status, ApiResponse<Void> body) throws java.io.IOException {
        response.setStatus(status.value());
        response.setContentType("application/json");
        objectMapper.writeValue(response.getWriter(), body);
    }
}
