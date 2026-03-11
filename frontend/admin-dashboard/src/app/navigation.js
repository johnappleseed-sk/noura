import { CAPABILITIES } from '../shared/auth/roles'

export const NAV_SECTIONS = [
  {
    label: 'Overview',
    items: [
      { label: 'Dashboard', to: '/admin', end: true, icon: 'home', capability: CAPABILITIES.OVERVIEW_DASHBOARD },
      { label: 'Analytics', to: '/admin/analytics', icon: 'chart', capability: CAPABILITIES.OVERVIEW_ANALYTICS }
    ]
  },
  {
    label: 'Commerce',
    items: [
      { label: 'Catalog', to: '/admin/commerce/catalog', icon: 'box', capability: CAPABILITIES.COMMERCE_CATALOG },
      { label: 'Carousels', to: '/admin/commerce/carousels', icon: 'chart', capability: CAPABILITIES.COMMERCE_CAROUSELS },
      { label: 'Recommendations', to: '/admin/commerce/recommendations', icon: 'chart', capability: CAPABILITIES.COMMERCE_RECOMMENDATIONS },
      { label: 'Merchandising', to: '/admin/commerce/merchandising', icon: 'chart', capability: CAPABILITIES.COMMERCE_MERCHANDISING },
      { label: 'Orders', to: '/admin/orders', icon: 'bag', capability: CAPABILITIES.COMMERCE_ORDERS },
      { label: 'Returns', to: '/admin/returns', icon: 'bag', capability: CAPABILITIES.COMMERCE_RETURNS },
      { label: 'Stores', to: '/admin/stores', icon: 'warehouse', capability: CAPABILITIES.COMMERCE_STORES },
      { label: 'Pricing', to: '/admin/pricing', icon: 'chart', capability: CAPABILITIES.COMMERCE_PRICING },
      { label: 'Users', to: '/admin/users', icon: 'users', capability: CAPABILITIES.COMMERCE_USERS },
      { label: 'Notifications', to: '/admin/notifications', icon: 'bell', capability: CAPABILITIES.COMMERCE_NOTIFICATIONS }
    ]
  },
  {
    label: 'Warehouse',
    items: [
      { label: 'Inventory catalog', to: '/admin/warehouse/catalog', icon: 'box', capability: CAPABILITIES.WAREHOUSE_CATALOG },
      { label: 'Locations', to: '/admin/warehouse/locations', icon: 'warehouse', capability: CAPABILITIES.WAREHOUSE_LOCATIONS },
      { label: 'Stock', to: '/admin/warehouse/stock', icon: 'warehouse', capability: CAPABILITIES.WAREHOUSE_STOCK },
      { label: 'Movements', to: '/admin/warehouse/movements', icon: 'chart', capability: CAPABILITIES.WAREHOUSE_MOVEMENTS },
      { label: 'Batches', to: '/admin/warehouse/batches', icon: 'box', capability: CAPABILITIES.WAREHOUSE_BATCHES },
      { label: 'Serials', to: '/admin/warehouse/serials', icon: 'box', capability: CAPABILITIES.WAREHOUSE_SERIALS },
      { label: 'Reports', to: '/admin/warehouse/reports', icon: 'chart', capability: CAPABILITIES.WAREHOUSE_REPORTS }
    ]
  },
  {
    label: 'Tools',
    items: [
      { label: 'Control center', to: '/admin/tools/control-center', icon: 'chart', capability: CAPABILITIES.TOOLS_CONTROL_CENTER },
      { label: 'Product generator', to: '/admin/tools/product-generator', icon: 'box', capability: CAPABILITIES.TOOLS_PRODUCT_GENERATOR }
    ]
  }
]

export const ADMIN_ONLY_ITEMS = [
  { label: 'Webhooks', to: '/admin/warehouse/webhooks', icon: 'chart', capability: CAPABILITIES.WAREHOUSE_WEBHOOKS },
  { label: 'Audit logs', to: '/admin/warehouse/audit-logs', icon: 'chart', capability: CAPABILITIES.WAREHOUSE_AUDIT_LOGS }
]
