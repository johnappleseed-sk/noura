import { useCallback, useEffect, useMemo, useRef, useState } from 'react';
import {
  createCommerceCategory,
  getCommerceCategoryTree,
  updateCommerceCategory,
} from '../shared/api/endpoints/commerceCategoriesApi';
import {
  addCommerceVariant,
  createCommerceProduct,
  deleteCommerceProduct,
  getCommerceProduct,
  listCommerceProducts,
  patchCommerceProduct,
  updateCommerceProduct,
  updateCommerceVariant,
  upsertCommerceStoreInventory,
} from '../shared/api/endpoints/commerceProductsApi';
import { listStores } from '../shared/api/endpoints/storesApi';
import { useConfirmDialog } from '../shared/ui/ConfirmDialogProvider';
import { Spinner } from '../shared/ui/Spinner';
import { SortableHeader } from '../shared/ui/SortableHeader';
import { formatCurrency } from '../shared/ui/formatters';
import { useToastFeedback } from '../shared/ui/useToastFeedback';
import { ProductMediaUploader } from '../features/catalog/ProductMediaUploader';
import { generateProduct } from '../shared/api/endpoints/productGeneratorApi';

/* --------------------------------------------------------------------------
   Constants & Helper Functions (unchanged, kept outside)
   -------------------------------------------------------------------------- */
const DEFAULT_CATEGORY_FORM = {
  parentId: '',
  name: '',
  description: '',
  classificationCode: '',
  managerId: '',
};

const DEFAULT_PRODUCT_FORM = {
  name: '',
  categoryId: '',
  category: '',
  brand: '',
  price: '',
  allowBackorder: false,
  flashSale: false,
  trending: false,
  bestSeller: false,
  shortDescription: '',
  longDescription: '',
  seoSlug: '',
  seoTitle: '',
  seoDescription: '',
  attributesJson: '{}',
};

const DEFAULT_VARIANT_FORM = {
  sku: '',
  color: '',
  size: '',
  price: '',
  stock: '0',
};

const DEFAULT_STORE_INV_FORM = {
  storeId: '',
  stock: '0',
  storePrice: '',
};

const DEFAULT_AI_CONTENT_PREVIEW = {
  description: '',
  shortDescription: '',
  bulletFeaturesText: '',
  seoTitle: '',
  seoDescription: '',
  tagsText: '',
};

const DEFAULT_AI_INSERT_OPTIONS = {
  overwriteDescription: false,
  overwriteSeo: false,
  overwriteFeatures: false,
};

function flattenTree(nodes = [], depth = 0, parentId = null) {
  return nodes.flatMap((node) => [
    { ...node, depth, parentId },
    ...flattenTree(node.children || [], depth + 1, node.id),
  ]);
}

function parseJson(text) {
  const trimmed = String(text || '').trim();
  if (!trimmed) return undefined;
  try {
    return JSON.parse(trimmed);
  } catch (_) {
    throw new Error('Attributes must be valid JSON.');
  }
}

function asNumber(value, fieldLabel) {
  if (value === '' || value === null || value === undefined) return null;
  const num = Number(value);
  if (Number.isNaN(num)) {
    throw new Error(`${fieldLabel} must be a number.`);
  }
  return num;
}

function asInt(value, fieldLabel) {
  const num = asNumber(value, fieldLabel);
  if (num === null) return null;
  return Math.trunc(num);
}

function truncateText(value, limit) {
  const text = String(value || '').trim();
  if (!text) return '';
  return text.length <= limit ? text : `${text.slice(0, Math.max(0, limit - 1)).trim()}…`;
}

function extractOverview(description) {
  const lines = String(description || '')
    .split('\n')
    .map((line) => line.trim())
    .filter(Boolean);
  if (!lines.length) return '';
  const overviewLine = lines.find((line) => line.toUpperCase().startsWith('OVERVIEW:'));
  if (overviewLine) {
    return overviewLine.replace(/^overview:\s*/i, '').trim();
  }
  return lines[0];
}

function extractBulletFeatures(description) {
  const lines = String(description || '')
    .split('\n')
    .map((line) => line.trim());
  const bullets = lines
    .filter((line) => line.startsWith('•') || line.startsWith('- '))
    .map((line) => line.replace(/^[•-]\s*/, '').trim())
    .filter(Boolean);
  if (bullets.length) {
    return bullets.slice(0, 8);
  }
  const fallback = String(description || '')
    .split(/[.?!]/)
    .map((part) => part.trim())
    .filter((part) => part.length > 20)
    .slice(0, 5);
  return fallback;
}

function parseTextList(value) {
  return String(value || '')
    .split('\n')
    .map((line) => line.trim())
    .filter(Boolean);
}

function parseTagList(value) {
  return String(value || '')
    .split(/[,\n]/)
    .map((tag) => tag.trim().toLowerCase())
    .filter(Boolean)
    .filter((tag, idx, list) => list.indexOf(tag) === idx);
}

function inferTags({ name, category, brand, targetAudience, features }) {
  const candidates = [
    name,
    category,
    brand,
    targetAudience,
    ...(features || []),
  ]
    .map((value) => String(value || '').trim())
    .filter(Boolean)
    .flatMap((value) => value.split(/[\s,/|]+/))
    .map((token) => token.trim().toLowerCase())
    .filter((token) => token.length >= 3 && token.length <= 24);
  return candidates.filter((token, idx) => candidates.indexOf(token) === idx).slice(0, 10);
}

/* --------------------------------------------------------------------------
   Custom Hook: useSafeAsync
   Provides a safe way to perform async operations with abort and loading/error states.
   (Encapsulates the pattern used in the original component)
   -------------------------------------------------------------------------- */
function useSafeAsync(initialLoading = false) {
  const [loading, setLoading] = useState(initialLoading);
  const [error, setError] = useState('');
  const [flash, setFlash] = useState('');
  const abortControllerRef = useRef(null);

  const safeExecute = useCallback(async (asyncFn, onSuccess, onError) => {
    // Abort any pending request
    if (abortControllerRef.current) {
      abortControllerRef.current.abort();
    }
    const controller = new AbortController();
    abortControllerRef.current = controller;

    setLoading(true);
    setError('');
    setFlash('');
    try {
      const result = await asyncFn({ signal: controller.signal });
      if (!controller.signal.aborted) {
        onSuccess?.(result);
      }
      return result;
    } catch (err) {
      if (!controller.signal.aborted) {
        const message = err.message || 'An unexpected error occurred.';
        setError(message);
        onError?.(err);
      }
    } finally {
      if (!controller.signal.aborted) {
        setLoading(false);
      }
    }
  }, []);

  // Cleanup on unmount
  useEffect(() => {
    return () => abortControllerRef.current?.abort();
  }, []);

  return { loading, setLoading, error, setError, flash, setFlash, safeExecute };
}

/* --------------------------------------------------------------------------
   Subcomponents
   -------------------------------------------------------------------------- */

/**
 * Sidebar for category management.
 */
