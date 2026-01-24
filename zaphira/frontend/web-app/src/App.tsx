import { useEffect, useState } from "react";
import { Toaster } from "@/components/ui/toaster";
import { Toaster as Sonner } from "@/components/ui/sonner";
import { TooltipProvider } from "@/components/ui/tooltip";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { BrowserRouter, Routes, Route, useNavigate, useParams } from "react-router-dom";

// Pages principales
import Index from "./pages/Index";
import NotFound from "./pages/NotFound";
import Dashboard from "./pages/Dashboard";

// Pages d'authentification
import Login from "./pages/auth/Login";
import Register from "./pages/auth/Register";
import ForgotPin from "./pages/auth/ForgotPin";
import OTPVerification from "./pages/auth/OTPVerification";
import SecurityQuestionsSetup from "./pages/auth/SecurityQuestionsSetup";

// Pages wallet
import Transfer from "./pages/wallet/Transfer";
import Withdraw from "./pages/wallet/Withdraw";
import TopUp from "./pages/wallet/TopUp";
import ReceiveMoney from "./pages/wallet/ReceiveMoney";
import WalletDetails from "./pages/wallet/WalletDetails";
import WalletManagement from "./pages/wallet/WalletManagement";
import WalletsManagement from "./pages/wallet/WalletsManagement";
import TransferBetweenWallets from "./pages/wallet/TransferBetweenWallets";

// Pages transactions
import TransactionList from "./pages/transactions/TransactionList";
import TransactionDetails from "./pages/transactions/TransactionDetails";
import ScheduledTransactions from "./pages/transactions/ScheduledTransactions";
import CreateScheduledTransaction from "./pages/transactions/CreateScheduledTransaction";

// Pages profil
import Profile from "./pages/profile/Profile";
import EditProfile from "./pages/profile/EditProfile";
import Settings from "./pages/profile/Settings";
import ChangePin from "./pages/profile/ChangePin";
import TwoFactorAuth from "./pages/profile/TwoFactorAuth";

// Pages support
import Notifications from "./pages/support/Notifications";
import HelpSupport from "./pages/support/HelpSupport";

import { Transaction } from "@/components/TransactionItem";
import type { AuthResponse } from "@/api/auth.api";
import { verifyOtp, resendOtp, updateUserProfile } from "@/api/user.api";
import { getWalletByNumber } from "@/api/wallet.api";
import { clearAuthToken } from "@/api/api";

const queryClient = new QueryClient();

