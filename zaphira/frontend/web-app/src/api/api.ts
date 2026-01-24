/**
 * Centralized API client for Zaphira
 */

const API_BASE_URL = import.meta.env.VITE_API_URL || 'http://localhost:8080';
const API_PREFIX = '/api/v1';

let accessToken: string | null = null;
let refreshToken: string | null = null;

export interface ApiError {
  status: number;
  message: string;
  code?: string;
  details?: Record<string, string[]> | Record<string, string>;
}

export interface ApiResponse<T> {
  success: boolean;
  message: string;
  data: T;
  timestamp?: string;
  path?: string;
}

export function setAuthToken(token: string, refresh?: string | null): void {
  accessToken = token;
  if (refresh) {
    refreshToken = refresh;
  }
  try {
    sessionStorage.setItem('zaphira_token', token);
    if (refresh) {
      sessionStorage.setItem('zaphira_refresh', refresh);
    }
  } catch {
    // Ignore storage errors
  }
}

export function clearAuthToken(): void {
  accessToken = null;
  refreshToken = null;
  try {
    sessionStorage.removeItem('zaphira_token');
    sessionStorage.removeItem('zaphira_refresh');
  } catch {
    // Ignore storage errors
  }
}

export function getAuthToken(): string | null {
  if (accessToken) return accessToken;
  try {
    return sessionStorage.getItem('zaphira_token');
  } catch {
    return null;
  }
}

export async function apiRequest<T>(
  path: string,
  options: RequestInit = {},
  config: { requiresAuth?: boolean } = {}
): Promise<T> {
  const { requiresAuth = true } = config;

  const headers: Record<string, string> = {
    'Content-Type': 'application/json',
    ...(options.headers as Record<string, string>),
  };

  if (requiresAuth) {
    const token = getAuthToken();
    if (token) {
      headers['Authorization'] = `Bearer ${token}`;
    }
  }

  const response = await fetch(`${API_BASE_URL}${API_PREFIX}${path}`, {
    ...options,
    headers,
  });

  const contentType = response.headers.get('Content-Type');
  const isJson = contentType?.includes('application/json');
  const responseBody = isJson ? await response.json().catch(() => null) : null;

  if (!response.ok) {
    const message = responseBody?.message || response.statusText || 'Request failed';
    const error: ApiError = {
      status: response.status,
      message,
      code: responseBody?.code,
      details: responseBody?.errors || responseBody?.validationErrors,
    };
    throw error;
  }

  if (responseBody && responseBody.success === false) {
    const error: ApiError = {
      status: response.status,
      message: responseBody.message || 'Request failed',
      code: responseBody.code,
      details: responseBody.errors || responseBody.validationErrors,
    };
    throw error;
  }

  return (responseBody ?? ({} as T)) as T;
}
