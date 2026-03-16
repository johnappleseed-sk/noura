package com.company.platform.security;

import com.company.platform.config.PlatformProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

@Configuration
@EnableMethodSecurity
@RequiredArgsConstructor
@ConditionalOnMissingBean(SecurityFilterChain.class)
public class PlatformSecurityConfiguration {

    private final PlatformProperties properties;

    @Bean
    public SecurityFilterChain platformSecurityFilterChain(HttpSecurity http) throws Exception {
        String healthEndpoint = properties.getApi().getVersionPrefix() + "/system/health";

        http.csrf(csrf -> csrf.disable())
                .cors(Customizer.withDefaults())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .headers(headers -> headers
                        .contentTypeOptions(Customizer.withDefaults())
                        .frameOptions(frame -> frame.deny())
                );

        if (properties.getSecurity().isEnabled()) {
            http.oauth2ResourceServer(oauth2 -> oauth2.jwt(Customizer.withDefaults()));
        }

        http.authorizeHttpRequests(auth -> {
            auth.requestMatchers("/actuator/health/**", "/actuator/info", "/swagger-ui/**", "/v3/api-docs/**").permitAll();
            auth.requestMatchers(HttpMethod.GET, healthEndpoint).permitAll();
            auth.requestMatchers(HttpMethod.OPTIONS, "/**").permitAll();

            if (properties.getSecurity().isPermitAll()) {
                auth.anyRequest().permitAll();
            } else {
                auth.anyRequest().authenticated();
            }
        });

        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(properties.getSecurity().getAllowedOrigins());
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("*"));
        configuration.setExposedHeaders(List.of("Authorization", "X-Correlation-ID"));
        configuration.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}
