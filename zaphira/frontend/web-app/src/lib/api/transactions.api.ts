/**
 * Transactions API
 * Handles money transfers, transaction history, scheduled transactions
 */

export interface Transaction {
  id: string;
  reference: string;
  type: 'SEND' | 'RECEIVE' | 'TOP_UP' | 'WITHDRAWAL' | 'FEE';
  amount: number;
  currency: string;
  status: 'PENDING' | 'COMPLETED' | 'FAILED' | 'CANCELLED';
  description?: string;
  createdAt: string;
  completedAt?: string;
  counterparty: {
    name: string;
    walletNumber: string;
  };
  fees?: number;
}

export interface CreateTransactionRequest {
  senderWalletNumber: string;
  receiverWalletNumber: string;
  amount: number;
  currency: string;
  type: string;
  channel: string;
  requestedBy: string;
  description?: string;
  processInstantly?: boolean;
}

export interface CreateTransactionResponse {
  transactionReference: string;
  status: string;
  amount: number;
  fees: number;
}

export interface ScheduledTransaction {
  id: string;
  walletNumber: string;
  receiverWalletNumber: string;
  amount: number;
  currency: string;
  frequency: 'DAILY' | 'WEEKLY' | 'MONTHLY';
  nextExecutionDate: string;
  status: 'ACTIVE' | 'PAUSED' | 'CANCELLED' | 'COMPLETED';
  description?: string;
  createdAt: string;
}

export interface CreateScheduledTransactionRequest {
  receiverWalletNumber: string;
  amount: number;
  currency: string;
  frequency: 'DAILY' | 'WEEKLY' | 'MONTHLY';
  startDate: string;
  endDate?: string;
  description?: string;
}

export interface TransactionFilters {
  type?: string;
  status?: string;
  startDate?: string;
  endDate?: string;
  minAmount?: number;
  maxAmount?: number;
  page?: number;
  limit?: number;
}

/**
 * Create a new transaction (send money)
 */
export const createTransaction = async (data: CreateTransactionRequest): Promise<CreateTransactionResponse> => {
  const response = await fetch(`${import.meta.env.VITE_API_URL || 'https://api.zaphira.app'}/api/transactions`, {
    method: 'POST',
    headers: { 
      'Content-Type': 'application/json',
      'Authorization': `Bearer ${sessionStorage.getItem('zaphira_token')}`,
      'Idempotency-Key': `${Date.now()}-${Math.random().toString(36).substring(2, 11)}`
    },
    body: JSON.stringify(data),
  });

  if (!response.ok) {
    const error = await response.json();
    throw new Error(error.message || 'Transaction failed');
  }

  return response.json();
};

/**
 * Get transaction history for wallet
 */
export const getTransactions = async (walletNumber: string, filters?: TransactionFilters): Promise<{
  transactions: Transaction[];
  total: number;
  page: number;
  totalPages: number;
}> => {
  const params = new URLSearchParams();
  if (filters) {
    Object.entries(filters).forEach(([key, value]) => {
      if (value !== undefined && value !== null) {
        params.append(key, value.toString());
      }
    });
  }

  const queryString = params.toString();
  const url = `${import.meta.env.VITE_API_URL || 'https://api.zaphira.app'}/api/transactions/wallet/${walletNumber}${queryString ? `?${queryString}` : ''}`;

  const response = await fetch(url, {
    headers: { 
      'Authorization': `Bearer ${sessionStorage.getItem('zaphira_token')}`
    },
  });

  if (!response.ok) {
    throw new Error('Failed to fetch transactions');
  }

  return response.json();
};

/**
 * Get transaction details by reference
 */
export const getTransactionByReference = async (reference: string): Promise<Transaction> => {
  const response = await fetch(`${import.meta.env.VITE_API_URL || 'https://api.zaphira.app'}/api/transactions/${reference}`, {
    headers: { 
      'Authorization': `Bearer ${sessionStorage.getItem('zaphira_token')}`
    },
  });

  if (!response.ok) {
    throw new Error('Transaction not found');
  }

  return response.json();
};

/**
 * Get scheduled transactions
 */
export const getScheduledTransactions = async (walletNumber: string): Promise<ScheduledTransaction[]> => {
  const response = await fetch(`${import.meta.env.VITE_API_URL || 'https://api.zaphira.app'}/api/transactions/scheduled?walletNumber=${walletNumber}`, {
    headers: { 
      'Authorization': `Bearer ${sessionStorage.getItem('zaphira_token')}`
    },
  });

  if (!response.ok) {
    throw new Error('Failed to fetch scheduled transactions');
  }

  return response.json();
};

/**
 * Create scheduled transaction
 */
export const createScheduledTransaction = async (walletNumber: string, data: CreateScheduledTransactionRequest): Promise<ScheduledTransaction> => {
  const response = await fetch(`${import.meta.env.VITE_API_URL || 'https://api.zaphira.app'}/api/transactions/scheduled`, {
    method: 'POST',
    headers: { 
      'Content-Type': 'application/json',
      'Authorization': `Bearer ${sessionStorage.getItem('zaphira_token')}`
    },
    body: JSON.stringify({ ...data, walletNumber }),
  });

  if (!response.ok) {
    const error = await response.json();
    throw new Error(error.message || 'Failed to create scheduled transaction');
  }

  return response.json();
};

/**
 * Cancel scheduled transaction
 */
export const cancelScheduledTransaction = async (id: string): Promise<{ success: boolean }> => {
  const response = await fetch(`${import.meta.env.VITE_API_URL || 'https://api.zaphira.app'}/api/transactions/scheduled/${id}/cancel`, {
    method: 'POST',
    headers: { 
      'Authorization': `Bearer ${sessionStorage.getItem('zaphira_token')}`
    },
  });

  if (!response.ok) {
    throw new Error('Failed to cancel scheduled transaction');
  }

  return response.json();
};

/**
 * Pause scheduled transaction
 */
export const pauseScheduledTransaction = async (id: string): Promise<{ success: boolean }> => {
  const response = await fetch(`${import.meta.env.VITE_API_URL || 'https://api.zaphira.app'}/api/transactions/scheduled/${id}/pause`, {
    method: 'POST',
    headers: { 
      'Authorization': `Bearer ${sessionStorage.getItem('zaphira_token')}`
    },
  });

  if (!response.ok) {
    throw new Error('Failed to pause scheduled transaction');
  }

  return response.json();
};

/**
 * Resume scheduled transaction
 */
export const resumeScheduledTransaction = async (id: string): Promise<{ success: boolean }> => {
  const response = await fetch(`${import.meta.env.VITE_API_URL || 'https://api.zaphira.app'}/api/transactions/scheduled/${id}/resume`, {
    method: 'POST',
    headers: { 
      'Authorization': `Bearer ${sessionStorage.getItem('zaphira_token')}`
    },
  });

  if (!response.ok) {
    throw new Error('Failed to resume scheduled transaction');
  }

  return response.json();
};
