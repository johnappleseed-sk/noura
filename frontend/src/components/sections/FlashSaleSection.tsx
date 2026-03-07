import type { Product } from '../../types';
import { ProductCard } from '../product/ProductCard';
import styles from './FlashSaleSection.module.css';

interface FlashSaleSectionProps {
  products: Product[];
  onAddToCart: (product: Product) => void;
}

/**
 * Creates a new FlashSaleSection instance.
 */
export function FlashSaleSection({
  products,
  onAddToCart,
}: FlashSaleSectionProps) {
  return (
    <section className={styles.section} aria-label="Taobao Flash Sale">
      <header className={styles.header}>
        <h2>Taobao Flash Sale</h2>
        <span>Updated every 30 minutes</span>
      </header>
      <div className={styles.grid}>
        {products.map((product) => (
          <ProductCard
            key={product.id}
            product={product}
            onAddToCart={onAddToCart}
            compact
          />
        ))}
      </div>
    </section>
  );
}
