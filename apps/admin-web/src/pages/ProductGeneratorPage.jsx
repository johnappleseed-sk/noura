import { useEffect, useState } from 'react'
import {
  fetchProductBarcodeImage,
  fetchProductQrImage,
  generateMissingFields,
  generateProduct,
  generateProductBarcode,
  generateProductDescription,
  generateProductQr,
  getExistingProduct,
  searchExistingProducts
} from '../shared/api/endpoints/productGeneratorApi'
import { Spinner } from '../shared/ui/Spinner'
import { useToastFeedback } from '../shared/ui/useToastFeedback'

const DEFAULT_FORM = {
  name: '',
  category: '',
  brand: '',
  targetAudience: ''
}

function getErrorMessage(error, fallbackMessage) {
  return error?.response?.data?.message || error?.message || fallbackMessage
}

function normalizeProductIdentifierQuery(query) {
  const value = String(query || '').trim()
  if (!value) return ''

  let candidate = value
  if (candidate.startsWith('{') && candidate.endsWith('}')) {
    candidate = candidate.slice(1, -1).trim()
  }
  if (/^0x/i.test(candidate)) {
    candidate = candidate.slice(2)
  }
  if (!/^[0-9a-fA-F]{32}$/.test(candidate)) {
    return ''
  }
  const normalized = [
    candidate.slice(0, 8),
    candidate.slice(8, 12),
    candidate.slice(12, 16),
    candidate.slice(16, 20),
    candidate.slice(20)
  ].join('-')
  return normalized.toLowerCase()
}

