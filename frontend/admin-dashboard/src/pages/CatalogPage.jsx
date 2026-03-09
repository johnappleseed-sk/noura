import { useEffect, useMemo, useState } from 'react'
import {
  createCategory,
  deleteCategory,
  getCategoryTree,
  updateCategory
} from '../shared/api/endpoints/inventoryCategoriesApi'
import {
  createProduct,
  deleteProduct,
  listProducts,
  updateProduct
} from '../shared/api/endpoints/inventoryProductsApi'
import { useAuth } from '../features/auth/useAuth'
import { ADMIN_ROLES, hasAnyRole } from '../shared/auth/roles'
import { formatCurrency, formatDateTime } from '../shared/ui/formatters'
import { Spinner } from '../shared/ui/Spinner'

const DEFAULT_CATEGORY_FORM = {
  parentId: '',
  categoryCode: '',
  name: '',
  description: '',
  sortOrder: '0',
  active: true
}

const DEFAULT_PRODUCT_FORM = {
  sku: '',
  name: '',
  description: '',
  status: 'ACTIVE',
  basePrice: '',
  currencyCode: 'USD',
  widthCm: '',
  heightCm: '',
  lengthCm: '',
  weightKg: '',
  batchTracked: false,
  serialTracked: false,
  barcodeValue: '',
  qrCodeValue: '',
  active: true,
  categoryIds: [],
  primaryCategoryId: ''
}

function flattenCategoryTree(items = [], depth = 0) {
  return items.flatMap((item) => [
    { ...item, depth },
    ...flattenCategoryTree(item.children || [], depth + 1)
  ])
}

function toProductForm(product) {
  return {
    sku: product.sku || '',
    name: product.name || '',
    description: product.description || '',
    status: product.status || 'ACTIVE',
    basePrice: product.basePrice?.toString() || '',
    currencyCode: product.currencyCode || 'USD',
    widthCm: product.widthCm?.toString() || '',
    heightCm: product.heightCm?.toString() || '',
    lengthCm: product.lengthCm?.toString() || '',
    weightKg: product.weightKg?.toString() || '',
    batchTracked: Boolean(product.batchTracked),
    serialTracked: Boolean(product.serialTracked),
    barcodeValue: product.barcodeValue || '',
    qrCodeValue: product.qrCodeValue || '',
    active: Boolean(product.active),
    categoryIds: product.categories?.map((item) => item.id) || [],
    primaryCategoryId: product.primaryCategory?.id || ''
  }
}

