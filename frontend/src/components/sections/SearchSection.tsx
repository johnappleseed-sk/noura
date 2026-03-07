import { useEffect, useState, type FormEvent } from 'react';
import { FiSearch } from 'react-icons/fi';
import { trendingTags } from '../../data/mockData';
import styles from './SearchSection.module.css';

interface SearchSectionProps {
  initialQuery?: string;
  onSearch: (query: string) => void;
}

/**
 * Renders the SearchSection component.
 *
 * @param param1 The param1 value.
 * @returns The result of search section.
 */
export function SearchSection({ initialQuery = '', onSearch }: SearchSectionProps) {
  const [query, setQuery] = useState(initialQuery);

  useEffect(() => {
    setQuery(initialQuery);
  }, [initialQuery]);

  /**
   * Executes handle submit.
   *
   * @param event The event value.
   * @returns The result of handle submit.
   */
  function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    onSearch(query);
  }

  return (
    <section className={styles.wrapper} aria-label="Search products">
      <form className={styles.form} onSubmit={handleSubmit}>
        <div className={styles.inputWrap}>
          <FiSearch aria-hidden="true" />
          <input
            value={query}
            onChange={(event) => setQuery(event.target.value)}
            placeholder="Search for the same style"
            aria-label="Search for products"
          />
        </div>
        <button type="submit" className={styles.searchButton}>
          Search
        </button>
      </form>

      <div className={styles.related}>
        <p className={styles.reference}>
          Tmall | long handle floor brush
        </p>
        <ul className={styles.tagList}>
          {trendingTags.map((tag) => (
            <li key={tag}>
              <button
                type="button"
                onClick={() => {
                  setQuery(tag);
                  onSearch(tag);
                }}
              >
                {tag}
              </button>
            </li>
          ))}
        </ul>
      </div>
    </section>
  );
}
