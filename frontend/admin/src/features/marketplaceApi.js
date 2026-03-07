import api from '../shared/api';

// Channels
export const getChannels = () => api.get('/api/marketplace/channels');
export const createChannel = (data) => api.post('/api/marketplace/channels', data);
export const testChannel = (id) => api.post(`/api/marketplace/channels/${id}/test`);

// Product Sync
export const syncProducts = (channelId, products) => 
  api.post(`/api/marketplace/channels/${channelId}/sync/products`, products);
export const syncInventory = (channelId, updates) => 
  api.post(`/api/marketplace/channels/${channelId}/sync/inventory`, updates);

// Product Mappings
export const getChannelMappings = (channelId) => 
  api.get(`/api/marketplace/channels/${channelId}/mappings`);
export const createMapping = (channelId, data) => 
  api.post(`/api/marketplace/channels/${channelId}/mappings`, data);

// Order Import
export const fetchOrders = (channelId, fromDate, toDate) => 
  api.post(`/api/marketplace/channels/${channelId}/orders/fetch`, { fromDate, toDate });
export const updateOrderShipment = (orderId, trackingNumber, carrier) => 
  api.post(`/api/marketplace/orders/${orderId}/ship`, { trackingNumber, carrier });
