import { unwrapApiResponse } from '../apiResult'
import { commerceApiClient } from '../httpClient'

export async function getCustomerAnalytics() {
  const response = await commerceApiClient.get('/customers/analytics')
  const payload = response.data

  // This endpoint currently returns a raw JSON object (not wrapped in ApiResponse),
  // but auth failures and platform errors return the standard ApiResponse envelope.
  if (typeof payload?.success === 'boolean') {
    return unwrapApiResponse(payload)
  }

  return payload
}
