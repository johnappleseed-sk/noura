export const NAV_SECTIONS = [
  {
    label: 'Overview',
    items: [
      { label: 'Dashboard', to: '/admin', end: true, icon: 'home' },
      { label: 'Analytics', to: '/admin/analytics', icon: 'chart' }
    ]
  },
  {
    label: 'Commerce',
    items: [
      { label: 'Catalog', to: '/admin/commerce/catalog', icon: 'box' },
      { label: 'Orders', to: '/admin/orders', icon: 'bag' },
      { label: 'Returns', to: '/admin/returns', icon: 'bag' },
      { label: 'Stores', to: '/admin/stores', icon: 'warehouse' },
      { label: 'Pricing', to: '/admin/pricing', icon: 'chart' },
      { label: 'Users', to: '/admin/users', icon: 'users' },
      { label: 'Notifications', to: '/admin/notifications', icon: 'bell' }
    ]
  },
  {
    label: 'Warehouse',
    items: [
      { label: 'Inventory catalog', to: '/admin/warehouse/catalog', icon: 'box' },
      { label: 'Locations', to: '/admin/warehouse/locations', icon: 'warehouse' },
      { label: 'Stock', to: '/admin/warehouse/stock', icon: 'warehouse' },
      { label: 'Movements', to: '/admin/warehouse/movements', icon: 'chart' },
      { label: 'Batches', to: '/admin/warehouse/batches', icon: 'box' },
      { label: 'Serials', to: '/admin/warehouse/serials', icon: 'box' },
      { label: 'Reports', to: '/admin/warehouse/reports', icon: 'chart' }
    ]
  },
  {
    label: 'Tools',
    items: [{ label: 'Control center', to: '/admin/tools/control-center', icon: 'chart' }]
  }
]

export const ADMIN_ONLY_ITEMS = [
  { label: 'Webhooks', to: '/admin/warehouse/webhooks', icon: 'chart' },
  { label: 'Audit logs', to: '/admin/warehouse/audit-logs', icon: 'chart' }
]

