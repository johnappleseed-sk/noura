export function formatCurrency(value, currency = 'USD') {
  const safeValue = typeof value === 'number' ? value : Number(value || 0)
  return new Intl.NumberFormat('en-US', {
    style: 'currency',
    currency,
    maximumFractionDigits: 2
  }).format(safeValue)
}

export function formatMeasure(value, unit) {
  if (value === null || value === undefined || value === '') {
    return 'N/A'
  }
  return unit ? `${value} ${unit}` : String(value)
}
