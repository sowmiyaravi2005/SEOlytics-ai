import { useContext } from 'react';
import { AuthContext } from './authState.js';

export function useAuth() {
  return useContext(AuthContext) || {
    user: null,
    loading: true,
    login: async () => {},
    register: async () => {},
    logout: () => {}
  };
}
