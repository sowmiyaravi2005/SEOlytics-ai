const TOKEN_KEY = 'seolytics.token';
const API_BASE_URL = (import.meta.env.VITE_API_BASE_URL || '').replace(/\/$/, '');

export function getToken() {
  return localStorage.getItem(TOKEN_KEY);
}

export function setToken(token) {
  if (token) localStorage.setItem(TOKEN_KEY, token);
  else localStorage.removeItem(TOKEN_KEY);
}

async function request(path, options = {}) {
  const headers = { ...(options.headers || {}) };
  if (!(options.body instanceof FormData) && options.body && !headers['Content-Type']) {
    headers['Content-Type'] = 'application/json';
  }
  const token = getToken();
  if (token) headers.Authorization = `Bearer ${token}`;
  const url = path.startsWith('http') ? path : `${API_BASE_URL}${path}`;
  let response;
  try {
    response = await fetch(url, { ...options, headers });
  } catch (err) {
    throw new Error('Unable to reach the SEOlytics API. Start the backend with mvn spring-boot:run, then try again.');
  }
  if (response.status === 401) {
    setToken(null);
    if (!path.includes('/api/auth/login')) {
      window.location.href = '/login';
    }
  }
  return response;
}

export async function apiJson(path, options = {}) {
  const response = await request(path, options);
  const data = await response.json().catch(() => ({}));
  if (!response.ok) {
    if (response.status === 502 || response.status === 503 || response.status === 504) {
      throw new Error('SEOlytics API is not running. Start the backend with mvn spring-boot:run, then try again.');
    }
    throw new Error(data.message || `Request failed (${response.status})`);
  }
  return data;
}

export async function apiBlob(path) {
  const response = await request(path);
  if (!response.ok) {
    const data = await response.json().catch(() => ({}));
    throw new Error(data.message || 'Download failed');
  }
  const blob = await response.blob();
  const disposition = response.headers.get('Content-Disposition') || '';
  const match = disposition.match(/filename="(.+)"/);
  return { blob, filename: match ? match[1] : 'seolytics-report' };
}
