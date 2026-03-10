import { useEffect, useMemo, useState } from 'react'
import {
  createCommerceCategory,
  getCommerceCategoryTree,
  updateCommerceCategory
} from '../shared/api/endpoints/commerceCategoriesApi'
import {
  addCommerceMedia,
  addCommerceVariant,
  createCommerceProduct,
  deleteCommerceProduct,
  getCommerceProduct,
  listCommerceProducts,
  patchCommerceProduct,
  updateCommerceProduct,
  updateCommerceVariant,
  upsertCommerceStoreInventory
} from '../shared/api/endpoints/commerceProductsApi'
import { listStores } from '../shared/api/endpoints/storesApi'
import { Spinner } from '../shared/ui/Spinner'
import { SortableHeader } from '../shared/ui/SortableHeader'
import { formatCurrency } from '../shared/ui/formatters'

const DEFAULT_CATEGORY_FORM = {
  parentId: '',
  name: '',
  description: '',
  classificationCode: '',
  managerId: ''
}

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
  attributesJson: '{}'
}

const DEFAULT_VARIANT_FORM = {
  sku: '',
  color: '',
  size: '',
  price: '',
  stock: '0'
}

const DEFAULT_MEDIA_FORM = {
  mediaType: 'IMAGE',
  url: '',
  sortOrder: '0',
  isPrimary: true
}

const DEFAULT_STORE_INV_FORM = {
  storeId: '',
  stock: '0',
  storePrice: ''
}

function flattenTree(nodes = [], depth = 0, parentId = null) {
  return nodes.flatMap((node) => [
    { ...node, depth, parentId },
    ...flattenTree(node.children || [], depth + 1, node.id)
  ])
}

function parseJson(text) {
  const trimmed = String(text || '').trim()
  if (!trimmed) return undefined
  try {
    return JSON.parse(trimmed)
  } catch (_) {
    throw new Error('Attributes must be valid JSON.')
  }
}

function asNumber(value, fieldLabel) {
  if (value === '' || value === null || value === undefined) return null
  const num = Number(value)
  if (Number.isNaN(num)) {
    throw new Error(`${fieldLabel} must be a number.`)
  }
  return num
}

function asInt(value, fieldLabel) {
  const num = asNumber(value, fieldLabel)
  if (num === null) return null
  return Math.trunc(num)
}

