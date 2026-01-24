import React, { useState } from 'react';
import { motion } from 'framer-motion';
import { useTranslation } from 'react-i18next';
import { ChevronRight, Receipt } from 'lucide-react';
import { BalanceCard } from '@/components/BalanceCard';
import { QuickActions } from '@/components/QuickActions';
import { TransactionItem, Transaction } from '@/components/TransactionItem';
import { EmptyState } from '@/components/ui/empty-state';
import { TransactionListSkeleton } from '@/components/ui/loading-skeleton';
import { BottomNav } from '@/components/BottomNav';

interface DashboardProps {
  user: {
    name: string;
    walletNumber: string;
  };
  balance: number;
  transactions: Transaction[];
  loading?: boolean;
  onAction: (action: string) => void;
  onTransactionClick: (tx: Transaction) => void;
  onViewAllTransactions: () => void;
  onNavigate?: (tab: string) => void;
}

export function Dashboard({
  user,
  balance,
  transactions,
  loading = false,
  onAction,
  onTransactionClick,
  onViewAllTransactions,
  onNavigate,
}: DashboardProps) {
  const { t } = useTranslation();
  const [activeTab, setActiveTab] = useState('home');

  const recentTransactions = transactions.slice(0, 5);

  const handleTabChange = (tab: string) => {
    setActiveTab(tab);
    if (onNavigate) {
      onNavigate(tab);
    }
  };

  return (
    <div className="min-h-screen bg-background pb-20">
      {/* Content */}
      <div className="px-4 pt-6 safe-area-top max-w-lg mx-auto">
        {/* Balance Card */}
        <BalanceCard
          balance={balance}
          walletNumber={user.walletNumber}
          userName={user.name}
          className="mb-6"
          onTopUp={() => onAction('topup')}
          onViewDetails={() => onAction('wallet-details')}
        />

        {/* Quick Actions */}
        <section className="mb-8">
          <h2 className="text-sm font-medium text-muted-foreground mb-4">
            {t('quick_actions')}
          </h2>
          <QuickActions onAction={onAction} />
        </section>

        {/* Recent Transactions */}
        <section>
          <div className="flex items-center justify-between mb-4">
            <h2 className="text-sm font-medium text-muted-foreground">
              {t('recent_transactions')}
            </h2>
            <button
              onClick={onViewAllTransactions}
              className="flex items-center gap-1 text-primary text-sm font-medium touch-target"
            >
              {t('see_all')}
              <ChevronRight className="w-4 h-4" />
            </button>
          </div>

          {loading ? (
            <TransactionListSkeleton count={3} />
          ) : recentTransactions.length > 0 ? (
            <motion.div
              initial={{ opacity: 0 }}
              animate={{ opacity: 1 }}
              className="space-y-2"
            >
              {recentTransactions.map((tx, index) => (
                <TransactionItem
                  key={tx.id}
                  transaction={tx}
                  onClick={() => onTransactionClick(tx)}
                  index={index}
                />
              ))}
            </motion.div>
          ) : (
            <EmptyState
              icon={Receipt}
              title={t('no_transactions')}
              description="Vos transactions récentes apparaîtront ici"
            />
          )}
        </section>
      </div>

      {/* Bottom Navigation */}
      <BottomNav activeTab={activeTab} onTabChange={handleTabChange} />
    </div>
  );
}

export default Dashboard;