const AppRoutes = () => {
  const navigate = useNavigate();
  
  // Mock user data - In production, this would come from authentication context
  const [user, setUser] = useState(() => {
    try {
      const raw = sessionStorage.getItem('zaphira_user');
      if (raw) {
        const parsed = JSON.parse(raw);
        if (parsed?.id) {
          return parsed;
        }
      }
    } catch {
      // Ignore storage errors
    }
    return {
      id: "user123",
      name: "Jean Dupont",
      firstName: "Jean",
      lastName: "Dupont",
      phoneNumber: "+237612345678",
      email: "jean.dupont@example.com",
      walletNumber: "12345678",
      createdAt: new Date().toISOString(),
    };
  });

  const persistUser = (nextUser: typeof user) => {
    setUser(nextUser);
    try {
      sessionStorage.setItem('zaphira_user', JSON.stringify(nextUser));
    } catch {
      // Ignore storage errors
    }
  };

  const [pendingRegistration, setPendingRegistration] = useState<{
    userId: number;
    phoneNumber: string;
    email: string;
  } | null>(null);

  useEffect(() => {
    try {
      const raw = sessionStorage.getItem('zaphira_pending_registration');
      if (raw) {
        const parsed = JSON.parse(raw);
        if (parsed?.userId && parsed?.phoneNumber) {
          setPendingRegistration(parsed);
        }
      }
    } catch {
      // Ignore storage errors
    }
  }, []);

  const [balance, setBalance] = useState(0);
  const [transactions] = useState<Transaction[]>([]);

  // Mock transaction detail
  const [selectedTransaction, setSelectedTransaction] = useState<Transaction | null>(null);

  useEffect(() => {
    if (!user.walletNumber) {
      setBalance(0);
      return;
    }

    let active = true;
    const loadBalance = async () => {
      try {
        const wallet = await getWalletByNumber(user.walletNumber);
        const totalBalance = wallet.totalBalance ?? wallet.availableBalance ?? 0;
        if (active) {
          setBalance(Number(totalBalance));
        }
      } catch (error) {
        console.error('Failed to load wallet balance', error);
        if (active) {
          setBalance(0);
        }
      } finally {
        // no-op
      }
    };

    loadBalance();

    return () => {
      active = false;
    };
  }, [user.walletNumber]);

  // Navigation handlers
  const handleLoginSuccess = async (response: AuthResponse, phone: string) => {
    if (response.user.accountStatus === 'PENDING_VERIFICATION') {
      const pending = {
        userId: response.user.id,
        phoneNumber: phone,
        email: response.user.email || user.email,
      };
      setPendingRegistration(pending);
      try {
        sessionStorage.setItem('zaphira_pending_registration', JSON.stringify(pending));
      } catch {
        // Ignore storage errors
      }
      navigate('/otp-verification');
      return;
    }

    const fullName = `${response.user.firstName} ${response.user.lastName}`.trim();
    persistUser({
      id: response.user.id.toString(),
      name: fullName || phone,
      firstName: response.user.firstName,
      lastName: response.user.lastName,
      phoneNumber: phone,
      email: response.user.email || user.email,
      walletNumber: response.user.walletId || '',
      createdAt: new Date().toISOString(),
    });
    navigate('/dashboard');
  };

  const handleRegisterSuccess = async (data: { userId: number; phoneNumber: string; email: string }) => {
    setPendingRegistration(data);
    try {
      sessionStorage.setItem('zaphira_pending_registration', JSON.stringify(data));
    } catch {
      // Ignore storage errors
    }
    const nextUser = {
      ...user,
      phoneNumber: data.phoneNumber,
      email: data.email,
    };
    persistUser(nextUser);
    navigate('/otp-verification');
  };

  const handleForgotPinComplete = () => {
    navigate('/login');
  };

  const handleOTPVerify = async (otp: string) => {
    if (!pendingRegistration) {
      throw new Error('Aucune inscription en cours');
    }
    const response = await verifyOtp({ phoneNumber: pendingRegistration.phoneNumber, otpCode: otp });
    setPendingRegistration(null);
    try {
      sessionStorage.removeItem('zaphira_pending_registration');
    } catch {
      // Ignore storage errors
    }
    if (response?.data) {
      const nextUser = {
        ...user,
        phoneNumber: response.data.phoneNumber || user.phoneNumber,
        email: response.data.email || user.email,
        walletNumber: response.data.walletId || user.walletNumber,
      };
      persistUser(nextUser);
    }
    navigate('/login');
  };

  const handleTransfer = async (data: {
    recipientWallet: string;
    amount: number;
    description?: string;
    pin: string;
  }) => {
    console.log('Transfer:', data);
    const reference = `TRX${Date.now()}`;
    navigate('/dashboard');
    return { reference };
  };

  const handleProfileSave = async (data: { firstName: string; lastName: string; email?: string }) => {
    const response = await updateUserProfile({
      firstName: data.firstName,
      lastName: data.lastName,
      email: data.email,
    });
    const updated = response?.data;
    const fullName = `${updated?.firstName ?? data.firstName} ${updated?.lastName ?? data.lastName}`.trim();
    const nextUser = {
      ...user,
      name: fullName,
      firstName: updated?.firstName ?? data.firstName,
      lastName: updated?.lastName ?? data.lastName,
      email: updated?.email ?? data.email ?? user.email,
    };
    persistUser(nextUser);
    navigate('/profile');
  };

  const handleLogout = () => {
    clearAuthToken();
    setPendingRegistration(null);
    try {
      sessionStorage.removeItem('zaphira_pending_registration');
    } catch {
      // Ignore storage errors
    }
    const emptyUser = {
      id: "",
      name: "",
      firstName: "",
      lastName: "",
      phoneNumber: "",
      email: "",
      walletNumber: "",
      createdAt: new Date().toISOString(),
    };
    try {
      sessionStorage.removeItem('zaphira_user');
    } catch {
      // Ignore storage errors
    }
    setUser(emptyUser);
    navigate('/login');
  };

  const handleSecurityQuestionsComplete = () => {
    navigate('/profile');
  };

  const handleChangePinComplete = () => {
    navigate('/settings');
  };

  const handleCreateScheduledTransaction = async (data: {
    receiverWalletNumber: string;
    amount: number;
    frequency: string;
    startDate: string;
    endDate?: string;
    description?: string;
    pin: string;
  }) => {
    console.log('Scheduled transaction:', data);
    navigate('/scheduled-transactions');
  };

  const handleWithdraw = async (data: { method: string; amount: number; pin: string }) => {
    console.log('Withdraw:', data);
    navigate('/dashboard');
  };

  const handleTopUp = async (data: { method: string; amount: number }) => {
    console.log('Top-up:', data);
    navigate('/dashboard');
  };

  const handleTransferBetweenWallets = async (data: { fromWallet: string; toWallet: string; amount: number; pin: string }) => {
    console.log('Transfer between wallets:', data);
    navigate('/wallet-management');
  };

  // Transaction details wrapper
  const TransactionDetailsWrapper = () => {
    const { reference } = useParams();
    const transaction =
      transactions.find(tx => tx.reference === reference) ||
      selectedTransaction ||
      null;

    useEffect(() => {
      if (!transaction) {
        navigate('/transactions');
      }
    }, [transaction]);

    if (!transaction) {
      return null;
    }

    return <TransactionDetails transaction={transaction} onBack={() => navigate('/transactions')} />;
  };

  const WalletDetailsWrapper = () => {
    const { walletNumber } = useParams();
    return (
      <WalletDetails
        walletNumber={walletNumber || user.walletNumber}
        onBack={() => navigate('/dashboard')}
        onTopUp={() => navigate('/topup')}
        onWithdraw={() => navigate('/withdraw')}
      />
    );
  };

  return (
    <Routes>
      <Route path="/" element={<Index />} />
      
      {/* Authentication Routes */}
      <Route 
        path="/login" 
        element={
          <Login 
            onLoginSuccess={handleLoginSuccess}
            onForgotPin={() => navigate('/forgot-pin')}
            onRegister={() => navigate('/register')}
          />
        } 
      />
      <Route 
        path="/register" 
        element={
          <Register 
            onRegisterSuccess={handleRegisterSuccess}
            onLogin={() => navigate('/login')}
          />
        } 
      />
      <Route 
        path="/forgot-pin" 
        element={
          <ForgotPin 
            onComplete={handleForgotPinComplete}
            onBackToLogin={() => navigate('/login')}
          />
        } 
      />
      <Route 
        path="/otp-verification" 
        element={
          <OTPVerification 
            phoneNumber={pendingRegistration?.phoneNumber || user.phoneNumber}
            onVerify={handleOTPVerify}
            onResend={async () => {
              if (!pendingRegistration) {
                throw new Error('Aucune inscription en cours');
              }
              await resendOtp(pendingRegistration.userId);
            }}
            onBack={() => navigate('/login')}
          />
        } 
      />

      {/* Main App Routes */}
      <Route 
        path="/dashboard" 
        element={
          <Dashboard 
            user={user}
            balance={balance}
            transactions={transactions.slice(0, 3)}
            onAction={(action) => {
              if (action === 'send') navigate('/transfer');
              if (action === 'receive') navigate('/receive');
              if (action === 'topup') navigate('/topup');
              if (action === 'withdraw') navigate('/withdraw');
              if (action === 'schedule') navigate('/scheduled-transactions/create');
              if (action === 'wallets') navigate('/wallet-management');
              if (action === 'transactions') navigate('/transactions');
              if (action === 'scheduled') navigate('/scheduled-transactions');
              if (action === 'wallet-details') navigate('/wallet-details');
            }}
            onTransactionClick={(tx) => {
              setSelectedTransaction(tx);
              navigate(`/transactions/${tx.reference}`);
            }}
            onViewAllTransactions={() => navigate('/transactions')}
            onNavigate={(tab) => {
              if (tab === 'home') navigate('/dashboard');
              if (tab === 'transactions') navigate('/transactions');
              if (tab === 'scheduled') navigate('/scheduled-transactions');
              if (tab === 'profile') navigate('/profile');
            }}
          />
        } 
      />
      <Route 
        path="/transfer" 
        element={
          <Transfer 
            balance={balance}
            onTransfer={handleTransfer}
            onClose={() => navigate('/dashboard')}
          />
        } 
      />
      <Route 
        path="/withdraw" 
        element={
          <Withdraw 
            balance={balance}
            onWithdraw={handleWithdraw}
            onBack={() => navigate('/dashboard')}
          />
        } 
      />
      <Route 
        path="/topup" 
        element={
          <TopUp 
            balance={balance}
            onTopUp={handleTopUp}
            onBack={() => navigate('/dashboard')}
          />
        } 
      />

      {/* Profile Routes */}
      <Route 
        path="/profile" 
        element={
          <Profile 
            user={user}
            onBack={() => navigate('/dashboard')}
            onEdit={() => navigate('/profile/edit')}
            onSecurityQuestions={() => navigate('/security-questions')}
            onChangePin={() => navigate('/change-pin')}
            on2FA={() => navigate('/profile/2fa')}
            onNotifications={() => navigate('/notifications')}
            onHelp={() => navigate('/help')}
            onLogout={handleLogout}
            onNavigate={(tab) => {
              if (tab === 'home') navigate('/dashboard');
              if (tab === 'transactions') navigate('/transactions');
              if (tab === 'scheduled') navigate('/scheduled-transactions');
              if (tab === 'profile') navigate('/profile');
            }}
          />
        } 
      />
      <Route 
        path="/profile/edit" 
        element={
          <EditProfile 
            user={{ firstName: user.firstName, lastName: user.lastName, email: user.email }}
            onSave={handleProfileSave}
            onBack={() => navigate('/profile')}
          />
        } 
      />
      <Route 
        path="/security-questions" 
        element={
          <SecurityQuestionsSetup 
            onComplete={handleSecurityQuestionsComplete}
            onBack={() => navigate('/profile')}
          />
        } 
      />
      <Route 
        path="/profile/2fa" 
        element={
          <TwoFactorAuth 
            user={user}
            twoFactorEnabled={false}
            onBack={() => navigate('/profile')}
            onEnableTwoFactor={async (method, pin) => {
              console.log('Enable 2FA:', { method, pin });
              // In production, this would call an API
            }}
            onDisableTwoFactor={async (pin) => {
              console.log('Disable 2FA:', { pin });
              // In production, this would call an API
            }}
            onVerifyOTP={async (otp) => {
              console.log('Verify OTP:', otp);
              // In production, this would verify with the backend
              return otp === '123456'; // Mock verification
            }}
          />
        } 
      />

      {/* Notifications & Help Routes */}
      <Route 
        path="/notifications" 
        element={
          <Notifications 
            onBack={() => navigate('/dashboard')}
          />
        } 
      />
      <Route 
        path="/receive" 
        element={
          <ReceiveMoney 
            user={user}
            onBack={() => navigate('/dashboard')}
          />
        } 
      />
      <Route 
        path="/help" 
        element={
          <HelpSupport 
            onBack={() => navigate('/profile')}
          />
        } 
      />

      {/* Transaction Routes */}
      <Route 
        path="/transactions" 
        element={
          <TransactionList 
            walletNumber={user.walletNumber}
            onBack={() => navigate('/dashboard')}
            onTransactionClick={(tx) => {
              setSelectedTransaction(tx);
              navigate(`/transactions/${tx.reference}`);
            }}
            onNavigate={(tab) => {
              if (tab === 'home') navigate('/dashboard');
              if (tab === 'transactions') navigate('/transactions');
              if (tab === 'scheduled') navigate('/scheduled-transactions');
              if (tab === 'profile') navigate('/profile');
            }}
          />
        } 
      />
      <Route 
        path="/transactions/:reference" 
        element={<TransactionDetailsWrapper />} 
      />

      {/* Scheduled Transactions Routes */}
      <Route 
        path="/scheduled-transactions" 
        element={
          <ScheduledTransactions 
            walletNumber={user.walletNumber}
            onBack={() => navigate('/dashboard')}
            onCreate={() => navigate('/scheduled-transactions/create')}
            onNavigate={(tab) => {
              if (tab === 'home') navigate('/dashboard');
              if (tab === 'transactions') navigate('/transactions');
              if (tab === 'scheduled') navigate('/scheduled-transactions');
              if (tab === 'profile') navigate('/profile');
            }}
          />
        } 
      />
      <Route 
        path="/scheduled-transactions/create" 
        element={
          <CreateScheduledTransaction 
            walletNumber={user.walletNumber}
            balance={balance}
            onCreate={handleCreateScheduledTransaction}
            onBack={() => navigate('/scheduled-transactions')}
          />
        } 
      />

      {/* Wallet Management */}
      <Route 
        path="/wallet-management" 
        element={
          <WalletsManagement 
            mainWalletNumber={user.walletNumber}
            mainBalance={balance}
            onBack={() => navigate('/dashboard')}
            onViewDetails={(walletNumber) => navigate(`/wallet-details/${walletNumber}`)}
            onTransferBetween={() => navigate('/transfer-between-wallets')}
          />
        } 
      />
      <Route 
        path="/transfer-between-wallets" 
        element={
          <TransferBetweenWallets 
            wallets={[
              { id: '1', name: 'Épargne Vacances', walletNumber: '87654321', balance: 500000, type: 'SAVINGS', currency: 'XAF' },
              { id: '2', name: 'Courses Mensuel', walletNumber: '11223344', balance: 75000, type: 'EXPENSES', currency: 'XAF' },
              { id: '3', name: 'Investissements', walletNumber: '55667788', balance: 250000, type: 'INVESTMENT', currency: 'XAF' },
              { id: '4', name: 'Business', walletNumber: '99887766', balance: 1200000, type: 'BUSINESS', currency: 'XAF' },
            ]}
            onBack={() => navigate('/wallet-management')}
            onTransfer={handleTransferBetweenWallets}
          />
        } 
      />
      <Route 
        path="/wallet-details/:walletNumber?" 
        element={<WalletDetailsWrapper />} 
      />

      {/* Settings Routes */}
      <Route 
        path="/settings" 
        element={
          <Settings 
            onBack={() => navigate('/dashboard')}
            onChangePin={() => navigate('/change-pin')}
          />
        } 
      />
      <Route 
        path="/change-pin" 
        element={
          <ChangePin 
            walletNumber={user.walletNumber}
            onComplete={handleChangePinComplete}
            onBack={() => navigate('/settings')}
          />
        } 
      />

      {/* 404 Route */}
      <Route path="*" element={<NotFound />} />
    </Routes>
  );
};

const App = () => (
  <QueryClientProvider client={queryClient}>
    <TooltipProvider>
      <Toaster />
      <Sonner />
      <BrowserRouter>
        <AppRoutes />
      </BrowserRouter>
    </TooltipProvider>
  </QueryClientProvider>
);

export default App;
