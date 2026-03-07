import { Product } from '@/types'

/**
 * Determines whether hash seed.
 *
 * @param value The value value.
 * @returns True when the condition is satisfied; otherwise false.
 */
const hashSeed = (value: string): number =>
  value.split('').reduce((acc, char, index) => acc + char.charCodeAt(0) * (index + 11), 17)

const clamp = (value: number, min: number, max: number): number => Math.max(min, Math.min(max, value))

/**
 * Creates build store inventory for product.
 *
 * @param product The product value.
 * @param storeIds The store ids value.
 * @returns The result of build store inventory for product.
 */
export const buildStoreInventoryForProduct = (
  product: Product,
  storeIds: string[],
): Record<string, number> => {
  const existing = product.storeInventory ?? {}
  if (Object.keys(existing).length >= storeIds.length) {
    return existing
  }

  const base = Math.max(0, product.stock)
  return storeIds.reduce<Record<string, number>>((acc, storeId, index) => {
    if (typeof existing[storeId] === 'number') {
      acc[storeId] = existing[storeId]
      return acc
    }

    const seed = hashSeed(`${product.id}-${storeId}-${index}`)
    const ratio = 0.08 + ((seed % 85) / 100)
    const stock = Math.round(base * ratio)
    acc[storeId] = clamp(stock, 0, Math.max(0, base))
    return acc
  }, {})
}

/**
 * Executes ensure store inventory.
 *
 * @param product The product value.
 * @param storeIds The store ids value.
 * @returns The result of ensure store inventory.
 */
export const ensureStoreInventory = (product: Product, storeIds: string[]): Product => ({
  ...product,
  storeInventory: buildStoreInventoryForProduct(product, storeIds),
})

/**
 * Retrieves get product stock at store.
 *
 * @param product The product value.
 * @param storeId The store id used to locate the target record.
 * @returns The result of get product stock at store.
 */
export const getProductStockAtStore = (
  product: Product,
  storeId: string | null | undefined,
): number => {
  if (!storeId) {
    return product.stock
  }
  return product.storeInventory?.[storeId] ?? product.stock
}

export const isProductAvailableAtStore = (
  product: Product,
  storeId: string | null | undefined,
): boolean => getProductStockAtStore(product, storeId) > 0
