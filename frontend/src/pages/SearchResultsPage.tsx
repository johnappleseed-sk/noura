import { useNavigate, useSearchParams } from 'react-router-dom';
import { LoadingSpinner } from '../components/common/LoadingSpinner';
import { ProductCard } from '../components/product/ProductCard';
import { SearchSection } from '../components/sections/SearchSection';
import { useCart } from '../context/CartContext';
import { useSearchProducts } from '../hooks/useSearchProducts';
import styles from './SearchResultsPage.module.css';

/**
 * Renders the SearchResultsPage component.
 *
 * @returns The result of search results page.
 */
function SearchResultsPage() {
  const [searchParams] = useSearchParams();
  const query = searchParams.get('q') ?? '';
  const navigate = useNavigate();
  const { addToCart } = useCart();
  const { results, loading, error } = useSearchProducts(query);

  /**
   * Executes handle search.
   *
   * @param nextQuery The search query text.
   * @returns The result of handle search.
   */
  const handleSearch = (nextQuery: string) => {
    navigate(`/search?q=${encodeURIComponent(nextQuery)}`);
  };

  return (
    <div className={styles.page}>
      <div className={styles.container}>
        <header className={styles.header}>
          <h1>Search Results</h1>
          <p>
            {query
              ? `Query: "${query}"`
              : 'Showing all available products in the mock catalog'}
          </p>
        </header>

        <SearchSection initialQuery={query} onSearch={handleSearch} />

        {loading ? <LoadingSpinner label="Searching products..." /> : null}

        {error ? (
          <section className={styles.stateCard} role="alert">
            <h2>Search unavailable</h2>
            <p>{error}</p>
          </section>
        ) : null}

        {!loading && !error && results.length === 0 ? (
          <section className={styles.stateCard}>
            <h2>No products found</h2>
            <p>Try a different keyword or use one of the suggested tags above.</p>
          </section>
        ) : null}

        {!loading && !error && results.length > 0 ? (
          <section className={styles.results}>
            {results.map((product) => (
              <ProductCard
                key={product.id}
                product={product}
                onAddToCart={addToCart}
              />
            ))}
          </section>
        ) : null}
      </div>
    </div>
  );
}

export default SearchResultsPage;
