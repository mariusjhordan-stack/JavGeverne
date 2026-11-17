import { createContext, useContext, useState, useEffect, useCallback } from 'react';
import * as authApi from '../api/auth';
import { tokens } from '../api/client';

const AuthContext = createContext(null);

const readUser = () => {
  try {
    return tokens.access ? JSON.parse(localStorage.getItem('user')) : null;
  } catch {
    return null;
  }
};

export function AuthProvider({ children }) {
  const [user, setUser] = useState(readUser);
  const [sessionExpired, setSessionExpired] = useState(false);

  useEffect(() => {
    const onExpired = () => { setUser(null); setSessionExpired(true); };
    window.addEventListener('session-expired', onExpired);
    return () => window.removeEventListener('session-expired', onExpired);
  }, []);

  const startSession = useCallback((data) => {
    tokens.set(data);
    const next = { email: data.email, firstName: data.firstName, lastName: data.lastName, role: data.role };
    localStorage.setItem('user', JSON.stringify(next));
    setUser(next);
    setSessionExpired(false);
    return next;
  }, []);

  const login = useCallback(async (email, password) => startSession(await authApi.login(email, password)), [startSession]);
  const register = useCallback(async (payload) => startSession(await authApi.register(payload)), [startSession]);

  const logout = useCallback(() => {
    const refreshToken = tokens.refresh;
    if (refreshToken) authApi.logout(refreshToken).catch(() => {});
    tokens.clear();
    setUser(null);
  }, []);

  const updateUser = useCallback((patch) => {
    setUser((prev) => {
      const next = { ...prev, ...patch };
      localStorage.setItem('user', JSON.stringify(next));
      return next;
    });
  }, []);

  const value = {
    user, login, register, logout, updateUser, sessionExpired,
    clearSessionExpired: () => setSessionExpired(false),
    isAuthenticated: !!user,
    // Only decides what the interface shows; every admin request is authorised again on the server.
    isAdmin: user?.role === 'ADMIN',
  };
  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}

// eslint-disable-next-line react-refresh/only-export-components
export function useAuth() {
  const ctx = useContext(AuthContext);
  if (!ctx) throw new Error('useAuth must be used within AuthProvider');
  return ctx;
}