function CategorySidebar({
  categoryTree,
  selectedCategoryId,
  categoryForm,
  saving,
  flatCategories,
  onSelectCategory,
  onCategoryFormChange,
  onSaveCategory,
  onResetCategory,
  onRefresh,
}) {
  return (
    <section className="panel catalog-sidebar">
      <div className="section-head">
        <div>
          <h3>Categories</h3>
          <p>Create and update platform category taxonomy (max depth enforced server-side).</p>
        </div>
        <button className="btn btn-outline btn-sm" onClick={onRefresh} disabled={saving}>
          Refresh
        </button>
      </div>

      <div className="filters">
        <label htmlFor="categorySelect">Select category</label>
        <select
          id="categorySelect"
          value={selectedCategoryId}
          onChange={(e) => onSelectCategory(e.target.value)}
        >
          <option value="">New category...</option>
          {flatCategories.map((category) => (
            <option key={category.id} value={category.id}>
              {`${'  '.repeat(category.depth)}${category.name}`}
            </option>
          ))}
        </select>
      </div>

      <div className="form-grid">
        <label htmlFor="categoryName">
          Name *
          <input
            id="categoryName"
            value={categoryForm.name}
            onChange={(e) => onCategoryFormChange('name', e.target.value)}
            placeholder="Electronics"
            required
          />
        </label>
        <label htmlFor="categoryParent">
          Parent
          <select
            id="categoryParent"
            value={categoryForm.parentId}
            onChange={(e) => onCategoryFormChange('parentId', e.target.value)}
          >
            <option value="">(Root)</option>
            {flatCategories
              .filter((cat) => String(cat.id) !== String(selectedCategoryId))
              .map((cat) => (
                <option key={cat.id} value={cat.id}>
                  {`${'  '.repeat(cat.depth)}${cat.name}`}
                </option>
              ))}
          </select>
        </label>
        <label className="span-2" htmlFor="categoryDescription">
          Description
          <input
            id="categoryDescription"
            value={categoryForm.description}
            onChange={(e) => onCategoryFormChange('description', e.target.value)}
            placeholder="Optional description"
          />
        </label>
        <label htmlFor="categoryClassCode">
          Classification code
          <input
            id="categoryClassCode"
            value={categoryForm.classificationCode}
            onChange={(e) => onCategoryFormChange('classificationCode', e.target.value)}
            placeholder="Optional"
          />
        </label>
        <label htmlFor="categoryManagerId">
          Manager id
          <input
            id="categoryManagerId"
            value={categoryForm.managerId}
            onChange={(e) => onCategoryFormChange('managerId', e.target.value)}
            placeholder="Optional UUID"
          />
        </label>
      </div>

      <div className="inline-actions">
        <button
          className="btn btn-primary"
          disabled={saving || !categoryForm.name.trim()}
          onClick={onSaveCategory}
        >
          {saving ? 'Saving...' : selectedCategoryId ? 'Update category' : 'Create category'}
        </button>
        <button className="btn btn-outline" disabled={saving} onClick={onResetCategory}>
          Reset
        </button>
      </div>
    </section>
  );
}

/**
 * Product listing table with filters.
 */
function ProductTable({
  productsPage,
  productFilters,
  showAdvancedFilters,
  flatCategories,
  stores,
  productSort,
  selectedProductId,
  saving,
  onFilterChange,
  onApplyFilters,
  onToggleAdvancedFilters,
  onSort,
  onSelectProduct,
  onDeleteProduct,
}) {
  return (
    <section className="panel catalog-main">
      <div className="section-head">
        <div>
          <h3>Products</h3>
          <p>{productsPage.totalElements || productsPage.content.length} products in catalog.</p>
        </div>
        <button className="btn btn-primary btn-sm" onClick={() => onSelectProduct('')}>
          New product
        </button>
      </div>

      <div className="catalog-toolbar">
        <label className="visually-hidden" htmlFor="productSearch">
          Search products
        </label>
        <input
          id="productSearch"
          className="toolbar-control toolbar-search"
          value={productFilters.query}
          onChange={(e) => onFilterChange('query', e.target.value)}
          placeholder="Name, description, slug..."
        />
        <label className="visually-hidden" htmlFor="productCategoryFilter">
          Category filter
        </label>
        <select
          id="productCategoryFilter"
          className="toolbar-control toolbar-select"
          value={productFilters.categoryId}
          onChange={(e) => onFilterChange('categoryId', e.target.value)}
        >
          <option value="">All</option>
          {flatCategories.map((cat) => (
            <option key={cat.id} value={cat.id}>
              {`${'  '.repeat(cat.depth)}${cat.name}`}
            </option>
          ))}
        </select>
        <button className="btn btn-outline toolbar-btn" onClick={onApplyFilters} disabled={saving}>
          Apply
        </button>
        <button className="btn btn-outline toolbar-btn" type="button" onClick={onToggleAdvancedFilters}>
          {showAdvancedFilters ? 'Hide filters' : 'More filters'}
        </button>
      </div>

      {showAdvancedFilters && (
        <div className="advanced-filters">
          <label className="advanced-filter-field" htmlFor="brandFilter">
            Brand
            <input
              id="brandFilter"
              value={productFilters.brand}
              onChange={(e) => onFilterChange('brand', e.target.value)}
              placeholder="e.g. Nike"
            />
          </label>
          <label className="advanced-filter-field" htmlFor="minPriceFilter">
            Min price
            <input
              id="minPriceFilter"
              type="number"
              min="0"
              step="0.01"
              value={productFilters.minPrice}
              onChange={(e) => onFilterChange('minPrice', e.target.value)}
              placeholder="0.00"
            />
          </label>
          <label className="advanced-filter-field" htmlFor="maxPriceFilter">
            Max price
            <input
              id="maxPriceFilter"
              type="number"
              min="0"
              step="0.01"
              value={productFilters.maxPrice}
              onChange={(e) => onFilterChange('maxPrice', e.target.value)}
              placeholder="9999.99"
            />
          </label>
          <label className="advanced-filter-field" htmlFor="minRatingFilter">
            Min rating
            <input
              id="minRatingFilter"
              type="number"
              min="0"
              max="5"
              step="0.1"
              value={productFilters.minRating}
              onChange={(e) => onFilterChange('minRating', e.target.value)}
              placeholder="0-5"
            />
          </label>
          <label className="advanced-filter-field" htmlFor="storeFilter">
            Store
            <select
              id="storeFilter"
              value={productFilters.storeId}
              onChange={(e) => onFilterChange('storeId', e.target.value)}
            >
              <option value="">All stores</option>
              {stores.map((store) => (
                <option key={store.id} value={store.id}>
                  {store.name}
                </option>
              ))}
            </select>
          </label>
          <label className="advanced-filter-field" htmlFor="flashSaleFilter">
            Flash sale
            <select
              id="flashSaleFilter"
              value={productFilters.flashSale}
              onChange={(e) => onFilterChange('flashSale', e.target.value)}
            >
              <option value="">Any</option>
              <option value="true">Yes</option>
              <option value="false">No</option>
            </select>
          </label>
          <label className="advanced-filter-field" htmlFor="trendingFilter">
            Trending
            <select
              id="trendingFilter"
              value={productFilters.trending}
              onChange={(e) => onFilterChange('trending', e.target.value)}
            >
              <option value="">Any</option>
              <option value="true">Yes</option>
              <option value="false">No</option>
            </select>
          </label>
        </div>
      )}

      <div className="table-wrap">
        <table>
          <thead>
            <tr>
              <SortableHeader
                label="Product"
                field="name"
                sortBy={productSort.sortBy}
                direction={productSort.direction}
                onSort={onSort}
              />
              <SortableHeader
                label="Category"
                field="category"
                sortBy={productSort.sortBy}
                direction={productSort.direction}
                onSort={onSort}
              />
              <SortableHeader
                label="Brand"
                field="brand"
                sortBy={productSort.sortBy}
                direction={productSort.direction}
                onSort={onSort}
              />
              <SortableHeader
                label="Price"
                field="price"
                sortBy={productSort.sortBy}
                direction={productSort.direction}
                onSort={onSort}
              />
              <SortableHeader
                label="Rating"
                field="averageRating"
                sortBy={productSort.sortBy}
                direction={productSort.direction}
                onSort={onSort}
              />
              <SortableHeader
                label="Reviews"
                field="reviewCount"
                sortBy={productSort.sortBy}
                direction={productSort.direction}
                onSort={onSort}
              />
              <SortableHeader
                label="Popularity"
                field="popularityScore"
                sortBy={productSort.sortBy}
                direction={productSort.direction}
                onSort={onSort}
              />
              <th>Tags</th>
              <th>Status</th>
              <th>Active</th>
              <th>Variants</th>
              <th>Media</th>
              <th>Actions</th>
            </tr>
          </thead>
          <tbody>
            {productsPage.content.length ? (
              productsPage.content.map((product) => (
                <tr
                  key={product.id}
                  className={String(product.id) === String(selectedProductId) ? 'row-active' : ''}
                >
                  <td>
                    <strong>{product.name}</strong>
                    {product.shortDescription && (
                      <div className="subtle-meta">
                        {product.shortDescription.substring(0, 60)}
                        {product.shortDescription.length > 60 ? '...' : ''}
                      </div>
                    )}
                  </td>
                  <td>{product.category || <span className="subtle-meta">—</span>}</td>
                  <td>{product.brand || <span className="subtle-meta">—</span>}</td>
                  <td>
                    <strong>{formatCurrency(product.price)}</strong>
                  </td>
                  <td>
                    {product.averageRating != null ? (
                      `${Number(product.averageRating).toFixed(1)} ★`
                    ) : (
                      <span className="subtle-meta">—</span>
                    )}
                  </td>
                  <td>{product.reviewCount ?? 0}</td>
                  <td>{product.popularityScore ?? 0}</td>
                  <td>
                    <div className="tag-group">
                      {product.flashSale && <span className="badge badge-warning">Flash</span>}
                      {product.trending && <span className="badge badge-info">Trending</span>}
                      {product.bestSeller && <span className="badge badge-success">Best</span>}
                      {product.allowBackorder && <span className="badge badge-outline">Backorder</span>}
                      {!product.flashSale &&
                        !product.trending &&
                        !product.bestSeller &&
                        !product.allowBackorder && <span className="subtle-meta">—</span>}
                    </div>
                  </td>
                  <td>
                    <span className="badge badge-muted">{product.status}</span>
                  </td>
                  <td>
                    <span className={`badge ${product.active ? 'badge-success' : 'badge-muted'}`}>
                      {product.active ? 'Yes' : 'No'}
                    </span>
                  </td>
                  <td>{product.variants?.length || 0}</td>
                  <td>{product.media?.length || 0}</td>
                  <td>
                    <div className="action-group">
                      <button
                        className="btn btn-sm btn-outline"
                        type="button"
                        onClick={() => onSelectProduct(product.id)}
                        title="Edit product"
                        disabled={saving}
                      >
                        Edit
                      </button>
                      <button
                        className="btn btn-sm btn-outline btn-danger"
                        type="button"
                        disabled={saving}
                        onClick={() => onDeleteProduct(product)}
                        title="Move product to trash"
                      >
                        Move to trash
                      </button>
                    </div>
                  </td>
                </tr>
              ))
            ) : (
              <tr>
                <td colSpan="13" className="empty-row">
                  No products found.
                </td>
              </tr>
            )}
          </tbody>
        </table>
      </div>
    </section>
  );
}

