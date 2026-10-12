const base = (import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080').replace(/\/$/, '');
export async function api(path, options = {}) {
  const token = sessionStorage.getItem('slcm.token');
  const headers = new Headers(options.headers || {});
  if (options.body && !(options.body instanceof FormData)) headers.set('Content-Type', 'application/json');
  if (token) headers.set('Authorization', `Bearer ${token}`);
  const response = await fetch(`${base}${path}`, { ...options, headers });
  if (response.status === 401) { sessionStorage.removeItem('slcm.token'); window.dispatchEvent(new Event('slcm:unauthorized')); }
  if (!response.ok) { let message = `Request failed (${response.status})`; try { const body = await response.json(); message = body.message || message; } catch {} throw new Error(message); }
  if (response.status === 204) return null;
  return response.json();
}
export async function login(username, password) { return api('/api/auth/login', { method: 'POST', body: JSON.stringify({ username, password }) }); }
export async function download(path) { const token=sessionStorage.getItem('slcm.token'); const response=await fetch(`${base}${path}`,{headers:{Authorization:`Bearer ${token}`}}); if(!response.ok) throw new Error(`Download failed (${response.status})`); return response.blob(); }
export { base as API_BASE_URL };
