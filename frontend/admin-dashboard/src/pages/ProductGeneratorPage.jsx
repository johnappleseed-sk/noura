import { useState } from 'react'
import { generateProduct } from '../shared/api/endpoints/productGeneratorApi'
import { Spinner } from '../shared/ui/Spinner'

const DEFAULT_FORM = {
  name: '',
  category: '',
  brand: '',
  targetAudience: ''
}

export function ProductGeneratorPage() {
  const [form, setForm] = useState(DEFAULT_FORM)
  const [generating, setGenerating] = useState(false)
  const [error, setError] = useState('')
  const [result, setResult] = useState(null)
  const [history, setHistory] = useState([])

  async function handleGenerate() {
    setGenerating(true)
    setError('')
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
    } catch (err) {
      setError(err.message || 'Failed to generate product.')
    } finally {
      setGenerating(false)
    }
  }

  function handleReset() {
    setForm(DEFAULT_FORM)
    setResult(null)
    setError('')
  }

  function copyToClipboard(text) {
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

      {error ? <div className="alert alert-error">{error}</div> : null}

      <div className="generator-layout">
        {/* ── Input panel ── */}
        <section className="panel generator-input">
          <div className="section-head">
            <div>
              <h3>Product parameters</h3>
              <p>Provide optional hints to shape the generated product. Leave blank for auto-generated values.</p>
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
            <button className="btn btn-primary" onClick={handleGenerate} disabled={generating}>
              {generating ? 'Generating...' : 'Generate product'}
            </button>
            <button className="btn btn-outline" onClick={handleReset} disabled={generating}>
              Reset
            </button>
          </div>
        </section>

        {/* ── Result panel ── */}
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

          {generating ? (
            <Spinner label="Generating product..." />
          ) : result ? (
            <div className="generator-output">
              <div className="output-header">
                <h4>{result.product_name}</h4>
                {result.qr_code_base64 ? (
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
                  <code>{result.barcode}</code>
                  <button className="btn btn-outline btn-sm" onClick={() => copyToClipboard(result.barcode)}>
                    Copy
                  </button>
                </div>
              </div>

              <div className="output-description">
                <label>Enterprise description</label>
                <pre className="description-block">{result.description}</pre>
              </div>
            </div>
          ) : (
            <p className="empty-copy">No product generated yet. Configure parameters and click Generate.</p>
          )}
        </section>
      </div>

      {/* ── History ── */}
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
