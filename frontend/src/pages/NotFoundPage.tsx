import { Link } from 'react-router-dom';
import styles from './NotFoundPage.module.css';

/**
 * Renders the NotFoundPage component.
 *
 * @returns The result of not found page.
 */
function NotFoundPage() {
  return (
    <div className={styles.page}>
      <section className={styles.card}>
        <p>404</p>
        <h1>Page not found</h1>
        <Link to="/">Return to homepage</Link>
      </section>
    </div>
  );
}

export default NotFoundPage;
