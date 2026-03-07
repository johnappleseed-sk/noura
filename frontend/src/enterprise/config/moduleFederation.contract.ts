export interface RemoteModuleDefinition {
  name: string
  entry: string
  scope: string
  exposedModule: string
}

export const remoteModuleRegistry: RemoteModuleDefinition[] = [
  { name: 'productApp', entry: '/mf/product/remoteEntry.js', scope: 'productApp', exposedModule: './ProductRoutes' },
  { name: 'cartApp', entry: '/mf/cart/remoteEntry.js', scope: 'cartApp', exposedModule: './CartRoutes' },
  { name: 'accountApp', entry: '/mf/account/remoteEntry.js', scope: 'accountApp', exposedModule: './AccountRoutes' },
]

/**
 * Contract for future Webpack Module Federation integration.
 * Keeps routing/actions compatible when this app is consumed as a shell or remote.
 */
export const moduleFederationContractVersion = '1.0.0'
