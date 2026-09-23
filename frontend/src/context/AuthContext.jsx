import { useEffect, useMemo, useState } from 'react';
import { apiJson, getToken, setToken } from '../api';
import { AuthContext } from './authState.js';

export function AuthProvider({ children }) {
  const [user, setUser] = useState(null);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    const token = getToken();
    if (!token) {
      setLoading(false);
      return;
    }
    apiJson('/api/auth/me')
      .then(setUser)
      .catch(() => setToken(null))
      .finally(() => setLoading(false));
  }, []);

  const value = useMemo(() => ({
    user,
    loading,
    async login(email, password) {
      const data = await apiJson('/api/auth/login', {
        method: 'POST',
        body: JSON.stringify({ email, password })
      });
      setToken(data.token);
      setUser(data.user);
    },
    async register(fullName, email, password) {
      const data = await apiJson('/api/auth/register', {
        method: 'POST',
        body: JSON.stringify({ fullName, email, password })
      });
      setToken(data.token);
      setUser(data.user);
    },
    logout() {
      setToken(null);
      setUser(null);
    }
  }), [user, loading]);

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}
