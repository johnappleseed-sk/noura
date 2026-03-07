import { FiPlus, FiShoppingCart } from 'react-icons/fi';
import type { Product } from '../../types';
import styles from './ProductCard.module.css';

interface ProductCardProps {
  product: Product;
  onAddToCart: (product: Product) => void;
  compact?: boolean;
}

/**
 * Creates a new ProductCard instance.
 */
export function ProductCard({
  product,
  onAddToCart,
  compact = false,
}: ProductCardProps) {
  return (
    <article className={`${styles.card} ${compact ? styles.compact : ''}`}>
      <div
        className={styles.media}
        style={{ background: product.imageGradient }}
        aria-label={product.imageLabel}
      >
        <span>{product.imageLabel}</span>
      </div>

      <div className={styles.content}>
        {product.badge ? <span className={styles.badge}>{product.badge}</span> : null}
        <h3 className={styles.title}>{product.name}</h3>
        <p className={styles.category}>{product.category}</p>
        <div className={styles.priceRow}>
          <strong className={styles.price}>¥{product.price}</strong>
          {product.originalPrice ? (
            <span className={styles.original}>¥{product.originalPrice}</span>
          ) : null}
        </div>
        <button
          type="button"
          className={styles.cartButton}
          onClick={() => onAddToCart(product)}
          aria-label={`Add ${product.name} to cart`}
        >
          {compact ? <FiPlus aria-hidden="true" /> : <FiShoppingCart aria-hidden="true" />}
          Add to cart
        </button>
      </div>
    </article>
  );
}
