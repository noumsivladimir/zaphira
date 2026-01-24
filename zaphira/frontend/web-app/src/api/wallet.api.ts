/**
 * Wallet API
 */
import { apiRequest } from './api';

export type WalletStatus = 'ACTIVE' | 'FROZEN' | 'SUSPENDED' | 'CLOSED';
export type WalletType = 'PRIMARY' | 'SECONDARY' | 'MERCHANT' | 'SUB_WALLET' | string;
export type SubWalletType = 'SPENDING' | 'SAVINGS' | 'ESCROW' | 'BONUS' | 'FEES';

export interface SubWallet {
  id: number;
  subWalletName: string;
  type: SubWalletType;
  currency: string;
  status: WalletStatus;
  availableBalance: number;
  blockedBalance: number;
  totalBalance: number;
  createdAt?: string;
  updatedAt?: string;
}

export interface WalletDTO {
  id: number;
  userId: number;
  walletNumber: string;
  type?: WalletType;
  status?: WalletStatus;
  currency?: string;
  availableBalance?: number;
  blockedBalance?: number;
  totalBalance?: number;
  subWallets?: SubWallet[];
  frozenReason?: string | null;
  frozenAt?: string | null;
  createdAt?: string;
  updatedAt?: string;
}

export interface WalletSummaryDTO {
  userId: number;
  walletId: number;
  totalWallets: number;
  walletNumber: string;
  subWalletsName: string[];
  totalBalanceAllWallets: number;
  currency: string;
}

export interface CreateWalletRequest {
  userId: number;
  availableBalance?: number;
  type?: string;
  status?: string;
}

export interface CreateWalletResponse {
  id: number;
  userId: number;
  walletNumber: string;
  type?: string;
  status?: string;
  merchantCode?: string | null;
  merchantName?: string | null;
}

export interface CreateSubWalletRequest {
  subWalletName: string;
  type: SubWalletType;
  walletNumberWhichCanManage: string[];
}

export type SubWalletResponse = SubWallet;

export interface FreezeWalletRequest {
  reason: string;
  frozenBy: string;
  notes?: string;
}

export async function createWallet(request: CreateWalletRequest): Promise<CreateWalletResponse> {
  return apiRequest<CreateWalletResponse>('/wallets', {
    method: 'POST',
    body: JSON.stringify(request),
  });
}

export async function getWalletByNumber(walletNumber: string): Promise<WalletDTO> {
  return apiRequest<WalletDTO>(`/wallets/${walletNumber}`, {
    method: 'GET',
  });
}

export async function getSubWalletsByWalletId(walletId: string | number): Promise<SubWalletResponse[]> {
  return apiRequest<SubWalletResponse[]>(`/wallets/id/${walletId}/subwallets`, {
    method: 'GET',
  });
}

export async function getWalletSummaryByUser(userId: number): Promise<WalletSummaryDTO> {
  return apiRequest<WalletSummaryDTO>(`/wallets/user/${userId}/summary`, {
    method: 'GET',
  });
}

export async function createSubWallet(request: CreateSubWalletRequest): Promise<SubWalletResponse> {
  return apiRequest<SubWalletResponse>('/wallet/subWallet/create', {
    method: 'POST',
    body: JSON.stringify(request),
  });
}

export async function freezeWallet(walletNumber: string, request: FreezeWalletRequest): Promise<WalletDTO> {
  return apiRequest<WalletDTO>(`/wallets/${walletNumber}/freeze`, {
    method: 'PUT',
    body: JSON.stringify(request),
  });
}

export async function unfreezeWallet(walletNumber: string, unfrozenBy: string, notes?: string): Promise<WalletDTO> {
  const params = new URLSearchParams({ unfrozenBy });
  if (notes) {
    params.set('notes', notes);
  }
  return apiRequest<WalletDTO>(`/wallets/${walletNumber}/unfreeze?${params.toString()}`, {
    method: 'PUT',
  });
}
