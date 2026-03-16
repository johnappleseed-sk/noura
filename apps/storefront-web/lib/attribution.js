'use client'

const storageKey = 'noura.analytics.last_product_click'

export function setLastProductClickAttribution({ productId, listName, slot, pagePath }) {
  if (!productId || !listName) {
    return
  }

  try {
    window.localStorage.setItem(
      storageKey,
      JSON.stringify({
        productId: String(productId),
        listName: String(listName),
        slot: typeof slot === 'number' ? slot : slot == null ? null : Number(slot),
        pagePath: pagePath ? String(pagePath) : null,
        occurredAt: new Date().toISOString()
      })
    )
  } catch {
    // Attribution must never interrupt commerce flows.
  }
}

export function getLastProductClickAttribution(productId, { maxAgeMs = 30 * 60 * 1000 } = {}) {
  if (!productId || typeof window === 'undefined') {
    return null
  }

  try {
    const raw = window.localStorage.getItem(storageKey)
    if (!raw) return null
    const parsed = JSON.parse(raw)
    if (!parsed || String(parsed.productId) !== String(productId)) {
      return null
    }

    const occurredAt = parsed.occurredAt ? new Date(parsed.occurredAt).getTime() : 0
    if (!occurredAt || Date.now() - occurredAt > maxAgeMs) {
      window.localStorage.removeItem(storageKey)
      return null
    }

    return {
      listName: parsed.listName || null,
      slot: typeof parsed.slot === 'number' ? parsed.slot : parsed.slot == null ? null : Number(parsed.slot),
      pagePath: parsed.pagePath || null,
      occurredAt: parsed.occurredAt || null
    }
  } catch {
    return null
  }
}

