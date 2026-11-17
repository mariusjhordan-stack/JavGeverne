import client from './client';

const data = (r) => r.data;

export const login = (email, password) => client.post('/api/auth/login', { email, password }).then(data);
export const register = (payload) => client.post('/api/auth/register', payload).then(data);
export const getProfile = () => client.get('/api/auth/profile').then(data);
export const logout = (refreshToken) => client.post('/api/auth/logout', { refreshToken }).then(data);