/**
 * Form for editing/creating a product.
 */
function ProductForm({
  selectedProduct,
  selectedProductId,
  productForm,
  saving,
  aiGenerating,
  aiInsertSaving,
  aiContentPreview,
  aiInsertOptions,
  flatCategories,
  onProductFormChange,
  onGenerateAiContent,
  onAiContentPreviewChange,
  onAiInsertOptionChange,
  onInsertAiDescription,
  onInsertAiSeo,
  onInsertAiFeatures,
  onInsertAiAll,
  onSaveProduct,
  onToggleActive,
  onDeleteProduct,
  onResetProductForm,
}) {
  const hasExistingDescription = Boolean(
    String(productForm.longDescription || '').trim() || String(productForm.shortDescription || '').trim()
  );
  const showAiContentTools = Boolean(selectedProductId) && (!hasExistingDescription || aiContentPreview);
  const aiBusy = aiGenerating || aiInsertSaving || saving;

  return (
    <section className="panel">
      <div className="section-head">
        <div>
          <h3>{selectedProductId ? `Edit product — ${selectedProduct?.name || ''}` : 'Create product'}</h3>
          <p>
            {selectedProductId
              ? 'Update product details, then manage variants, media, and store inventory below.'
              : 'Fill in the fields then press create.'}
          </p>
        </div>
        {selectedProductId && (
          <button className="btn btn-outline btn-sm" onClick={onResetProductForm} disabled={saving}>
            Cancel edit
          </button>
        )}
      </div>

      <div className="product-form-grid">
        {/* Left Column */}
        <div className="product-form-col">
          <fieldset className="form-fieldset">
            <legend>Basic information</legend>
            <div className="form-grid">
              <label className="span-2" htmlFor="productName">
                Name <span className="required">*</span>
                <input
                  id="productName"
                  value={productForm.name}
                  onChange={(e) => onProductFormChange('name', e.target.value)}
                  placeholder="Product name"
                  required
                />
              </label>
              <label htmlFor="productCategoryId">
                Category
                <select
                  id="productCategoryId"
                  value={productForm.categoryId}
                  onChange={(e) => onProductFormChange('categoryId', e.target.value)}
                >
                  <option value="">(Use category name)</option>
                  {flatCategories.map((cat) => (
                    <option key={cat.id} value={cat.id}>
                      {`${'  '.repeat(cat.depth)}${cat.name}`}
                    </option>
                  ))}
                </select>
              </label>
              <label htmlFor="productCategory">
                Category name
                <input
                  id="productCategory"
                  value={productForm.category}
                  onChange={(e) => onProductFormChange('category', e.target.value)}
                  placeholder="Fallback category name"
                />
              </label>
              <label htmlFor="productBrand">
                Brand
                <input
                  id="productBrand"
                  value={productForm.brand}
                  onChange={(e) => onProductFormChange('brand', e.target.value)}
                  placeholder="e.g. Nike, Apple"
                />
              </label>
              <label htmlFor="productPrice">
                Base price <span className="required">*</span>
                <input
                  id="productPrice"
                  type="number"
                  min="0"
                  step="0.01"
                  value={productForm.price}
                  onChange={(e) => onProductFormChange('price', e.target.value)}
                  placeholder="19.99"
                  required
                />
              </label>
            </div>
          </fieldset>

          <fieldset className="form-fieldset">
            <legend>Descriptions</legend>
            <div className="form-grid">
              <label className="span-2" htmlFor="productShortDesc">
                Short description
                <input
                  id="productShortDesc"
                  value={productForm.shortDescription}
                  onChange={(e) => onProductFormChange('shortDescription', e.target.value)}
                  placeholder="Shown in listing cards"
                />
              </label>
              <label className="span-2" htmlFor="productLongDesc">
                Long description
                <textarea
                  id="productLongDesc"
                  rows="5"
                  value={productForm.longDescription}
                  onChange={(e) => onProductFormChange('longDescription', e.target.value)}
                  placeholder="Full product description shown on the product page"
                />
              </label>
            </div>
          </fieldset>

          {showAiContentTools && (
            <fieldset className="form-fieldset">
              <legend>AI content generator</legend>
              <div className="inline-actions">
                <button
                  type="button"
                  className="btn btn-outline"
                  onClick={onGenerateAiContent}
                  disabled={aiBusy}
                >
                  {aiGenerating ? 'Generating…' : 'Generate Content with AI'}
                </button>
              </div>

              {aiContentPreview && (
                <>
                  <div className="form-grid" style={{ marginTop: 'var(--space-3)' }}>
                    <label className="span-2" htmlFor="aiDescription">
                      Generated description
                      <textarea
                        id="aiDescription"
                        rows="5"
                        value={aiContentPreview.description}
                        onChange={(e) => onAiContentPreviewChange('description', e.target.value)}
                        disabled={aiBusy}
                      />
                    </label>
                    <label className="span-2" htmlFor="aiShortDescription">
                      Generated short description
                      <textarea
                        id="aiShortDescription"
                        rows="3"
                        value={aiContentPreview.shortDescription}
                        onChange={(e) => onAiContentPreviewChange('shortDescription', e.target.value)}
                        disabled={aiBusy}
                      />
                    </label>
                    <label className="span-2" htmlFor="aiBulletFeatures">
                      Generated bullet features (one per line)
                      <textarea
                        id="aiBulletFeatures"
                        rows="5"
                        value={aiContentPreview.bulletFeaturesText}
                        onChange={(e) => onAiContentPreviewChange('bulletFeaturesText', e.target.value)}
                        disabled={aiBusy}
                      />
                    </label>
                    <label className="span-2" htmlFor="aiSeoTitle">
                      Generated SEO title
                      <input
                        id="aiSeoTitle"
                        value={aiContentPreview.seoTitle}
                        onChange={(e) => onAiContentPreviewChange('seoTitle', e.target.value)}
                        disabled={aiBusy}
                      />
                    </label>
                    <label className="span-2" htmlFor="aiSeoDescription">
                      Generated SEO description
                      <textarea
                        id="aiSeoDescription"
                        rows="3"
                        value={aiContentPreview.seoDescription}
                        onChange={(e) => onAiContentPreviewChange('seoDescription', e.target.value)}
                        disabled={aiBusy}
                      />
                    </label>
                    <label className="span-2" htmlFor="aiTags">
                      Generated tags (comma or newline separated, optional)
                      <textarea
                        id="aiTags"
                        rows="2"
                        value={aiContentPreview.tagsText}
                        onChange={(e) => onAiContentPreviewChange('tagsText', e.target.value)}
                        disabled={aiBusy}
                      />
                    </label>
                  </div>

                  <div className="toggle-grid" style={{ marginTop: 'var(--space-3)' }}>
                    <label className="toggle-card">
                      <input
                        type="checkbox"
                        checked={aiInsertOptions.overwriteDescription}
                        onChange={(e) => onAiInsertOptionChange('overwriteDescription', e.target.checked)}
                        disabled={aiBusy}
                      />
                      <div>
                        <strong>Overwrite existing description</strong>
                        <small>Off by default to protect current product text.</small>
                      </div>
                    </label>
                    <label className="toggle-card">
                      <input
                        type="checkbox"
                        checked={aiInsertOptions.overwriteSeo}
                        onChange={(e) => onAiInsertOptionChange('overwriteSeo', e.target.checked)}
                        disabled={aiBusy}
                      />
                      <div>
                        <strong>Overwrite existing SEO</strong>
                        <small>Only replace SEO fields when explicitly enabled.</small>
                      </div>
                    </label>
                    <label className="toggle-card">
                      <input
                        type="checkbox"
                        checked={aiInsertOptions.overwriteFeatures}
                        onChange={(e) => onAiInsertOptionChange('overwriteFeatures', e.target.checked)}
                        disabled={aiBusy}
                      />
                      <div>
                        <strong>Overwrite existing features/tags</strong>
                        <small>Preserves existing attributes unless enabled.</small>
                      </div>
                    </label>
                  </div>

                  <div className="inline-actions wrap" style={{ marginTop: 'var(--space-3)' }}>
                    <button type="button" className="btn btn-outline" onClick={onInsertAiDescription} disabled={aiBusy}>
                      Insert Description
                    </button>
                    <button type="button" className="btn btn-outline" onClick={onInsertAiSeo} disabled={aiBusy}>
                      Insert SEO
                    </button>
                    <button type="button" className="btn btn-outline" onClick={onInsertAiFeatures} disabled={aiBusy}>
                      Insert Features
                    </button>
                    <button type="button" className="btn btn-primary" onClick={onInsertAiAll} disabled={aiBusy}>
                      {aiInsertSaving ? 'Saving…' : 'Insert All'}
                    </button>
                  </div>
                </>
              )}
            </fieldset>
          )}

          <fieldset className="form-fieldset">
            <legend>Attributes (JSON)</legend>
            <textarea
              rows="5"
              value={productForm.attributesJson}
              onChange={(e) => onProductFormChange('attributesJson', e.target.value)}
              placeholder='{"material": "leather", "warranty": "2 years"}'
              className="mono"
            />
          </fieldset>
        </div>

        {/* Right Column */}
        <div className="product-form-col">
          <fieldset className="form-fieldset">
            <legend>SEO</legend>
            <div className="form-grid">
              <label className="span-2" htmlFor="productSeoSlug">
                Slug
                <input
                  id="productSeoSlug"
                  value={productForm.seoSlug}
                  onChange={(e) => onProductFormChange('seoSlug', e.target.value)}
                  placeholder="my-product-slug"
                />
              </label>
              <label className="span-2" htmlFor="productSeoTitle">
                Meta title
                <input
                  id="productSeoTitle"
                  value={productForm.seoTitle}
                  onChange={(e) => onProductFormChange('seoTitle', e.target.value)}
                  placeholder="Page title for search engines"
                />
              </label>
              <label className="span-2" htmlFor="productSeoDesc">
                Meta description
                <textarea
                  id="productSeoDesc"
                  rows="3"
                  value={productForm.seoDescription}
                  onChange={(e) => onProductFormChange('seoDescription', e.target.value)}
                  placeholder="Short description for search engine results"
                />
              </label>
            </div>
          </fieldset>

          <fieldset className="form-fieldset">
            <legend>Flags &amp; visibility</legend>
            <div className="toggle-grid">
              <label className="toggle-card">
                <input
                  type="checkbox"
                  checked={productForm.allowBackorder}
                  onChange={(e) => onProductFormChange('allowBackorder', e.target.checked)}
                />
                <div>
                  <strong>Allow backorder</strong>
                  <small>Accept orders when out of stock</small>
                </div>
              </label>
              <label className="toggle-card">
                <input
                  type="checkbox"
                  checked={productForm.flashSale}
                  onChange={(e) => onProductFormChange('flashSale', e.target.checked)}
                />
                <div>
                  <strong>Flash sale</strong>
                  <small>Show flash-sale badge on storefront</small>
                </div>
              </label>
              <label className="toggle-card">
                <input
                  type="checkbox"
                  checked={productForm.trending}
                  onChange={(e) => onProductFormChange('trending', e.target.checked)}
                />
                <div>
                  <strong>Trending</strong>
                  <small>Feature in trending sections</small>
                </div>
              </label>
              <label className="toggle-card">
                <input
                  type="checkbox"
                  checked={productForm.bestSeller}
                  onChange={(e) => onProductFormChange('bestSeller', e.target.checked)}
                />
                <div>
                  <strong>Best seller</strong>
                  <small>Show best-seller badge</small>
                </div>
              </label>
            </div>
          </fieldset>

          {selectedProduct && (
            <fieldset className="form-fieldset">
              <legend>Read-only stats</legend>
              <div className="stat-row">
                <div className="stat-item">
                  <small>Avg. rating</small>
                  <strong>
                    {selectedProduct.averageRating != null
                      ? `${Number(selectedProduct.averageRating).toFixed(1)} ★`
                      : '—'}
                  </strong>
                </div>
                <div className="stat-item">
                  <small>Reviews</small>
                  <strong>{selectedProduct.reviewCount ?? 0}</strong>
                </div>
                <div className="stat-item">
                  <small>Popularity</small>
                  <strong>{selectedProduct.popularityScore ?? 0}</strong>
                </div>
                <div className="stat-item">
                  <small>Status</small>
                  <strong>
                    <span
                      className={`badge ${selectedProduct.active ? 'badge-success' : 'badge-muted'}`}
                    >
                      {selectedProduct.status}
                    </span>
                  </strong>
                </div>
              </div>
            </fieldset>
          )}
        </div>
      </div>

      <div className="inline-actions wrap">
        <button
          className="btn btn-primary"
          disabled={saving || !productForm.name.trim()}
          onClick={onSaveProduct}
        >
          {saving ? 'Saving...' : selectedProductId ? 'Update product' : 'Create product'}
        </button>
        {selectedProductId && (
          <>
            <button className="btn btn-outline" disabled={saving} onClick={onToggleActive}>
              {selectedProduct?.active ? 'Deactivate' : 'Activate'}
            </button>
            <button className="btn btn-outline btn-danger" disabled={saving} onClick={onDeleteProduct}>
              Move to trash
            </button>
          </>
        )}
      </div>
    </section>
  );
}

