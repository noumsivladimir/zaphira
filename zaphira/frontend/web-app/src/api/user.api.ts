/**
 * User API
 */
import { apiRequest } from './api';

export interface UserRegistrationRequest {
  phoneNumber: string;
  pin: string;
  email: string;
  firstName: string;
  lastName: string;
  dateOfBirth: string; // ISO date (YYYY-MM-DD)
  country: string;
  city?: string;
  neighborhood?: string;
  region?: string;
  preferredLanguage?: string;
  securityAnswers: SecurityAnswerRequest[];
}

export interface SecurityAnswerRequest {
  questionId: number;
  answer: string;
}

export interface UpdateUserProfileRequest {
  firstName?: string;
  lastName?: string;
  email?: string;
}

export interface VerifyPinRequest {
  oldPin: string;
}

export interface ChangePinRequest {
  walletNumber: string;
  oldPin: string;
  newPin: string;
  newPinConfirmation: string;
}

export interface UsersRegistrationResponse {
  userId: number;
  walletId?: string | null;
  phoneNumber: string;
  firstName: string;
  lastName: string;
  email: string;
  dateOfBirth?: string;
  country?: string;
  preferredCurrency?: string;
  registrationDate?: string;
  accountStatus?: string;
  roleType?: string;
}

export interface ApiResponse<T> {
  success: boolean;
  message: string;
  data: T;
  timestamp?: string;
  path?: string;
}

export interface VerifyOtpRequest {
  phoneNumber: string;
  otpCode: string;
}

export interface VerifyEmailRequest {
  email: string;
  verificationCode: string;
}

export interface SecurityQuestionResponse {
  id: number;
  question: string;
}

export async function registerUser(request: UserRegistrationRequest): Promise<ApiResponse<UsersRegistrationResponse>> {
  return apiRequest<ApiResponse<UsersRegistrationResponse>>('/users/register', {
    method: 'POST',
    body: JSON.stringify(request),
  }, { requiresAuth: false });
}

export async function updateUserProfile(request: UpdateUserProfileRequest): Promise<ApiResponse<UsersRegistrationResponse>> {
  return apiRequest<ApiResponse<UsersRegistrationResponse>>('/users/profile', {
    method: 'PUT',
    body: JSON.stringify(request),
  });
}

export async function verifyCurrentPin(walletNumber: string, request: VerifyPinRequest): Promise<ApiResponse<{ success: boolean; message: string }>> {
  return apiRequest<ApiResponse<{ success: boolean; message: string }>>(`/users/${walletNumber}/pin/verify`, {
    method: 'POST',
    body: JSON.stringify(request),
  });
}

export async function changePin(walletNumber: string, request: ChangePinRequest): Promise<ApiResponse<{ success: boolean; message: string }>> {
  return apiRequest<ApiResponse<{ success: boolean; message: string }>>(`/users/${walletNumber}/pin`, {
    method: 'PUT',
    body: JSON.stringify(request),
  });
}

export async function getSecurityQuestions(): Promise<ApiResponse<SecurityQuestionResponse[]>> {
  return apiRequest<ApiResponse<SecurityQuestionResponse[]>>('/users/security-questions', {
    method: 'GET',
  }, { requiresAuth: false });
}

export async function verifyOtp(request: VerifyOtpRequest): Promise<ApiResponse<UsersRegistrationResponse>> {
  return apiRequest<ApiResponse<UsersRegistrationResponse>>('/users/verify-otp', {
    method: 'POST',
    body: JSON.stringify(request),
  }, { requiresAuth: false });
}

export async function verifyEmail(request: VerifyEmailRequest): Promise<ApiResponse<UsersRegistrationResponse>> {
  return apiRequest<ApiResponse<UsersRegistrationResponse>>('/users/verify-email', {
    method: 'POST',
    body: JSON.stringify(request),
  }, { requiresAuth: false });
}

export async function resendOtp(userId: number): Promise<ApiResponse<Record<string, unknown>>> {
  return apiRequest<ApiResponse<Record<string, unknown>>>(`/users/send-otp/${userId}`, {
    method: 'POST',
  }, { requiresAuth: false });
}
