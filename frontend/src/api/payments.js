import client from './client';

export const createPreference = (data) =>
  client.post('/api/payments/preference', data).then((r) => r.data);

export const getPaymentByOrder = (orderId) =>
  client.get(`/api/payments/order/${orderId}`).then((r) => r.data);
