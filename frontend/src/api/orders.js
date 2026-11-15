import client from './client';

export const createOrder = (data) =>
  client.post('/api/orders', data).then((r) => r.data);

export const getOrders = () =>
  client.get('/api/orders').then((r) => r.data);

export const getOrderById = (id) =>
  client.get(`/api/orders/${id}`).then((r) => r.data);

export const cancelOrder = (id) =>
  client.post(`/api/orders/${id}/cancel`).then((r) => r.data);
