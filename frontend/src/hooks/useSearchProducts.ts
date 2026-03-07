import { useEffect, useMemo, useState } from 'react';
import { allProducts } from '../data/mockData';
import type { Product } from '../types';

interface SearchState {
  results: Product[];
  loading: boolean;
  error: string | null;
}

/**
 * Executes use search products.
 *
 * @param query The search query text.
 * @returns The result of use search products.
 */
export function useSearchProducts(query: string): SearchState {
  const [results, setResults] = useState<Product[]>(allProducts);
  const [error, setError] = useState<string | null>(null);
  const [completedQuery, setCompletedQuery] = useState('');

  const normalizedQuery = useMemo(() => query.trim().toLowerCase(), [query]);
  const loading = normalizedQuery !== completedQuery;

  useEffect(() => {
    let active = true;

    const timer = window.setTimeout(() => {
      if (!active) {
        return;
      }

      if (normalizedQuery === 'trigger-error') {
        setError('Search service is temporarily unavailable. Please try again.');
        setResults([]);
        setCompletedQuery(normalizedQuery);
        return;
      }

      if (!normalizedQuery) {
        setError(null);
        setResults(allProducts);
        setCompletedQuery(normalizedQuery);
        return;
      }

      const filtered = allProducts.filter((product) => {
        const searchableContent = [
          product.name,
          product.category,
          ...product.tags,
          product.description ?? '',
        ]
          .join(' ')
          .toLowerCase();

        return searchableContent.includes(normalizedQuery);
      });

      setError(null);
      setResults(filtered);
      setCompletedQuery(normalizedQuery);
    }, 450);

    return () => {
      active = false;
      window.clearTimeout(timer);
    };
  }, [normalizedQuery]);

  const visibleError = completedQuery === normalizedQuery ? error : null;

  return { results, loading, error: visibleError };
}
