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
  const [productFilters, setProductFilters] = useState({ query: '', categoryId: '' })
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
      sortBy: 'createdAt',
      direction: 'desc',
      query: nextFilters.query || undefined,
      categoryId: nextFilters.categoryId || undefined
    })
  }

  useEffect(() => {
    load()
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [])

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

      <div className="panel-grid">
        <section className="panel">
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

        <section className="panel">
          <div className="section-head">
            <div>
              <h3>Products</h3>
              <p>Create products, adjust storefront metadata, and attach variants/media/inventory.</p>
            </div>
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
            <button className="btn btn-outline" onClick={resetProductForm}>
              New
            </button>
          </div>

          <div className="table-wrap">
            <table>
              <thead>
                <tr>
                  <th>Product</th>
                  <th>Category</th>
                  <th>Brand</th>
                  <th>Price</th>
                  <th>Status</th>
                  <th>Active</th>
                </tr>
              </thead>
              <tbody>
                {productsPage.content.length ? (
                  productsPage.content.map((product) => (
                    <tr
                      key={product.id}
                      className={String(product.id) === String(selectedProductId) ? 'row-selected' : ''}
                      onClick={() => selectProduct(product.id)}
                      role="button"
                      tabIndex={0}
                    >
                      <td>
                        <strong>{product.name}</strong>
                        <div className="subtle-meta mono">{product.id}</div>
                      </td>
                      <td>{product.category || '-'}</td>
                      <td>{product.brand || '-'}</td>
                      <td>{formatCurrency(product.price)}</td>
                      <td>{product.status}</td>
                      <td>
                        <span className={`badge ${product.active ? 'badge-success' : 'badge-muted'}`}>
                          {product.active ? 'Yes' : 'No'}
                        </span>
                      </td>
                    </tr>
                  ))
                ) : (
                  <tr>
                    <td colSpan="6" className="empty-row">No products found.</td>
                  </tr>
                )}
              </tbody>
            </table>
          </div>

          <div className="form-grid">
            <label className="span-2">
              Name
              <input
                value={productForm.name}
                onChange={(event) => setProductForm((current) => ({ ...current, name: event.target.value }))}
                placeholder="Product name"
                required
              />
            </label>
            <label>
              Category (by id)
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
                placeholder="Optional"
              />
            </label>
            <label>
              Base price
              <input
                value={productForm.price}
                onChange={(event) => setProductForm((current) => ({ ...current, price: event.target.value }))}
                placeholder="19.99"
                required
              />
            </label>
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
                rows="4"
                value={productForm.longDescription}
                onChange={(event) => setProductForm((current) => ({ ...current, longDescription: event.target.value }))}
                placeholder="Full product description"
              />
            </label>
            <label>
              SEO slug
              <input
                value={productForm.seoSlug}
                onChange={(event) => setProductForm((current) => ({ ...current, seoSlug: event.target.value }))}
                placeholder="my-product-slug"
              />
            </label>
            <label>
              SEO title
              <input
                value={productForm.seoTitle}
                onChange={(event) => setProductForm((current) => ({ ...current, seoTitle: event.target.value }))}
                placeholder="Optional"
              />
            </label>
            <label className="span-2">
              SEO description
              <input
                value={productForm.seoDescription}
                onChange={(event) => setProductForm((current) => ({ ...current, seoDescription: event.target.value }))}
                placeholder="Optional"
              />
            </label>
            <label className="span-2">
              Attributes (JSON)
              <textarea
                rows="6"
                value={productForm.attributesJson}
                onChange={(event) => setProductForm((current) => ({ ...current, attributesJson: event.target.value }))}
              />
            </label>
          </div>

          <div className="toggle-row">
            <label className="toggle">
              <input
                type="checkbox"
                checked={productForm.allowBackorder}
                onChange={(event) => setProductForm((current) => ({ ...current, allowBackorder: event.target.checked }))}
              />
              Allow backorder
            </label>
            <label className="toggle">
              <input
                type="checkbox"
                checked={productForm.flashSale}
                onChange={(event) => setProductForm((current) => ({ ...current, flashSale: event.target.checked }))}
              />
              Flash sale
            </label>
            <label className="toggle">
              <input
                type="checkbox"
                checked={productForm.trending}
                onChange={(event) => setProductForm((current) => ({ ...current, trending: event.target.checked }))}
              />
              Trending
            </label>
            <label className="toggle">
              <input
                type="checkbox"
                checked={productForm.bestSeller}
                onChange={(event) => setProductForm((current) => ({ ...current, bestSeller: event.target.checked }))}
              />
              Best seller
            </label>
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
                <button className="btn btn-outline" disabled={saving} onClick={removeProduct}>
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
    </div>
  )
}
