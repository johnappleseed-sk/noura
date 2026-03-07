import { FiArrowRight, FiZap } from 'react-icons/fi';
import styles from './MainContentSection.module.css';

interface MainContentSectionProps {
  guessCategories: string[];
  goldenTip: string;
  onCategorySelect?: (query: string) => void;
}

/**
 * Creates a new MainContentSection instance.
 */
export function MainContentSection({
  guessCategories,
  goldenTip,
  onCategorySelect,
}: MainContentSectionProps) {
  return (
    <section className={styles.wrapper} aria-label="Main recommendations">
      <article className={styles.card}>
        <header className={styles.header}>
          <h3>Guessing you like</h3>
          <span>AI preference engine</span>
        </header>

        <ul className={styles.categoryList}>
          {guessCategories.map((category) => (
            <li key={category}>
              <button type="button" onClick={() => onCategorySelect?.(category)}>
                {category}
              </button>
            </li>
          ))}
        </ul>
      </article>

      <article className={styles.card}>
        <header className={styles.header}>
          <h3>Four golden tips</h3>
          <span>
            <FiZap aria-hidden="true" /> Value optimized
          </span>
        </header>
        <p className={styles.tipText}>{goldenTip}</p>
      </article>

      <article className={styles.card}>
        <header className={styles.header}>
          <h3>80+ 包</h3>
          <a href="#" className={styles.moreLink}>
            More low-priced goods
            <FiArrowRight aria-hidden="true" />
          </a>
        </header>
        <p className={styles.tipText}>
          Curated multi-pack daily supplies with high sell-through and
          consistent price protection.
        </p>
      </article>
    </section>
  );
}
