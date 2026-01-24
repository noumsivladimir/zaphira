import React from 'react';
import { motion } from 'framer-motion';
import { ArrowUpRight, ArrowDownLeft, Clock, CheckCircle2, XCircle } from 'lucide-react';
import { formatCurrency } from '@/lib/currency';
import { AvatarDisplay } from '@/components/ui/avatar-display';
import { cn } from '@/lib/utils';

export interface Transaction {
  id: string;
  reference: string;
  type: 'TRANSFER' | 'DEPOSIT' | 'WITHDRAWAL' | 'SEND' | 'RECEIVE';
  direction: 'in' | 'out';
  amount: number;
  currency: string;
  status: 'PENDING' | 'COMPLETED' | 'FAILED';
  description?: string;
  createdAt: string;
  completedAt?: string;
  fees?: number;
  counterparty: {
    name: string;
    walletNumber: string;
  };
}

interface TransactionItemProps {
  transaction: Transaction;
  onClick?: () => void;
  index?: number;
}

const statusIcons = {
  PENDING: Clock,
  COMPLETED: CheckCircle2,
  FAILED: XCircle,
};

const statusColors = {
  PENDING: 'text-warning',
  COMPLETED: 'text-success',
  FAILED: 'text-destructive',
};

export function TransactionItem({ transaction, onClick, index = 0 }: TransactionItemProps) {
  const { direction, amount, currency, status, counterparty, createdAt, description } = transaction;
  const isIncoming = direction === 'in';
  const StatusIcon = statusIcons[status];

  const formattedDate = new Date(createdAt).toLocaleDateString('fr-FR', {
    day: 'numeric',
    month: 'short',
    hour: '2-digit',
    minute: '2-digit',
  });

  return (
    <motion.button
      initial={{ opacity: 0, y: 10 }}
      animate={{ opacity: 1, y: 0 }}
      transition={{ delay: index * 0.05 }}
      onClick={onClick}
      className="transaction-item w-full text-left"
    >
      {/* Avatar with direction indicator */}
      <div className="relative">
        <AvatarDisplay name={counterparty.name} size="lg" />
        <div
          className={cn(
            'absolute -bottom-1 -right-1 w-5 h-5 rounded-full flex items-center justify-center',
            isIncoming ? 'bg-success' : 'bg-primary'
          )}
        >
          {isIncoming ? (
            <ArrowDownLeft className="w-3 h-3 text-white" />
          ) : (
            <ArrowUpRight className="w-3 h-3 text-white" />
          )}
        </div>
      </div>

      {/* Details */}
      <div className="flex-1 min-w-0">
        <p className="font-medium text-foreground truncate">{counterparty.name}</p>
        <p className="text-sm text-muted-foreground truncate">
          {description || formattedDate}
        </p>
      </div>

      {/* Amount and status */}
      <div className="text-right">
        <p
          className={cn(
            'font-semibold font-display',
            isIncoming ? 'currency-positive' : 'text-foreground'
          )}
        >
          {isIncoming ? '+' : '-'} {formatCurrency(amount, currency)}
        </p>
        <div className={cn('flex items-center justify-end gap-1', statusColors[status])}>
          <StatusIcon className="w-3 h-3" />
          <span className="text-xs capitalize">
            {status === 'PENDING' ? 'En attente' : status === 'COMPLETED' ? 'Terminé' : 'Échoué'}
          </span>
        </div>
      </div>
    </motion.button>
  );
}
