/**
 * Authentication Service
 * Handles all authentication-related API calls to Zaphira backend
 */

import { apiRequest } from '../api-client';
import type {
  LoginRequest,
  LoginResponse,
  RefreshTokenRequest,
  RefreshTokenResponse,
  EmailVerificationRequest,
  EmailVerificationResponse,
  SendOtpRequest,
  VerifyOtpRequest
} from '../types/api-types';

/**
 * Login with phone number and PIN
 * POST /api/auth/login
 */
export async function login(credentials: LoginRequest): Promise<LoginResponse> {
  return apiRequest<LoginResponse>('/api/auth/login', {
    method: 'POST',
    body: JSON.stringify(credentials),
  });
}

/**
 * Refresh access token
 * POST /api/auth/refresh
 */
export async function refreshToken(request: RefreshTokenRequest): Promise<RefreshTokenResponse> {
  return apiRequest<RefreshTokenResponse>('/api/auth/refresh', {
    method: 'POST',
    body: JSON.stringify(request),
  });
}

/**
 * Validate JWT token
 * GET /api/auth/validate
 */
export async function validateToken(): Promise<{ valid: boolean }> {
  return apiRequest<{ valid: boolean }>('/api/auth/validate', {
    method: 'GET',
  });
}

/**
 * Send OTP for various purposes
 * POST /api/auth/otp/send
 */
export async function sendOtp(request: SendOtpRequest): Promise<{ success: boolean; message: string }> {
  return apiRequest<{ success: boolean; message: string }>('/api/auth/otp/send', {
    method: 'POST',
    body: JSON.stringify(request),
  });
}

/**
 * Verify OTP code
 * POST /api/auth/otp/verify
 */
export async function verifyOtp(request: VerifyOtpRequest): Promise<{ success: boolean; token?: string }> {
  return apiRequest<{ success: boolean; token?: string }>('/api/auth/otp/verify', {
    method: 'POST',
    body: JSON.stringify(request),
  });
}

/**
 * Logout (client-side cleanup)
 */
export function logout(): void {
  // Clear tokens from storage
  try {
    sessionStorage.removeItem('zaphira_token');
    sessionStorage.removeItem('zaphira_refresh');
    localStorage.removeItem('zaphira_token');
    localStorage.removeItem('zaphira_refresh');
  } catch (error) {
    console.error('Error during logout:', error);
  }
}
