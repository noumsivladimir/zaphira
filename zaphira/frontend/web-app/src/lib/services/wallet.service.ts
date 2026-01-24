/**
 * Wallet Service
 * Handles all wallet-related API calls to Zaphira backend
 */

import { apiRequest } from '../api-client';
import type {
  WalletResponse,
  CreateWalletRequest
} from '../types/api-types';

/**
 * Get wallet by wallet number
 * GET /api/wallets/{walletNumber}
 */
export async function getWallet(walletNumber: string): Promise<WalletResponse> {
  return apiRequest<WalletResponse>(`/api/wallets/${walletNumber}`, {
    method: 'GET',
    authenticated: true,
  });
}

/**
 * Get all wallets for a user
 * GET /api/wallets/user/{userId}
 */
export async function getUserWallets(userId: number): Promise<WalletResponse[]> {
  return apiRequest<WalletResponse[]>(`/api/wallets/user/${userId}`, {
    method: 'GET',
    authenticated: true,
  });
}

/**
 * Get primary wallet for a user
 * GET /api/wallets/user/{userId}/primary
 */
export async function getPrimaryWallet(userId: number): Promise<WalletResponse> {
  return apiRequest<WalletResponse>(`/api/wallets/user/${userId}/primary`, {
    method: 'GET',
    authenticated: true,
  });
}

/**
 * Create a new wallet
 * POST /api/wallets
 */
export async function createWallet(request: CreateWalletRequest): Promise<WalletResponse> {
  return apiRequest<WalletResponse>('/api/wallets', {
    method: 'POST',
    body: JSON.stringify(request),
    authenticated: true,
  });
}
