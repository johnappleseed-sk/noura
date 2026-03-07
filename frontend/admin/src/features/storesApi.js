import api from '../shared/api';

// Stores
export const getStores = () => api.get('/api/stores');
export const getActiveStores = () => api.get('/api/stores/active');
export const getStore = (id) => api.get(`/api/stores/${id}`);
export const createStore = (data) => api.post('/api/stores', data);
export const updateStore = (id, data) => api.put(`/api/stores/${id}`, data);
export const deleteStore = (id) => api.delete(`/api/stores/${id}`);

// Store Inventory
export const getStoreInventory = (storeId) => api.get(`/api/stores/${storeId}/inventory`);
export const getProductInventory = (storeId, productId, variantId = null) => {
  const params = variantId ? `?variantId=${variantId}` : '';
  return api.get(`/api/stores/${storeId}/inventory/${productId}${params}`);
};
export const adjustInventory = (storeId, data) => 
  api.post(`/api/stores/${storeId}/inventory/adjust`, data);
export const transferInventory = (data) => api.post('/api/stores/transfer', data);
