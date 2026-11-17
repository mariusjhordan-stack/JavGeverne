import client from './client';

const data = (r) => r.data;

export const startCheckout = (orderId) => client.post('/api/payments/preference', { orderId }).then(data);
export const getPaymentForOrder = (orderId) => client.get(`/api/payments/order/${orderId}`).then(data);
/** Asks the server to verify the payment with the provider; the browser never decides the outcome. */
export const syncPayment = (orderId) => client.post(`/api/payments/order/${orderId}/sync`).then(data);
