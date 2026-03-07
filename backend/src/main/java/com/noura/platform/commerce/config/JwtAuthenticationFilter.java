package com.noura.platform.commerce.config;

import com.noura.platform.commerce.service.AppUserDetailsService;
import com.noura.platform.commerce.service.JwtTokenService;
import com.noura.platform.commerce.customers.domain.CustomerAccountStatus;
import com.noura.platform.commerce.customers.domain.StorefrontCustomerPrincipal;
import com.noura.platform.commerce.customers.infrastructure.CustomerAccountRepo;
import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {
    private final JwtTokenService jwtTokenService;
    private final AppUserDetailsService appUserDetailsService;
    private final CustomerAccountRepo customerAccountRepo;

    /**
     * Executes the JwtAuthenticationFilter operation.
     * <p>Return value: A fully initialized JwtAuthenticationFilter instance.</p>
     *
     * @param jwtTokenService Parameter of type {@code JwtTokenService} used by this operation.
     * @param appUserDetailsService Parameter of type {@code AppUserDetailsService} used by this operation.
     * <p>Possible exceptions: Runtime exceptions from downstream dependencies may propagate unchanged.</p>
     * <p>Edge cases: Null, empty, and boundary inputs are handled by the existing control flow and validations.</p>
     */
    public JwtAuthenticationFilter(JwtTokenService jwtTokenService,
                                  AppUserDetailsService appUserDetailsService,
                                  CustomerAccountRepo customerAccountRepo) {
        this.jwtTokenService = jwtTokenService;
        this.appUserDetailsService = appUserDetailsService;
        this.customerAccountRepo = customerAccountRepo;
    }

    /**
     * Executes the doFilterInternal operation.
     *
     * @param request Parameter of type {@code HttpServletRequest} used by this operation.
     * @param response Parameter of type {@code HttpServletResponse} used by this operation.
     * @param filterChain Parameter of type {@code FilterChain} used by this operation.
     * @return void No value is returned; the method applies side effects to existing state.
     * @throws ServletException If the operation cannot complete successfully.
     * @throws IOException If the operation cannot complete successfully.
     * <p>Edge cases: Null, empty, and boundary inputs are handled by the existing control flow and validations.</p>
     */
    /**
     * Executes the doFilterInternal operation.
     *
     * @param request Parameter of type {@code HttpServletRequest} used by this operation.
     * @param response Parameter of type {@code HttpServletResponse} used by this operation.
     * @param filterChain Parameter of type {@code FilterChain} used by this operation.
     * @return void No value is returned; the method applies side effects to existing state.
     * @throws ServletException If the operation cannot complete successfully.
     * @throws IOException If the operation cannot complete successfully.
     * <p>Edge cases: Null, empty, and boundary inputs are handled by the existing control flow and validations.</p>
     */
    /**
     * Executes the doFilterInternal operation.
     *
     * @param request Parameter of type {@code HttpServletRequest} used by this operation.
     * @param response Parameter of type {@code HttpServletResponse} used by this operation.
     * @param filterChain Parameter of type {@code FilterChain} used by this operation.
     * @return void No value is returned; the method applies side effects to existing state.
     * @throws ServletException If the operation cannot complete successfully.
     * @throws IOException If the operation cannot complete successfully.
     * <p>Edge cases: Null, empty, and boundary inputs are handled by the existing control flow and validations.</p>
     */
    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String authorization = request.getHeader("Authorization");
        if (authorization == null || !authorization.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        if (SecurityContextHolder.getContext().getAuthentication() != null) {
            filterChain.doFilter(request, response);
            return;
        }

        String token = authorization.substring("Bearer ".length()).trim();
        try {
            Claims claims = jwtTokenService.parseAccessToken(token);
            String principal = claims.get("email", String.class);
            if (principal == null || principal.isBlank()) {
                principal = claims.get("username", String.class);
            }
            if (principal != null && !principal.isBlank()) {
                UserDetails userDetails = appUserDetailsService.loadUserByUsername(principal);
                UsernamePasswordAuthenticationToken authenticationToken =
                        new UsernamePasswordAuthenticationToken(
                                userDetails,
                                null,
                                userDetails.getAuthorities()
                        );
                authenticationToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                SecurityContextHolder.getContext().setAuthentication(authenticationToken);
                filterChain.doFilter(request, response);
                return;
            }
        } catch (Exception ignored) {
            // Ignore staff-token parse failures and try storefront customer token.
        }

        try {
            Claims customerClaims = jwtTokenService.parseCustomerAccessToken(token);
            Long customerId = jwtTokenService.parseCustomerId(customerClaims);
            String email = jwtTokenService.parseCustomerEmail(customerClaims);
            if (customerId == null || customerId <= 0) {
                customerId = resolveIdFromSubject(customerClaims.getSubject());
            }
            if (customerId == null && (email == null || email.isBlank())) {
                return;
            }

            Optional<com.noura.platform.commerce.customers.domain.CustomerAccount> customerOpt = customerId != null
                    ? customerAccountRepo.findById(customerId)
                    : customerAccountRepo.findByEmailIgnoreCase(email);
            if (customerOpt.isEmpty()) {
                return;
            }
            var customer = customerOpt.get();
            if (customer.getStatus() != CustomerAccountStatus.ACTIVE) {
                return;
            }

            StorefrontCustomerPrincipal customerPrincipal = new StorefrontCustomerPrincipal(
                    customer.getId(),
                    customer.getEmail(),
                    customer.getFirstName(),
                    customer.getLastName()
            );
            UsernamePasswordAuthenticationToken customerAuthentication =
                    new UsernamePasswordAuthenticationToken(
                            customerPrincipal,
                            null,
                            List.of(new SimpleGrantedAuthority("ROLE_CUSTOMER"))
                    );
            customerAuthentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
            SecurityContextHolder.getContext().setAuthentication(customerAuthentication);
            filterChain.doFilter(request, response);
            return;
        } catch (Exception ignored) {
            SecurityContextHolder.clearContext();
        }

        filterChain.doFilter(request, response);
    }

    private Long resolveIdFromSubject(String subject) {
        if (subject == null || subject.isBlank()) {
            return null;
        }
        try {
            return Long.parseLong(subject);
        } catch (NumberFormatException ex) {
            return null;
        }
    }
}
