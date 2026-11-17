import client from './client';

const data = (r) => r.data;

export const getProfile = () => client.get('/api/auth/profile').then(data);
export const updateProfile = (payload) => client.put('/api/auth/profile', payload).then(data);
export const changePassword = (currentPassword, newPassword) => client.put('/api/auth/password', { currentPassword, newPassword });
export const listAddresses = () => client.get('/api/auth/addresses').then(data);
export const createAddress = (payload) => client.post('/api/auth/addresses', payload).then(data);
export const updateAddress = (id, payload) => client.put(`/api/auth/addresses/${id}`, payload).then(data);
export const deleteAddress = (id) => client.delete(`/api/auth/addresses/${id}`);
export const getNotifications = () => client.get('/api/notifications').then(data);
