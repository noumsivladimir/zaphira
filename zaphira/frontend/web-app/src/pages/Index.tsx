import React, { useState, useCallback } from 'react';
import { useNavigate } from 'react-router-dom';
import { AnimatePresence } from 'framer-motion';
import '@/lib/i18n';
import Login from './auth/Login';
import type { AuthResponse } from '@/api/auth.api';
import OTPVerification from './auth/OTPVerification';
import Dashboard from './Dashboard';
import Transfer from './wallet/Transfer';
import { Transaction } from '@/components/TransactionItem';

type AppScreen = 'login' | 'otp' | 'dashboard' | 'transfer';

// Mock data for demo
const mockUser = {
  name: 'Marie Nkeng',
  walletNumber: '10000001',
};

const mockTransactions: Transaction[] = [
  {
    id: '1',
    reference: 'TXN-001',
    type: 'TRANSFER',
    direction: 'out',
    amount: 25000,
    currency: 'XAF',
    status: 'COMPLETED',
    description: 'Paiement loyer',
    createdAt: '2026-01-14T10:30:00Z',
    counterparty: { name: 'Jean Kamga', walletNumber: '10000002' },
  },
  {
    id: '2',
    reference: 'TXN-002',
    type: 'TRANSFER',
    direction: 'in',
    amount: 50000,
    currency: 'XAF',
    status: 'COMPLETED',
    description: 'Remboursement',
    createdAt: '2026-01-13T15:45:00Z',
    counterparty: { name: 'Paul Mballa', walletNumber: '10000003' },
  },
  {
    id: '3',
    reference: 'TXN-003',
    type: 'DEPOSIT',
    direction: 'in',
    amount: 100000,
    currency: 'XAF',
    status: 'PENDING',
    createdAt: '2026-01-13T09:00:00Z',
    counterparty: { name: 'Mobile Money', walletNumber: 'SYSTEM' },
  },
  {
    id: '4',
    reference: 'TXN-004',
    type: 'TRANSFER',
    direction: 'out',
    amount: 15000,
    currency: 'XAF',
    status: 'COMPLETED',
    description: 'Courses marché',
    createdAt: '2026-01-12T18:20:00Z',
    counterparty: { name: 'Yvette Fouda', walletNumber: '10000004' },
  },
];

const Index = () => {
  const navigate = useNavigate();
  const [screen, setScreen] = useState<AppScreen>('login');
  const [phoneNumber, setPhoneNumber] = useState('');
  const [balance, setBalance] = useState(175000);
  const [transactions, setTransactions] = useState<Transaction[]>(mockTransactions);
  const [pendingTransfer, setPendingTransfer] = useState<Transaction | null>(null);

  // Login handler
  const handleLoginSuccess = useCallback(async (_response: AuthResponse, phone: string) => {
    setPhoneNumber(phone);
    setScreen('dashboard');
  }, []);

  // OTP verification
  const handleVerifyOTP = useCallback(async (otp: string) => {
    await new Promise((resolve) => setTimeout(resolve, 1000));
    if (otp === '123456') {
      setScreen('dashboard');
    } else {
      throw new Error('Code invalide');
    }
  }, []);

  const handleResendOTP = useCallback(async () => {
    await new Promise((resolve) => setTimeout(resolve, 500));
  }, []);

  // Quick action handler
  const handleAction = useCallback((action: string) => {
    if (action === 'send') {
      setScreen('transfer');
    }
    // Handle other actions...
  }, []);

  // Transfer handler with optimistic update
  const handleTransfer = useCallback(
    async (data: {
      recipientWallet: string;
      amount: number;
      description?: string;
      pin: string;
    }) => {
      const reference = `TXN-${Date.now()}`;

      // Create optimistic transaction
      const optimisticTx: Transaction = {
        id: reference,
        reference,
        type: 'TRANSFER',
        direction: 'out',
        amount: data.amount,
        currency: 'XAF',
        status: 'PENDING',
        description: data.description,
        createdAt: new Date().toISOString(),
        counterparty: {
          name: 'Jean Dupont',
          walletNumber: data.recipientWallet,
        },
      };

      // Optimistic update
      setPendingTransfer(optimisticTx);
      setTransactions((prev) => [optimisticTx, ...prev]);
      setBalance((prev) => prev - data.amount);

      // Simulate API call
      await new Promise((resolve) => setTimeout(resolve, 2000));

      // Update to completed
      setTransactions((prev) =>
        prev.map((tx) =>
          tx.id === reference ? { ...tx, status: 'COMPLETED' as const } : tx
        )
      );
      setPendingTransfer(null);

      return { reference };
    },
    []
  );

  const handleTransactionClick = useCallback((tx: Transaction) => {
    // Navigate to transaction details
    console.log('Transaction clicked:', tx.reference);
  }, []);

  return (
    <div className="min-h-screen bg-background">
      <AnimatePresence mode="wait">
        {screen === 'login' && (
          <Login
            key="login"
            onLoginSuccess={handleLoginSuccess}
            onForgotPin={() => navigate('/forgot-pin')}
            onRegister={() => navigate('/register')}
          />
        )}

        {screen === 'otp' && (
          <OTPVerification
            key="otp"
            phoneNumber={phoneNumber}
            onVerify={handleVerifyOTP}
            onResend={handleResendOTP}
            onBack={() => setScreen('login')}
          />
        )}

        {screen === 'dashboard' && (
          <Dashboard
            key="dashboard"
            user={mockUser}
            balance={balance}
            transactions={transactions}
            onAction={handleAction}
            onTransactionClick={handleTransactionClick}
            onViewAllTransactions={() => {}}
          />
        )}

        {screen === 'transfer' && (
          <Transfer
            key="transfer"
            balance={balance}
            onTransfer={handleTransfer}
            onClose={() => setScreen('dashboard')}
          />
        )}
      </AnimatePresence>
    </div>
  );
};

export default Index;
