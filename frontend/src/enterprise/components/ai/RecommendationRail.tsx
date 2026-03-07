import { Product } from '@/types'
import { ProductCard } from '@/components/product/ProductCard'

interface RecommendationRailProps {
  title: string
  subtitle: string
  products: Product[]
}

/**
 * Renders the RecommendationRail component.
 *
 * @param param1 The param1 value.
 * @returns The rendered component tree.
 */
export const RecommendationRail = ({ title, subtitle, products }: RecommendationRailProps): JSX.Element | null => {
  if (products.length === 0) {
    return null
  }

  return (
    <section aria-label={title} className="space-y-4">
      <div className="flex flex-col gap-1">
        <h2 className="m3-title">{title}</h2>
        <p className="m3-subtitle">{subtitle}</p>
      </div>
      <div className="grid auto-rows-fr grid-cols-2 gap-4 md:grid-cols-3 xl:grid-cols-6">
        {products.map((product) => (
          <ProductCard key={product.id} product={product} />
        ))}
      </div>
    </section>
  )
}
