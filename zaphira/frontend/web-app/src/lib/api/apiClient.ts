/**
 * API Client - Re-export from main api-client for backward compatibility
 * Centralized API client with auth, retry, idempotency
 */

export { api, setAuthToken, clearAuthToken, getAuthToken } from '../api-client';
export type { ApiError, ApiResponse } from '../api-client';
