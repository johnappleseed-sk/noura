import { FiChevronDown, FiShoppingCart, FiUser } from 'react-icons/fi';
import { Link, NavLink } from 'react-router-dom';
import { headerLinks } from '../../data/mockData';
import { useCart } from '../../context/CartContext';
import styles from './Header.module.css';

const utilityActions = ['Register', 'Store setup', 'Login'];

/**
 * Renders the Header component.
 *
 * @returns The result of header.
 */
export function Header() {
  const { itemCount } = useCart();

  return (
    <header className={styles.header}>
      <div className={styles.utilityStrip}>
        <div className={styles.container}>
          <p className={styles.region} aria-label="Current market region">
            China Mainland <FiChevronDown aria-hidden="true" />
          </p>
          <div className={styles.utilityRight}>
            <button type="button" className={styles.utilityButton}>
              Language <FiChevronDown aria-hidden="true" />
            </button>
            <Link to="/cart" className={styles.utilityCart}>
              <FiShoppingCart aria-hidden="true" />
              Cart ({itemCount})
            </Link>
          </div>
        </div>
      </div>

      <div className={styles.mainBar}>
        <div className={styles.container}>
          <Link className={styles.logo} to="/" aria-label="noura homepage">
            noura
          </Link>

          <nav className={styles.nav} aria-label="Primary navigation">
            {headerLinks.map((link) => (
              <NavLink
                key={link.id}
                to={link.path}
                className={({ isActive }) =>
                  isActive ? `${styles.navLink} ${styles.navLinkActive}` : styles.navLink
                }
              >
                {link.label}
              </NavLink>
            ))}
          </nav>

          <div className={styles.actions} aria-label="User actions">
            {utilityActions.map((action) => (
              <button key={action} type="button" className={styles.actionButton}>
                <FiUser aria-hidden="true" />
                {action}
              </button>
            ))}
            <Link
              to="/cart"
              className={styles.cartButton}
              aria-label={`Shopping cart with ${itemCount} items`}
            >
              <FiShoppingCart aria-hidden="true" />
              {itemCount}
            </Link>
          </div>
        </div>
      </div>
    </header>
  );
}
