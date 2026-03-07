import api from '../shared/api';

// Companies
export const getCompanies = (params = {}) => {
  const query = new URLSearchParams(params).toString();
  return api.get(`/api/b2b/companies?${query}`);
};
export const getCompany = (id) => api.get(`/api/b2b/companies/${id}`);
export const createCompany = (data) => api.post('/api/b2b/companies', data);
export const updateCompany = (id, data) => api.put(`/api/b2b/companies/${id}`, data);
export const approveCompany = (id) => api.post(`/api/b2b/companies/${id}/approve`);
export const suspendCompany = (id) => api.post(`/api/b2b/companies/${id}/suspend`);

// Contacts
export const getCompanyContacts = (companyId) => 
  api.get(`/api/b2b/companies/${companyId}/contacts`);
export const addContact = (companyId, data) => 
  api.post(`/api/b2b/companies/${companyId}/contacts`, data);

// Price Lists
export const getPriceLists = (activeOnly = true) => 
  api.get(`/api/b2b/price-lists?activeOnly=${activeOnly}`);
export const getPriceList = (id) => api.get(`/api/b2b/price-lists/${id}`);
export const createPriceList = (data) => api.post('/api/b2b/price-lists', data);
export const addPriceListItem = (priceListId, data) => 
  api.post(`/api/b2b/price-lists/${priceListId}/items`, data);

// Pricing
export const getEffectivePrice = (companyId, productId, variantId = null, quantity = 1) => {
  const params = new URLSearchParams({ companyId, productId, quantity });
  if (variantId) params.append('variantId', variantId);
  return api.get(`/api/b2b/pricing?${params}`);
};

// Purchase Orders
export const getPurchaseOrders = (params = {}) => {
  const query = new URLSearchParams(params).toString();
  return api.get(`/api/b2b/purchase-orders?${query}`);
};
export const getPurchaseOrder = (id) => api.get(`/api/b2b/purchase-orders/${id}`);
export const createDraftOrder = (data) => api.post('/api/b2b/purchase-orders', data);
export const addOrderItem = (orderId, data) => 
  api.post(`/api/b2b/purchase-orders/${orderId}/items`, data);
export const submitOrder = (id) => api.post(`/api/b2b/purchase-orders/${id}/submit`);
export const approveOrder = (id, approvedBy) => 
  api.post(`/api/b2b/purchase-orders/${id}/approve?approvedBy=${encodeURIComponent(approvedBy)}`);
export const rejectOrder = (id, reason = '') => 
  api.post(`/api/b2b/purchase-orders/${id}/reject?reason=${encodeURIComponent(reason)}`);
export const cancelOrder = (id) => api.post(`/api/b2b/purchase-orders/${id}/cancel`);
