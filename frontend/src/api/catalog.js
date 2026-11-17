import client from './client';

const data = (r) => r.data;

/** params: q, brandId, categoryId, minPrice, maxPrice, movementType, featured, sort (newest|price_asc|price_desc|name), page, size */
export const getProducts = (params = {}) => client.get('/api/products', { params }).then(data);
export const getProductById = (id) => client.get(`/api/products/${id}`).then(data);
export const getFeaturedProducts = () => client.get('/api/products/featured').then(data);
export const getBrands = () => client.get('/api/brands').then(data);
export const getBrand = (slug) => client.get(`/api/brands/${slug}`).then(data);
export const getCategories = () => client.get('/api/categories').then(data);
