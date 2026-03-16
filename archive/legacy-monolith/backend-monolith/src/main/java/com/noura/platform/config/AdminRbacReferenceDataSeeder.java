package com.noura.platform.config;

import com.noura.platform.domain.entity.AdminPermission;
import com.noura.platform.domain.entity.AdminRole;
import com.noura.platform.domain.entity.AdminRolePermission;
import com.noura.platform.domain.entity.AdminUserRole;
import com.noura.platform.domain.entity.UserAccount;
import com.noura.platform.domain.entity.id.AdminRolePermissionId;
import com.noura.platform.domain.entity.id.AdminUserRoleId;
import com.noura.platform.domain.enums.RoleType;
import com.noura.platform.repository.AdminPermissionRepository;
import com.noura.platform.repository.AdminRolePermissionRepository;
import com.noura.platform.repository.AdminRoleRepository;
import com.noura.platform.repository.AdminUserRoleRepository;
import com.noura.platform.repository.UserAccountRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Seeds and maintains enterprise RBAC reference data in an idempotent way.
 *
 * This keeps local and long-lived environments aligned with the canonical RBAC
 * catalog even when schema migrations were not applied yet.
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "app.rbac", name = "bootstrap-enabled", havingValue = "true", matchIfMissing = true)
public class AdminRbacReferenceDataSeeder implements ApplicationRunner {

    private static final String ALL_PERMISSIONS = "*";

