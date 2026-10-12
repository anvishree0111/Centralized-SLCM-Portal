import { createContext, useContext, useEffect, useMemo, useState } from 'react';
import { api, login } from '../services/api';
const AuthContext = createContext(null);
export function AuthProvider({ children }) {
  const [user, setUser] = useState(null); const [loading, setLoading] = useState(true);
  useEffect(() => { const expire=()=>setUser(null); window.addEventListener('slcm:unauthorized',expire); if (!sessionStorage.getItem('slcm.token')) { setLoading(false); return () => window.removeEventListener('slcm:unauthorized',expire); } api('/api/auth/me').then(setUser).catch(() => sessionStorage.removeItem('slcm.token')).finally(() => setLoading(false)); return () => window.removeEventListener('slcm:unauthorized',expire); }, []);
  const value = useMemo(() => ({ user, loading, async signIn(username, password) { const result = await login(username, password); sessionStorage.setItem('slcm.token', result.token); setUser({ username: result.username, role: result.role }); return result; }, signOut() { sessionStorage.removeItem('slcm.token'); setUser(null); } }), [user, loading]);
  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}
export const useAuth = () => useContext(AuthContext);
