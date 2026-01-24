/**
 * Wallet API
 * Handles wallet operations, balance, sub-wallets, status management
 */

export interface Wallet {
  id: string;
  walletNumber: string;
  balance: number;
  currency: string;
  status: 'ACTIVE' | 'FROZEN' | 'CLOSED';
  type: 'MAIN' | 'SUB';
  userId: string;
  createdAt: string;
}

export interface WalletSummary {
  walletNumber: string;
  balance: number;
  currency: string;
  status: string;
  totalSubWallets?: number;
}

export interface CreateSubWalletRequest {
  name: string;
  initialBalance?: number;
}

export interface SubWallet {
  id: string;
  name: string;
  walletNumber: string;
  balance: number;
  currency: string;
  status: string;
  parentWalletId: string;
}

/**
 * Get wallet summary for user
 */
export const getWalletSummary = async (userId: string): Promise<WalletSummary> => {
  const response = await fetch(`${import.meta.env.VITE_API_URL || 'https://api.zaphira.app'}/api/wallets/user/${userId}/summary`, {
    headers: { 
      'Authorization': `Bearer ${sessionStorage.getItem('zaphira_token')}`
    },
  });

  if (!response.ok) {
    throw new Error('Failed to fetch wallet summary');
  }

  return response.json();
};

/**
 * Get wallet details
 */
export const getWalletDetails = async (walletNumber: string): Promise<Wallet> => {
  const response = await fetch(`${import.meta.env.VITE_API_URL || 'https://api.zaphira.app'}/api/wallets/${walletNumber}`, {
    headers: { 
      'Authorization': `Bearer ${sessionStorage.getItem('zaphira_token')}`
    },
  });

  if (!response.ok) {
    throw new Error('Failed to fetch wallet details');
  }

  return response.json();
};

/**
 * Get all sub-wallets
 */
export const getSubWallets = async (walletNumber: string): Promise<SubWallet[]> => {
  const response = await fetch(`${import.meta.env.VITE_API_URL || 'https://api.zaphira.app'}/api/wallets/${walletNumber}/sub-wallets`, {
    headers: { 
      'Authorization': `Bearer ${sessionStorage.getItem('zaphira_token')}`
    },
  });

  if (!response.ok) {
    throw new Error('Failed to fetch sub-wallets');
  }

  return response.json();
};

/**
 * Create sub-wallet
 */
export const createSubWallet = async (walletNumber: string, data: CreateSubWalletRequest): Promise<SubWallet> => {
  const response = await fetch(`${import.meta.env.VITE_API_URL || 'https://api.zaphira.app'}/api/wallets/${walletNumber}/sub-wallets`, {
    method: 'POST',
    headers: { 
      'Content-Type': 'application/json',
      'Authorization': `Bearer ${sessionStorage.getItem('zaphira_token')}`
    },
    body: JSON.stringify(data),
  });

  if (!response.ok) {
    const error = await response.json();
    throw new Error(error.message || 'Failed to create sub-wallet');
  }

  return response.json();
};

/**
 * Freeze wallet
 */
export const freezeWallet = async (walletNumber: string): Promise<{ success: boolean }> => {
  const response = await fetch(`${import.meta.env.VITE_API_URL || 'https://api.zaphira.app'}/api/wallets/${walletNumber}/freeze`, {
    method: 'POST',
    headers: { 
      'Authorization': `Bearer ${sessionStorage.getItem('zaphira_token')}`
    },
  });

  if (!response.ok) {
    throw new Error('Failed to freeze wallet');
  }

  return response.json();
};

/**
 * Unfreeze wallet
 */
export const unfreezeWallet = async (walletNumber: string): Promise<{ success: boolean }> => {
  const response = await fetch(`${import.meta.env.VITE_API_URL || 'https://api.zaphira.app'}/api/wallets/${walletNumber}/unfreeze`, {
    method: 'POST',
    headers: { 
      'Authorization': `Bearer ${sessionStorage.getItem('zaphira_token')}`
    },
  });

  if (!response.ok) {
    throw new Error('Failed to unfreeze wallet');
  }

  return response.json();
};

/**
 * Get wallet balance
 */
export const getWalletBalance = async (walletNumber: string): Promise<{ balance: number; currency: string }> => {
  const response = await fetch(`${import.meta.env.VITE_API_URL || 'https://api.zaphira.app'}/api/wallets/${walletNumber}/balance`, {
    headers: { 
      'Authorization': `Bearer ${sessionStorage.getItem('zaphira_token')}`
    },
  });

  if (!response.ok) {
    throw new Error('Failed to fetch wallet balance');
  }

  return response.json();
};

/**
 * Top-up wallet (recharge)
 */
export interface TopUpRequest {
  walletNumber: string;
  amount: number;
  currency: string;
  method: 'mobile_money' | 'card' | 'bank_transfer';
  phoneNumber?: string;
  operator?: string;
  cardNumber?: string;
  cardExpiry?: string;
  cardCvv?: string;
  cardName?: string;
}

export interface TopUpResponse {
  transactionReference: string;
  status: string;
  amount: number;
  paymentUrl?: string; // For card payments
  qrCode?: string; // For mobile money
}

export const topUpWallet = async (data: TopUpRequest): Promise<TopUpResponse> => {
  const response = await fetch(`${import.meta.env.VITE_API_URL || 'https://api.zaphira.app'}/api/wallets/top-up`, {
    method: 'POST',
    headers: { 
      'Content-Type': 'application/json',
      'Authorization': `Bearer ${sessionStorage.getItem('zaphira_token')}`
    },
    body: JSON.stringify(data),
  });

  if (!response.ok) {
    const error = await response.json();
    throw new Error(error.message || 'Failed to top up wallet');
  }

  return response.json();
};

/**
 * Withdraw from wallet
 */
export interface WithdrawRequest {
  walletNumber: string;
  amount: number;
  currency: string;
  method: 'mobile_money' | 'bank';
  phoneNumber?: string;
  operator?: string;
  bankAccount?: string;
  bankCode?: string;
  accountName?: string;
  pin: string;
}

export interface WithdrawResponse {
  transactionReference: string;
  status: string;
  amount: number;
  estimatedTime?: string;
}

export const withdrawFromWallet = async (data: WithdrawRequest): Promise<WithdrawResponse> => {
  const response = await fetch(`${import.meta.env.VITE_API_URL || 'https://api.zaphira.app'}/api/wallets/withdraw`, {
    method: 'POST',
    headers: { 
      'Content-Type': 'application/json',
      'Authorization': `Bearer ${sessionStorage.getItem('zaphira_token')}`
    },
    body: JSON.stringify(data),
  });

  if (!response.ok) {
    const error = await response.json();
    throw new Error(error.message || 'Failed to withdraw from wallet');
  }

  return response.json();
};
