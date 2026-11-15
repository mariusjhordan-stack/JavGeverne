import client from './client';

export const getProducts = (params = {}) =>
  client.get('/api/products', { params }).then((r) => r.data);

export const getProductById = (id) =>
  client.get(`/api/products/${id}`).then((r) => r.data);

export const getFeaturedProducts = () =>
  client.get('/api/products/featured').then((r) => r.data);

export const searchProducts = (query) =>
  client.get('/api/products/search', { params: { q: query } }).then((r) => r.data);

export const getBrands = () =>
  client.get('/api/brands').then((r) => r.data);

export const getCategories = () =>
  client.get('/api/categories').then((r) => r.data);

export const createProduct = (data) =>
  client.post('/api/products', data).then((r) => r.data);

export const updateProduct = (id, data) =>
  client.put(`/api/products/${id}`, data).then((r) => r.data);