    private static final List<PermissionSeed> PERMISSION_SEEDS = List.of(
            permission("dashboard", "read_overview", "Dashboard Overview", "View executive dashboard overview.", "core", 10, false),
            permission("dashboard", "read_kpis", "Dashboard KPIs", "View KPI widgets and scorecards.", "core", 11, false),
            permission("dashboard", "read_activity", "Dashboard Activity", "View live operational activity stream.", "core", 12, false),
            permission("dashboard", "configure_widgets", "Dashboard Widgets", "Configure dashboard widget layout and preferences.", "core", 13, false),

            permission("catalog", "read_products", "Catalog Read", "View catalog products and variants.", "commerce", 20, false),
            permission("catalog", "create_products", "Catalog Create", "Create products and variants.", "commerce", 21, false),
            permission("catalog", "update_products", "Catalog Update", "Update product attributes and merchandising metadata.", "commerce", 22, false),
            permission("catalog", "publish_products", "Catalog Publish", "Publish products to storefront channels.", "commerce", 23, true),
            permission("catalog", "manage_media", "Catalog Media", "Manage product media assets and ordering.", "commerce", 24, false),

            permission("inventory", "read_stock", "Inventory Read", "View stock and warehouse inventory levels.", "operations", 30, false),
            permission("inventory", "update_stock", "Inventory Update", "Update stock levels and stock policy.", "operations", 31, false),
            permission("inventory", "adjust_stock", "Inventory Adjust", "Create stock adjustments and corrections.", "operations", 32, true),
            permission("inventory", "manage_warehouses", "Inventory Warehouses", "Manage warehouse and bin structures.", "operations", 33, false),
            permission("inventory", "approve_adjustments", "Inventory Approve", "Approve inventory adjustments.", "operations", 34, true),

            permission("orders", "read_orders", "Orders Read", "View order workflows and order history.", "operations", 40, false),
            permission("orders", "update_orders", "Orders Update", "Update order status and operational fields.", "operations", 41, false),
            permission("orders", "manage_fulfillment", "Orders Fulfillment", "Manage order fulfillment and exceptions.", "operations", 42, false),
            permission("orders", "approve_returns", "Orders Returns", "Approve return requests and reversal actions.", "operations", 43, true),
            permission("orders", "export_orders", "Orders Export", "Export order datasets.", "operations", 44, false),

            permission("customers", "read_customers", "Customers Read", "View customer profiles and segmentation.", "commerce", 50, false),
            permission("customers", "update_customers", "Customers Update", "Update customer operational metadata.", "commerce", 51, false),
            permission("customers", "export_customers", "Customers Export", "Export customer datasets.", "commerce", 52, false),
            permission("customers", "manage_segments", "Customers Segments", "Manage customer segments and cohorts.", "commerce", 53, false),

            permission("sales_promotions", "read_promotions", "Promotions Read", "View promotion and coupon setup.", "commerce", 60, false),
            permission("sales_promotions", "create_promotions", "Promotions Create", "Create promotions and discount rules.", "commerce", 61, false),
            permission("sales_promotions", "update_promotions", "Promotions Update", "Update promotion and coupon rules.", "commerce", 62, false),
            permission("sales_promotions", "approve_promotions", "Promotions Approve", "Approve promotion publishing.", "commerce", 63, true),
            permission("sales_promotions", "manage_coupons", "Coupons Manage", "Manage coupon lifecycle and usage caps.", "commerce", 64, false),

            permission("finance", "read_ledger", "Finance Ledger", "View finance ledgers and settlements.", "finance", 70, true),
            permission("finance", "manage_invoices", "Finance Invoices", "Manage invoices and billing records.", "finance", 71, true),
            permission("finance", "approve_payouts", "Finance Payouts", "Approve payout and disbursement operations.", "finance", 72, true),
            permission("finance", "reconcile_payments", "Finance Reconcile", "Reconcile payment transactions.", "finance", 73, true),
            permission("finance", "export_financials", "Finance Export", "Export finance and accounting datasets.", "finance", 74, true),

            permission("marketing", "manage_campaigns", "Marketing Campaigns", "Manage campaign setup and execution.", "growth", 80, false),
            permission("marketing", "manage_carousels", "Marketing Carousels", "Manage homepage and campaign carousels.", "growth", 81, false),
            permission("marketing", "manage_recommendations", "Marketing Recommendations", "Manage recommendation strategy controls.", "growth", 82, false),
            permission("marketing", "manage_content", "Marketing Content", "Manage marketing copy and content modules.", "growth", 83, false),
            permission("marketing", "publish_marketing", "Marketing Publish", "Publish marketing assets.", "growth", 84, true),

            permission("support", "read_tickets", "Support Read", "View customer support tickets.", "operations", 90, false),
            permission("support", "respond_tickets", "Support Respond", "Respond to customer support tickets.", "operations", 91, false),
            permission("support", "resolve_tickets", "Support Resolve", "Resolve support tickets.", "operations", 92, false),
            permission("support", "escalate_tickets", "Support Escalate", "Escalate support tickets.", "operations", 93, true),

            permission("vendors", "read_vendors", "Vendors Read", "View vendor records and status.", "supply_chain", 100, false),
            permission("vendors", "manage_vendors", "Vendors Manage", "Manage vendor profiles and agreements.", "supply_chain", 101, false),
            permission("vendors", "approve_vendors", "Vendors Approve", "Approve vendor onboarding and activation.", "supply_chain", 102, true),

            permission("analytics", "read_analytics", "Analytics Read", "View analytics dashboards.", "insights", 110, false),
            permission("analytics", "export_analytics", "Analytics Export", "Export analytics data.", "insights", 111, false),
            permission("analytics", "configure_dashboards", "Analytics Configure", "Configure analytics dashboards.", "insights", 112, false),

            permission("shipping", "read_shipments", "Shipping Read", "View shipment status and timelines.", "operations", 120, false),
            permission("shipping", "create_shipments", "Shipping Create", "Create shipment records.", "operations", 121, false),
            permission("shipping", "dispatch_shipments", "Shipping Dispatch", "Dispatch shipments to carriers.", "operations", 122, false),
            permission("shipping", "track_shipments", "Shipping Track", "Track shipment progress.", "operations", 123, false),
            permission("shipping", "manage_carriers", "Shipping Carriers", "Manage carrier integrations and contracts.", "operations", 124, true),

            permission("localization", "read_locales", "Localization Read", "View locale and language setup.", "commerce", 130, false),
            permission("localization", "manage_translations", "Localization Translations", "Manage translations for commerce content.", "commerce", 131, false),
            permission("localization", "publish_localization", "Localization Publish", "Publish localization changes.", "commerce", 132, true),

            permission("procurement", "read_purchase_orders", "Procurement Read", "View purchase orders.", "supply_chain", 140, false),
            permission("procurement", "create_purchase_orders", "Procurement Create", "Create purchase orders.", "supply_chain", 141, false),
            permission("procurement", "approve_purchase_orders", "Procurement Approve", "Approve purchase orders.", "supply_chain", 142, true),
            permission("procurement", "receive_goods", "Procurement Receive", "Receive goods and update inventory records.", "supply_chain", 143, false),
            permission("procurement", "manage_suppliers", "Procurement Suppliers", "Manage supplier terms and onboarding.", "supply_chain", 144, false),

            permission("users", "read", "Users Read", "Read user accounts.", "governance", 200, true),
            permission("users", "update", "Users Update", "Update user accounts.", "governance", 201, true),
            permission("roles", "read", "Roles Read", "Read RBAC roles.", "governance", 202, true),
            permission("roles", "create", "Roles Create", "Create RBAC roles.", "governance", 203, true),
            permission("roles", "update", "Roles Update", "Update RBAC roles and permissions.", "governance", 204, true),
            permission("roles", "delete", "Roles Delete", "Deactivate RBAC roles.", "governance", 205, true),
            permission("roles", "approve", "Roles Approve", "Approve privileged RBAC changes.", "governance", 206, true),
            permission("audit_logs", "read", "Audit Logs Read", "Read audit log events.", "governance", 207, true),
            permission("audit_logs", "export", "Audit Logs Export", "Export audit log events.", "governance", 208, true),
            permission("audit_logs", "approve", "Audit Logs Approve", "Approve sensitive audit-log actions.", "governance", 209, true),
            permission("integrations", "read", "Integrations Read", "Read integration settings.", "governance", 210, true),
            permission("integrations", "update", "Integrations Update", "Update integration settings.", "governance", 211, true),
            permission("settings", "read", "Settings Read", "Read platform settings.", "governance", 212, true),
            permission("settings", "update", "Settings Update", "Update platform settings.", "governance", 213, true),
            permission("reports", "read", "Reports Read", "Read operational reports.", "insights", 214, false),
            permission("reports", "export", "Reports Export", "Export operational reports.", "insights", 215, false)
    );

