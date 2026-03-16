package com.noura.platform.service.impl;

import com.noura.platform.domain.entity.AdminRole;
import com.noura.platform.domain.entity.AdminRolePermission;
import com.noura.platform.domain.enums.RoleType;
import com.noura.platform.dto.admin.AdminAuthorizationMatrixDto;
import com.noura.platform.dto.admin.AdminPermissionScopeDto;
import com.noura.platform.dto.admin.AdminRolePermissionDto;
import com.noura.platform.repository.AdminRolePermissionRepository;
import com.noura.platform.repository.AdminRoleRepository;
import com.noura.platform.repository.AdminUserRoleRepository;
import com.noura.platform.repository.projection.AdminRoleAssignmentCountProjection;
import com.noura.platform.service.AdminAuthorizationService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Module: Admin Authorization
 * Purpose: Centralized policy service for runtime capabilities and persisted RBAC matrix projection.
 * Responsibilities:
 * - Resolve capability flags from role codes and role grants.
 * - Build versioned RBAC matrix payload from persisted role-permission data.
 * - Expose known role codes for controller-level role normalization.
 * Related modules:
 * - AdminDashboardController
 * - AdminRoleManagementService
 * - frontend admin role/capability guards
 */
@Service
@RequiredArgsConstructor
public class AdminAuthorizationServiceImpl implements AdminAuthorizationService {

    private static final String MATRIX_VERSION = "rbac-matrix-v3-enterprise";

    private static final List<String> ACTION_CATALOG = List.of(
            "read",
            "create",
            "update",
            "delete",
            "approve",
            "export",
            "manage",
            "publish",
            "dispatch",
            "reconcile",
            "resolve",
            "receive",
            "configure"
    );
    private static final List<String> CAPABILITY_CATALOG = List.of(
            "overview.dashboard",
            "overview.analytics",
            "commerce.catalog",
            "commerce.carousels",
            "commerce.recommendations",
            "commerce.merchandising",
            "commerce.orders",
            "commerce.returns",
            "commerce.stores",
            "commerce.pricing",
            "commerce.users",
            "commerce.notifications",
            "warehouse.catalog",
            "warehouse.locations",
            "warehouse.stock",
            "warehouse.stock.adjust",
            "warehouse.movements",
            "warehouse.batches",
            "warehouse.serials",
            "warehouse.reports",
            "warehouse.webhooks",
            "warehouse.auditLogs",
            "tools.controlCenter",
            "tools.productGenerator",
            "governance.recovery",
            "governance.recovery.purge",
            "governance.rbac"
    );

    private static final List<AdminPermissionScopeDto> SCOPE_CATALOG = List.of(
            scope("dashboard", "Dashboard", "Operational dashboard access, widgets, and executive visibility."),
            scope("catalog", "Catalog", "Product catalog lifecycle, media, category taxonomy, and publishing."),
            scope("inventory", "Inventory", "Stock operations, warehouse controls, and adjustment governance."),
            scope("orders", "Orders", "Order orchestration, returns review, and fulfillment interventions."),
            scope("customers", "Customers", "Customer profile management, segmentation, and lifecycle tooling."),
            scope("sales_promotions", "Sales & Promotions", "Promotion campaigns, coupon policy, and approval workflows."),
            scope("finance", "Finance", "Payment reconciliation, invoicing controls, and finance exports."),
            scope("marketing", "Marketing", "Campaign operations, recommendations, content, and feature messaging."),
            scope("support", "Support", "Ticket handling, escalation workflows, and customer issue resolution."),
            scope("vendors", "Vendors", "Vendor onboarding, governance, and supplier relationship controls."),
            scope("analytics", "Analytics", "Analytics dashboards, data exports, and KPI governance."),
            scope("shipping", "Shipping", "Shipment lifecycle, dispatch controls, and carrier operations."),
            scope("localization", "Localization", "Locale, translation, and region-specific merchandising governance."),
            scope("procurement", "Procurement", "Purchase order management, receiving, and procurement approvals."),
            scope("contracts", "Contracts", "Contract-based merchant onboarding and store activation governance."),
            scope("product_submissions", "Product submissions", "Store-submitted product onboarding workflow and dedupe governance."),
            scope("users", "Users", "Admin user provisioning and account state management."),
            scope("roles", "Roles", "Role definition, permission assignments, and access governance."),
            scope("audit_logs", "Audit logs", "Audit events, change tracking, and compliance exports.")
    );

