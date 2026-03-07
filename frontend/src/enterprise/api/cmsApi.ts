import { productsApi } from '@/api/productsApi'
import { CmsMenuItem } from '@/types'

const toMenuItems = (labels: string[]): CmsMenuItem[] =>
  labels.map((label, index) => ({
    id: `menu-${label.toLowerCase().replace(/\s+/g, '-')}`,
    label,
    path: `/products?category=${encodeURIComponent(label)}`,
    order: index + 1,
  }))

/**
 * Retrieves derive category menu.
 *
 * @returns A list of matching items.
 */
const deriveCategoryMenu = async (): Promise<CmsMenuItem[]> => {
  const products = await productsApi.getProducts()
  const counts = new Map<string, number>()

  for (const product of products) {
    counts.set(product.category, (counts.get(product.category) ?? 0) + 1)
  }

  const topCategories = Array.from(counts.entries())
    .sort((left, right) => right[1] - left[1])
    .map(([category]) => category)
    .slice(0, 6)

  return toMenuItems(topCategories)
}

export const cmsApi = {
  getMenuItems: async (): Promise<CmsMenuItem[]> => {
    try {
      return await deriveCategoryMenu()
    } catch {
      return []
    }
  },
}