    private static final List<RoleSeed> ROLE_SEEDS = List.of(
            role("SUPER_ADMIN", "Super Admin", "Global unrestricted administrative role.", true, true, true, Set.of(ALL_PERMISSIONS)),
            role("ADMIN", "Admin", "Enterprise operations admin with broad governance rights.", true, true, true, Set.of(ALL_PERMISSIONS)),

            role("STORE_MANAGER", "Store Manager", "Owns store-level operations, catalog, orders, and fulfillment.", true, true, true,
                    keys(
                            module("dashboard", "read_overview", "read_kpis", "read_activity", "configure_widgets"),
                            module("catalog", "read_products", "create_products", "update_products", "publish_products", "manage_media"),
                            module("inventory", "read_stock", "update_stock", "manage_warehouses"),
                            module("orders", "read_orders", "update_orders", "manage_fulfillment", "export_orders"),
                            module("customers", "read_customers", "update_customers", "export_customers", "manage_segments"),
                            module("sales_promotions", "read_promotions", "create_promotions", "update_promotions", "manage_coupons"),
                            module("shipping", "read_shipments", "create_shipments", "dispatch_shipments", "track_shipments"),
                            module("analytics", "read_analytics", "export_analytics"),
                            module("support", "read_tickets", "respond_tickets", "resolve_tickets"),
                            module("settings", "read")
                    )
            ),
            role("PRODUCT_MANAGER", "Product Manager", "Owns product lifecycle, merchandising, and localization quality.", true, true, true,
                    keys(
                            module("dashboard", "read_overview", "read_kpis"),
                            module("catalog", "read_products", "create_products", "update_products", "publish_products", "manage_media"),
                            module("sales_promotions", "read_promotions", "create_promotions", "update_promotions", "manage_coupons"),
                            module("marketing", "manage_campaigns", "manage_carousels", "manage_recommendations", "manage_content", "publish_marketing"),
                            module("analytics", "read_analytics", "export_analytics"),
                            module("localization", "read_locales", "manage_translations", "publish_localization")
                    )
            ),
            role("INVENTORY_MANAGER", "Inventory Manager", "Owns inventory accuracy, warehouses, and procurement operations.", true, true, true,
                    keys(
                            module("dashboard", "read_overview", "read_kpis"),
                            module("inventory", "read_stock", "update_stock", "adjust_stock", "manage_warehouses", "approve_adjustments"),
                            module("shipping", "read_shipments", "create_shipments", "dispatch_shipments", "track_shipments", "manage_carriers"),
                            module("procurement", "read_purchase_orders", "create_purchase_orders", "approve_purchase_orders", "receive_goods", "manage_suppliers"),
                            module("vendors", "read_vendors", "manage_vendors", "approve_vendors"),
                            module("orders", "read_orders", "update_orders", "manage_fulfillment"),
                            module("analytics", "read_analytics", "export_analytics")
                    )
            ),
            role("FINANCE_OFFICER", "Finance Officer", "Owns finance governance, reconciliation, and commercial controls.", true, true, true,
                    keys(
                            module("dashboard", "read_overview", "read_kpis"),
                            module("finance", "read_ledger", "manage_invoices", "approve_payouts", "reconcile_payments", "export_financials"),
                            module("orders", "read_orders", "export_orders"),
                            module("sales_promotions", "read_promotions", "approve_promotions"),
                            module("analytics", "read_analytics", "export_analytics"),
                            module("reports", "read", "export"),
                            module("audit_logs", "read", "export")
                    )
            ),
            role("CUSTOMER_SUPPORT", "Customer Support", "Handles customer support operations and ticket escalations.", true, true, true,
                    keys(
                            module("dashboard", "read_overview", "read_activity"),
                            module("support", "read_tickets", "respond_tickets", "resolve_tickets", "escalate_tickets"),
                            module("customers", "read_customers", "update_customers"),
                            module("orders", "read_orders", "update_orders", "approve_returns"),
                            module("shipping", "read_shipments", "track_shipments")
                    )
            ),
            role("MARKETING_MANAGER", "Marketing Manager", "Owns growth campaigns and promotion execution.", true, true, true,
                    keys(
                            module("dashboard", "read_overview", "read_kpis"),
                            module("catalog", "read_products", "update_products", "publish_products", "manage_media"),
                            module("marketing", "manage_campaigns", "manage_carousels", "manage_recommendations", "manage_content", "publish_marketing"),
                            module("sales_promotions", "read_promotions", "create_promotions", "update_promotions", "approve_promotions", "manage_coupons"),
                            module("analytics", "read_analytics", "export_analytics", "configure_dashboards"),
                            module("localization", "read_locales", "manage_translations", "publish_localization")
                    )
            ),

            role("MANAGER", "Manager", "Cross-functional operational manager.", true, true, true,
                    keys(
                            module("dashboard", "read_overview", "read_kpis", "read_activity"),
                            module("catalog", "read_products", "create_products", "update_products", "publish_products"),
                            module("orders", "read_orders", "update_orders", "manage_fulfillment", "export_orders"),
                            module("inventory", "read_stock", "update_stock", "adjust_stock"),
                            module("customers", "read_customers", "update_customers"),
                            module("analytics", "read_analytics", "export_analytics"),
                            module("users", "read", "update"),
                            module("roles", "read")
                    )
            ),
            role("ORDER_MANAGER", "Order Manager", "Owns order flow quality and issue resolution.", true, true, true,
                    keys(
                            module("dashboard", "read_overview", "read_activity"),
                            module("orders", "read_orders", "update_orders", "manage_fulfillment", "approve_returns", "export_orders"),
                            module("support", "read_tickets", "respond_tickets", "resolve_tickets"),
                            module("customers", "read_customers", "update_customers"),
                            module("shipping", "read_shipments", "track_shipments")
                    )
            ),
            role("SUPPORT_AGENT", "Support Agent", "Legacy support role alias.", true, true, true,
                    keys(module("support", "read_tickets", "respond_tickets", "resolve_tickets", "escalate_tickets"),
                            module("customers", "read_customers", "update_customers"),
                            module("orders", "read_orders", "update_orders"),
                            module("shipping", "read_shipments", "track_shipments"))
            ),
            role("FINANCE", "Finance", "Legacy finance role alias.", true, true, true,
                    keys(module("finance", "read_ledger", "manage_invoices", "approve_payouts", "reconcile_payments", "export_financials"),
                            module("orders", "read_orders", "export_orders"),
                            module("analytics", "read_analytics", "export_analytics"),
                            module("reports", "read", "export"))
            ),
            role("CONTENT_MANAGER", "Content Manager", "Owns merchandising and content quality.", true, true, true,
                    keys(module("catalog", "read_products", "update_products", "manage_media"),
                            module("marketing", "manage_carousels", "manage_recommendations", "manage_content", "publish_marketing"),
                            module("localization", "read_locales", "manage_translations", "publish_localization"))
            ),
            role("WAREHOUSE_MANAGER", "Warehouse Manager", "Warehouse operations lead.", true, true, true,
                    keys(module("inventory", "read_stock", "update_stock", "adjust_stock", "manage_warehouses", "approve_adjustments"),
                            module("shipping", "read_shipments", "create_shipments", "dispatch_shipments", "track_shipments"),
                            module("procurement", "read_purchase_orders", "create_purchase_orders", "approve_purchase_orders", "receive_goods"),
                            module("vendors", "read_vendors", "manage_vendors"),
                            module("reports", "read", "export"))
            ),
            role("ANALYST", "Analyst", "Read-oriented analytics access role.", true, true, true,
                    keys(module("dashboard", "read_overview", "read_kpis"),
                            module("analytics", "read_analytics", "export_analytics"),
                            module("reports", "read", "export"),
                            module("orders", "read_orders"),
                            module("customers", "read_customers"),
                            module("inventory", "read_stock"))
            ),
            role("STAFF", "Staff", "General operations staff with constrained read access.", true, true, true,
                    keys(module("dashboard", "read_overview"), module("orders", "read_orders"), module("customers", "read_customers"), module("inventory", "read_stock"))
            ),
            role("VIEWER", "Viewer", "Read-only operations visibility role.", true, true, true,
                    keys(module("dashboard", "read_overview"), module("inventory", "read_stock"), module("orders", "read_orders"), module("analytics", "read_analytics"), module("shipping", "read_shipments"))
            ),
            role("CUSTOMER", "Customer", "Compatibility role for customer accounts.", true, false, true, Set.of()),
            role("B2B", "B2B", "Compatibility role for enterprise customer accounts.", true, false, true, Set.of())
    );

