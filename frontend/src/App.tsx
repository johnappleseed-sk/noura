import { Suspense, lazy } from 'react';
import { Navigate, Route, Routes } from 'react-router-dom';
import { LoadingSpinner } from './components/common/LoadingSpinner';
import { Footer } from './components/layout/Footer';
import { Header } from './components/layout/Header';
import styles from './App.module.css';

const HomePage = lazy(() => import('./pages/HomePage'));
const SearchResultsPage = lazy(() => import('./pages/SearchResultsPage'));
const CartPage = lazy(() => import('./pages/CartPage'));
const NotFoundPage = lazy(() => import('./pages/NotFoundPage'));

/**
 * Renders the App component.
 *
 * @returns The result of app.
 */
function App() {
  return (
    <div className={styles.appShell}>
      <Header />
      <main className={styles.mainArea} aria-live="polite">
        <div className={styles.backgroundGlow} aria-hidden="true" />
        <div className={styles.contentLayer}>
          <Suspense fallback={<LoadingSpinner label="Loading storefront..." />}>
            <Routes>
              <Route path="/" element={<HomePage />} />
              <Route path="/search" element={<SearchResultsPage />} />
              <Route path="/cart" element={<CartPage />} />
              <Route path="/home" element={<Navigate to="/" replace />} />
              <Route path="*" element={<NotFoundPage />} />
            </Routes>
          </Suspense>
        </div>
      </main>
      <Footer />
    </div>
  );
}

export default App;
