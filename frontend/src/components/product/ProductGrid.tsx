import type { Product } from '../../types';
import { ProductCard } from './ProductCard';
import styles from './ProductGrid.module.css';

interface ProductGridProps {
  title: string;
  products: Product[];
  onAddToCart: (product: Product) => void;
}

/**
 * Renders the ProductGrid component.
 *
 * @param param1 The param1 value.
 * @returns The result of product grid.
 */
export function ProductGrid({ title, products, onAddToCart }: ProductGridProps) {
  return (
    <section className={styles.section} aria-label={title}>
      <header className={styles.header}>
        <h2>{title}</h2>
        <span>{products.length} products</span>
      </header>
      <div className={styles.grid}>
        {products.map((product) => (
          <ProductCard key={product.id} product={product} onAddToCart={onAddToCart} />
        ))}
      </div>
    </section>
  );
}
