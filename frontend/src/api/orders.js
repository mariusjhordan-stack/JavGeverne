import client from './client';

const data = (r) => r.data;

/** items: [{productId, quantity}] — prices are decided by the server. */
export const createOrder = (items, shippingAddress) => client.post('/api/orders', { items, shippingAddress }).then(data);
export const getOrders = () => client.get('/api/orders').then(data);
export const getOrderById = (id) => client.get(`/api/orders/${id}`).then(data);
export const cancelOrder = (id) => client.post(`/api/orders/${id}/cancel`).then(data);
