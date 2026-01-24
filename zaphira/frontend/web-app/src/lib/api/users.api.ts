/**
 * Users API
 * Handles user registration, profile management, security questions
 */

import { api } from './apiClient';

export interface User {
  id: string;
  name: string;
  phoneNumber: string;
  email?: string;
  walletNumber: string;
  createdAt?: string;
}

export interface RegisterRequest {
  phoneNumber: string;
  name: string;
  email?: string;
  pin: string;
}

export interface RegisterResponse {
  userId: string;
  otpSent: boolean;
}

export interface UpdateProfileRequest {
  name?: string;
  email?: string;
}

export interface SecurityQuestion {
  id: string;
  question: string;
  category: string;
}

export interface SecurityAnswer {
  questionId: string;
  answer: string;
}

/**
 * Register new user
 */
export const register = async (data: RegisterRequest): Promise<RegisterResponse> => {
  // Backend endpoint: POST /api/users/register
  const response = await fetch(`${import.meta.env.VITE_API_URL || 'https://api.zaphira.app'}/api/users/register`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(data),
  });

  if (!response.ok) {
    const error = await response.json();
    throw new Error(error.message || 'Registration failed');
  }

  return response.json();
};

/**
 * Get current user profile
 */
export const getProfile = (): Promise<User> => {
  return api.getProfile() as Promise<User>;
};

/**
 * Update user profile
 */
export const updateProfile = (data: UpdateProfileRequest): Promise<User> => {
  return api.updateProfile(data) as Promise<User>;
};

/**
 * Get available security questions
 */
export const getSecurityQuestions = async (): Promise<SecurityQuestion[]> => {
  // Backend endpoint: GET /api/security-questions
  const response = await fetch(`${import.meta.env.VITE_API_URL || 'https://api.zaphira.app'}/api/security-questions`, {
    headers: { 
      'Authorization': `Bearer ${sessionStorage.getItem('zaphira_token')}`
    },
  });

  if (!response.ok) {
    throw new Error('Failed to fetch security questions');
  }

  return response.json();
};

/**
 * Get user's answered security questions
 */
export const getUserSecurityQuestions = async (): Promise<Array<{ id: string; question: string }>> => {
  // Backend endpoint: GET /api/security-questions/user
  const response = await fetch(`${import.meta.env.VITE_API_URL || 'https://api.zaphira.app'}/api/security-questions/user`, {
    headers: { 
      'Authorization': `Bearer ${sessionStorage.getItem('zaphira_token')}`
    },
  });

  if (!response.ok) {
    throw new Error('Failed to fetch user security questions');
  }

  return response.json();
};

/**
 * Set up security questions and answers
 */
export const setupSecurityQuestions = async (answers: SecurityAnswer[]): Promise<{ success: boolean }> => {
  // Backend endpoint: POST /api/security-questions/setup
  const response = await fetch(`${import.meta.env.VITE_API_URL || 'https://api.zaphira.app'}/api/security-questions/setup`, {
    method: 'POST',
    headers: { 
      'Content-Type': 'application/json',
      'Authorization': `Bearer ${sessionStorage.getItem('zaphira_token')}`
    },
    body: JSON.stringify({ answers }),
  });

  if (!response.ok) {
    throw new Error('Failed to setup security questions');
  }

  return response.json();
};

/**
 * Update security question answers
 */
export const updateSecurityQuestions = async (answers: SecurityAnswer[]): Promise<{ success: boolean }> => {
  // Backend endpoint: PUT /api/security-questions
  const response = await fetch(`${import.meta.env.VITE_API_URL || 'https://api.zaphira.app'}/api/security-questions`, {
    method: 'PUT',
    headers: { 
      'Content-Type': 'application/json',
      'Authorization': `Bearer ${sessionStorage.getItem('zaphira_token')}`
    },
    body: JSON.stringify({ answers }),
  });

  if (!response.ok) {
    throw new Error('Failed to update security questions');
  }

  return response.json();
};
