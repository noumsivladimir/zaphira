/**
 * Auth API
 */
import { apiRequest, setAuthToken } from './api';

export interface LoginRequest {
  phoneNumber: string;
  pin: string;
}

export interface AuthUser {
  id: number;
  firstName: string;
  lastName: string;
  email?: string | null;
  accountStatus?: string | null;
  role: string;
  walletId: string | null;
}

export interface AuthResponse {
  accessToken: string;
  refreshToken?: string | null;
  user: AuthUser;
}

export async function login(request: LoginRequest): Promise<AuthResponse> {
  const response = await apiRequest<AuthResponse>('/auth/login', {
    method: 'POST',
    body: JSON.stringify(request),
  }, { requiresAuth: false });

  if (response.accessToken) {
    setAuthToken(response.accessToken, response.refreshToken || null);
  }

  return response;
}
