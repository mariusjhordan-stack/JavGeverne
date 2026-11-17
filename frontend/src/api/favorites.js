import client from './client';

const data = (r) => r.data;

export const listFavorites = () => client.get('/api/favorites').then(data);
export const addFavorite = (productId) => client.put(`/api/favorites/${productId}`).then(data);
export const removeFavorite = (productId) => client.delete(`/api/favorites/${productId}`);
