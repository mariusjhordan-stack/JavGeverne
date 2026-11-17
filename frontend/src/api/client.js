import axios from 'axios';

const API_URL = import.meta.env.VITE_API_URL || '';

const client = axios.create({ baseURL: API_URL, headers: { 'Content-Type': 'application/json' } });

export const tokens = {
  get access() { return localStorage.getItem('token'); },
  get refresh() { return localStorage.getItem('refreshToken'); },
  set({ token, refreshToken }) {
    localStorage.setItem('token', token);
    if (refreshToken) localStorage.setItem('refreshToken', refreshToken);
  },
  clear() {
    localStorage.removeItem('token');
    localStorage.removeItem('refreshToken');
    localStorage.removeItem('user');
  },
};

/** The server always answers errors as {error, code}; this is the one place that reads them. */
export function errorMessage(err, fallback = 'Something went wrong. Please try again.') {
  if (!err?.response) return 'We could not reach the server. Check your connection and try again.';
  return err.response.data?.error || fallback;
}

export const errorCode = (err) => err?.response?.data?.code;

client.interceptors.request.use((config) => {
  if (tokens.access && !config.skipAuth) config.headers.Authorization = `Bearer ${tokens.access}`;
  return config;
});

let refreshing = null;

function refreshSession() {
  if (!refreshing) {
    refreshing = axios
      .post(`${API_URL}/api/auth/refresh`, { refreshToken: tokens.refresh })
      .then((r) => { tokens.set(r.data); return r.data.token; })
      .finally(() => { refreshing = null; });
  }
  return refreshing;
}

function endSession() {
  const hadSession = !!tokens.access;
  tokens.clear();
  if (hadSession) window.dispatchEvent(new Event('session-expired'));
}

client.interceptors.response.use(
  (response) => response,
  async (error) => {
    const { config, response } = error;
    const isAuthCall = config?.url?.startsWith('/api/auth/') && !config.url.startsWith('/api/auth/profile')
      && !config.url.startsWith('/api/auth/addresses') && !config.url.startsWith('/api/auth/password');
    if (response?.status !== 401 || !config || config.retried || isAuthCall) {
      return Promise.reject(error);
    }
    if (!tokens.refresh) {
      endSession();
      return Promise.reject(error);
    }
    try {
      const token = await refreshSession();
      return client({ ...config, retried: true, headers: { ...config.headers, Authorization: `Bearer ${token}` } });
    } catch {
      endSession();
      return Promise.reject(error);
    }
  },
);

export default client;
