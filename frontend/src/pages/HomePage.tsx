import { FiArrowRight } from 'react-icons/fi';
import { useNavigate } from 'react-router-dom';
import { ProductGrid } from '../components/product/ProductGrid';
import { CategoryNav } from '../components/sections/CategoryNav';
import { FlashSaleSection } from '../components/sections/FlashSaleSection';
import { MainContentSection } from '../components/sections/MainContentSection';
import { PromoBannerGrid } from '../components/sections/PromoBannerGrid';
import { SearchSection } from '../components/sections/SearchSection';
import { useCart } from '../context/CartContext';
import {
  categoryGroups,
  flashSaleProducts,
  goldenTipsProduct,
  gridProducts,
  guessYouLikeCategories,
  promoPanels,
} from '../data/mockData';
import styles from './HomePage.module.css';

/**
 * Renders the HomePage component.
 *
 * @returns The result of home page.
 */
function HomePage() {
  const navigate = useNavigate();
  const { addToCart } = useCart();

  /**
   * Executes navigate to search.
   *
   * @param query The search query text.
   * @returns The result of navigate to search.
   */
  const navigateToSearch = (query: string) => {
    navigate(`/search?q=${encodeURIComponent(query)}`);
  };

  return (
    <div className={styles.page}>
      <div className={styles.container}>
        <section className={styles.springBanner} aria-label="Seasonal campaign">
          <p>38开门红 超级滋补日 爆款7折起</p>
          <button type="button">
            立即前往 <FiArrowRight aria-hidden="true" />
          </button>
        </section>

        <CategoryNav categories={categoryGroups} onCategorySelect={navigateToSearch} />

        <SearchSection onSearch={navigateToSearch} />

        <PromoBannerGrid panels={promoPanels} />

        <FlashSaleSection products={flashSaleProducts} onAddToCart={addToCart} />

        <MainContentSection
          guessCategories={guessYouLikeCategories}
          goldenTip={goldenTipsProduct}
          onCategorySelect={navigateToSearch}
        />

        <ProductGrid
          title="Recommended Product Grid"
          products={gridProducts}
          onAddToCart={addToCart}
        />
      </div>
    </div>
  );
}

export default HomePage;
