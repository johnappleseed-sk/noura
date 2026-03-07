// src/components/layout/Footer.tsx
import { footerLinks } from '@/data/footerLinks';
import { Link } from 'react-router-dom'; // or your router's Link
import styles from './Footer.module.css';

/**
 * Renders the Footer component.
 *
 * @returns The result of footer.
 */
export function Footer() {
  const currentYear = new Date().getFullYear();

  return (
    <footer className={styles.footer}>
      <div className={styles.container}>
        <nav aria-label="Footer quick links" className={styles.links}>
          {footerLinks.map(({ label, href, isExternal }) =>
            isExternal ? (
              <a
                key={label}
                href={href}
                className={styles.link}
                target="_blank"
                rel="noopener noreferrer"
              >
                {label}
              </a>
            ) : (
              <Link key={label} to={href} className={styles.link}>
                {label}
              </Link>
            )
          )}
        </nav>
        <p className={styles.copy}>
          © {currentYear} noura commerce front-end prototype
        </p>
      </div>
    </footer>
  );
}