    private static final Map<String, Set<String>> ROLE_CAPABILITY_DEFAULTS = createRoleCapabilityDefaults();

    private final AdminRoleRepository adminRoleRepository;
    private final AdminRolePermissionRepository adminRolePermissionRepository;
    private final AdminUserRoleRepository adminUserRoleRepository;

    /**
     * Builds the persisted RBAC matrix payload.
     *
     * @return Versioned matrix payload with role metadata and grants.
     */
    @Override
    @Transactional(readOnly = true)
    public AdminAuthorizationMatrixDto matrix() {
        List<AdminRole> roles = adminRoleRepository.findAllByOrderByCodeAsc();
        if (roles.isEmpty()) {
            return new AdminAuthorizationMatrixDto(MATRIX_VERSION, ACTION_CATALOG, SCOPE_CATALOG, List.of());
        }

        List<UUID> roleIds = roles.stream().map(AdminRole::getId).toList();
        List<AdminRolePermission> assignments = adminRolePermissionRepository.findDetailedByRoleIds(roleIds);

        Map<UUID, List<AdminRolePermission>> assignmentsByRoleId = assignments.stream()
                .collect(Collectors.groupingBy(
                        assignment -> assignment.getRole().getId(),
                        LinkedHashMap::new,
                        Collectors.toList()
                ));
        Map<UUID, Long> userCountsByRoleId = adminUserRoleRepository.countByRoleIds(roleIds).stream()
                .collect(Collectors.toMap(AdminRoleAssignmentCountProjection::getRoleId, AdminRoleAssignmentCountProjection::getUserCount));

        Map<String, Set<String>> permissionKeysByRoleCode = rolePermissionKeysByRoleCode(roles, assignments);

        List<AdminRolePermissionDto> roleDtos = roles.stream()
                .map(role -> {
                    Map<String, List<String>> grants = assignmentsByRoleId.getOrDefault(role.getId(), List.of()).stream()
                            .collect(Collectors.groupingBy(
                                    assignment -> assignment.getPermission().getScope(),
                                    LinkedHashMap::new,
                                    Collectors.mapping(
                                            assignment -> assignment.getPermission().getAction(),
                                            Collectors.collectingAndThen(Collectors.toCollection(LinkedHashSet::new), ArrayList::new)
                                    )
                            ));

                    List<String> capabilities = enabledCapabilities(capabilitiesForRoleCodes(
                            Set.of(role.getCode()),
                            permissionKeysByRoleCode
                    ));

                    return new AdminRolePermissionDto(
                            role.getId(),
                            role.getCode(),
                            role.getLabel(),
                            role.getDescription(),
                            role.isSystemRole(),
                            role.isAssignable(),
                            role.isActive(),
                            userCountsByRoleId.getOrDefault(role.getId(), 0L),
                            grants,
                            capabilities
                    );
                })
                .toList();

        return new AdminAuthorizationMatrixDto(MATRIX_VERSION, ACTION_CATALOG, SCOPE_CATALOG, roleDtos);
    }

    /**
     * Resolves capability booleans for provided role codes.
     *
     * @param roles Normalized role codes.
     * @return Deterministic capability map.
     */
    @Override
    @Transactional(readOnly = true)
    public Map<String, Boolean> capabilitiesForRoles(Set<String> roles) {
        Set<String> normalizedRoles = normalizeRoles(roles);
        if (normalizedRoles.isEmpty()) {
            return emptyCapabilityMap();
        }

        List<AdminRole> persistedRoles = adminRoleRepository.findAllByCodes(normalizedRoles);
        Map<String, Set<String>> permissionKeysByRoleCode = persistedRoles.isEmpty()
                ? Map.of()
                : rolePermissionKeysByRoleCode(
                        persistedRoles,
                        adminRolePermissionRepository.findDetailedByRoleIds(persistedRoles.stream().map(AdminRole::getId).toList())
                );

        return capabilitiesForRoleCodes(normalizedRoles, permissionKeysByRoleCode);
    }

    /**
     * Publishes known role codes from persisted roles and legacy role enums.
     *
     * @return Known role code set.
     */
    @Override
    @Transactional(readOnly = true)
    public Set<String> knownRoleCodes() {
        LinkedHashSet<String> known = new LinkedHashSet<>();
        known.addAll(RoleType.values().length == 0 ? Set.of() : Arrays.stream(RoleType.values()).map(Enum::name).toList());
        known.addAll(ROLE_CAPABILITY_DEFAULTS.keySet());
        known.addAll(adminRoleRepository.findActiveCodes().stream().map(value -> value.toUpperCase(Locale.ROOT)).toList());
        return known;
    }

