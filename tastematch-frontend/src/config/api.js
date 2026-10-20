import { clearAuthSession, getAuthToken } from './auth';

// Local development uses the Spring Boot server directly. In production, use
// same-origin /api requests so the Vercel rewrite can proxy them to BACKEND_URL.
// CRA replaces NODE_ENV at build time, so this decision is stable in the bundle.
const fallbackBaseUrl =
  process.env.NODE_ENV === 'production' ? '' : 'http://localhost:8080';
const configuredBaseUrl = process.env.REACT_APP_API_BASE_URL;

const resolveApiBaseUrl = () => {
  if (!configuredBaseUrl) {
    return fallbackBaseUrl;
  }

  const normalizedValue = configuredBaseUrl.trim();
  if (!normalizedValue || normalizedValue === '/' || normalizedValue.toLowerCase() === 'same-origin') {
    return '';
  }

  return normalizedValue.replace(/\/+$/, '');
};

export const API_BASE_URL = resolveApiBaseUrl();
export const ENABLE_DEMO_FALLBACK = process.env.REACT_APP_ENABLE_DEMO_FALLBACK === 'true';

export const apiUrl = (path) => {
  const normalizedPath = path.startsWith('/') ? path : `/${path}`;
  return `${API_BASE_URL}${normalizedPath}`;
};

export const apiFetch = async (path, options = {}) => {
  const { body, headers, ...rest } = options;
  const requestHeaders = new Headers(headers || {});
  const token = getAuthToken();

  if (token && !requestHeaders.has('Authorization')) {
    requestHeaders.set('Authorization', `Bearer ${token}`);
  }

  if (body && !(body instanceof FormData) && !requestHeaders.has('Content-Type')) {
    requestHeaders.set('Content-Type', 'application/json');
  }

  const response = await fetch(path.startsWith('http') ? path : apiUrl(path), {
    ...rest,
    body,
    headers: requestHeaders,
  });

  if (response.status === 401) {
    clearAuthSession();
  }

  return response;
};
