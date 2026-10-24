const BASE = import.meta.env.VITE_API_URL || 'http://localhost:8080';

export const money = (n) => 'S/ ' + Number(n ?? 0).toFixed(2);
export const fmtDate = (d) => (d ? String(d).slice(0, 10) : '');
export const fmtDateTime = (d) => (d ? String(d).replace('T', ' ').slice(0, 16) : '');
export const daysTo = (d) => Math.ceil((new Date(d) - new Date()) / 86400000);

export async function api(path, { method = 'GET', body } = {}) {
  const token = localStorage.getItem('token');
  const res = await fetch(BASE + path, {
    method,
    headers: {
      'Content-Type': 'application/json',
      ...(token ? { Authorization: `Bearer ${token}` } : {}),
    },
    body: body ? JSON.stringify(body) : undefined,
  });
  if (res.status === 401 && !path.startsWith('/auth')) {
    localStorage.clear();
    window.location.href = '/login';
    return;
  }
  if (!res.ok) {
    const text = await res.text();
    let msg = text;
    try { msg = JSON.parse(text).message || text; } catch {  }
    throw new Error(msg || `Error ${res.status}`);
  }
  return res.status === 204 ? null : res.json();
}
