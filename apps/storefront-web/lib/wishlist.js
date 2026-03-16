const WISHLIST_STORAGE_KEY = 'noura_wishlist_items_v1'
const WISHLIST_UPDATED_EVENT = 'noura:wishlist-updated'

function parseItems(raw) {
  if (!raw) {
    return []
  }

  try {
    const parsed = JSON.parse(raw)
    if (!Array.isArray(parsed)) {
      return []
    }
    return parsed
      .filter((item) => item && item.id)
      .map((item) => ({
        id: String(item.id),
        name: item.name || '',
        price: Number(item.price || 0),
        imageUrl: item.imageUrl || null,
        categoryName: item.categoryName || null
      }))
  } catch {
    return []
  }
}

function readStorageItems() {
  if (typeof window === 'undefined') {
    return []
  }
  return parseItems(window.localStorage.getItem(WISHLIST_STORAGE_KEY))
}

function writeStorageItems(items) {
  if (typeof window === 'undefined') {
    return
  }
  window.localStorage.setItem(WISHLIST_STORAGE_KEY, JSON.stringify(items))
}

function notifyWishlistUpdated() {
  if (typeof window === 'undefined') {
    return
  }
  window.dispatchEvent(new CustomEvent(WISHLIST_UPDATED_EVENT))
}

function normalizeWishlistItem(item) {
  return {
    id: String(item?.id || ''),
    name: item?.name || '',
    price: Number(item?.price || 0),
    imageUrl: item?.imageUrl || null,
    categoryName: item?.categoryName || null
  }
}

export function getWishlistItems() {
  return readStorageItems()
}

export function getWishlistCount() {
  return readStorageItems().length
}

export function isWishlisted(id) {
  const itemId = String(id || '')
  if (!itemId) {
    return false
  }
  return readStorageItems().some((item) => item.id === itemId)
}

/**
 * Adds/removes a product snapshot from wishlist.
 *
 * @returns {boolean} next active state
 */
export function toggleWishlistItem(item) {
  const normalized = normalizeWishlistItem(item)
  if (!normalized.id) {
    return false
  }

  const current = readStorageItems()
  const exists = current.some((entry) => entry.id === normalized.id)
  const next = exists
    ? current.filter((entry) => entry.id !== normalized.id)
    : [normalized, ...current].slice(0, 500)

  writeStorageItems(next)
  notifyWishlistUpdated()
  return !exists
}

export function removeWishlistItem(id) {
  const itemId = String(id || '')
  if (!itemId) {
    return
  }
  const current = readStorageItems()
  const next = current.filter((entry) => entry.id !== itemId)
  writeStorageItems(next)
  notifyWishlistUpdated()
}

export function clearWishlist() {
  if (typeof window === 'undefined') {
    return
  }
  window.localStorage.removeItem(WISHLIST_STORAGE_KEY)
  notifyWishlistUpdated()
}

export function subscribeWishlist(listener) {
  if (typeof window === 'undefined') {
    return () => {}
  }

  const onChange = () => listener?.()
  const onStorage = (event) => {
    if (event.key === WISHLIST_STORAGE_KEY) {
      listener?.()
    }
  }

  window.addEventListener(WISHLIST_UPDATED_EVENT, onChange)
  window.addEventListener('storage', onStorage)

  return () => {
    window.removeEventListener(WISHLIST_UPDATED_EVENT, onChange)
    window.removeEventListener('storage', onStorage)
  }
}

