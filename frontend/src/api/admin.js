import client from './client';

const data = (r) => r.data;

export const listProducts = (params = {}) => client.get('/api/admin/products', { params }).then(data);
export const createProduct = (payload) => client.post('/api/admin/products', payload).then(data);
export const updateProduct = (id, payload) => client.put(`/api/admin/products/${id}`, payload).then(data);
export const adjustStock = (id, delta, note) => client.post(`/api/admin/products/${id}/stock-adjustments`, { delta, note }).then(data);
export const inventorySummary = () => client.get('/api/admin/inventory/summary').then(data);
export const inventoryMovements = (params = {}) => client.get('/api/admin/inventory/movements', { params }).then(data);

export const listOrders = (params = {}) => client.get('/api/admin/orders', { params }).then(data);
export const getOrder = (id) => client.get(`/api/admin/orders/${id}`).then(data);
export const orderStats = () => client.get('/api/admin/orders/stats').then(data);
export const setOrderStatus = (id, status) => client.put(`/api/admin/orders/${id}/status`, { status }).then(data);

export const listPayments = (params = {}) => client.get('/api/admin/payments', { params }).then(data);
export const paymentStats = () => client.get('/api/admin/payments/stats').then(data);

export const listUsers = (params = {}) => client.get('/api/admin/users', { params }).then(data);
export const getUser = (id) => client.get(`/api/admin/users/${id}`).then(data);
