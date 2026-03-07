import { Product } from '@/types'

/**
 * Retrieves get predictive suggestions.
 *
 * @param products The products value.
 * @param query The search query text.
 * @param limit The limit value.
 * @returns A list of matching items.
 */
export const getPredictiveSuggestions = (products: Product[], query: string, limit = 6): string[] => {
  const normalized = query.trim().toLowerCase()

  if (!normalized) {
    return []
  }

  const phrases = new Set<string>()

  for (const product of products) {
    const tokens = [product.name, product.category, product.brand, ...product.tags]
    for (const token of tokens) {
      if (token.toLowerCase().includes(normalized)) {
        phrases.add(token)
      }
      if (phrases.size >= limit * 2) {
        break
      }
    }
  }

  return Array.from(phrases).slice(0, limit)
}
