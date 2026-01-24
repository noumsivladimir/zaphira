import React, { useState } from 'react';
import { motion } from 'framer-motion';
import { Eye, EyeOff, Copy, Check, Plus, ChevronRight } from 'lucide-react';
import { useTranslation } from 'react-i18next';
import { formatCurrency, maskWalletNumber, formatWalletNumber } from '@/lib/currency';
import { cn } from '@/lib/utils';

interface BalanceCardProps {
  balance: number;
  currency?: string;
  walletNumber: string;
  userName?: string;
  className?: string;
  onTopUp?: () => void;
  onViewDetails?: () => void;
}

export function BalanceCard({
  balance,
  currency = 'XAF',
  walletNumber,
  userName,
  className,
  onTopUp,
  onViewDetails,
}: BalanceCardProps) {
  const { t } = useTranslation();
  const [showBalance, setShowBalance] = useState(true);
  const [copied, setCopied] = useState(false);

  const handleCopyWallet = async () => {
    try {
      await navigator.clipboard.writeText(walletNumber);
      setCopied(true);
      setTimeout(() => setCopied(false), 2000);
    } catch (err) {
      console.error('Failed to copy');
    }
  };

  return (
    <motion.div
      initial={{ opacity: 0, scale: 0.95 }}
      animate={{ opacity: 1, scale: 1 }}
      className={cn('balance-card', className)}
    >
      {/* Header with greeting */}
      <div className="flex items-center justify-between mb-4">
        <div>
          <p className="text-white/70 text-sm">{t('hello')},</p>
          <p className="text-white font-semibold text-lg">{userName || 'User'}</p>
        </div>
        <button
          onClick={() => setShowBalance(!showBalance)}
          className="w-10 h-10 rounded-full bg-white/10 flex items-center justify-center
                     hover:bg-white/20 transition-colors touch-target"
          aria-label={showBalance ? 'Hide balance' : 'Show balance'}
        >
          {showBalance ? (
            <EyeOff className="w-5 h-5 text-white" />
          ) : (
            <Eye className="w-5 h-5 text-white" />
          )}
        </button>
      </div>

      {/* Balance */}
      <div className="mb-4">
        <p className="text-white/70 text-sm mb-1">{t('available_balance')}</p>
        <div className="flex items-center gap-3">
          <motion.p
            className="currency-display text-3xl sm:text-4xl text-white"
            initial={false}
            animate={{ opacity: showBalance ? 1 : 0.5 }}
          >
            {showBalance ? formatCurrency(balance, currency) : '••••••••'}
          </motion.p>
          {onTopUp && (
            <button
              onClick={onTopUp}
              className="w-9 h-9 rounded-full bg-white/20 flex items-center justify-center
                         hover:bg-white/30 transition-colors touch-target"
              aria-label="Top up wallet"
            >
              <Plus className="w-5 h-5 text-white" />
            </button>
          )}
        </div>
      </div>

      {/* Wallet number */}
      <div 
        className={cn(
          "flex items-center justify-between pt-4 border-t border-white/10",
          onViewDetails && "cursor-pointer hover:bg-white/5 -mx-6 px-6 transition-colors"
        )}
        onClick={onViewDetails}
      >
        <div>
          <p className="text-white/50 text-xs mb-0.5">{t('wallet_number')}</p>
          <p className="text-white/90 font-mono text-sm">
            {showBalance
              ? formatWalletNumber(walletNumber)
              : formatWalletNumber(maskWalletNumber(walletNumber))}
          </p>
        </div>
        <div className="flex items-center gap-2">
          <button
            onClick={(e) => {
              e.stopPropagation();
              handleCopyWallet();
            }}
            className="w-8 h-8 rounded-full bg-white/10 flex items-center justify-center
                       hover:bg-white/20 transition-colors touch-target"
            aria-label="Copy wallet number"
          >
            {copied ? (
              <Check className="w-4 h-4 text-white" />
            ) : (
              <Copy className="w-4 h-4 text-white" />
            )}
          </button>
          {onViewDetails && (
            <ChevronRight className="w-5 h-5 text-white/50" />
          )}
        </div>
      </div>
    </motion.div>
  );
}
