import { FiChevronRight } from 'react-icons/fi';
import styles from './CategoryNav.module.css';
import type { CategoryGroup } from '../../types';

interface CategoryNavProps {
  categories: CategoryGroup[];
  onCategorySelect?: (keyword: string) => void;
}

/**
 * Renders the CategoryNav component.
 *
 * @param param1 The param1 value.
 * @returns The result of category nav.
 */
export function CategoryNav({ categories, onCategorySelect }: CategoryNavProps) {
  return (
    <section className={styles.wrapper} aria-label="Category navigation">
      <ul className={styles.list}>
        {categories.map((category) => (
          <li key={category.id} className={styles.item}>
            <button
              type="button"
              className={styles.button}
              onClick={() => onCategorySelect?.(category.keywords[0])}
              aria-label={`Browse category ${category.label}`}
            >
              <span>{category.label}</span>
              <FiChevronRight aria-hidden="true" />
            </button>
          </li>
        ))}
      </ul>
    </section>
  );
}
