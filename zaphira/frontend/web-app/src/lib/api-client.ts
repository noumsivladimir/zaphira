/**
 * Zaphira API Client
 * Centralized API management with auth, retry, idempotency, and error handling
 */

const API_BASE_URL = import.meta.env.VITE_API_URL || 'http://localhost:8080';

// Token storage (in production, use secure storage)
let accessToken: string | null = null;
let refreshToken: string | null = null;

// Request queue for token refresh
let isRefreshing = false;
let refreshQueue: Array<{
  resolve: (token: string) => void;
  reject: (error: Error) => void;
}> = [];

// Rate limit tracking
let rateLimitRetryAfter: number | null = null;

export interface ApiError {
  status: number;
  message: string;
  code?: string;
  retryAfter?: number;
  details?: Record<string, string[]>;
}

export interface ApiResponse<T> {
  data: T;
  success: boolean;
}

// Error messages mapped by HTTP status
const ERROR_MESSAGES: Record<number, string> = {
  400: 'error_invalid_request',
  401: 'error_session_expired',
  403: 'error_forbidden',
  404: 'error_not_found',
  409: 'error_duplicate',
  429: 'error_rate_limit',
  500: 'error_server',
  502: 'error_server',
  503: 'error_server',
};

// Generate idempotency key
function generateIdempotencyKey(): string {
  return `${Date.now()}-${Math.random().toString(36).substring(2, 11)}-${Math.random().toString(36).substring(2, 11)}`;
}

// Token management
export function setAuthToken(token: string, refresh?: string): void {
  accessToken = token;
  if (refresh) {
    refreshToken = refresh;
  }
  // In production, store in secure storage
  try {
    sessionStorage.setItem('zaphira_token', token);
    if (refresh) {
      sessionStorage.setItem('zaphira_refresh', refresh);
    }
  } catch (e) {
    console.warn('Unable to store token');
  }
}

