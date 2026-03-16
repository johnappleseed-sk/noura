const CART_UPDATED_EVENT = 'noura:cart-updated'

/**
 * Broadcasts cart mutations so UI surfaces (header badge, mini-cart) can refresh
 * without polling or route-coupled refetches.
 */
export function notifyCartUpdated() {
  if (typeof window === 'undefined') {
    return
  }
  window.dispatchEvent(new CustomEvent(CART_UPDATED_EVENT))
}

/**
 * Subscribes to cart mutation notifications.
 *
 * @param {() => void} listener
 * @returns {() => void}
 */
export function subscribeCartUpdated(listener) {
  if (typeof window === 'undefined') {
    return () => {}
  }

  const handler = () => listener?.()
  window.addEventListener(CART_UPDATED_EVENT, handler)
  return () => window.removeEventListener(CART_UPDATED_EVENT, handler)
}

