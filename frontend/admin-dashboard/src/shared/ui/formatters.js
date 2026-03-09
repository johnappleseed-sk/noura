export function formatDateTime(value) {
  if (!value) return '-'
  return new Intl.DateTimeFormat('en-US', {
    dateStyle: 'medium',
    timeStyle: 'short'
  }).format(new Date(value))
}

export function formatDecimal(value, digits = 2) {
  const numeric = Number(value || 0)
  return new Intl.NumberFormat('en-US', {
    minimumFractionDigits: digits,
    maximumFractionDigits: digits
  }).format(numeric)
}

export function formatCurrency(value, currency = 'USD') {
  const numeric = Number(value || 0)
  return new Intl.NumberFormat('en-US', {
    style: 'currency',
    currency,
    minimumFractionDigits: 2
  }).format(numeric)
}