export function CommerceCatalogPage() {
  const [loading, setLoading] = useState(true)
  const [saving, setSaving] = useState(false)
  const [error, setError] = useState('')
  const [flash, setFlash] = useState('')

  const [categoryTree, setCategoryTree] = useState([])
  const [selectedCategoryId, setSelectedCategoryId] = useState('')
  const [categoryForm, setCategoryForm] = useState(DEFAULT_CATEGORY_FORM)

  const [productsPage, setProductsPage] = useState({ content: [], totalElements: 0 })
  const [productFilters, setProductFilters] = useState({ query: '', categoryId: '', brand: '', minPrice: '', maxPrice: '', minRating: '', storeId: '', flashSale: '', trending: '' })
  const [showAdvancedFilters, setShowAdvancedFilters] = useState(false)
  const [productSort, setProductSort] = useState({ sortBy: 'createdAt', direction: 'desc' })
  const [selectedProductId, setSelectedProductId] = useState('')
  const [productForm, setProductForm] = useState(DEFAULT_PRODUCT_FORM)

  const [stores, setStores] = useState([])
  const [variantForm, setVariantForm] = useState(DEFAULT_VARIANT_FORM)
  const [variantDrafts, setVariantDrafts] = useState({})
  const [mediaForm, setMediaForm] = useState(DEFAULT_MEDIA_FORM)
  const [storeInvForm, setStoreInvForm] = useState(DEFAULT_STORE_INV_FORM)

  const flatCategories = useMemo(() => flattenTree(categoryTree), [categoryTree])
  const selectedCategory = flatCategories.find((item) => String(item.id) === String(selectedCategoryId)) || null
  const selectedProduct = productsPage.content.find((item) => String(item.id) === String(selectedProductId)) || null

  async function load(nextFilters = productFilters) {
    setLoading(true)
    setError('')
    try {
      const [tree, storePage] = await Promise.all([
        getCommerceCategoryTree('en'),
        listStores({ page: 0, size: 100, sortBy: 'name', direction: 'asc' }).catch(() => ({ content: [] }))
      ])
      setCategoryTree(tree || [])
      setStores(storePage?.content || [])
      await loadProducts(nextFilters)
    } catch (err) {
      setError(err.message || 'Failed to load commerce catalog.')
    } finally {
      setLoading(false)
    }
  }

  async function loadProducts(nextFilters = productFilters) {
    const response = await fetchProducts(nextFilters)
    setProductsPage(response || { content: [], totalElements: 0 })
    if (selectedProductId && !(response?.content || []).some((item) => String(item.id) === String(selectedProductId))) {
      setSelectedProductId('')
      setProductForm(DEFAULT_PRODUCT_FORM)
    }
  }

  async function fetchProducts(nextFilters) {
    return listCommerceProducts({
      page: 0,
      size: 80,
      sortBy: productSort.sortBy,
      direction: productSort.direction,
      query: nextFilters.query || undefined,
      categoryId: nextFilters.categoryId || undefined,
      brand: nextFilters.brand || undefined,
      minPrice: nextFilters.minPrice !== '' ? Number(nextFilters.minPrice) : undefined,
      maxPrice: nextFilters.maxPrice !== '' ? Number(nextFilters.maxPrice) : undefined,
      minRating: nextFilters.minRating !== '' ? Number(nextFilters.minRating) : undefined,
      storeId: nextFilters.storeId || undefined,
      flashSale: nextFilters.flashSale === 'true' ? true : nextFilters.flashSale === 'false' ? false : undefined,
      trending: nextFilters.trending === 'true' ? true : nextFilters.trending === 'false' ? false : undefined
    })
  }

  function handleProductSort(field, dir) {
    setProductSort({ sortBy: field, direction: dir })
  }

  useEffect(() => {
    load()
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [])

  useEffect(() => {
    loadProducts(productFilters)
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [productSort.sortBy, productSort.direction])

  useEffect(() => {
    if (!selectedProduct?.variants?.length) {
      setVariantDrafts({})
      return
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
            stock: String(variant.stock ?? 0)
          }
        ])
      )
    )
  }, [selectedProductId, selectedProduct])

  function resetCategoryForm() {
    setSelectedCategoryId('')
    setCategoryForm(DEFAULT_CATEGORY_FORM)
  }

  function resetProductForm() {
    setSelectedProductId('')
    setProductForm(DEFAULT_PRODUCT_FORM)
    setVariantForm(DEFAULT_VARIANT_FORM)
    setVariantDrafts({})
    setMediaForm(DEFAULT_MEDIA_FORM)
    setStoreInvForm(DEFAULT_STORE_INV_FORM)
  }

  function selectCategory(categoryId) {
    setFlash('')
    setError('')
    setSelectedCategoryId(categoryId)
    const category = flatCategories.find((item) => String(item.id) === String(categoryId))
    if (!category) {
      setCategoryForm(DEFAULT_CATEGORY_FORM)
      return
    }
    setCategoryForm({
      parentId: category.parentId || '',
      name: category.name || '',
      description: category.description || '',
      classificationCode: category.classificationCode || '',
      managerId: category.managerId || ''
    })
  }

  async function saveCategory() {
    setSaving(true)
    setFlash('')
    setError('')
    try {
      const payload = {
        name: categoryForm.name.trim(),
        description: categoryForm.description?.trim() || null,
        classificationCode: categoryForm.classificationCode?.trim() || null,
        parentId: categoryForm.parentId || null,
        managerId: categoryForm.managerId?.trim() || null
      }

      if (selectedCategoryId) {
        await updateCommerceCategory(selectedCategoryId, payload)
        setFlash('Category updated.')
      } else {
        await createCommerceCategory(payload)
        setFlash('Category created.')
      }
      await load()
      resetCategoryForm()
    } catch (err) {
      setError(err.message || 'Unable to save category.')
    } finally {
      setSaving(false)
    }
  }

  function selectProduct(productId) {
    setFlash('')
    setError('')
    setSelectedProductId(productId)
    const product = productsPage.content.find((item) => String(item.id) === String(productId))
    if (!product) {
      setProductForm(DEFAULT_PRODUCT_FORM)
      return
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
      attributesJson: JSON.stringify(product.attributes || {}, null, 2)
    })

    setVariantForm(DEFAULT_VARIANT_FORM)
    setMediaForm(DEFAULT_MEDIA_FORM)
    setStoreInvForm((current) => ({
      ...DEFAULT_STORE_INV_FORM,
      storeId: current.storeId || '',
      stock: '0',
      storePrice: ''
    }))
  }

  async function saveProduct() {
    setSaving(true)
    setFlash('')
    setError('')
    try {
      const attributes = parseJson(productForm.attributesJson) || {}
      const payload = {
        name: productForm.name.trim(),
        description: productForm.shortDescription?.trim() || null,
        categoryId: productForm.categoryId || null,
        category: productForm.category?.trim() || null,
        brand: productForm.brand?.trim() || null,
        price: asNumber(productForm.price, 'Price'),
        attributes,
        allowBackorder: Boolean(productForm.allowBackorder),
        flashSale: Boolean(productForm.flashSale),
        trending: Boolean(productForm.trending),
        bestSeller: Boolean(productForm.bestSeller),
        shortDescription: productForm.shortDescription?.trim() || null,
        longDescription: productForm.longDescription?.trim() || null,
        seo: {
          slug: productForm.seoSlug?.trim() || null,
          metaTitle: productForm.seoTitle?.trim() || null,
          metaDescription: productForm.seoDescription?.trim() || null
        },
        variants: null,
        media: null,
        inventory: null
      }

      if (selectedProductId) {
        await updateCommerceProduct(selectedProductId, payload)
        setFlash('Product updated.')
      } else {
        const created = await createCommerceProduct(payload)
        setFlash('Product created.')
        setSelectedProductId(created.id)
      }

      await loadProducts(productFilters)
      if (selectedProductId) {
        const refreshed = await getCommerceProduct(selectedProductId)
        setProductsPage((current) => ({
          ...current,
          content: current.content.map((item) => (String(item.id) === String(selectedProductId) ? refreshed : item))
        }))
      }
    } catch (err) {
      setError(err.message || 'Unable to save product.')
    } finally {
      setSaving(false)
    }
  }

  async function toggleProductActive(nextActive) {
    if (!selectedProductId) return
    setSaving(true)
    setFlash('')
    setError('')
    try {
      const updated = await patchCommerceProduct(selectedProductId, { active: Boolean(nextActive) })
      setFlash(`Product ${nextActive ? 'activated' : 'deactivated'}.`)
      setProductsPage((current) => ({
        ...current,
        content: current.content.map((item) => (String(item.id) === String(selectedProductId) ? updated : item))
      }))
    } catch (err) {
      setError(err.message || 'Unable to update active flag.')
    } finally {
      setSaving(false)
    }
  }

  async function removeProduct() {
    if (!selectedProductId) return
    setSaving(true)
    setFlash('')
    setError('')
    try {
      await deleteCommerceProduct(selectedProductId)
      setFlash('Product deleted (soft).')
      resetProductForm()
      await loadProducts(productFilters)
    } catch (err) {
      setError(err.message || 'Unable to delete product.')
    } finally {
      setSaving(false)
    }
  }

  async function addVariant() {
    if (!selectedProductId) return
    setSaving(true)
    setFlash('')
    setError('')
    try {
      const payload = {
        sku: variantForm.sku.trim(),
        color: variantForm.color?.trim() || null,
        size: variantForm.size?.trim() || null,
        price: variantForm.price === '' ? null : asNumber(variantForm.price, 'Variant price'),
        stock: variantForm.stock === '' ? 0 : asInt(variantForm.stock, 'Stock')
      }
      await addCommerceVariant(selectedProductId, payload)
      const refreshed = await getCommerceProduct(selectedProductId)
      setProductsPage((current) => ({
        ...current,
        content: current.content.map((item) => (String(item.id) === String(selectedProductId) ? refreshed : item))
      }))
      setVariantForm(DEFAULT_VARIANT_FORM)
      setFlash('Variant added.')
    } catch (err) {
      setError(err.message || 'Unable to add variant.')
    } finally {
      setSaving(false)
    }
  }

  async function saveVariant(variant) {
    const variantId = variant?.id
    if (!selectedProductId || !variantId) return
    setSaving(true)
    setFlash('')
    setError('')
    try {
      const draft = variantDrafts[variantId]
      if (!draft?.sku?.trim()) {
        throw new Error('Variant SKU is required.')
      }
      const payload = {
        sku: draft.sku.trim(),
        color: draft.color?.trim() || null,
        size: draft.size?.trim() || null,
        attributes: variant.attributes || null,
        price: draft.price === '' ? null : asNumber(draft.price, 'Price override'),
        stock: draft.stock === '' ? 0 : asInt(draft.stock, 'Stock') ?? 0
      }
      await updateCommerceVariant(variantId, payload)
      const refreshed = await getCommerceProduct(selectedProductId)
      setProductsPage((current) => ({
        ...current,
        content: current.content.map((item) => (String(item.id) === String(selectedProductId) ? refreshed : item))
      }))
      setFlash('Variant updated.')
    } catch (err) {
      setError(err.message || 'Unable to update variant.')
    } finally {
      setSaving(false)
    }
  }

  async function addMedia() {
    if (!selectedProductId) return
    setSaving(true)
    setFlash('')
    setError('')
    try {
      const payload = {
        mediaType: mediaForm.mediaType,
        url: mediaForm.url.trim(),
        sortOrder: asInt(mediaForm.sortOrder, 'Sort order') ?? 0,
        isPrimary: Boolean(mediaForm.isPrimary)
      }
      await addCommerceMedia(selectedProductId, payload)
      const refreshed = await getCommerceProduct(selectedProductId)
      setProductsPage((current) => ({
        ...current,
        content: current.content.map((item) => (String(item.id) === String(selectedProductId) ? refreshed : item))
      }))
      setMediaForm(DEFAULT_MEDIA_FORM)
      setFlash('Media added.')
    } catch (err) {
      setError(err.message || 'Unable to add media.')
    } finally {
      setSaving(false)
    }
  }

  async function upsertStoreInventory() {
    if (!selectedProductId) return
    setSaving(true)
    setFlash('')
    setError('')
    try {
      const payload = {
        storeId: storeInvForm.storeId,
        stock: asInt(storeInvForm.stock, 'Stock') ?? 0,
        storePrice: asNumber(storeInvForm.storePrice, 'Store price')
      }
      await upsertCommerceStoreInventory(selectedProductId, payload)
      const refreshed = await getCommerceProduct(selectedProductId)
      setProductsPage((current) => ({
        ...current,
        content: current.content.map((item) => (String(item.id) === String(selectedProductId) ? refreshed : item))
      }))
      setFlash('Store inventory upserted.')
    } catch (err) {
      setError(err.message || 'Unable to upsert store inventory.')
    } finally {
      setSaving(false)
    }
  }

  if (loading) {
    return <Spinner label="Loading commerce catalog..." />
  }

  return (
    <div className="page">
      <div className="page-head">
        <h2>Commerce catalog</h2>
        <p>Manage platform products, categories, variants, media, and per-store inventory pricing.</p>
      </div>

      {flash ? <div className="alert alert-success">{flash}</div> : null}
      {error ? <div className="alert alert-error">{error}</div> : null}

      {/* ─── Row 1: Categories sidebar + Product listing table ─── */}
      <div className="catalog-layout">
        <section className="panel catalog-sidebar">
          <div className="section-head">
            <div>
              <h3>Categories</h3>
              <p>Create and update platform category taxonomy (max depth enforced server-side).</p>
            </div>
            <button className="btn btn-outline btn-sm" onClick={() => load()}>
              Refresh
            </button>
          </div>

          <div className="filters">
            <label>
              Select category
              <select value={selectedCategoryId} onChange={(event) => selectCategory(event.target.value)}>
                <option value="">New category...</option>
                {flatCategories.map((category) => (
                  <option key={category.id} value={category.id}>
                    {`${'  '.repeat(category.depth)}${category.name}`}
                  </option>
                ))}
              </select>
            </label>
          </div>

          <div className="form-grid">
            <label>
              Name
              <input
                value={categoryForm.name}
                onChange={(event) => setCategoryForm((current) => ({ ...current, name: event.target.value }))}
                placeholder="Electronics"
                required
              />
            </label>
            <label>
              Parent
              <select
                value={categoryForm.parentId}
                onChange={(event) => setCategoryForm((current) => ({ ...current, parentId: event.target.value }))}
              >
                <option value="">(Root)</option>
                {flatCategories
                  .filter((category) => String(category.id) !== String(selectedCategoryId))
                  .map((category) => (
                    <option key={category.id} value={category.id}>
                      {`${'  '.repeat(category.depth)}${category.name}`}
                    </option>
                  ))}
              </select>
            </label>
            <label className="span-2">
              Description
              <input
                value={categoryForm.description}
                onChange={(event) => setCategoryForm((current) => ({ ...current, description: event.target.value }))}
                placeholder="Optional description"
              />
            </label>
            <label>
              Classification code
              <input
                value={categoryForm.classificationCode}
                onChange={(event) => setCategoryForm((current) => ({ ...current, classificationCode: event.target.value }))}
                placeholder="Optional"
              />
            </label>
            <label>
              Manager id
              <input
                value={categoryForm.managerId}
                onChange={(event) => setCategoryForm((current) => ({ ...current, managerId: event.target.value }))}
                placeholder="Optional UUID"
              />
            </label>
          </div>

          <div className="inline-actions">
            <button className="btn btn-primary" disabled={saving || !categoryForm.name.trim()} onClick={saveCategory}>
              {saving ? 'Saving...' : selectedCategoryId ? 'Update category' : 'Create category'}
            </button>
            <button className="btn btn-outline" disabled={saving} onClick={resetCategoryForm}>
              Reset
            </button>
          </div>
        </section>

        <section className="panel catalog-main">
          <div className="section-head">
            <div>
              <h3>Products</h3>
              <p>{productsPage.totalElements || productsPage.content.length} products in catalog.</p>
            </div>
            <button className="btn btn-primary btn-sm" onClick={resetProductForm}>
              New product
            </button>
          </div>

          <div className="filters">
            <label>
              Search
              <input
                value={productFilters.query}
                onChange={(event) => setProductFilters((current) => ({ ...current, query: event.target.value }))}
                placeholder="Name, description, slug..."
              />
            </label>
            <label>
              Category
              <select
                value={productFilters.categoryId}
                onChange={(event) => setProductFilters((current) => ({ ...current, categoryId: event.target.value }))}
              >
                <option value="">All</option>
                {flatCategories.map((category) => (
                  <option key={category.id} value={category.id}>
                    {`${'  '.repeat(category.depth)}${category.name}`}
                  </option>
                ))}
              </select>
            </label>
            <button className="btn btn-outline" onClick={() => loadProducts(productFilters)}>
              Apply
            </button>
            <button className="filter-toggle" type="button" onClick={() => setShowAdvancedFilters((v) => !v)}>
              {showAdvancedFilters ? 'Hide filters' : 'More filters'}
            </button>
          </div>

          {showAdvancedFilters && (
            <div className="advanced-filters">
              <label>
                Brand
                <input
                  value={productFilters.brand}
                  onChange={(event) => setProductFilters((current) => ({ ...current, brand: event.target.value }))}
                  placeholder="e.g. Nike"
                />
              </label>
              <label>
                Min price
                <input
                  type="number"
                  min="0"
                  step="0.01"
                  value={productFilters.minPrice}
                  onChange={(event) => setProductFilters((current) => ({ ...current, minPrice: event.target.value }))}
                  placeholder="0.00"
                />
              </label>
              <label>
                Max price
                <input
                  type="number"
                  min="0"
                  step="0.01"
                  value={productFilters.maxPrice}
                  onChange={(event) => setProductFilters((current) => ({ ...current, maxPrice: event.target.value }))}
                  placeholder="9999.99"
                />
              </label>
              <label>
                Min rating
                <input
                  type="number"
                  min="0"
                  max="5"
                  step="0.1"
                  value={productFilters.minRating}
                  onChange={(event) => setProductFilters((current) => ({ ...current, minRating: event.target.value }))}
                  placeholder="0-5"
                />
              </label>
              <label>
                Store
                <select
                  value={productFilters.storeId}
                  onChange={(event) => setProductFilters((current) => ({ ...current, storeId: event.target.value }))}
                >
                  <option value="">All stores</option>
                  {stores.map((store) => (
                    <option key={store.id} value={store.id}>
                      {store.name}
                    </option>
                  ))}
                </select>
              </label>
              <label>
                Flash sale
                <select
                  value={productFilters.flashSale}
                  onChange={(event) => setProductFilters((current) => ({ ...current, flashSale: event.target.value }))}
                >
                  <option value="">Any</option>
                  <option value="true">Yes</option>
                  <option value="false">No</option>
                </select>
              </label>
              <label>
                Trending
                <select
                  value={productFilters.trending}
                  onChange={(event) => setProductFilters((current) => ({ ...current, trending: event.target.value }))}
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
                  <SortableHeader label="Product" field="name" sortBy={productSort.sortBy} direction={productSort.direction} onSort={handleProductSort} />
                  <SortableHeader label="Category" field="category" sortBy={productSort.sortBy} direction={productSort.direction} onSort={handleProductSort} />
                  <SortableHeader label="Brand" field="brand" sortBy={productSort.sortBy} direction={productSort.direction} onSort={handleProductSort} />
                  <SortableHeader label="Price" field="price" sortBy={productSort.sortBy} direction={productSort.direction} onSort={handleProductSort} />
                  <SortableHeader label="Rating" field="averageRating" sortBy={productSort.sortBy} direction={productSort.direction} onSort={handleProductSort} />
                  <SortableHeader label="Reviews" field="reviewCount" sortBy={productSort.sortBy} direction={productSort.direction} onSort={handleProductSort} />
                  <SortableHeader label="Popularity" field="popularityScore" sortBy={productSort.sortBy} direction={productSort.direction} onSort={handleProductSort} />
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
                          <div className="subtle-meta">{product.shortDescription.substring(0, 60)}{product.shortDescription.length > 60 ? '...' : ''}</div>
                        )}
                      </td>
                      <td>{product.category || <span className="subtle-meta">—</span>}</td>
                      <td>{product.brand || <span className="subtle-meta">—</span>}</td>
                      <td><strong>{formatCurrency(product.price)}</strong></td>
                      <td>{product.averageRating != null ? `${Number(product.averageRating).toFixed(1)} ★` : <span className="subtle-meta">—</span>}</td>
                      <td>{product.reviewCount ?? 0}</td>
                      <td>{product.popularityScore ?? 0}</td>
                      <td>
                        <div className="tag-group">
                          {product.flashSale && <span className="badge badge-warning">Flash</span>}
                          {product.trending && <span className="badge badge-info">Trending</span>}
                          {product.bestSeller && <span className="badge badge-success">Best</span>}
                          {product.allowBackorder && <span className="badge badge-outline">Backorder</span>}
                          {!product.flashSale && !product.trending && !product.bestSeller && !product.allowBackorder && (
                            <span className="subtle-meta">—</span>
                          )}
                        </div>
                      </td>
                      <td><span className="badge badge-muted">{product.status}</span></td>
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
                            onClick={() => selectProduct(product.id)}
                            title="Edit product"
                          >
                            Edit
                          </button>
                          <button
                            className="btn btn-sm btn-outline btn-danger"
                            type="button"
                            disabled={saving}
                            onClick={() => {
                              if (window.confirm(`Delete product "${product.name}"?`)) {
                                setSelectedProductId(product.id)
                                deleteCommerceProduct(product.id).then(() => {
                                  setFlash('Product deleted.')
                                  resetProductForm()
                                  loadProducts(productFilters)
                                }).catch((err) => {
                                  setError(err.message || 'Failed to delete product.')
                                })
                              }
                            }}
                            title="Delete product"
                          >
                            Delete
                          </button>
                        </div>
                      </td>
                    </tr>
                  ))
                ) : (
                  <tr>
                    <td colSpan="13" className="empty-row">No products found.</td>
                  </tr>
                )}
              </tbody>
            </table>
          </div>
        </section>
      </div>

      {/* ─── Row 2: Product Form (full width) ─── */}
      <section className="panel">
        <div className="section-head">
          <div>
            <h3>{selectedProductId ? `Edit product — ${selectedProduct?.name || ''}` : 'Create product'}</h3>
            <p>{selectedProductId ? 'Update product details, then manage variants, media, and store inventory below.' : 'Fill in the fields then press create.'}</p>
          </div>
          {selectedProductId && (
            <button className="btn btn-outline btn-sm" onClick={resetProductForm}>
              Cancel edit
            </button>
          )}
        </div>

        {/* ── Product details grid: 2 columns ── */}
        <div className="product-form-grid">

          {/* LEFT COLUMN: Core info + Descriptions */}
          <div className="product-form-col">
            <fieldset className="form-fieldset">
              <legend>Basic information</legend>
              <div className="form-grid">
                <label className="span-2">
                  Name <span className="required">*</span>
                  <input
                    value={productForm.name}
                    onChange={(event) => setProductForm((current) => ({ ...current, name: event.target.value }))}
                    placeholder="Product name"
                    required
                  />
                </label>
                <label>
                  Category
                  <select
                    value={productForm.categoryId}
                    onChange={(event) => setProductForm((current) => ({ ...current, categoryId: event.target.value }))}
                  >
                    <option value="">(Use category name)</option>
                    {flatCategories.map((category) => (
                      <option key={category.id} value={category.id}>
                        {`${'  '.repeat(category.depth)}${category.name}`}
                      </option>
                    ))}
                  </select>
                </label>
                <label>
                  Category name
                  <input
                    value={productForm.category}
                    onChange={(event) => setProductForm((current) => ({ ...current, category: event.target.value }))}
                    placeholder="Fallback category name"
                  />
                </label>
                <label>
                  Brand
                  <input
                    value={productForm.brand}
                    onChange={(event) => setProductForm((current) => ({ ...current, brand: event.target.value }))}
                    placeholder="e.g. Nike, Apple"
                  />
                </label>
                <label>
                  Base price <span className="required">*</span>
                  <input
                    type="number"
                    min="0"
                    step="0.01"
                    value={productForm.price}
                    onChange={(event) => setProductForm((current) => ({ ...current, price: event.target.value }))}
                    placeholder="19.99"
                    required
                  />
                </label>
              </div>
            </fieldset>

            <fieldset className="form-fieldset">
              <legend>Descriptions</legend>
              <div className="form-grid">
                <label className="span-2">
                  Short description
                  <input
                    value={productForm.shortDescription}
                    onChange={(event) => setProductForm((current) => ({ ...current, shortDescription: event.target.value }))}
                    placeholder="Shown in listing cards"
                  />
                </label>
                <label className="span-2">
                  Long description
                  <textarea
                    rows="5"
                    value={productForm.longDescription}
                    onChange={(event) => setProductForm((current) => ({ ...current, longDescription: event.target.value }))}
                    placeholder="Full product description shown on the product page"
                  />
                </label>
              </div>
            </fieldset>

            <fieldset className="form-fieldset">
              <legend>Attributes (JSON)</legend>
              <textarea
                rows="5"
                value={productForm.attributesJson}
                onChange={(event) => setProductForm((current) => ({ ...current, attributesJson: event.target.value }))}
                placeholder='{"material": "leather", "warranty": "2 years"}'
                className="mono"
              />
            </fieldset>
          </div>

          {/* RIGHT COLUMN: SEO + Flags + Status */}
          <div className="product-form-col">
            <fieldset className="form-fieldset">
              <legend>SEO</legend>
              <div className="form-grid">
                <label className="span-2">
                  Slug
                  <input
                    value={productForm.seoSlug}
                    onChange={(event) => setProductForm((current) => ({ ...current, seoSlug: event.target.value }))}
                    placeholder="my-product-slug"
                  />
                </label>
                <label className="span-2">
                  Meta title
                  <input
                    value={productForm.seoTitle}
                    onChange={(event) => setProductForm((current) => ({ ...current, seoTitle: event.target.value }))}
                    placeholder="Page title for search engines"
                  />
                </label>
                <label className="span-2">
                  Meta description
                  <textarea
                    rows="3"
                    value={productForm.seoDescription}
                    onChange={(event) => setProductForm((current) => ({ ...current, seoDescription: event.target.value }))}
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
                    onChange={(event) => setProductForm((current) => ({ ...current, allowBackorder: event.target.checked }))}
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
                    onChange={(event) => setProductForm((current) => ({ ...current, flashSale: event.target.checked }))}
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
                    onChange={(event) => setProductForm((current) => ({ ...current, trending: event.target.checked }))}
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
                    onChange={(event) => setProductForm((current) => ({ ...current, bestSeller: event.target.checked }))}
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
                    <strong>{selectedProduct.averageRating != null ? `${Number(selectedProduct.averageRating).toFixed(1)} ★` : '—'}</strong>
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
                    <strong><span className={`badge ${selectedProduct.active ? 'badge-success' : 'badge-muted'}`}>{selectedProduct.status}</span></strong>
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
            onClick={saveProduct}
          >
            {saving ? 'Saving...' : selectedProductId ? 'Update product' : 'Create product'}
          </button>
          {selectedProductId ? (
            <>
              <button className="btn btn-outline" disabled={saving} onClick={() => toggleProductActive(!selectedProduct?.active)}>
                {selectedProduct?.active ? 'Deactivate' : 'Activate'}
              </button>
              <button className="btn btn-outline btn-danger" disabled={saving} onClick={removeProduct}>
                Delete
              </button>
            </>
          ) : null}
        </div>

          {selectedProduct ? (
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
                              onChange={(e) =>
                                setVariantDrafts((current) => ({
                                  ...current,
                                  [variant.id]: { ...(current[variant.id] || {}), sku: e.target.value }
                                }))
                              }
                            />
                          </td>
                          <td>
                            <input
                              value={variantDrafts[variant.id]?.color ?? variant.color ?? ''}
                              onChange={(e) =>
                                setVariantDrafts((current) => ({
                                  ...current,
                                  [variant.id]: { ...(current[variant.id] || {}), color: e.target.value }
                                }))
                              }
                              placeholder="Optional"
                            />
                          </td>
                          <td>
                            <input
                              value={variantDrafts[variant.id]?.size ?? variant.size ?? ''}
                              onChange={(e) =>
                                setVariantDrafts((current) => ({
                                  ...current,
                                  [variant.id]: { ...(current[variant.id] || {}), size: e.target.value }
                                }))
                              }
                              placeholder="Optional"
                            />
                          </td>
                          <td>
                            <input
                              value={
                                variantDrafts[variant.id]?.price ??
                                (variant.priceOverride != null ? variant.priceOverride.toString?.() : '')
                              }
                              onChange={(e) =>
                                setVariantDrafts((current) => ({
                                  ...current,
                                  [variant.id]: { ...(current[variant.id] || {}), price: e.target.value }
                                }))
                              }
                              placeholder="Optional"
                            />
                          </td>
                          <td>
                            <input
                              value={variantDrafts[variant.id]?.stock ?? String(variant.stock ?? 0)}
                              onChange={(e) =>
                                setVariantDrafts((current) => ({
                                  ...current,
                                  [variant.id]: { ...(current[variant.id] || {}), stock: e.target.value }
                                }))
                              }
                            />
                          </td>
                          <td>
                            <button className="btn btn-outline btn-sm" disabled={saving} onClick={() => saveVariant(variant)}>
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
                <label>
                  SKU
                  <input value={variantForm.sku} onChange={(e) => setVariantForm((c) => ({ ...c, sku: e.target.value }))} placeholder="SKU-001-BLK" />
                </label>
                <label>
                  Color
                  <input value={variantForm.color} onChange={(e) => setVariantForm((c) => ({ ...c, color: e.target.value }))} placeholder="Black" />
                </label>
                <label>
                  Size
                  <input value={variantForm.size} onChange={(e) => setVariantForm((c) => ({ ...c, size: e.target.value }))} placeholder="M" />
                </label>
                <label>
                  Price override
                  <input value={variantForm.price} onChange={(e) => setVariantForm((c) => ({ ...c, price: e.target.value }))} placeholder="Optional" />
                </label>
                <label>
                  Stock
                  <input value={variantForm.stock} onChange={(e) => setVariantForm((c) => ({ ...c, stock: e.target.value }))} />
                </label>
              </div>
              <button className="btn btn-outline" disabled={saving || !variantForm.sku.trim()} onClick={addVariant}>
                Add variant
              </button>

              <div className="divider" />

              <div className="section-head">
                <div>
                  <h3>Media</h3>
                  <p>Attach primary and secondary images to the product record.</p>
                </div>
              </div>

              {selectedProduct.media?.length ? (
                <div className="table-wrap">
                  <table>
                    <thead>
                      <tr>
                        <th>Type</th>
                        <th>URL</th>
                        <th>Primary</th>
                        <th>Sort</th>
                      </tr>
                    </thead>
                    <tbody>
                      {selectedProduct.media.map((item) => (
                        <tr key={item.id}>
                          <td>{item.mediaType}</td>
                          <td className="mono">{item.url}</td>
                          <td>{item.primary ? 'Yes' : 'No'}</td>
                          <td>{item.sortOrder}</td>
                        </tr>
                      ))}
                    </tbody>
                  </table>
                </div>
              ) : (
                <p className="empty-copy">No media attached.</p>
              )}

              <div className="form-grid">
                <label>
                  Type
                  <select value={mediaForm.mediaType} onChange={(e) => setMediaForm((c) => ({ ...c, mediaType: e.target.value }))}>
                    <option value="IMAGE">IMAGE</option>
                    <option value="VIDEO">VIDEO</option>
                  </select>
                </label>
                <label className="span-2">
                  URL
                  <input value={mediaForm.url} onChange={(e) => setMediaForm((c) => ({ ...c, url: e.target.value }))} placeholder="https://..." />
                </label>
                <label>
                  Sort order
                  <input value={mediaForm.sortOrder} onChange={(e) => setMediaForm((c) => ({ ...c, sortOrder: e.target.value }))} />
                </label>
                <label className="toggle">
                  <input type="checkbox" checked={mediaForm.isPrimary} onChange={(e) => setMediaForm((c) => ({ ...c, isPrimary: e.target.checked }))} />
                  Primary
                </label>
              </div>
              <button className="btn btn-outline" disabled={saving || !mediaForm.url.trim()} onClick={addMedia}>
                Add media
              </button>

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
                <label className="span-2">
                  Store
                  <select
                    value={storeInvForm.storeId}
                    onChange={(e) => setStoreInvForm((c) => ({ ...c, storeId: e.target.value }))}
                    disabled={!stores.length}
                  >
                    <option value="">Select store...</option>
                    {stores.map((store) => (
                      <option key={store.id} value={store.id}>
                        {store.name}
                      </option>
                    ))}
                  </select>
                </label>
                <label>
                  Stock
                  <input value={storeInvForm.stock} onChange={(e) => setStoreInvForm((c) => ({ ...c, stock: e.target.value }))} />
                </label>
                <label>
                  Store price
                  <input value={storeInvForm.storePrice} onChange={(e) => setStoreInvForm((c) => ({ ...c, storePrice: e.target.value }))} placeholder="19.99" />
                </label>
              </div>
              <button className="btn btn-outline" disabled={saving || !storeInvForm.storeId || !storeInvForm.storePrice} onClick={upsertStoreInventory}>
                Upsert store inventory
              </button>
            </>
          ) : null}
      </section>
    </div>
  )
}