/**
 * Variants management table and form.
 */
function VariantsSection({
  selectedProduct,
  variantDrafts,
  saving,
  variantForm,
  onVariantDraftChange,
  onSaveVariant,
  onVariantFormChange,
  onAddVariant,
}) {
  if (!selectedProduct) return null;
  return (
    <>
      <div className="divider" />
      <div className="section-head">
        <div>
          <h3>Variants</h3>
          <p>Manage sellable SKUs under this product.</p>
        </div>
      </div>

      {selectedProduct.variants?.length ? (
        <div className="table-wrap">
          <table>
            <thead>
              <tr>
                <th>SKU</th>
                <th>Color</th>
                <th>Size</th>
                <th>Price override</th>
                <th>Stock</th>
                <th>Actions</th>
              </tr>
            </thead>
            <tbody>
              {selectedProduct.variants.map((variant) => (
                <tr key={variant.id}>
                  <td className="mono">
                    <input
                      value={variantDrafts[variant.id]?.sku ?? variant.sku ?? ''}
                      onChange={(e) => onVariantDraftChange(variant.id, 'sku', e.target.value)}
                      disabled={saving}
                    />
                  </td>
                  <td>
                    <input
                      value={variantDrafts[variant.id]?.color ?? variant.color ?? ''}
                      onChange={(e) => onVariantDraftChange(variant.id, 'color', e.target.value)}
                      placeholder="Optional"
                      disabled={saving}
                    />
                  </td>
                  <td>
                    <input
                      value={variantDrafts[variant.id]?.size ?? variant.size ?? ''}
                      onChange={(e) => onVariantDraftChange(variant.id, 'size', e.target.value)}
                      placeholder="Optional"
                      disabled={saving}
                    />
                  </td>
                  <td>
                    <input
                      value={
                        variantDrafts[variant.id]?.price ??
                        (variant.priceOverride != null ? variant.priceOverride.toString() : '')
                      }
                      onChange={(e) => onVariantDraftChange(variant.id, 'price', e.target.value)}
                      placeholder="Optional"
                      disabled={saving}
                    />
                  </td>
                  <td>
                    <input
                      value={variantDrafts[variant.id]?.stock ?? String(variant.stock ?? 0)}
                      onChange={(e) => onVariantDraftChange(variant.id, 'stock', e.target.value)}
                      disabled={saving}
                    />
                  </td>
                  <td>
                    <button
                      className="btn btn-outline btn-sm"
                      disabled={saving}
                      onClick={() => onSaveVariant(variant)}
                    >
                      Save
                    </button>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      ) : (
        <p className="empty-copy">No variants yet.</p>
      )}

      <div className="form-grid">
        <label htmlFor="variantSku">
          SKU
          <input
            id="variantSku"
            value={variantForm.sku}
            onChange={(e) => onVariantFormChange('sku', e.target.value)}
            placeholder="SKU-001-BLK"
            disabled={saving}
          />
        </label>
        <label htmlFor="variantColor">
          Color
          <input
            id="variantColor"
            value={variantForm.color}
            onChange={(e) => onVariantFormChange('color', e.target.value)}
            placeholder="Black"
            disabled={saving}
          />
        </label>
        <label htmlFor="variantSize">
          Size
          <input
            id="variantSize"
            value={variantForm.size}
            onChange={(e) => onVariantFormChange('size', e.target.value)}
            placeholder="M"
            disabled={saving}
          />
        </label>
        <label htmlFor="variantPrice">
          Price override
          <input
            id="variantPrice"
            value={variantForm.price}
            onChange={(e) => onVariantFormChange('price', e.target.value)}
            placeholder="Optional"
            disabled={saving}
          />
        </label>
        <label htmlFor="variantStock">
          Stock
          <input
            id="variantStock"
            value={variantForm.stock}
            onChange={(e) => onVariantFormChange('stock', e.target.value)}
            disabled={saving}
          />
        </label>
      </div>
      <button
        className="btn btn-outline"
        disabled={saving || !variantForm.sku.trim()}
        onClick={onAddVariant}
      >
        Add variant
      </button>
    </>
  );
}

/**
 * Media management form and list.
 */
function MediaSection({ selectedProduct, saving, onMediaChanged, onFlash, onError }) {
  if (!selectedProduct) return null;
  return (
    <>
      <div className="divider" />
      <div className="section-head">
        <div>
          <h3>Media</h3>
          <p>Manage internal/external product images with drag-drop, paste, URL import, and sorting.</p>
        </div>
      </div>
      <ProductMediaUploader
        productId={selectedProduct.id}
        mediaItems={selectedProduct.media || []}
        disabled={saving}
        onMediaChanged={onMediaChanged}
        onFlash={onFlash}
        onError={onError}
      />
    </>
  );
}

/**
 * Per-store inventory management.
 */
function StoreInventorySection({
  selectedProduct,
  storeInvForm,
  stores,
  saving,
  onStoreInvFormChange,
  onUpsertStoreInventory,
}) {
  if (!selectedProduct) return null;
  return (
    <>
      <div className="divider" />
      <div className="section-head">
        <div>
          <h3>Per-store inventory</h3>
          <p>Upsert stock and store price overrides for this product.</p>
        </div>
      </div>

      {selectedProduct.storeInventory?.length ? (
        <div className="table-wrap">
          <table>
            <thead>
              <tr>
                <th>Store</th>
                <th>Stock</th>
                <th>Store price</th>
              </tr>
            </thead>
            <tbody>
              {selectedProduct.storeInventory.map((item) => (
                <tr key={item.storeId}>
                  <td>{item.storeName}</td>
                  <td>{item.stock}</td>
                  <td>{formatCurrency(item.storePrice)}</td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      ) : (
        <p className="empty-copy">No store inventory rows.</p>
      )}

      <div className="form-grid">
        <label className="span-2" htmlFor="storeSelect">
          Store
          <select
            id="storeSelect"
            value={storeInvForm.storeId}
            onChange={(e) => onStoreInvFormChange('storeId', e.target.value)}
            disabled={!stores.length || saving}
          >
            <option value="">Select store...</option>
            {stores.map((store) => (
              <option key={store.id} value={store.id}>
                {store.name}
              </option>
            ))}
          </select>
        </label>
        <label htmlFor="storeStock">
          Stock
          <input
            id="storeStock"
            value={storeInvForm.stock}
            onChange={(e) => onStoreInvFormChange('stock', e.target.value)}
            disabled={saving}
          />
        </label>
        <label htmlFor="storePrice">
          Store price
          <input
            id="storePrice"
            value={storeInvForm.storePrice}
            onChange={(e) => onStoreInvFormChange('storePrice', e.target.value)}
            placeholder="19.99"
            disabled={saving}
          />
        </label>
      </div>
      <button
        className="btn btn-outline"
        disabled={saving || !storeInvForm.storeId || !storeInvForm.storePrice}
        onClick={onUpsertStoreInventory}
      >
        Upsert store inventory
      </button>
    </>
  );
}

/* --------------------------------------------------------------------------
   Main Component
   -------------------------------------------------------------------------- */
export function CommerceCatalogPage() {
  const confirm = useConfirmDialog();
  const { loading, error, setError, flash, setFlash, safeExecute } = useSafeAsync(true);
  useToastFeedback({ successMessage: flash, errorMessage: error });
  const saving = loading;

  // State
  const [categoryTree, setCategoryTree] = useState([]);
  const [selectedCategoryId, setSelectedCategoryId] = useState('');
  const [categoryForm, setCategoryForm] = useState(DEFAULT_CATEGORY_FORM);

  const [productsPage, setProductsPage] = useState({ content: [], totalElements: 0 });
  const [productFilters, setProductFilters] = useState({
    query: '',
    categoryId: '',
    brand: '',
    minPrice: '',
    maxPrice: '',
    minRating: '',
    storeId: '',
    flashSale: '',
    trending: '',
  });
  const [showAdvancedFilters, setShowAdvancedFilters] = useState(false);
  const [productSort, setProductSort] = useState({ sortBy: 'createdAt', direction: 'desc' });
  const [selectedProductId, setSelectedProductId] = useState('');
  const [productForm, setProductForm] = useState(DEFAULT_PRODUCT_FORM);

  const [stores, setStores] = useState([]);
  const [variantForm, setVariantForm] = useState(DEFAULT_VARIANT_FORM);
  const [variantDrafts, setVariantDrafts] = useState({});
  const [storeInvForm, setStoreInvForm] = useState(DEFAULT_STORE_INV_FORM);
  const [aiGenerating, setAiGenerating] = useState(false);
  const [aiInsertSaving, setAiInsertSaving] = useState(false);
  const [aiContentPreview, setAiContentPreview] = useState(null);
  const [aiInsertOptions, setAiInsertOptions] = useState(DEFAULT_AI_INSERT_OPTIONS);

  // Memoized values
  const flatCategories = useMemo(() => flattenTree(categoryTree), [categoryTree]);
  const selectedProduct = useMemo(
    () => productsPage.content.find((item) => String(item.id) === String(selectedProductId)) || null,
    [productsPage.content, selectedProductId]
  );

  const applyProductToForm = useCallback((product) => {
    if (!product) {
      setProductForm(DEFAULT_PRODUCT_FORM);
      return;
    }
    setProductForm({
      name: product.name || '',
      categoryId: '',
      category: product.category || '',
      brand: product.brand || '',
      price: product.price?.toString?.() || '',
      allowBackorder: Boolean(product.allowBackorder),
      flashSale: Boolean(product.flashSale),
      trending: Boolean(product.trending),
      bestSeller: Boolean(product.bestSeller),
      shortDescription: product.shortDescription || '',
      longDescription: product.longDescription || '',
      seoSlug: product.seoSlug || product.seo?.slug || '',
      seoTitle: product.seoTitle || product.seo?.metaTitle || '',
      seoDescription: product.seoDescription || product.seo?.metaDescription || '',
      attributesJson: JSON.stringify(product.attributes || {}, null, 2),
    });
  }, []);

  /* ------------------------------------------------------------------------
     Data fetching
     ------------------------------------------------------------------------ */
  const load = useCallback(async () => {
    await safeExecute(
      async ({ signal }) => {
        const [tree, storePage] = await Promise.all([
          getCommerceCategoryTree('en', { signal }),
          listStores({ page: 0, size: 100, sortBy: 'name', direction: 'asc' }, { signal }).catch(
            () => ({ content: [] })
          ),
        ]);
        setCategoryTree(tree || []);
        setStores(storePage?.content || []);
        await loadProducts(productFilters, { signal });
      },
      null,
      (err) => setError(err.message || 'Failed to load commerce catalog.')
    );
  }, [safeExecute, productFilters]);

  const loadProducts = useCallback(
    async (filters, { signal } = {}) => {
      try {
        const response = await listCommerceProducts(
          {
            page: 0,
            size: 80,
            sortBy: productSort.sortBy,
            direction: productSort.direction,
            query: filters.query || undefined,
            categoryId: filters.categoryId || undefined,
            brand: filters.brand || undefined,
            minPrice: filters.minPrice !== '' ? Number(filters.minPrice) : undefined,
            maxPrice: filters.maxPrice !== '' ? Number(filters.maxPrice) : undefined,
            minRating: filters.minRating !== '' ? Number(filters.minRating) : undefined,
            storeId: filters.storeId || undefined,
            flashSale:
              filters.flashSale === 'true' ? true : filters.flashSale === 'false' ? false : undefined,
            trending:
              filters.trending === 'true' ? true : filters.trending === 'false' ? false : undefined,
          },
          { signal }
        );
        if (!signal?.aborted) {
          setProductsPage(response || { content: [], totalElements: 0 });
          // If selected product is no longer in the list, clear selection
          if (
            selectedProductId &&
            !(response?.content || []).some((item) => String(item.id) === String(selectedProductId))
          ) {
            setSelectedProductId('');
            applyProductToForm(null);
            setAiContentPreview(null);
            setAiInsertOptions(DEFAULT_AI_INSERT_OPTIONS);
          }
        }
      } catch (err) {
        if (!signal?.aborted) {
          setError(err.message || 'Failed to load products.');
        }
      }
    },
    [productSort.sortBy, productSort.direction, selectedProductId, applyProductToForm]
  );

  // Initial load
  useEffect(() => {
    load();
  }, [load]);

  // Reload products when sort changes
  useEffect(() => {
    const abortController = new AbortController();
    loadProducts(productFilters, { signal: abortController.signal }).catch(() => {});
    return () => abortController.abort();
  }, [loadProducts, productFilters, productSort.sortBy, productSort.direction]); // added productFilters to deps

  // Update variant drafts when selected product changes
  useEffect(() => {
    if (!selectedProduct?.variants?.length) {
      setVariantDrafts({});
      return;
    }
    setVariantDrafts(
      Object.fromEntries(
        selectedProduct.variants.map((variant) => [
          variant.id,
          {
            sku: variant.sku || '',
            color: variant.color || '',
            size: variant.size || '',
            price: variant.priceOverride?.toString?.() || '',
            stock: String(variant.stock ?? 0),
          },
        ])
      )
    );
  }, [selectedProductId, selectedProduct]);

  /* ------------------------------------------------------------------------
     Handlers
     ------------------------------------------------------------------------ */
  const resetCategoryForm = useCallback(() => {
    setSelectedCategoryId('');
    setCategoryForm(DEFAULT_CATEGORY_FORM);
  }, []);

  const resetProductForm = useCallback(() => {
    setSelectedProductId('');
    applyProductToForm(null);
    setVariantForm(DEFAULT_VARIANT_FORM);
    setVariantDrafts({});
    setStoreInvForm(DEFAULT_STORE_INV_FORM);
    setAiContentPreview(null);
    setAiInsertOptions(DEFAULT_AI_INSERT_OPTIONS);
  }, [applyProductToForm]);

  const selectCategory = useCallback(
    (categoryId) => {
      setFlash('');
      setError('');
      setSelectedCategoryId(categoryId);
      const category = flatCategories.find((item) => String(item.id) === String(categoryId));
      if (!category) {
        setCategoryForm(DEFAULT_CATEGORY_FORM);
        return;
      }
      setCategoryForm({
        parentId: category.parentId || '',
        name: category.name || '',
        description: category.description || '',
        classificationCode: category.classificationCode || '',
        managerId: category.managerId || '',
      });
    },
    [flatCategories]
  );

  const saveCategory = useCallback(async () => {
    await safeExecute(
      async () => {
        const payload = {
          name: categoryForm.name.trim(),
          description: categoryForm.description?.trim() || null,
          classificationCode: categoryForm.classificationCode?.trim() || null,
          parentId: categoryForm.parentId || null,
          managerId: categoryForm.managerId?.trim() || null,
        };
        if (selectedCategoryId) {
          await updateCommerceCategory(selectedCategoryId, payload);
          setFlash('Category updated.');
        } else {
          await createCommerceCategory(payload);
          setFlash('Category created.');
        }
        await load();
        resetCategoryForm();
      },
      null,
      (err) => setError(err.message || 'Unable to save category.')
    );
  }, [safeExecute, categoryForm, selectedCategoryId, load, resetCategoryForm]);

  const selectProduct = useCallback(
    (productId) => {
      setFlash('');
      setError('');
      if (!productId) {
        resetProductForm();
        return;
      }
      setSelectedProductId(productId);
      const product = productsPage.content.find((item) => String(item.id) === String(productId));
      applyProductToForm(product || null);
      setVariantForm(DEFAULT_VARIANT_FORM);
      setStoreInvForm((current) => ({
        ...DEFAULT_STORE_INV_FORM,
        storeId: current.storeId || '',
        stock: '0',
        storePrice: '',
      }));
      setAiContentPreview(null);
      setAiInsertOptions(DEFAULT_AI_INSERT_OPTIONS);
    },
    [productsPage.content, resetProductForm, applyProductToForm]
  );

  const buildProductPayload = useCallback((nextForm, options = {}) => {
    const attributes =
      options.attributes !== undefined
        ? options.attributes
        : parseJson(nextForm.attributesJson) || {};
    return {
      name: nextForm.name.trim(),
      description: nextForm.shortDescription?.trim() || null,
      categoryId: nextForm.categoryId || null,
      category: nextForm.category?.trim() || null,
      brand: nextForm.brand?.trim() || null,
      price: asNumber(nextForm.price, 'Price'),
      attributes,
      allowBackorder: Boolean(nextForm.allowBackorder),
      flashSale: Boolean(nextForm.flashSale),
      trending: Boolean(nextForm.trending),
      bestSeller: Boolean(nextForm.bestSeller),
      shortDescription: nextForm.shortDescription?.trim() || null,
      longDescription: nextForm.longDescription?.trim() || null,
      seo: {
        slug: nextForm.seoSlug?.trim() || null,
        metaTitle: nextForm.seoTitle?.trim() || null,
        metaDescription: nextForm.seoDescription?.trim() || null,
      },
      variants: null,
      media: null,
      inventory: null,
    };
  }, []);

  const saveProduct = useCallback(async () => {
    await safeExecute(
      async () => {
        const payload = buildProductPayload(productForm);

        let newProductId;
        if (selectedProductId) {
          await updateCommerceProduct(selectedProductId, payload);
          setFlash('Product updated.');
          newProductId = selectedProductId;
        } else {
          const created = await createCommerceProduct(payload);
          setFlash('Product created.');
          newProductId = created.id;
          setSelectedProductId(created.id);
        }

        await loadProducts(productFilters);
        if (newProductId) {
          const refreshed = await getCommerceProduct(newProductId);
          setProductsPage((current) => ({
            ...current,
            content: current.content.map((item) =>
              String(item.id) === String(newProductId) ? refreshed : item
            ),
          }));
          if (String(newProductId) === String(selectedProductId)) {
            applyProductToForm(refreshed);
          }
        }
      },
      null,
      (err) => setError(err.message || 'Unable to save product.')
    );
  }, [safeExecute, productForm, selectedProductId, productFilters, loadProducts, buildProductPayload, applyProductToForm]);

  const toggleProductActive = useCallback(async () => {
    if (!selectedProductId) return;
    await safeExecute(
      async () => {
        const nextActive = !selectedProduct?.active;
        const updated = await patchCommerceProduct(selectedProductId, { active: Boolean(nextActive) });
        setFlash(`Product ${nextActive ? 'activated' : 'deactivated'}.`);
        setProductsPage((current) => ({
          ...current,
          content: current.content.map((item) =>
            String(item.id) === String(selectedProductId) ? updated : item
          ),
        }));
      },
      null,
      (err) => setError(err.message || 'Unable to update active flag.')
    );
  }, [safeExecute, selectedProductId, selectedProduct]);

  const removeProduct = useCallback(async () => {
    if (!selectedProductId) return;
    await safeExecute(
      async () => {
        await deleteCommerceProduct(selectedProductId);
        setFlash('Product moved to trash.');
        resetProductForm();
        await loadProducts(productFilters);
      },
      null,
      (err) => setError(err.message || 'Unable to move product to trash.')
    );
  }, [safeExecute, selectedProductId, productFilters, resetProductForm, loadProducts]);

  const handleDeleteProductRow = useCallback(
    async (product) => {
      const confirmed = await confirm({
        title: 'Move product to trash?',
        message: 'This action moves the selected commerce product to trash. You can restore it later from the Recovery Center.',
        description: product.name,
        confirmLabel: 'Move to trash',
      });
      if (!confirmed) return;
      setSelectedProductId(product.id);
      await safeExecute(
        async () => {
          await deleteCommerceProduct(product.id);
          setFlash('Product moved to trash.');
          resetProductForm();
          await loadProducts(productFilters);
        },
        null,
        (err) => setError(err.message || 'Failed to move product to trash.')
      );
    },
    [confirm, safeExecute, productFilters, resetProductForm, loadProducts]
  );

  const addVariant = useCallback(async () => {
    if (!selectedProductId) return;
    await safeExecute(
      async () => {
        const payload = {
          sku: variantForm.sku.trim(),
          color: variantForm.color?.trim() || null,
          size: variantForm.size?.trim() || null,
          price: variantForm.price === '' ? null : asNumber(variantForm.price, 'Variant price'),
          stock: variantForm.stock === '' ? 0 : asInt(variantForm.stock, 'Stock'),
        };
        await addCommerceVariant(selectedProductId, payload);
        const refreshed = await getCommerceProduct(selectedProductId);
        setProductsPage((current) => ({
          ...current,
          content: current.content.map((item) =>
            String(item.id) === String(selectedProductId) ? refreshed : item
          ),
        }));
        setVariantForm(DEFAULT_VARIANT_FORM);
        setFlash('Variant added.');
      },
      null,
      (err) => setError(err.message || 'Unable to add variant.')
    );
  }, [safeExecute, selectedProductId, variantForm]);

  const saveVariant = useCallback(
    async (variant) => {
      const variantId = variant?.id;
      if (!selectedProductId || !variantId) return;
      await safeExecute(
        async () => {
          const draft = variantDrafts[variantId];
          if (!draft?.sku?.trim()) {
            throw new Error('Variant SKU is required.');
          }
          const payload = {
            sku: draft.sku.trim(),
            color: draft.color?.trim() || null,
            size: draft.size?.trim() || null,
            attributes: variant.attributes || null,
            price: draft.price === '' ? null : asNumber(draft.price, 'Price override'),
            stock: draft.stock === '' ? 0 : asInt(draft.stock, 'Stock') ?? 0,
          };
          await updateCommerceVariant(variantId, payload);
          const refreshed = await getCommerceProduct(selectedProductId);
          setProductsPage((current) => ({
            ...current,
            content: current.content.map((item) =>
              String(item.id) === String(selectedProductId) ? refreshed : item
            ),
          }));
          setFlash('Variant updated.');
        },
        null,
        (err) => setError(err.message || 'Unable to update variant.')
      );
    },
    [safeExecute, selectedProductId, variantDrafts]
  );

  const refreshSelectedProduct = useCallback(
    async (productId = selectedProductId) => {
      if (!productId) return null;
      const refreshed = await getCommerceProduct(productId);
      setProductsPage((current) => ({
        ...current,
        content: current.content.map((item) =>
          String(item.id) === String(productId) ? refreshed : item
        ),
      }));
      if (String(productId) === String(selectedProductId)) {
        applyProductToForm(refreshed);
      }
      return refreshed;
    },
    [selectedProductId, applyProductToForm]
  );

  const upsertStoreInventory = useCallback(async () => {
    if (!selectedProductId) return;
    await safeExecute(
      async () => {
        const payload = {
          storeId: storeInvForm.storeId,
          stock: asInt(storeInvForm.stock, 'Stock') ?? 0,
          storePrice: asNumber(storeInvForm.storePrice, 'Store price'),
        };
        await upsertCommerceStoreInventory(selectedProductId, payload);
        const refreshed = await getCommerceProduct(selectedProductId);
        setProductsPage((current) => ({
          ...current,
          content: current.content.map((item) =>
            String(item.id) === String(selectedProductId) ? refreshed : item
          ),
        }));
        setFlash('Store inventory upserted.');
      },
      null,
      (err) => setError(err.message || 'Unable to upsert store inventory.')
    );
  }, [safeExecute, selectedProductId, storeInvForm]);

  const handleGenerateAiContent = useCallback(async () => {
    if (!selectedProductId || aiGenerating || aiInsertSaving) return;
    setAiGenerating(true);
    setError('');
    setFlash('');
    try {
      let rawAttributes = {};
      try {
        rawAttributes = parseJson(productForm.attributesJson) || {};
      } catch (_) {
        rawAttributes = {};
      }
      const targetAudience =
        selectedProduct?.targetAudience || rawAttributes?.targetAudience || rawAttributes?.audience || '';
      const generated = await generateProduct({
        name: productForm.name?.trim() || selectedProduct?.name || '',
        category: productForm.category?.trim() || selectedProduct?.category || '',
        brand: productForm.brand?.trim() || selectedProduct?.brand || '',
        targetAudience: String(targetAudience || '').trim(),
      });

      const generatedDescription = String(generated?.description || '').trim();
      const overview = extractOverview(generatedDescription);
      const shortDescription = truncateText(overview || generatedDescription, 180);
      const features = extractBulletFeatures(generatedDescription);
      const seoTitle = truncateText(
        `${productForm.name?.trim() || selectedProduct?.name || 'Product'}${productForm.brand?.trim() ? ` | ${productForm.brand.trim()}` : ''}`,
        70
      );
      const seoDescription = truncateText(shortDescription || generatedDescription, 160);
      const tags = inferTags({
        name: productForm.name,
        category: productForm.category || selectedProduct?.category,
        brand: productForm.brand,
        targetAudience,
        features,
      });

      setAiContentPreview({
        description: generatedDescription,
        shortDescription,
        bulletFeaturesText: features.join('\n'),
        seoTitle,
        seoDescription,
        tagsText: tags.join(', '),
      });
      setFlash('AI content generated. Review and insert the fields you want.');
    } catch (err) {
      setError(err?.response?.data?.message || err?.message || 'Failed to generate AI content.');
    } finally {
      setAiGenerating(false);
    }
  }, [selectedProductId, aiGenerating, aiInsertSaving, productForm, selectedProduct, setError, setFlash]);

  const handleAiContentPreviewChange = useCallback((field, value) => {
    setAiContentPreview((prev) => ({ ...(prev || DEFAULT_AI_CONTENT_PREVIEW), [field]: value }));
  }, []);

  const handleAiInsertOptionChange = useCallback((field, value) => {
    setAiInsertOptions((prev) => ({ ...prev, [field]: Boolean(value) }));
  }, []);

  const applyAiInsert = useCallback(
    async ({ includeDescription = false, includeSeo = false, includeFeatures = false }) => {
      if (!selectedProductId || !aiContentPreview || aiGenerating || aiInsertSaving) return;
      setAiInsertSaving(true);
      setError('');
      setFlash('');
      try {
        const nextForm = { ...productForm };
        const parsedAttributes = parseJson(productForm.attributesJson) || {};
        const nextAttributes = { ...parsedAttributes };

        if (includeDescription) {
          const hasLong = Boolean(String(nextForm.longDescription || '').trim());
          const hasShort = Boolean(String(nextForm.shortDescription || '').trim());
          if ((hasLong || hasShort) && !aiInsertOptions.overwriteDescription) {
            throw new Error('Description already exists. Enable "Overwrite existing description" to replace it.');
          }
          if (String(aiContentPreview.description || '').trim()) {
            nextForm.longDescription = aiContentPreview.description;
          }
          if (String(aiContentPreview.shortDescription || '').trim()) {
            nextForm.shortDescription = aiContentPreview.shortDescription;
          }
        }

        if (includeSeo) {
          const hasSeo = Boolean(
            String(nextForm.seoTitle || '').trim() || String(nextForm.seoDescription || '').trim()
          );
          if (hasSeo && !aiInsertOptions.overwriteSeo) {
            throw new Error('SEO already exists. Enable "Overwrite existing SEO" to replace it.');
          }
          if (String(aiContentPreview.seoTitle || '').trim()) {
            nextForm.seoTitle = aiContentPreview.seoTitle;
          }
          if (String(aiContentPreview.seoDescription || '').trim()) {
            nextForm.seoDescription = aiContentPreview.seoDescription;
          }
        }

        if (includeFeatures) {
          const features = parseTextList(aiContentPreview.bulletFeaturesText);
          const tags = parseTagList(aiContentPreview.tagsText);
          const hasExistingFeatureContent = Boolean(
            nextAttributes.aiContent?.bulletFeatures?.length || nextAttributes.aiContent?.tags?.length
          );
          if (hasExistingFeatureContent && !aiInsertOptions.overwriteFeatures) {
            throw new Error('Feature attributes already exist. Enable overwrite to replace them.');
          }
          nextAttributes.aiContent = {
            ...(nextAttributes.aiContent || {}),
            bulletFeatures: features,
            tags,
            generatedAt: new Date().toISOString(),
          };
        }

        nextForm.attributesJson = JSON.stringify(nextAttributes, null, 2);
        const payload = buildProductPayload(nextForm, { attributes: nextAttributes });
        await updateCommerceProduct(selectedProductId, payload);
        await refreshSelectedProduct(selectedProductId);
        setFlash('Generated content inserted and saved.');
      } catch (err) {
        setError(err?.response?.data?.message || err?.message || 'Failed to insert generated content.');
      } finally {
        setAiInsertSaving(false);
      }
    },
    [
      selectedProductId,
      aiContentPreview,
      aiGenerating,
      aiInsertSaving,
      productForm,
      aiInsertOptions,
      buildProductPayload,
      refreshSelectedProduct,
      setError,
      setFlash,
    ]
  );

  const handleInsertAiDescription = useCallback(() => {
    applyAiInsert({ includeDescription: true });
  }, [applyAiInsert]);

  const handleInsertAiSeo = useCallback(() => {
    applyAiInsert({ includeSeo: true });
  }, [applyAiInsert]);

  const handleInsertAiFeatures = useCallback(() => {
    applyAiInsert({ includeFeatures: true });
  }, [applyAiInsert]);

  const handleInsertAiAll = useCallback(() => {
    applyAiInsert({ includeDescription: true, includeSeo: true, includeFeatures: true });
  }, [applyAiInsert]);

  // Generic change handlers for forms
  const handleCategoryFormChange = useCallback((field, value) => {
    setCategoryForm((prev) => ({ ...prev, [field]: value }));
  }, []);

  const handleProductFormChange = useCallback((field, value) => {
    setProductForm((prev) => ({ ...prev, [field]: value }));
  }, []);

  const handleProductFilterChange = useCallback((field, value) => {
    setProductFilters((prev) => ({ ...prev, [field]: value }));
  }, []);

  const handleVariantFormChange = useCallback((field, value) => {
    setVariantForm((prev) => ({ ...prev, [field]: value }));
  }, []);

  const handleVariantDraftChange = useCallback((variantId, field, value) => {
    setVariantDrafts((prev) => ({
      ...prev,
      [variantId]: { ...(prev[variantId] || {}), [field]: value },
    }));
  }, []);

  const handleMediaFlash = useCallback((message) => {
    setError('');
    setFlash(message || '');
  }, [setError, setFlash]);

  const handleMediaError = useCallback((message) => {
    setError(message || 'Media operation failed.');
  }, [setError]);

  const handleStoreInvFormChange = useCallback((field, value) => {
    setStoreInvForm((prev) => ({ ...prev, [field]: value }));
  }, []);

  const handleProductSort = useCallback((field, direction) => {
    setProductSort({ sortBy: field, direction });
  }, []);

  const handleApplyFilters = useCallback(() => {
    loadProducts(productFilters).catch(() => {});
  }, [loadProducts, productFilters]);

  const handleToggleAdvancedFilters = useCallback(() => {
    setShowAdvancedFilters((v) => !v);
  }, []);

  // Loading state
  if (loading) {
    return <Spinner label="Loading commerce catalog..." />;
  }

  return (
    <div className="page" aria-busy={saving ? 'true' : 'false'}>
      <div className="page-head">
        <h2>Commerce catalog</h2>
        <p>Manage platform products, categories, variants, media, and per-store inventory pricing.</p>
      </div>

      {/* Row 1: Categories sidebar + Product listing table */}
      <div className="catalog-layout">
        <CategorySidebar
          categoryTree={categoryTree}
          selectedCategoryId={selectedCategoryId}
          categoryForm={categoryForm}
          saving={saving}
          flatCategories={flatCategories}
          onSelectCategory={selectCategory}
          onCategoryFormChange={handleCategoryFormChange}
          onSaveCategory={saveCategory}
          onResetCategory={resetCategoryForm}
          onRefresh={load}
        />

        <ProductTable
          productsPage={productsPage}
          productFilters={productFilters}
          showAdvancedFilters={showAdvancedFilters}
          flatCategories={flatCategories}
          stores={stores}
          productSort={productSort}
          selectedProductId={selectedProductId}
          saving={saving}
          onFilterChange={handleProductFilterChange}
          onApplyFilters={handleApplyFilters}
          onToggleAdvancedFilters={handleToggleAdvancedFilters}
          onSort={handleProductSort}
          onSelectProduct={selectProduct}
          onDeleteProduct={handleDeleteProductRow}
        />
      </div>

      {/* Row 2: Product Form (full width) */}
      <ProductForm
        selectedProduct={selectedProduct}
        selectedProductId={selectedProductId}
        productForm={productForm}
        saving={saving}
        aiGenerating={aiGenerating}
        aiInsertSaving={aiInsertSaving}
        aiContentPreview={aiContentPreview}
        aiInsertOptions={aiInsertOptions}
        flatCategories={flatCategories}
        onProductFormChange={handleProductFormChange}
        onGenerateAiContent={handleGenerateAiContent}
        onAiContentPreviewChange={handleAiContentPreviewChange}
        onAiInsertOptionChange={handleAiInsertOptionChange}
        onInsertAiDescription={handleInsertAiDescription}
        onInsertAiSeo={handleInsertAiSeo}
        onInsertAiFeatures={handleInsertAiFeatures}
        onInsertAiAll={handleInsertAiAll}
        onSaveProduct={saveProduct}
        onToggleActive={toggleProductActive}
        onDeleteProduct={removeProduct}
        onResetProductForm={resetProductForm}
      />

      {/* Nested sections only when a product is selected */}
      {selectedProduct && (
        <>
          <VariantsSection
            selectedProduct={selectedProduct}
            variantDrafts={variantDrafts}
            saving={saving}
            variantForm={variantForm}
            onVariantDraftChange={handleVariantDraftChange}
            onSaveVariant={saveVariant}
            onVariantFormChange={handleVariantFormChange}
            onAddVariant={addVariant}
          />

          <MediaSection
            selectedProduct={selectedProduct}
            saving={saving}
            onMediaChanged={refreshSelectedProduct}
            onFlash={handleMediaFlash}
            onError={handleMediaError}
          />

          <StoreInventorySection
            selectedProduct={selectedProduct}
            storeInvForm={storeInvForm}
            stores={stores}
            saving={saving}
            onStoreInvFormChange={handleStoreInvFormChange}
            onUpsertStoreInventory={upsertStoreInventory}
          />
        </>
      )}
    </div>
  );
}
