import { FiMinus, FiPlus, FiTrash2 } from 'react-icons/fi';
import { Link } from 'react-router-dom';
import { useCart } from '../context/CartContext';
import styles from './CartPage.module.css';

/**
 * Renders the CartPage component.
 *
 * @returns The result of cart page.
 */
function CartPage() {
  const { items, totalPrice, updateQuantity, removeFromCart, clearCart } = useCart();

  return (
    <div className={styles.page}>
      <div className={styles.container}>
        <header className={styles.header}>
          <h1>Shopping Cart</h1>
          <p>{items.length} product lines</p>
        </header>

        {items.length === 0 ? (
          <section className={styles.emptyState}>
            <h2>Your cart is empty</h2>
            <p>Add products from the homepage or search results.</p>
            <Link to="/" className={styles.backButton}>
              Continue shopping
            </Link>
          </section>
        ) : (
          <div className={styles.layout}>
            <section className={styles.list} aria-label="Cart items">
              {items.map((item) => (
                <article key={item.id} className={styles.item}>
                  <div
                    className={styles.preview}
                    style={{ background: item.imageGradient }}
                    aria-hidden="true"
                  />
                  <div className={styles.details}>
                    <h2>{item.name}</h2>
                    <p>{item.category}</p>
                    <strong>¥{item.price}</strong>
                  </div>

                  <div className={styles.controls}>
                    <button
                      type="button"
                      onClick={() => updateQuantity(item.id, item.quantity - 1)}
                      aria-label={`Decrease quantity for ${item.name}`}
                    >
                      <FiMinus aria-hidden="true" />
                    </button>
                    <span aria-live="polite">{item.quantity}</span>
                    <button
                      type="button"
                      onClick={() => updateQuantity(item.id, item.quantity + 1)}
                      aria-label={`Increase quantity for ${item.name}`}
                    >
                      <FiPlus aria-hidden="true" />
                    </button>
                    <button
                      type="button"
                      className={styles.deleteButton}
                      onClick={() => removeFromCart(item.id)}
                      aria-label={`Remove ${item.name} from cart`}
                    >
                      <FiTrash2 aria-hidden="true" />
                    </button>
                  </div>
                </article>
              ))}
            </section>

            <aside className={styles.summary}>
              <h2>Order Summary</h2>
              <p>Total amount</p>
              <strong>¥{totalPrice}</strong>
              <button type="button" className={styles.checkoutButton}>
                Proceed to checkout
              </button>
              <button type="button" className={styles.clearButton} onClick={clearCart}>
                Clear cart
              </button>
            </aside>
          </div>
        )}
      </div>
    </div>
  );
}

export default CartPage;
