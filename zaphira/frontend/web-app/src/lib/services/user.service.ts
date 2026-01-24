/**
 * User Service
 * Handles all user-related API calls to Zaphira backend
 */

import { apiRequest } from '../api-client';
import type {
  UserRegistrationRequest,
  UserRegistrationResponse,
  EmailVerificationRequest,
  EmailVerificationResponse,
  UserResponse,
  UpdateUserProfileRequest
} from '../types/api-types';

/**
 * Register a new user
 * POST /api/users/register
 */
export async function registerUser(request: UserRegistrationRequest): Promise<UserRegistrationResponse> {
  return apiRequest<UserRegistrationResponse>('/api/users/register', {
    method: 'POST',
    body: JSON.stringify(request),
  });
}

/**
 * Verify email with OTP code
 * POST /api/users/verify-email
 */
export async function verifyEmail(request: EmailVerificationRequest): Promise<EmailVerificationResponse> {
  return apiRequest<EmailVerificationResponse>('/api/users/verify-email', {
    method: 'POST',
    body: JSON.stringify(request),
  });
}

/**
 * Get current user profile
 * GET /api/users/profile
 */
export async function getUserProfile(): Promise<UserResponse> {
  return apiRequest<UserResponse>('/api/users/profile', {
    method: 'GET',
    authenticated: true,
  });
}

/**
 * Update user profile
 * PUT /api/users/profile
 */
export async function updateUserProfile(request: UpdateUserProfileRequest): Promise<UserResponse> {
  return apiRequest<UserResponse>('/api/users/profile', {
    method: 'PUT',
    body: JSON.stringify(request),
    authenticated: true,
  });
}

/**
 * Get user by ID
 * GET /api/users/{id}
 */
export async function getUserById(userId: number): Promise<UserResponse> {
  return apiRequest<UserResponse>(`/api/users/${userId}`, {
    method: 'GET',
    authenticated: true,
  });
}