export function clearAuthToken(): void {
  accessToken = null;
  refreshToken = null;
  try {
    sessionStorage.removeItem('zaphira_token');
    sessionStorage.removeItem('zaphira_refresh');
  } catch (e) {
    // Ignore
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

// Refresh token flow
async function refreshAccessToken(): Promise<string> {
  const storedRefresh = refreshToken || sessionStorage.getItem('zaphira_refresh');
  
  if (!storedRefresh) {
    throw new Error('No refresh token available');
  }

  const response = await fetch(`${API_BASE_URL}/api/auth/refresh`, {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
    },
    body: JSON.stringify({ refreshToken: storedRefresh }),
  });

  if (!response.ok) {
    clearAuthToken();
    throw new Error('Token refresh failed');
  }

  const data = await response.json();
  setAuthToken(data.accessToken, data.refreshToken);
  return data.accessToken;
}

// Handle token refresh with queue
async function handleTokenRefresh(): Promise<string> {
  if (isRefreshing) {
    return new Promise((resolve, reject) => {
      refreshQueue.push({ resolve, reject });
    });
  }

  isRefreshing = true;

  try {
    const newToken = await refreshAccessToken();
    refreshQueue.forEach(({ resolve }) => resolve(newToken));
    return newToken;
  } catch (error) {
    refreshQueue.forEach(({ reject }) => reject(error as Error));
    throw error;
  } finally {
    isRefreshing = false;
    refreshQueue = [];
  }
}

// Exponential backoff
function getBackoffDelay(attempt: number): number {
  const baseDelay = 1000;
  const maxDelay = 30000;
  const delay = Math.min(baseDelay * Math.pow(2, attempt), maxDelay);
  return delay + Math.random() * 1000; // Add jitter
}

// Main request function
async function request<T>(
  endpoint: string,
  options: RequestInit = {},
  config: {
    requiresAuth?: boolean;
    useIdempotency?: boolean;
    retries?: number;
    currentRetry?: number;
  } = {}
): Promise<T> {
  const {
    requiresAuth = true,
    useIdempotency = false,
    retries = 3,
    currentRetry = 0,
  } = config;

  // Check rate limit
  if (rateLimitRetryAfter && Date.now() < rateLimitRetryAfter) {
    const waitTime = Math.ceil((rateLimitRetryAfter - Date.now()) / 1000);
    throw {
      status: 429,
      message: 'error_rate_limit',
      retryAfter: waitTime,
    } as ApiError;
  }

  const headers: Record<string, string> = {
    'Content-Type': 'application/json',
    ...(options.headers as Record<string, string>),
  };

  // Add auth header
  if (requiresAuth) {
    const token = getAuthToken();
    if (token) {
      headers['Authorization'] = `Bearer ${token}`;
    }
  }

  // Add idempotency key for POST requests
  if (useIdempotency && options.method === 'POST') {
    headers['Idempotency-Key'] = generateIdempotencyKey();
  }

  try {
    const response = await fetch(`${API_BASE_URL}${endpoint}`, {
      ...options,
      headers,
    });

    // Handle rate limiting
    if (response.status === 429) {
      const retryAfter = response.headers.get('Retry-After');
      const waitSeconds = retryAfter ? parseInt(retryAfter, 10) : 60;
      rateLimitRetryAfter = Date.now() + waitSeconds * 1000;
      
      throw {
        status: 429,
        message: 'error_rate_limit',
        retryAfter: waitSeconds,
      } as ApiError;
    }

    // Handle 401 - attempt token refresh
    if (response.status === 401 && requiresAuth && currentRetry === 0) {
      try {
        await handleTokenRefresh();
        return request<T>(endpoint, options, { ...config, currentRetry: 1 });
      } catch {
        clearAuthToken();
        window.dispatchEvent(new CustomEvent('auth:logout'));
        throw {
          status: 401,
          message: 'error_session_expired',
        } as ApiError;
      }
    }

    // Handle other errors
    if (!response.ok) {
      const errorData = await response.json().catch(() => ({}));
      throw {
        status: response.status,
        message: ERROR_MESSAGES[response.status] || 'error_server',
        code: errorData.code,
        details: errorData.errors,
      } as ApiError;
    }

    // Parse response
    const contentType = response.headers.get('Content-Type');
    if (contentType?.includes('application/json')) {
      return response.json();
    }
    return {} as T;
  } catch (error) {
    // Network error - retry with backoff
    if (error instanceof TypeError && error.message === 'Failed to fetch') {
      if (currentRetry < retries) {
        const delay = getBackoffDelay(currentRetry);
        await new Promise((resolve) => setTimeout(resolve, delay));
        return request<T>(endpoint, options, {
          ...config,
          currentRetry: currentRetry + 1,
        });
      }
      throw {
        status: 0,
        message: 'error_network',
      } as ApiError;
    }
    throw error;
  }
}

// API methods
export const api = {
  // Auth
  login: (phoneNumber: string, pin: string) =>
    request<{ accessToken: string; refreshToken: string; user: unknown }>(
      '/api/auth/login',
      {
        method: 'POST',
        body: JSON.stringify({ phoneNumber, pin }),
      },
      { requiresAuth: false }
    ),

  // Users
  register: (data: { phoneNumber: string; name: string; email?: string }) =>
    request<{ userId: string }>('/api/users/register', {
      method: 'POST',
      body: JSON.stringify(data),
    }, { requiresAuth: false }),

  verifyOtp: (phoneNumber: string, otp: string) =>
    request<{ verified: boolean }>('/api/users/verify-otp', {
      method: 'POST',
      body: JSON.stringify({ phoneNumber, otp }),
    }, { requiresAuth: false }),

  getProfile: () =>
    request<{
      id: string;
      name: string;
      phoneNumber: string;
      email?: string;
      walletNumber: string;
    }>('/api/users/profile'),

  updateProfile: (data: Partial<{ name: string; email: string }>) =>
    request('/api/users/profile', {
      method: 'PUT',
      body: JSON.stringify(data),
    }),

  // Wallets
  getWalletSummary: (userId: string) =>
    request<{
      walletNumber: string;
      balance: number;
      currency: string;
      status: string;
    }>(`/api/wallets/user/${userId}/summary`),

  // Transactions
  createTransaction: (data: {
    senderWalletNumber: string;
    receiverWalletNumber: string;
    amount: number;
    currency: string;
    type: string;
    channel: string;
    requestedBy: string;
    description?: string;
    processInstantly?: boolean;
  }) =>
    request<{
      transactionReference: string;
      status: string;
      amount: number;
      fees: number;
    }>('/api/transactions', {
      method: 'POST',
      body: JSON.stringify(data),
    }, { useIdempotency: true }),

  getTransactions: (walletNumber: string, params?: { page?: number; limit?: number }) =>
    request<{
      transactions: Array<{
        id: string;
        reference: string;
        type: string;
        amount: number;
        currency: string;
        status: string;
        description?: string;
        createdAt: string;
        counterparty: {
          name: string;
          walletNumber: string;
        };
      }>;
      total: number;
      page: number;
    }>(`/api/transactions/wallet/${walletNumber}?${new URLSearchParams(params as Record<string, string>).toString()}`),

  // Exchange rates (public)
  getExchangeRates: (from: string, to: string) =>
    request<{
      from: string;
      to: string;
      rate: number;
      expiresAt: string;
    }>(`/api/exchange-rates?from=${from}&to=${to}`, {}, { requiresAuth: false }),
};

export default api;
