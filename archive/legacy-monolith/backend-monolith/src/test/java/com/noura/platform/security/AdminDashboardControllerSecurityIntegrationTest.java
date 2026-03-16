package com.noura.platform.security;

import com.noura.platform.config.CorrelationIdFilter;
import com.noura.platform.config.RateLimitFilter;
import com.noura.platform.config.SecurityConfig;
import com.noura.platform.controller.AdminDashboardController;
import com.noura.platform.dto.admin.AdminAuthorizationMatrixDto;
import com.noura.platform.service.AdminAuthorizationService;
import com.noura.platform.service.AdminDashboardService;
import com.noura.platform.service.UserAccountService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Module: Admin Authorization Security
 * Purpose: Verifies method-level role restrictions for admin capabilities and RBAC matrix endpoints.
 * Responsibilities:
 * - Ensure anonymous requests are blocked.
 * - Ensure insufficient roles are denied.
 * - Ensure admin users can access RBAC matrix data.
 * Related modules:
 * - AdminDashboardController
 * - SecurityConfig
 */
@WebMvcTest(controllers = AdminDashboardController.class)
@Import({
        SecurityConfig.class,
        JwtAuthenticationFilter.class,
        JwtTokenProvider.class,
        CorrelationIdFilter.class,
        RateLimitFilter.class
})
@TestPropertySource(properties = {
        "app.api.version-prefix=/api/v1",
        "app.jwt.secret=0123456789abcdef0123456789abcdef",
        "app.jwt.issuer=noura-test",
        "spring.main.allow-bean-definition-overriding=true"
})
class AdminDashboardControllerSecurityIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @SpringBootConfiguration
    @EnableAutoConfiguration
    @Import(AdminDashboardController.class)
    static class TestApplication {
    }

    @MockBean
    private CustomUserDetailsService customUserDetailsService;

    @MockBean
    private AdminDashboardService adminDashboardService;

    @MockBean
    private AdminAuthorizationService adminAuthorizationService;

    @MockBean
    private UserAccountService userAccountService;

    @Test
    void capabilities_shouldRejectAnonymousRequest() throws Exception {
        mockMvc.perform(get("/api/v1/admin/capabilities"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error.code").value("AUTH_REQUIRED"));
    }

    @Test
    void capabilities_shouldAllowAuthenticatedRole() throws Exception {
        when(adminAuthorizationService.knownRoleCodes()).thenReturn(Set.of("CUSTOMER"));
        when(adminAuthorizationService.capabilitiesForRoles(Set.of("CUSTOMER")))
                .thenReturn(Map.of("overview.dashboard", false));

        mockMvc.perform(get("/api/v1/admin/capabilities")
                        .with(user("customer@noura.test").roles("CUSTOMER")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.roles[0]").value("CUSTOMER"))
                .andExpect(jsonPath("$['data']['capabilities']['overview.dashboard']").value(false));
    }

    @Test
    void authorizationMatrix_shouldAllowAdminRole() throws Exception {
        when(adminAuthorizationService.matrix()).thenReturn(
                new AdminAuthorizationMatrixDto("rbac-matrix-v1", List.of("read"), List.of(), List.of())
        );

        mockMvc.perform(get("/api/v1/admin/authorization/matrix")
                        .with(user("admin@noura.test").roles("ADMIN")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.version").value("rbac-matrix-v1"));
    }

    @Test
    void authorizationMatrix_shouldAllowRolesReadPermission() throws Exception {
        when(adminAuthorizationService.matrix()).thenReturn(
                new AdminAuthorizationMatrixDto("rbac-matrix-v1", List.of("read"), List.of(), List.of())
        );

        mockMvc.perform(get("/api/v1/admin/authorization/matrix")
                        .with(user("rbac@noura.test")
                                .authorities(new SimpleGrantedAuthority("PERM_ROLES_READ"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.version").value("rbac-matrix-v1"));
    }

    @Test
    void authorizationMatrix_shouldRejectWarehouseManagerRole() throws Exception {
        mockMvc.perform(get("/api/v1/admin/authorization/matrix")
                        .with(user("wm@noura.test").roles("WAREHOUSE_MANAGER")))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error.code").value("ACCESS_DENIED"));
    }
}