    private Map<String, Boolean> capabilitiesForRoleCodes(
            Set<String> normalizedRoles,
            Map<String, Set<String>> permissionKeysByRoleCode
    ) {
        Map<String, Boolean> capabilityMap = emptyCapabilityMap();

        for (String roleCode : normalizedRoles) {
            Set<String> defaults = ROLE_CAPABILITY_DEFAULTS.getOrDefault(roleCode, Set.of());
            defaults.forEach(capability -> capabilityMap.put(capability, true));

            Set<String> permissionKeys = permissionKeysByRoleCode.getOrDefault(roleCode, Set.of());
            permissionDerivedCapabilities(permissionKeys).forEach(capability -> capabilityMap.put(capability, true));
        }

        return capabilityMap;
    }

    private static Set<String> permissionDerivedCapabilities(Set<String> permissionKeys) {
        if (permissionKeys == null || permissionKeys.isEmpty()) {
            return Set.of();
        }

        LinkedHashSet<String> capabilities = new LinkedHashSet<>();
        boolean hasAnyPermission = !permissionKeys.isEmpty();
        boolean hasReportsAccess = hasAny(permissionKeys,
                "reports.read", "reports.export",
                "analytics.read", "analytics.export", "analytics.configure_dashboards");
        boolean hasCatalogAccess = hasAny(permissionKeys,
                "products.read", "categories.read", "media.read",
                "catalog.read_products", "catalog.create_products", "catalog.update_products", "catalog.publish_products");
        boolean hasOrdersAccess = hasAny(permissionKeys,
                "orders.read", "orders.update", "orders.export",
                "orders.read_orders", "orders.update_orders", "orders.manage_fulfillment");
        boolean hasInventoryAccess = hasAny(permissionKeys,
                "inventory.read", "inventory.update", "inventory.approve",
                "inventory.read_stock", "inventory.update_stock", "inventory.manage_warehouses");
        boolean canAdjustInventory = hasAny(permissionKeys,
                "inventory.update", "inventory.approve", "inventory.delete",
                "inventory.adjust_stock", "inventory.approve_adjustments");
        boolean canManageUsers = hasAny(permissionKeys, "users.read", "users.update", "staff.read", "staff.update");
        boolean canManageRoles = hasAny(permissionKeys, "roles.read", "roles.update", "roles.approve");
        boolean canManageIntegrations = hasAny(permissionKeys,
                "integrations.update", "integrations.approve",
                "shipping.manage_carriers", "vendors.manage_vendors", "vendors.approve_vendors");
        boolean canManageAuditLogs = hasAny(permissionKeys, "audit_logs.read", "audit_logs.export");
        boolean hasPricingAccess = hasAny(permissionKeys,
                "discounts.read", "discounts.update", "settings.update",
                "sales_promotions.read_promotions", "sales_promotions.create_promotions",
                "sales_promotions.update_promotions", "sales_promotions.approve_promotions",
                "finance.read_ledger", "finance.reconcile_payments", "finance.export_financials");
        boolean hasMarketingAccess = hasAny(permissionKeys,
                "marketing.manage_campaigns", "marketing.manage_carousels",
                "marketing.manage_recommendations", "marketing.manage_content",
                "marketing.publish_marketing");
        boolean hasSupportAccess = hasAny(permissionKeys,
                "support.read_tickets", "support.respond_tickets", "support.resolve_tickets", "support.escalate_tickets");
        boolean hasShippingAccess = hasAny(permissionKeys,
                "shipping.read_shipments", "shipping.create_shipments", "shipping.dispatch_shipments",
                "shipping.track_shipments", "shipping.manage_carriers");
        boolean hasProcurementAccess = hasAny(permissionKeys,
                "procurement.read_purchase_orders", "procurement.create_purchase_orders",
                "procurement.approve_purchase_orders", "procurement.receive_goods", "procurement.manage_suppliers");
        boolean hasLocalizationAccess = hasAny(permissionKeys,
                "localization.read_locales", "localization.manage_translations", "localization.publish_localization");

        if (hasAnyPermission) {
            capabilities.add("overview.dashboard");
        }
        if (hasReportsAccess) {
            capabilities.add("overview.analytics");
        }
        if (hasCatalogAccess) {
            capabilities.add("commerce.catalog");
            capabilities.add("commerce.carousels");
            capabilities.add("commerce.recommendations");
            capabilities.add("commerce.merchandising");
            capabilities.add("tools.productGenerator");
        }
        if (hasMarketingAccess) {
            capabilities.add("commerce.carousels");
            capabilities.add("commerce.recommendations");
            capabilities.add("commerce.merchandising");
        }
        if (hasOrdersAccess) {
            capabilities.add("commerce.orders");
            capabilities.add("commerce.returns");
            capabilities.add("commerce.notifications");
        }
        if (hasSupportAccess) {
            capabilities.add("commerce.orders");
            capabilities.add("commerce.returns");
            capabilities.add("commerce.notifications");
        }
        if (hasInventoryAccess) {
            capabilities.add("warehouse.catalog");
            capabilities.add("warehouse.locations");
            capabilities.add("warehouse.stock");
            capabilities.add("warehouse.movements");
            capabilities.add("warehouse.batches");
            capabilities.add("warehouse.serials");
        }
        if (hasShippingAccess) {
            capabilities.add("commerce.orders");
            capabilities.add("commerce.returns");
            capabilities.add("warehouse.movements");
        }
        if (canAdjustInventory) {
            capabilities.add("warehouse.stock.adjust");
        }
        if (hasReportsAccess || hasInventoryAccess || hasProcurementAccess) {
            capabilities.add("warehouse.reports");
        }
        if (hasPricingAccess) {
            capabilities.add("commerce.pricing");
        }
        if (canManageUsers) {
            capabilities.add("commerce.users");
            capabilities.add("tools.controlCenter");
        }
        if (canManageRoles) {
            capabilities.add("governance.rbac");
        }
        if (canManageIntegrations) {
            capabilities.add("warehouse.webhooks");
        }
        if (canManageAuditLogs) {
            capabilities.add("warehouse.auditLogs");
            capabilities.add("governance.recovery");
            if (hasAny(permissionKeys, "audit_logs.approve", "audit_logs.delete")) {
                capabilities.add("governance.recovery.purge");
            }
        }
        if (hasLocalizationAccess) {
            capabilities.add("commerce.catalog");
        }
        if (hasAny(permissionKeys,
                "settings.read", "settings.update", "inventory.read", "inventory.read_stock",
                "vendors.read_vendors", "procurement.read_purchase_orders", "localization.read_locales")) {
            capabilities.add("commerce.stores");
        }

        return capabilities;
    }

