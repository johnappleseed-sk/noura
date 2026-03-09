export function unwrapApiResponse(payload) {
  if (payload?.success) {
    return payload.data
  }
  throw new Error(payload?.error?.detail || payload?.message || 'Request failed.')
}