export function ProductGeneratorPage() {
  const [form, setForm] = useState(DEFAULT_FORM)
  const [generating, setGenerating] = useState(false)
  const [generatingExisting, setGeneratingExisting] = useState(false)
  const [loadingExisting, setLoadingExisting] = useState(false)
  const [searching, setSearching] = useState(false)
  const [error, setError] = useState('')
  const [result, setResult] = useState(null)
  const [history, setHistory] = useState([])
  const [searchQuery, setSearchQuery] = useState('')
  const [searchResults, setSearchResults] = useState([])
  const [selectedProductId, setSelectedProductId] = useState('')
  const [mirrorWarning, setMirrorWarning] = useState('')
  const [barcodeImageUrl, setBarcodeImageUrl] = useState('')
  const [qrImageUrl, setQrImageUrl] = useState('')
  useToastFeedback({ errorMessage: error, warningMessage: mirrorWarning })

  useEffect(() => {
    return () => {
      if (barcodeImageUrl) URL.revokeObjectURL(barcodeImageUrl)
      if (qrImageUrl) URL.revokeObjectURL(qrImageUrl)
    }
  }, [barcodeImageUrl, qrImageUrl])

  async function handleGenerate() {
    setGenerating(true)
    setError('')
    setMirrorWarning('')
    setResult(null)
    try {
      const payload = {}
      if (form.name.trim()) payload.name = form.name.trim()
      if (form.category.trim()) payload.category = form.category.trim()
      if (form.brand.trim()) payload.brand = form.brand.trim()
      if (form.targetAudience.trim()) payload.targetAudience = form.targetAudience.trim()

      const data = await generateProduct(payload)
      setResult(data)
      setHistory((prev) => [data, ...prev].slice(0, 20))
      setSelectedProductId('')
      clearImageUrls()
    } catch (err) {
      setError(getErrorMessage(err, 'Failed to generate product.'))
    } finally {
      setGenerating(false)
    }
  }

  async function handleSearchExisting() {
    const query = searchQuery.trim()
    if (!query) {
      setSearchResults([])
      return
    }
    setSearching(true)
    setError('')
    setMirrorWarning('')
    try {
      const data = await searchExistingProducts(query)
      let results = Array.isArray(data) ? data : []
      if (!results.length) {
        const normalizedQuery = normalizeProductIdentifierQuery(query)
        if (normalizedQuery && normalizedQuery !== query) {
          const normalizedData = await searchExistingProducts(normalizedQuery)
          results = Array.isArray(normalizedData) ? normalizedData : []
        }
      }
      setSearchResults(results)
    } catch (err) {
      setError(getErrorMessage(err, 'Failed to search products.'))
      setSearchResults([])
    } finally {
      setSearching(false)
    }
  }

  async function handleSelectExisting(productId) {
    setLoadingExisting(true)
    setError('')
    setMirrorWarning('')
    try {
      const product = await getExistingProduct(productId)
      setSelectedProductId(product.id)
      setForm({
        name: product.name || '',
        category: product.category || '',
        brand: product.brand || '',
        targetAudience: product.targetAudience || ''
      })

      setResult({
        product_name: product.name,
        description: product.description || product.longDescription || product.shortDescription || '',
        barcode: product.barcode || '',
        qr_code: product.qrCode || ''
      })

      await refreshImageUrls(product.id, product.barcode, product.qrCode)
    } catch (err) {
      setError(getErrorMessage(err, 'Failed to load selected product.'))
    } finally {
      setLoadingExisting(false)
    }
  }

  async function handleGenerateMissing() {
    if (!selectedProductId) return
    setGeneratingExisting(true)
    setError('')
    try {
      const data = await generateMissingFields(selectedProductId)
      applyEnrichmentResult(data)
    } catch (err) {
      setError(getErrorMessage(err, 'Failed to generate missing fields.'))
    } finally {
      setGeneratingExisting(false)
    }
  }

  async function handleGenerateDescription() {
    if (!selectedProductId) return
    setGeneratingExisting(true)
    setError('')
    try {
      const data = await generateProductDescription(selectedProductId)
      applyEnrichmentResult(data)
    } catch (err) {
      setError(getErrorMessage(err, 'Failed to generate description.'))
    } finally {
      setGeneratingExisting(false)
    }
  }

  async function handleGenerateBarcode() {
    if (!selectedProductId) return
    setGeneratingExisting(true)
    setError('')
    try {
      const data = await generateProductBarcode(selectedProductId)
      applyEnrichmentResult(data)
    } catch (err) {
      setError(getErrorMessage(err, 'Failed to generate barcode.'))
    } finally {
      setGeneratingExisting(false)
    }
  }

  async function handleGenerateQr() {
    if (!selectedProductId) return
    setGeneratingExisting(true)
    setError('')
    try {
      const data = await generateProductQr(selectedProductId)
      applyEnrichmentResult(data)
    } catch (err) {
      setError(getErrorMessage(err, 'Failed to generate QR code.'))
    } finally {
      setGeneratingExisting(false)
    }
  }

  function applyEnrichmentResult(data) {
    setResult((prev) => ({
      ...prev,
      product_name: data.product_name || prev?.product_name,
      description: data.description || prev?.description || '',
      barcode: data.barcode || prev?.barcode || '',
      qr_code: data.qr_code || prev?.qr_code || ''
    }))
    setMirrorWarning(data.mirror_warning || '')
    refreshImageUrls(selectedProductId, data.barcode, data.qr_code)
  }

  function clearImageUrls() {
    if (barcodeImageUrl) URL.revokeObjectURL(barcodeImageUrl)
    if (qrImageUrl) URL.revokeObjectURL(qrImageUrl)
    setBarcodeImageUrl('')
    setQrImageUrl('')
  }

  async function refreshImageUrls(productId, barcode, qrCode) {
    clearImageUrls()
    let nextBarcodeImageUrl = ''
    let nextQrImageUrl = ''

    try {
      if (barcode) {
        nextBarcodeImageUrl = await fetchProductBarcodeImage(productId)
      }
      if (qrCode) {
        nextQrImageUrl = await fetchProductQrImage(productId)
      }
      setBarcodeImageUrl(nextBarcodeImageUrl)
      setQrImageUrl(nextQrImageUrl)
    } catch (err) {
      if (nextBarcodeImageUrl) URL.revokeObjectURL(nextBarcodeImageUrl)
      if (nextQrImageUrl) URL.revokeObjectURL(nextQrImageUrl)
      setError(getErrorMessage(err, 'Failed to load code images.'))
    }
  }

  function handleReset() {
    setForm(DEFAULT_FORM)
    setResult(null)
    setError('')
    setMirrorWarning('')
    setSelectedProductId('')
    clearImageUrls()
  }

  function copyToClipboard(text) {
    if (!text) return
    navigator.clipboard.writeText(text)
  }

  function downloadJson(data) {
    const json = JSON.stringify(data, null, 2)
    const blob = new Blob([json], { type: 'application/json' })
    const url = URL.createObjectURL(blob)
    const a = document.createElement('a')
    a.href = url
    a.download = `product-${data.barcode || 'generated'}.json`
    a.click()
    URL.revokeObjectURL(url)
  }

  return (
    <div className="page">
      <div className="page-head">
        <h2>Product generator</h2>
        <p>Generate enterprise-grade product descriptions with unique barcodes and QR codes for system integration.</p>
      </div>

      {mirrorWarning ? <p className="subtle-meta">{mirrorWarning}</p> : null}

      <section className="panel" style={{ marginBottom: 'var(--space-6)' }}>
        <div className="section-head">
          <div>
            <h3>Find Existing Product</h3>
            <p>Search by Product ID, Product Name, or SKU, then generate only missing fields.</p>
          </div>
        </div>

        <div className="filters product-generator-search">
          <label htmlFor="existingProductSearch">Search</label>
          <input
            id="existingProductSearch"
            value={searchQuery}
            onChange={(event) => setSearchQuery(event.target.value)}
            placeholder="UUID, product name, or SKU"
          />
          <button className="btn btn-outline" onClick={handleSearchExisting} disabled={searching}>
            {searching ? 'Searching...' : 'Search'}
          </button>
        </div>

        {searching ? (
          <Spinner label="Searching products..." />
        ) : searchResults.length ? (
          <div className="selection-list" style={{ marginTop: 'var(--space-4)' }}>
            {searchResults.map((item) => (
              <button
                key={item.id}
                className={`selection-item ${selectedProductId === item.id ? 'active' : ''}`}
                onClick={() => handleSelectExisting(item.id)}
                disabled={loadingExisting}
              >
                <span className="selection-indent">
                  <strong>{item.name}</strong>
                  <small className="subtle-meta">{item.category || 'Uncategorized'}</small>
                </span>
                <span className="inline-actions">
                  <span className={`badge ${item.description_missing ? 'badge-warning' : 'badge-success'}`}>
                    Description: {item.description_missing ? 'Missing' : 'Present'}
                  </span>
                  <span className={`badge ${item.barcode_missing ? 'badge-warning' : 'badge-success'}`}>
                    Barcode: {item.barcode_missing ? 'Missing' : 'Present'}
                  </span>
                  <span className={`badge ${item.qr_missing ? 'badge-warning' : 'badge-success'}`}>
                    QR: {item.qr_missing ? 'Missing' : 'Present'}
                  </span>
                </span>
              </button>
            ))}
          </div>
        ) : searchQuery.trim() ? (
          <p className="empty-copy">No matching products found.</p>
        ) : null}
      </section>

      <div className="generator-layout">
        <section className="panel generator-input">
          <div className="section-head">
            <div>
              <h3>Product parameters</h3>
              <p>Existing product data is loaded here. You can still run legacy generation using these values.</p>
            </div>
          </div>

          <div className="form-grid">
            <label>
              Product name
              <input
                value={form.name}
                onChange={(e) => setForm((c) => ({ ...c, name: e.target.value }))}
                placeholder="Auto-generated if empty"
              />
            </label>
            <label>
              Category
              <input
                value={form.category}
                onChange={(e) => setForm((c) => ({ ...c, category: e.target.value }))}
                placeholder="e.g. Electronics, Footwear, Home Appliance"
              />
            </label>
            <label>
              Brand
              <input
                value={form.brand}
                onChange={(e) => setForm((c) => ({ ...c, brand: e.target.value }))}
                placeholder="Defaults to Noura"
              />
            </label>
            <label>
              Target audience
              <input
                value={form.targetAudience}
                onChange={(e) => setForm((c) => ({ ...c, targetAudience: e.target.value }))}
                placeholder="e.g. enterprise buyers, fitness enthusiasts"
              />
            </label>
          </div>

          <div className="inline-actions" style={{ marginTop: 'var(--space-4)' }}>
            <button
              className="btn btn-primary"
              onClick={handleGenerateMissing}
              disabled={generatingExisting || !selectedProductId || loadingExisting}
            >
              {generatingExisting ? 'Generating...' : 'Generate Missing Fields'}
            </button>
            <button
              className="btn btn-outline"
              onClick={handleGenerateDescription}
              disabled={generatingExisting || !selectedProductId || loadingExisting}
            >
              Generate Description
            </button>
            <button
              className="btn btn-outline"
              onClick={handleGenerateBarcode}
              disabled={generatingExisting || !selectedProductId || loadingExisting}
            >
              Generate Barcode
            </button>
            <button
              className="btn btn-outline"
              onClick={handleGenerateQr}
              disabled={generatingExisting || !selectedProductId || loadingExisting}
            >
              Generate QR Code
            </button>
          </div>

          <div className="inline-actions" style={{ marginTop: 'var(--space-4)' }}>
            <button className="btn btn-outline" onClick={handleGenerate} disabled={generating}>
              {generating ? 'Generating...' : 'Legacy: Generate Product'}
            </button>
            <button className="btn btn-outline" onClick={handleReset} disabled={generating || generatingExisting}>
              Reset
            </button>
          </div>
        </section>

        <section className="panel generator-result">
          <div className="section-head">
            <div>
              <h3>Generated output</h3>
              <p>Product description, barcode ID, and QR code.</p>
            </div>
            {result ? (
              <div className="inline-actions">
                <button className="btn btn-outline btn-sm" onClick={() => copyToClipboard(JSON.stringify(result, null, 2))}>
                  Copy JSON
                </button>
                <button className="btn btn-outline btn-sm" onClick={() => downloadJson(result)}>
                  Download
                </button>
              </div>
            ) : null}
          </div>

          {generating || generatingExisting || loadingExisting ? (
            <Spinner label="Loading output..." />
          ) : result ? (
            <div className="generator-output">
              <div className="output-header">
                <h4>{result.product_name}</h4>
                {qrImageUrl ? (
                  <img
                    className="qr-code-img"
                    src={qrImageUrl}
                    alt="QR Code"
                    width="140"
                    height="140"
                  />
                ) : result.qr_code_base64 ? (
                  <img
                    className="qr-code-img"
                    src={`data:image/png;base64,${result.qr_code_base64}`}
                    alt="QR Code"
                    width="140"
                    height="140"
                  />
                ) : null}
              </div>

              <div className="output-barcode">
                <label>Barcode ID</label>
                <div className="barcode-display">
                  <code>{result.barcode || 'Not available'}</code>
                  {result.barcode ? (
                    <button className="btn btn-outline btn-sm" onClick={() => copyToClipboard(result.barcode)}>
                      Copy
                    </button>
                  ) : null}
                </div>
                {barcodeImageUrl ? (
                  <div className="barcode-image-wrap">
                    <img src={barcodeImageUrl} alt="Barcode" className="barcode-img" />
                  </div>
                ) : null}
              </div>

              <div className="output-description">
                <label>Enterprise description</label>
                <pre className="description-block">{result.description || 'No description available.'}</pre>
              </div>
            </div>
          ) : (
            <p className="empty-copy">No product generated yet. Search for an existing product or run legacy generation.</p>
          )}
        </section>
      </div>

      {history.length > 0 ? (
        <section className="panel">
          <div className="section-head">
            <div>
              <h3>Generation history</h3>
              <p>Recent products generated in this session (max 20).</p>
            </div>
            <button className="btn btn-outline btn-sm" onClick={() => setHistory([])}>
              Clear
            </button>
          </div>

          <div className="table-wrap">
            <table>
              <thead>
                <tr>
                  <th>Product name</th>
                  <th>Barcode</th>
                  <th>Actions</th>
                </tr>
              </thead>
              <tbody>
                {history.map((item, idx) => (
                  <tr key={item.barcode || idx}>
                    <td><strong>{item.product_name}</strong></td>
                    <td><code className="mono">{item.barcode}</code></td>
                    <td>
                      <div className="inline-actions">
                        <button className="btn btn-outline btn-sm" onClick={() => setResult(item)}>
                          View
                        </button>
                        <button className="btn btn-outline btn-sm" onClick={() => downloadJson(item)}>
                          JSON
                        </button>
                        <button className="btn btn-outline btn-sm" onClick={() => copyToClipboard(item.barcode)}>
                          Copy ID
                        </button>
                      </div>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </section>
      ) : null}
    </div>
  )
}