    private static boolean hasAny(Set<String> permissionKeys, String... keys) {
        for (String key : keys) {
            if (permissionKeys.contains(key)) {
                return true;
            }
        }
        return false;
    }

    private static List<String> enabledCapabilities(Map<String, Boolean> capabilityMap) {
        return capabilityMap.entrySet().stream()
                .filter(Map.Entry::getValue)
                .map(Map.Entry::getKey)
                .sorted()
                .toList();
    }

    private static Map<String, Boolean> emptyCapabilityMap() {
        LinkedHashMap<String, Boolean> capabilities = new LinkedHashMap<>();
        CAPABILITY_CATALOG.forEach(capability -> capabilities.put(capability, false));
        return capabilities;
    }

    private static Set<String> normalizeRoles(Set<String> roles) {
        if (roles == null || roles.isEmpty()) {
            return Set.of();
        }
        return roles.stream()
                .filter(Objects::nonNull)
                .map(String::trim)
                .filter(value -> !value.isBlank())
                .map(value -> value.toUpperCase(Locale.ROOT))
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    private static Map<String, Set<String>> rolePermissionKeysByRoleCode(
            List<AdminRole> roles,
            List<AdminRolePermission> assignments
    ) {
        Map<UUID, String> roleCodeById = roles.stream()
                .collect(Collectors.toMap(AdminRole::getId, role -> role.getCode().toUpperCase(Locale.ROOT)));

        LinkedHashMap<String, Set<String>> permissionKeysByRoleCode = new LinkedHashMap<>();
        for (AdminRole role : roles) {
            permissionKeysByRoleCode.put(role.getCode().toUpperCase(Locale.ROOT), new LinkedHashSet<>());
        }

        for (AdminRolePermission assignment : assignments) {
            String roleCode = roleCodeById.get(assignment.getRole().getId());
            if (roleCode == null || assignment.getPermission() == null) {
                continue;
            }
            String permissionKey = assignment.getPermission().getScope().toLowerCase(Locale.ROOT)
                    + "."
                    + assignment.getPermission().getAction().toLowerCase(Locale.ROOT);
            permissionKeysByRoleCode.computeIfAbsent(roleCode, ignored -> new LinkedHashSet<>()).add(permissionKey);
        }

        return permissionKeysByRoleCode;
    }

    private static AdminPermissionScopeDto scope(String scope, String label, String description) {
        return new AdminPermissionScopeDto(scope, label, description, ACTION_CATALOG);
    }

    private static Map<String, Set<String>> createRoleCapabilityDefaults() {
        LinkedHashMap<String, Set<String>> defaults = new LinkedHashMap<>();

        Set<String> all = new LinkedHashSet<>(CAPABILITY_CATALOG);
        defaults.put("SUPER_ADMIN", Set.copyOf(all));
        defaults.put("ADMIN", Set.copyOf(all));

        defaults.put("MANAGER", Set.of(
                "overview.dashboard", "overview.analytics", "commerce.catalog", "commerce.carousels",
                "commerce.recommendations", "commerce.merchandising", "commerce.orders", "commerce.returns",
                "commerce.stores", "commerce.pricing", "commerce.users", "commerce.notifications",
                "warehouse.catalog", "warehouse.locations", "warehouse.stock", "warehouse.movements",
                "warehouse.batches", "warehouse.serials", "warehouse.reports",
                "tools.controlCenter", "tools.productGenerator"
        ));
        defaults.put("CONTENT_MANAGER", Set.of(
                "overview.dashboard", "commerce.catalog", "commerce.carousels",
                "commerce.recommendations", "commerce.merchandising"
        ));
        defaults.put("PRODUCT_MANAGER", Set.of(
                "overview.dashboard", "commerce.catalog", "commerce.carousels",
                "commerce.recommendations", "commerce.merchandising", "tools.productGenerator"
        ));
        defaults.put("STORE_MANAGER", Set.of(
                "overview.dashboard", "overview.analytics", "commerce.catalog", "commerce.carousels",
                "commerce.recommendations", "commerce.merchandising", "commerce.orders", "commerce.returns",
                "commerce.stores", "commerce.pricing", "commerce.users", "commerce.notifications",
                "warehouse.catalog", "warehouse.locations", "warehouse.stock", "warehouse.movements",
                "warehouse.batches", "warehouse.serials", "warehouse.reports",
                "tools.controlCenter", "tools.productGenerator"
        ));
        defaults.put("INVENTORY_MANAGER", Set.of(
                "overview.dashboard", "warehouse.catalog", "warehouse.locations", "warehouse.stock",
                "warehouse.stock.adjust", "warehouse.movements", "warehouse.batches",
                "warehouse.serials", "warehouse.reports"
        ));
        defaults.put("ORDER_MANAGER", Set.of(
                "overview.dashboard", "commerce.orders", "commerce.returns", "commerce.notifications"
        ));
        defaults.put("SUPPORT_AGENT", Set.of(
                "overview.dashboard", "commerce.orders", "commerce.returns", "commerce.notifications"
        ));
        defaults.put("CUSTOMER_SUPPORT", Set.of(
                "overview.dashboard", "commerce.orders", "commerce.returns", "commerce.notifications"
        ));
        defaults.put("FINANCE", Set.of("overview.dashboard", "overview.analytics", "commerce.pricing"));
        defaults.put("FINANCE_OFFICER", Set.of("overview.dashboard", "overview.analytics", "commerce.pricing"));
        defaults.put("MARKETING_MANAGER", Set.of(
                "overview.dashboard", "overview.analytics", "commerce.catalog", "commerce.carousels",
                "commerce.recommendations", "commerce.merchandising", "commerce.pricing", "tools.productGenerator"
        ));
        defaults.put("ANALYST", Set.of("overview.dashboard", "overview.analytics", "warehouse.reports"));
        defaults.put("STAFF", Set.of("overview.dashboard"));

        defaults.put("WAREHOUSE_MANAGER", Set.of(
                "overview.dashboard", "warehouse.catalog", "warehouse.locations", "warehouse.stock",
                "warehouse.stock.adjust", "warehouse.movements", "warehouse.batches",
                "warehouse.serials", "warehouse.reports"
        ));
        defaults.put("VIEWER", Set.of(
                "overview.dashboard", "warehouse.catalog", "warehouse.locations", "warehouse.stock",
                "warehouse.movements", "warehouse.batches", "warehouse.serials", "warehouse.reports"
        ));

        return defaults;
    }
}