    private final AdminPermissionRepository adminPermissionRepository;
    private final AdminRoleRepository adminRoleRepository;
    private final AdminRolePermissionRepository adminRolePermissionRepository;
    private final AdminUserRoleRepository adminUserRoleRepository;
    private final UserAccountRepository userAccountRepository;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        int permissionMutations = upsertPermissions();
        int roleMutations = upsertRoles();
        int roleGrantMutations = upsertRolePermissions();
        int assignmentMutations = ensureAdminUserAssignments();

        int totalMutations = permissionMutations + roleMutations + roleGrantMutations + assignmentMutations;
        if (totalMutations > 0) {
            log.info(
                    "Admin RBAC reference data synchronized (permissions={}, roles={}, grants={}, assignments={})",
                    permissionMutations,
                    roleMutations,
                    roleGrantMutations,
                    assignmentMutations
            );
        }
    }

    private int upsertPermissions() {
        Map<String, AdminPermission> existingByKey = adminPermissionRepository.findAllByOrderByScopeAscActionAsc().stream()
                .collect(Collectors.toMap(
                        permission -> key(permission.getScope(), permission.getAction()),
                        permission -> permission,
                        (left, right) -> left,
                        LinkedHashMap::new
                ));

        int mutations = 0;
        for (PermissionSeed seed : PERMISSION_SEEDS) {
            String key = key(seed.scope(), seed.action());
            AdminPermission permission = existingByKey.get(key);
            boolean isNew = permission == null;
            if (isNew) {
                permission = new AdminPermission();
                permission.setScope(seed.scope());
                permission.setAction(seed.action());
            }

            boolean changed = isNew
                    || !seed.label().equals(permission.getLabel())
                    || !equalsNullable(seed.description(), permission.getDescription())
                    || !seed.moduleGroup().equals(permission.getModuleGroup())
                    || seed.displayOrder() != permission.getDisplayOrder()
                    || seed.sensitive() != permission.isSensitive();

            if (changed) {
                permission.setLabel(seed.label());
                permission.setDescription(seed.description());
                permission.setModuleGroup(seed.moduleGroup());
                permission.setDisplayOrder(seed.displayOrder());
                permission.setSensitive(seed.sensitive());
                adminPermissionRepository.save(permission);
                mutations++;
                existingByKey.put(key, permission);
            }
        }

        return mutations;
    }

    private int upsertRoles() {
        Map<String, AdminRole> existingByCode = adminRoleRepository.findAllByOrderByCodeAsc().stream()
                .collect(Collectors.toMap(role -> normalizeToken(role.getCode()), role -> role, (left, right) -> left, LinkedHashMap::new));

        int mutations = 0;
        for (RoleSeed seed : ROLE_SEEDS) {
            String normalizedCode = normalizeToken(seed.code());
            AdminRole role = existingByCode.get(normalizedCode);
            boolean isNew = role == null;
            if (isNew) {
                role = new AdminRole();
                role.setCode(seed.code());
            }

            boolean changed = isNew
                    || !seed.label().equals(role.getLabel())
                    || !equalsNullable(seed.description(), role.getDescription())
                    || seed.systemRole() != role.isSystemRole()
                    || seed.assignable() != role.isAssignable()
                    || seed.active() != role.isActive();

            if (changed) {
                role.setLabel(seed.label());
                role.setDescription(seed.description());
                role.setSystemRole(seed.systemRole());
                role.setAssignable(seed.assignable());
                role.setActive(seed.active());
                role = adminRoleRepository.save(role);
                mutations++;
            }

            existingByCode.put(normalizedCode, role);
        }

        return mutations;
    }

    private int upsertRolePermissions() {
        Map<String, AdminPermission> permissionByKey = adminPermissionRepository.findAllByOrderByDisplayOrderAscScopeAscActionAsc().stream()
                .collect(Collectors.toMap(
                        permission -> key(permission.getScope(), permission.getAction()),
                        permission -> permission,
                        (left, right) -> left,
                        LinkedHashMap::new
                ));
        Map<String, AdminRole> roleByCode = adminRoleRepository.findAllByOrderByCodeAsc().stream()
                .collect(Collectors.toMap(role -> normalizeToken(role.getCode()), role -> role, (left, right) -> left, LinkedHashMap::new));

        List<UUID> roleIds = roleByCode.values().stream().map(AdminRole::getId).toList();
        Map<UUID, Set<UUID>> existingPermissionIdsByRoleId = adminRolePermissionRepository.findDetailedByRoleIds(roleIds).stream()
                .collect(Collectors.groupingBy(
                        assignment -> assignment.getRole().getId(),
                        LinkedHashMap::new,
                        Collectors.mapping(assignment -> assignment.getPermission().getId(), Collectors.toCollection(LinkedHashSet::new))
                ));

        List<AdminRolePermission> toCreate = new ArrayList<>();

        for (RoleSeed seed : ROLE_SEEDS) {
            AdminRole role = roleByCode.get(normalizeToken(seed.code()));
            if (role == null) {
                continue;
            }

            Set<UUID> current = existingPermissionIdsByRoleId.computeIfAbsent(role.getId(), ignored -> new LinkedHashSet<>());
            Set<UUID> required = resolveRequiredPermissionIds(seed, permissionByKey);

            for (UUID permissionId : required) {
                if (!current.add(permissionId)) {
                    continue;
                }
                AdminPermission permission = findPermission(permissionByKey.values(), permissionId);
                if (permission == null) {
                    continue;
                }
                AdminRolePermission assignment = new AdminRolePermission();
                assignment.setId(new AdminRolePermissionId(role.getId(), permission.getId()));
                assignment.setRole(role);
                assignment.setPermission(permission);
                toCreate.add(assignment);
            }
        }

        if (!toCreate.isEmpty()) {
            adminRolePermissionRepository.saveAll(toCreate);
        }

        return toCreate.size();
    }

    private int ensureAdminUserAssignments() {
        AdminRole adminRole = adminRoleRepository.findByCodeIgnoreCase("ADMIN").orElse(null);
        if (adminRole == null) {
            return 0;
        }

        Set<String> existingAssignments = adminUserRoleRepository.findAll().stream()
                .map(assignment -> assignment.getUser().getId() + "|" + assignment.getRole().getId())
                .collect(Collectors.toCollection(LinkedHashSet::new));

        List<AdminUserRole> toCreate = new ArrayList<>();
        for (UserAccount adminUser : userAccountRepository.findByRole(RoleType.ADMIN)) {
            String key = adminUser.getId() + "|" + adminRole.getId();
            if (existingAssignments.contains(key)) {
                continue;
            }
            AdminUserRole assignment = new AdminUserRole();
            assignment.setId(new AdminUserRoleId(adminUser.getId(), adminRole.getId()));
            assignment.setUser(adminUser);
            assignment.setRole(adminRole);
            toCreate.add(assignment);
            existingAssignments.add(key);
        }

        if (!toCreate.isEmpty()) {
            adminUserRoleRepository.saveAll(toCreate);
        }
        return toCreate.size();
    }

    private static Set<UUID> resolveRequiredPermissionIds(RoleSeed seed, Map<String, AdminPermission> permissionByKey) {
        if (seed.permissionKeys().contains(ALL_PERMISSIONS)) {
            return permissionByKey.values().stream().map(AdminPermission::getId).collect(Collectors.toCollection(LinkedHashSet::new));
        }

        LinkedHashSet<UUID> ids = new LinkedHashSet<>();
        for (String permissionKey : seed.permissionKeys()) {
            AdminPermission permission = permissionByKey.get(normalizeToken(permissionKey));
            if (permission != null) {
                ids.add(permission.getId());
            }
        }
        return ids;
    }

    private static AdminPermission findPermission(Collection<AdminPermission> permissions, UUID permissionId) {
        for (AdminPermission permission : permissions) {
            if (permission.getId() != null && permission.getId().equals(permissionId)) {
                return permission;
            }
        }
        return null;
    }

    private static PermissionSeed permission(
            String scope,
            String action,
            String label,
            String description,
            String moduleGroup,
            int displayOrder,
            boolean sensitive
    ) {
        return new PermissionSeed(
                normalizeToken(scope),
                normalizeToken(action),
                label,
                description,
                normalizeToken(moduleGroup),
                displayOrder,
                sensitive
        );
    }

    private static RoleSeed role(
            String code,
            String label,
            String description,
            boolean systemRole,
            boolean assignable,
            boolean active,
            Set<String> permissionKeys
    ) {
        return new RoleSeed(
                code.trim().toUpperCase(Locale.ROOT),
                label,
                description,
                systemRole,
                assignable,
                active,
                permissionKeys.stream().map(AdminRbacReferenceDataSeeder::normalizeToken).collect(Collectors.toCollection(LinkedHashSet::new))
        );
    }

    private static Set<String> keys(Set<String>... chunks) {
        LinkedHashSet<String> keys = new LinkedHashSet<>();
        for (Set<String> chunk : chunks) {
            keys.addAll(chunk);
        }
        return keys;
    }

    private static Set<String> module(String scope, String... actions) {
        return Arrays.stream(actions)
                .map(action -> key(scope, action))
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    private static String key(String scope, String action) {
        return normalizeToken(scope) + "." + normalizeToken(action);
    }

    private static String normalizeToken(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }

    private static boolean equalsNullable(String left, String right) {
        if (left == null) {
            return right == null;
        }
        return left.equals(right);
    }

    private record PermissionSeed(
            String scope,
            String action,
            String label,
            String description,
            String moduleGroup,
            int displayOrder,
            boolean sensitive
    ) {
    }

    private record RoleSeed(
            String code,
            String label,
            String description,
            boolean systemRole,
            boolean assignable,
            boolean active,
            Set<String> permissionKeys
    ) {
    }
}
