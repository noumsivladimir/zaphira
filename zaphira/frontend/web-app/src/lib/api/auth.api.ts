/**
 * Authentication API
 * Handles login, logout, token refresh, PIN management
 */

import { api } from './apiClient';

export interface LoginRequest {
  phoneNumber: string;
  pin: string;
}

export interface LoginResponse {
  accessToken: string;
  refreshToken: string;
  user: {
    id: string;
    name: string;
    phoneNumber: string;
    walletNumber: string;
  };
}

export interface VerifyOtpRequest {
  phoneNumber: string;
  otp: string;
}

export interface VerifyOtpResponse {
  verified: boolean;
  tempToken?: string;
}

export interface ResetPinRequest {
  phoneNumber: string;
}

export interface SetNewPinRequest {
  tempToken: string;
  newPin: string;
}

export interface ChangePinRequest {
  currentPin: string;
  newPin: string;
}

/**
 * Login with phone number and PIN
 */
export const login = (data: LoginRequest): Promise<LoginResponse> => {
  return api.login(data.phoneNumber, data.pin) as Promise<LoginResponse>;
};

/**
 * Verify OTP code
 */
export const verifyOtp = (data: VerifyOtpRequest): Promise<VerifyOtpResponse> => {
  return api.verifyOtp(data.phoneNumber, data.otp) as Promise<VerifyOtpResponse>;
};

/**
 * Initiate PIN reset - sends OTP
 */
export const requestPinReset = async (data: ResetPinRequest): Promise<{ otpSent: boolean }> => {
  // Backend endpoint: POST /api/pin-reset/request
  const response = await fetch(`${import.meta.env.VITE_API_URL || 'https://api.zaphira.app'}/api/pin-reset/request`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(data),
  });

  if (!response.ok) {
    throw new Error('Failed to request PIN reset');
  }

  return response.json();
};

/**
 * Verify OTP for PIN reset
 */
export const verifyPinResetOtp = async (data: VerifyOtpRequest): Promise<{ tempToken: string }> => {
  // Backend endpoint: POST /api/pin-reset/verify-otp
  const response = await fetch(`${import.meta.env.VITE_API_URL || 'https://api.zaphira.app'}/api/pin-reset/verify-otp`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(data),
  });

  if (!response.ok) {
    throw new Error('OTP verification failed');
  }

  return response.json();
};

/**
 * Answer security questions for PIN reset
 */
export const answerSecurityQuestions = async (data: {
  tempToken: string;
  answers: Array<{ questionId: string; answer: string }>;
}): Promise<{ verified: boolean; resetToken: string }> => {
  // Backend endpoint: POST /api/pin-reset/verify-security
  const response = await fetch(`${import.meta.env.VITE_API_URL || 'https://api.zaphira.app'}/api/pin-reset/verify-security`, {
    method: 'POST',
    headers: { 
      'Content-Type': 'application/json',
      'Authorization': `Bearer ${data.tempToken}`
    },
    body: JSON.stringify({ answers: data.answers }),
  });

  if (!response.ok) {
    throw new Error('Security questions verification failed');
  }

  return response.json();
};

/**
 * Set new PIN after reset
 */
export const setNewPin = async (data: SetNewPinRequest): Promise<{ success: boolean }> => {
  // Backend endpoint: POST /api/pin-reset/complete
  const response = await fetch(`${import.meta.env.VITE_API_URL || 'https://api.zaphira.app'}/api/pin-reset/complete`, {
    method: 'POST',
    headers: { 
      'Content-Type': 'application/json',
      'Authorization': `Bearer ${data.tempToken}`
    },
    body: JSON.stringify({ newPin: data.newPin }),
  });

  if (!response.ok) {
    throw new Error('Failed to set new PIN');
  }

  return response.json();
};

/**
 * Change PIN (requires current PIN)
 */
export const changePin = async (data: ChangePinRequest): Promise<{ success: boolean }> => {
  // Backend endpoint: POST /api/users/change-pin
  const response = await fetch(`${import.meta.env.VITE_API_URL || 'https://api.zaphira.app'}/api/users/change-pin`, {
    method: 'POST',
    headers: { 
      'Content-Type': 'application/json',
      'Authorization': `Bearer ${sessionStorage.getItem('zaphira_token')}`
    },
    body: JSON.stringify(data),
  });

  if (!response.ok) {
    throw new Error('Failed to change PIN');
  }

  return response.json();
};