export function CatalogPage() {
  const { auth } = useAuth()
  const canManage = hasAnyRole(auth?.roles, ADMIN_ROLES)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')
  const [flash, setFlash] = useState('')
  const [categoryTree, setCategoryTree] = useState([])
  const [productsPage, setProductsPage] = useState({ content: [], totalElements: 0 })
  const [selectedCategoryId, setSelectedCategoryId] = useState('')
  const [selectedProductId, setSelectedProductId] = useState('')
  const [categoryForm, setCategoryForm] = useState(DEFAULT_CATEGORY_FORM)
  const [productForm, setProductForm] = useState(DEFAULT_PRODUCT_FORM)
  const [productFilters, setProductFilters] = useState({
    query: '',
    categoryId: '',
    active: ''
  })
  const [categorySaving, setCategorySaving] = useState(false)
  const [productSaving, setProductSaving] = useState(false)

  const flatCategories = useMemo(() => flattenCategoryTree(categoryTree), [categoryTree])
  const selectedCategory = flatCategories.find((item) => item.id === selectedCategoryId) || null
  const selectedProduct = productsPage.content.find((item) => item.id === selectedProductId) || null

  async function load(nextFilters = productFilters) {
    setLoading(true)
    setError('')
    try {
      const [treeData, productData] = await Promise.all([
        getCategoryTree(false),
        listProducts({
          page: 0,
          size: 100,
          sortBy: 'createdAt',
          direction: 'desc',
          query: nextFilters.query || undefined,
          categoryId: nextFilters.categoryId || undefined,
          active: nextFilters.active === '' ? undefined : nextFilters.active
        })
      ])
      setCategoryTree(treeData || [])
      setProductsPage(productData || { content: [], totalElements: 0 })
    } catch (err) {
      setError(err.message || 'Failed to load catalog workspace.')
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => {
    load()
  }, [])

  function resetCategoryForm(parentId = '') {
    setSelectedCategoryId('')
    setCategoryForm({
      ...DEFAULT_CATEGORY_FORM,
      parentId
    })
  }

  function resetProductForm() {
    setSelectedProductId('')
    setProductForm(DEFAULT_PRODUCT_FORM)
  }

  function handleCategorySelect(category) {
    setSelectedCategoryId(category.id)
    setCategoryForm({
      parentId: category.parentId || '',
      categoryCode: category.categoryCode || '',
      name: category.name || '',
      description: category.description || '',
      sortOrder: String(category.sortOrder ?? 0),
      active: Boolean(category.active)
    })
  }

  function handleProductSelect(product) {
    setSelectedProductId(product.id)
    setProductForm(toProductForm(product))
  }

  function toggleProductCategory(categoryId) {
    setProductForm((current) => {
      const exists = current.categoryIds.includes(categoryId)
      const categoryIds = exists
        ? current.categoryIds.filter((item) => item !== categoryId)
        : [...current.categoryIds, categoryId]
      const primaryCategoryId = categoryIds.includes(current.primaryCategoryId)
        ? current.primaryCategoryId
        : categoryIds[0] || ''
      return {
        ...current,
        categoryIds,
        primaryCategoryId
      }
    })
  }

  async function saveCategory(event) {
    event.preventDefault()
    if (!canManage) return
    setCategorySaving(true)
    setFlash('')
    setError('')
    try {
      const payload = {
        parentId: categoryForm.parentId || null,
        categoryCode: categoryForm.categoryCode.trim(),
        name: categoryForm.name.trim(),
        description: categoryForm.description || null,
        sortOrder: Number(categoryForm.sortOrder || 0),
        active: categoryForm.active
      }
      const saved = selectedCategoryId
        ? await updateCategory(selectedCategoryId, payload)
        : await createCategory(payload)
      setFlash(selectedCategoryId ? 'Category updated.' : 'Category created.')
      await load()
      setSelectedCategoryId(saved.id)
      handleCategorySelect(saved)
    } catch (err) {
      setError(err.message || 'Failed to save category.')
    } finally {
      setCategorySaving(false)
    }
  }

  async function removeCategory() {
    if (!canManage || !selectedCategoryId) return
    if (!window.confirm('Delete this category?')) return
    setCategorySaving(true)
    setFlash('')
    setError('')
    try {
      await deleteCategory(selectedCategoryId)
      setFlash('Category deleted.')
      resetCategoryForm()
      await load()
    } catch (err) {
      setError(err.message || 'Failed to delete category.')
    } finally {
      setCategorySaving(false)
    }
  }

  async function saveProduct(event) {
    event.preventDefault()
    if (!canManage) return
    setProductSaving(true)
    setFlash('')
    setError('')
    try {
      const payload = {
        sku: productForm.sku.trim(),
        name: productForm.name.trim(),
        description: productForm.description || null,
        status: productForm.status.trim(),
        basePrice: Number(productForm.basePrice || 0),
        currencyCode: productForm.currencyCode.trim().toUpperCase(),
        widthCm: productForm.widthCm === '' ? null : Number(productForm.widthCm),
        heightCm: productForm.heightCm === '' ? null : Number(productForm.heightCm),
        lengthCm: productForm.lengthCm === '' ? null : Number(productForm.lengthCm),
        weightKg: productForm.weightKg === '' ? null : Number(productForm.weightKg),
        batchTracked: productForm.batchTracked,
        serialTracked: productForm.serialTracked,
        barcodeValue: productForm.barcodeValue || null,
        qrCodeValue: productForm.qrCodeValue || null,
        active: productForm.active,
        categoryIds: productForm.categoryIds,
        primaryCategoryId: productForm.primaryCategoryId || null
      }
      const saved = selectedProductId
        ? await updateProduct(selectedProductId, payload)
        : await createProduct(payload)
      setFlash(selectedProductId ? 'Product updated.' : 'Product created.')
      await load(productFilters)
      setSelectedProductId(saved.id)
      setProductForm(toProductForm(saved))
    } catch (err) {
      setError(err.message || 'Failed to save product.')
    } finally {
      setProductSaving(false)
    }
  }

  async function removeProduct() {
    if (!canManage || !selectedProductId) return
    if (!window.confirm('Delete this product?')) return
    setProductSaving(true)
    setFlash('')
    setError('')
    try {
      await deleteProduct(selectedProductId)
      setFlash('Product deleted.')
      resetProductForm()
      await load(productFilters)
    } catch (err) {
      setError(err.message || 'Failed to delete product.')
    } finally {
      setProductSaving(false)
    }
  }

  async function applyProductFilters(event) {
    event.preventDefault()
    await load(productFilters)
  }

  if (loading) {
    return <Spinner label="Loading catalog workspace..." />
  }

  return (
    <div className="page">
      <div className="page-head">
        <h2>Catalog workspace</h2>
        <p>Manage hierarchical categories and inventory products with batch and serial tracking options.</p>
      </div>

      {flash ? <div className="alert alert-success">{flash}</div> : null}
      {error ? <div className="alert alert-error">{error}</div> : null}
      {!canManage ? <div className="alert alert-error">Your role is read-only in this workspace. Switch to an admin account to create, update, or delete catalog records.</div> : null}

      <div className="workspace-grid">
        <section className="panel">
          <div className="section-head">
            <div>
              <h3>Category tree</h3>
              <p>Unlimited hierarchy rendered from the live category tree endpoint.</p>
            </div>
            <button className="btn btn-outline btn-sm" type="button" onClick={() => resetCategoryForm(selectedCategoryId)}>
              New category
            </button>
          </div>

          <div className="selection-list">
            {flatCategories.length ? (
              flatCategories.map((category) => (
                <button
                  key={category.id}
                  type="button"
                  className={`selection-item ${selectedCategoryId === category.id ? 'active' : ''}`}
                  onClick={() => handleCategorySelect(category)}
                >
                  <span className="selection-indent" style={{ paddingLeft: `${category.depth * 18}px` }}>
                    <strong>{category.name}</strong>
                    <small className="mono">{category.categoryCode}</small>
                  </span>
                  <span className={`badge ${category.active ? 'badge-success' : 'badge-muted'}`}>
                    {category.active ? 'Active' : 'Hidden'}
                  </span>
                </button>
              ))
            ) : (
              <p className="empty-copy">No categories exist yet.</p>
            )}
          </div>

          <form className="stack-form" onSubmit={saveCategory}>
            <div className="section-head compact">
              <div>
                <h3>{selectedCategory ? 'Edit category' : 'Create category'}</h3>
                <p>{selectedCategory ? `Editing ${selectedCategory.name}` : 'Create a new node anywhere in the hierarchy.'}</p>
              </div>
            </div>

            <label>
              Parent
              <select
                value={categoryForm.parentId}
                onChange={(event) => setCategoryForm((current) => ({ ...current, parentId: event.target.value }))}
              >
                <option value="">Root level</option>
                {flatCategories
                  .filter((item) => item.id !== selectedCategoryId)
                  .map((item) => (
                    <option key={item.id} value={item.id}>
                      {'-'.repeat(item.depth + 1)} {item.name}
                    </option>
                  ))}
              </select>
            </label>

            <label>
              Category code
              <input
                value={categoryForm.categoryCode}
                onChange={(event) => setCategoryForm((current) => ({ ...current, categoryCode: event.target.value }))}
                placeholder="FOOTWEAR"
                required
              />
            </label>

            <label>
              Name
              <input
                value={categoryForm.name}
                onChange={(event) => setCategoryForm((current) => ({ ...current, name: event.target.value }))}
                placeholder="Footwear"
                required
              />
            </label>

            <label>
              Description
              <textarea
                value={categoryForm.description}
                onChange={(event) => setCategoryForm((current) => ({ ...current, description: event.target.value }))}
                placeholder="Optional internal description"
                rows="4"
              />
            </label>

            <div className="filters two-up">
              <label>
                Sort order
                <input
                  type="number"
                  value={categoryForm.sortOrder}
                  onChange={(event) => setCategoryForm((current) => ({ ...current, sortOrder: event.target.value }))}
                />
              </label>

              <label className="checkbox-tile">
                <span>Active</span>
                <input
                  type="checkbox"
                  checked={categoryForm.active}
                  onChange={(event) => setCategoryForm((current) => ({ ...current, active: event.target.checked }))}
                />
              </label>
            </div>

            <div className="inline-actions">
              <button className="btn btn-primary" type="submit" disabled={!canManage || categorySaving}>
                {categorySaving ? 'Saving...' : selectedCategoryId ? 'Update category' : 'Create category'}
              </button>
              <button className="btn btn-outline" type="button" onClick={() => resetCategoryForm()}>
                Reset
              </button>
              {selectedCategoryId ? (
                <button className="btn btn-outline btn-danger" type="button" disabled={!canManage || categorySaving} onClick={removeCategory}>
                  Delete
                </button>
              ) : null}
            </div>
          </form>
        </section>

        <section className="panel">
          <div className="section-head">
            <div>
              <h3>Products</h3>
              <p>{productsPage.totalElements || productsPage.content.length} inventory products currently match the active filter set.</p>
            </div>
            <button className="btn btn-outline btn-sm" type="button" onClick={resetProductForm}>
              New product
            </button>
          </div>

          <form className="filters three-up" onSubmit={applyProductFilters}>
            <label>
              Search
              <input
                value={productFilters.query}
                onChange={(event) => setProductFilters((current) => ({ ...current, query: event.target.value }))}
                placeholder="Name or SKU"
              />
            </label>

            <label>
              Category
              <select
                value={productFilters.categoryId}
                onChange={(event) => setProductFilters((current) => ({ ...current, categoryId: event.target.value }))}
              >
                <option value="">All categories</option>
                {flatCategories.map((category) => (
                  <option key={category.id} value={category.id}>
                    {category.name}
                  </option>
                ))}
              </select>
            </label>

            <label>
              Active state
              <select
                value={productFilters.active}
                onChange={(event) => setProductFilters((current) => ({ ...current, active: event.target.value }))}
              >
                <option value="">All</option>
                <option value="true">Active only</option>
                <option value="false">Inactive only</option>
              </select>
            </label>

            <div className="inline-actions">
              <button className="btn btn-primary" type="submit">
                Apply
              </button>
              <button
                className="btn btn-outline"
                type="button"
                onClick={() => {
                  const nextFilters = { query: '', categoryId: '', active: '' }
                  setProductFilters(nextFilters)
                  load(nextFilters)
                }}
              >
                Clear
              </button>
            </div>
          </form>

          <div className="table-wrap">
            <table>
              <thead>
                <tr>
                  <th>Product</th>
                  <th>Price</th>
                  <th>Tracking</th>
                  <th>Status</th>
                  <th>Updated</th>
                </tr>
              </thead>
              <tbody>
                {productsPage.content.length ? (
                  productsPage.content.map((product) => (
                    <tr
                      key={product.id}
                      className={selectedProductId === product.id ? 'row-active' : ''}
                      onClick={() => handleProductSelect(product)}
                    >
                      <td>
                        <strong>{product.name}</strong>
                        <div className="subtle-meta mono">{product.sku}</div>
                      </td>
                      <td>{formatCurrency(product.basePrice, product.currencyCode)}</td>
                      <td>{product.batchTracked ? 'Batch ' : ''}{product.serialTracked ? 'Serial' : product.batchTracked ? '' : 'Standard'}</td>
                      <td>
                        <span className={`badge ${product.active ? 'badge-success' : 'badge-muted'}`}>
                          {product.status}
                        </span>
                      </td>
                      <td>{formatDateTime(product.updatedAt)}</td>
                    </tr>
                  ))
                ) : (
                  <tr>
                    <td colSpan="5" className="empty-row">No products match the current filters.</td>
                  </tr>
                )}
              </tbody>
            </table>
          </div>

          <form className="stack-form" onSubmit={saveProduct}>
            <div className="section-head compact">
              <div>
                <h3>{selectedProduct ? 'Edit product' : 'Create product'}</h3>
                <p>{selectedProduct ? `Editing ${selectedProduct.name}` : 'Create an inventory product and assign it to one or more categories.'}</p>
              </div>
            </div>

            <div className="filters two-up">
              <label>
                SKU
                <input
                  value={productForm.sku}
                  onChange={(event) => setProductForm((current) => ({ ...current, sku: event.target.value }))}
                  placeholder="SKU-1001"
                  required
                />
              </label>

              <label>
                Name
                <input
                  value={productForm.name}
                  onChange={(event) => setProductForm((current) => ({ ...current, name: event.target.value }))}
                  placeholder="Trail shoe"
                  required
                />
              </label>
            </div>

            <label>
              Description
              <textarea
                value={productForm.description}
                onChange={(event) => setProductForm((current) => ({ ...current, description: event.target.value }))}
                rows="4"
                placeholder="Internal inventory description"
              />
            </label>

            <div className="filters three-up">
              <label>
                Status
                <input
                  value={productForm.status}
                  onChange={(event) => setProductForm((current) => ({ ...current, status: event.target.value }))}
                  placeholder="ACTIVE"
                />
              </label>

              <label>
                Base price
                <input
                  type="number"
                  min="0"
                  step="0.01"
                  value={productForm.basePrice}
                  onChange={(event) => setProductForm((current) => ({ ...current, basePrice: event.target.value }))}
                  required
                />
              </label>

              <label>
                Currency
                <input
                  value={productForm.currencyCode}
                  onChange={(event) => setProductForm((current) => ({ ...current, currencyCode: event.target.value.toUpperCase() }))}
                  maxLength="3"
                  required
                />
              </label>
            </div>

            <div className="filters four-up">
              <label>
                Width cm
                <input type="number" min="0" step="0.01" value={productForm.widthCm} onChange={(event) => setProductForm((current) => ({ ...current, widthCm: event.target.value }))} />
              </label>
              <label>
                Height cm
                <input type="number" min="0" step="0.01" value={productForm.heightCm} onChange={(event) => setProductForm((current) => ({ ...current, heightCm: event.target.value }))} />
              </label>
              <label>
                Length cm
                <input type="number" min="0" step="0.01" value={productForm.lengthCm} onChange={(event) => setProductForm((current) => ({ ...current, lengthCm: event.target.value }))} />
              </label>
              <label>
                Weight kg
                <input type="number" min="0" step="0.01" value={productForm.weightKg} onChange={(event) => setProductForm((current) => ({ ...current, weightKg: event.target.value }))} />
              </label>
            </div>

            <div className="filters two-up">
              <label>
                Barcode value
                <input value={productForm.barcodeValue} onChange={(event) => setProductForm((current) => ({ ...current, barcodeValue: event.target.value }))} />
              </label>
              <label>
                QR value
                <input value={productForm.qrCodeValue} onChange={(event) => setProductForm((current) => ({ ...current, qrCodeValue: event.target.value }))} />
              </label>
            </div>

            <div className="toggle-group">
              <button
                type="button"
                className={`toggle-chip ${productForm.batchTracked ? 'active' : ''}`}
                onClick={() => setProductForm((current) => ({ ...current, batchTracked: !current.batchTracked }))}
              >
                Batch tracked
              </button>
              <button
                type="button"
                className={`toggle-chip ${productForm.serialTracked ? 'active' : ''}`}
                onClick={() => setProductForm((current) => ({ ...current, serialTracked: !current.serialTracked }))}
              >
                Serial tracked
              </button>
              <button
                type="button"
                className={`toggle-chip ${productForm.active ? 'active' : ''}`}
                onClick={() => setProductForm((current) => ({ ...current, active: !current.active }))}
              >
                Active
              </button>
            </div>

            <div className="section-head compact">
              <div>
                <h3>Category assignment</h3>
                <p>Select one or more categories and identify the primary category.</p>
              </div>
            </div>

            <div className="checkbox-list">
              {flatCategories.map((category) => (
                <label key={category.id} className="checkbox-row">
                  <input
                    type="checkbox"
                    checked={productForm.categoryIds.includes(category.id)}
                    onChange={() => toggleProductCategory(category.id)}
                  />
                  <span style={{ paddingLeft: `${category.depth * 18}px` }}>
                    {category.name} <small className="mono">{category.categoryCode}</small>
                  </span>
                </label>
              ))}
            </div>

            <label>
              Primary category
              <select
                value={productForm.primaryCategoryId}
                onChange={(event) => setProductForm((current) => ({ ...current, primaryCategoryId: event.target.value }))}
              >
                <option value="">Select primary category</option>
                {flatCategories
                  .filter((category) => productForm.categoryIds.includes(category.id))
                  .map((category) => (
                    <option key={category.id} value={category.id}>
                      {category.name}
                    </option>
                  ))}
              </select>
            </label>

            <div className="inline-actions">
              <button className="btn btn-primary" type="submit" disabled={!canManage || productSaving}>
                {productSaving ? 'Saving...' : selectedProductId ? 'Update product' : 'Create product'}
              </button>
              <button className="btn btn-outline" type="button" onClick={resetProductForm}>
                Reset
              </button>
              {selectedProductId ? (
                <button className="btn btn-outline btn-danger" type="button" disabled={!canManage || productSaving} onClick={removeProduct}>
                  Delete
                </button>
              ) : null}
            </div>
          </form>
        </section>
      </div>
    </div>
  )
}
