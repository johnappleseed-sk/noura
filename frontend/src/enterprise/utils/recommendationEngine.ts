import { Product } from '@/types'

interface RecommendationOptions {
  activeProductId?: string
  limit?: number
}

/**
 * Executes rank recommendations.
 *
 * @param products The products value.
 * @param browsingHistory The browsing history value.
 * @param options Additional options that customize this operation.
 * @returns A list of matching items.
 */
export const rankRecommendations = (
  products: Product[],
  browsingHistory: string[],
  options: RecommendationOptions = {},
): Product[] => {
  if (products.length === 0) {
    return []
  }

  const { activeProductId, limit = 6 } = options
  const historySet = new Set(browsingHistory)
  const historyProducts = products.filter((product) => historySet.has(product.id))
  const categoryWeights = historyProducts.reduce<Record<string, number>>((acc, product) => {
    acc[product.category] = (acc[product.category] ?? 0) + 2
    return acc
  }, {})
  const brandWeights = historyProducts.reduce<Record<string, number>>((acc, product) => {
    acc[product.brand] = (acc[product.brand] ?? 0) + 1
    return acc
  }, {})

  return products
    .filter((product) => product.id !== activeProductId)
    .map((product) => {
      const popularityScore = product.popularity / 100
      const ratingScore = product.rating / 5
      const categoryScore = categoryWeights[product.category] ?? 0
      const brandScore = brandWeights[product.brand] ?? 0
      const recencyScore = (Date.now() - new Date(product.createdAt).getTime()) / (1000 * 60 * 60 * 24)
      const freshness = Math.max(0, 1 - recencyScore / 120)
      const score = popularityScore + ratingScore + categoryScore + brandScore + freshness

      return { product, score }
    })
    .sort((a, b) => b.score - a.score)
    .slice(0, limit)
    .map((entry) => entry.product)
}

/**
 * Retrieves get related products.
 *
 * @param products The products value.
 * @param activeProduct The active product value.
 * @param limit The limit value.
 * @returns A list of matching items.
 */
export const getRelatedProducts = (products: Product[], activeProduct: Product, limit = 4): Product[] => {
  const tagSet = new Set(activeProduct.tags)
  return products
    .filter((product) => product.id !== activeProduct.id)
    .map((product) => {
      const sharedTags = product.tags.filter((tag) => tagSet.has(tag)).length
      const sameCategory = product.category === activeProduct.category ? 3 : 0
      const sameBrand = product.brand === activeProduct.brand ? 1 : 0
      const score = sharedTags * 2 + sameCategory + sameBrand + product.popularity / 100
      return { product, score }
    })
    .sort((a, b) => b.score - a.score)
    .slice(0, limit)
    .map((entry) => entry.product)
}
