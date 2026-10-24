import { createContext, useContext, useState } from 'react';
import { api } from './api';

const Ctx = createContext(null);
export const useAuth = () => useContext(Ctx);

export function AuthProvider({ children }) {
  const [user, setUser] = useState(() => JSON.parse(localStorage.getItem('user') || 'null'));

  const login = async (correo, password) => {
    const r = await api('/auth/login', { method: 'POST', body: { correo, password } });
    localStorage.setItem('token', r.token);
    localStorage.setItem('user', JSON.stringify(r.usuario));
    setUser(r.usuario);
  };
  const logout = () => {
    localStorage.clear();
    setUser(null);
  };

  return <Ctx.Provider value={{ user, login, logout }}>{children}</Ctx.Provider>;
}
