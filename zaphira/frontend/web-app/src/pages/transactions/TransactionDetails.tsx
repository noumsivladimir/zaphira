import React from 'react';
import { motion } from 'framer-motion';
import { useTranslation } from 'react-i18next';
import { ChevronLeft, Copy, CheckCircle2, XCircle, Clock, ArrowUpRight, ArrowDownLeft } from 'lucide-react';
import { Button } from '@/components/ui/button';
import { Badge } from '@/components/ui/badge';
import { Separator } from '@/components/ui/separator';
import { useToast } from '@/hooks/use-toast';
import { formatCurrency } from '@/lib/currency';
import { cn } from '@/lib/utils';
import { Transaction } from '@/components/TransactionItem';

interface TransactionDetailsProps {
  transaction: Transaction;
  onBack: () => void;
}

export function TransactionDetails({ transaction, onBack }: TransactionDetailsProps) {
  const { t } = useTranslation();
  const { toast } = useToast();

  const getStatusIcon = () => {
    switch (transaction.status) {
      case 'COMPLETED':
        return CheckCircle2;
      case 'FAILED':
        return XCircle;
      case 'PENDING':
        return Clock;
      default:
        return Clock;
    }
  };

  const getStatusColor = () => {
    switch (transaction.status) {
      case 'COMPLETED':
        return 'text-green-600 dark:text-green-500 bg-green-100 dark:bg-green-900/30';
      case 'FAILED':
        return 'text-red-600 dark:text-red-500 bg-red-100 dark:bg-red-900/30';
      case 'PENDING':
        return 'text-yellow-600 dark:text-yellow-500 bg-yellow-100 dark:bg-yellow-900/30';
      default:
        return 'text-gray-600 dark:text-gray-500 bg-gray-100 dark:bg-gray-900/30';
    }
  };

  const StatusIcon = getStatusIcon();

  const copyToClipboard = (text: string, label: string) => {
    navigator.clipboard.writeText(text);
    toast({
      title: t('copied'),
      description: `${label} ${t('copied_to_clipboard')}`,
    });
  };

  const formatDate = (dateString: string) => {
    const date = new Date(dateString);
    return new Intl.DateTimeFormat('fr-FR', {
      day: '2-digit',
      month: 'long',
      year: 'numeric',
      hour: '2-digit',
      minute: '2-digit',
    }).format(date);
  };

  const isSent = transaction.type === 'SEND';

  return (
    <div className="min-h-screen bg-background">
      {/* Header */}
      <header className="safe-area-top px-4 py-4 flex items-center border-b bg-background sticky top-0 z-10">
        <button
          onClick={onBack}
          className="w-10 h-10 rounded-full bg-secondary flex items-center justify-center touch-target mr-3"
        >
          <ChevronLeft className="w-5 h-5" />
        </button>
        <h1 className="text-lg font-semibold">{t('transaction_details')}</h1>
      </header>

      <div className="max-w-lg mx-auto px-4 py-8">
        {/* Status & Amount */}
        <motion.div
          initial={{ opacity: 0, y: 20 }}
          animate={{ opacity: 1, y: 0 }}
          className="text-center mb-8"
        >
          <div className={cn('w-16 h-16 rounded-full mx-auto mb-4 flex items-center justify-center', getStatusColor())}>
            {isSent ? (
              <ArrowUpRight className="w-8 h-8" />
            ) : (
              <ArrowDownLeft className="w-8 h-8" />
            )}
          </div>
          <h2 className={cn('text-4xl font-bold mb-2', isSent ? 'text-red-600 dark:text-red-500' : 'text-green-600 dark:text-green-500')}>
            {isSent ? '-' : '+'} {formatCurrency(transaction.amount, transaction.currency)}
          </h2>
          <div className="flex items-center justify-center gap-2">
            <Badge variant={transaction.status === 'COMPLETED' ? 'default' : 'secondary'} className="gap-1">
              <StatusIcon className="w-3 h-3" />
              {t(transaction.status.toLowerCase())}
            </Badge>
          </div>
        </motion.div>

        {/* Details Card */}
        <motion.div
          initial={{ opacity: 0, y: 20 }}
          animate={{ opacity: 1, y: 0 }}
          transition={{ delay: 0.1 }}
          className="bg-card rounded-lg p-4 space-y-4"
        >
          {/* Reference */}
          <div>
            <p className="text-xs text-muted-foreground mb-1">{t('transaction_reference')}</p>
            <button
              onClick={() => copyToClipboard(transaction.reference, t('reference'))}
              className="flex items-center gap-2 text-sm font-mono font-medium hover:text-primary transition-colors"
            >
              {transaction.reference}
              <Copy className="w-4 h-4" />
            </button>
          </div>

          <Separator />

          {/* Counterparty */}
          <div>
            <p className="text-xs text-muted-foreground mb-1">
              {isSent ? t('recipient') : t('sender')}
            </p>
            <p className="text-sm font-medium">{transaction.counterparty.name}</p>
            <p className="text-xs text-muted-foreground font-mono">
              {transaction.counterparty.walletNumber}
            </p>
          </div>

          <Separator />

          {/* Date */}
          <div>
            <p className="text-xs text-muted-foreground mb-1">{t('date')}</p>
            <p className="text-sm font-medium">{formatDate(transaction.createdAt)}</p>
          </div>

          {transaction.completedAt && (
            <>
              <Separator />
              <div>
                <p className="text-xs text-muted-foreground mb-1">{t('completed_at')}</p>
                <p className="text-sm font-medium">{formatDate(transaction.completedAt)}</p>
              </div>
            </>
          )}

          {transaction.description && (
            <>
              <Separator />
              <div>
                <p className="text-xs text-muted-foreground mb-1">{t('description')}</p>
                <p className="text-sm">{transaction.description}</p>
              </div>
            </>
          )}

          {transaction.fees !== undefined && transaction.fees > 0 && (
            <>
              <Separator />
              <div>
                <p className="text-xs text-muted-foreground mb-1">{t('fees')}</p>
                <p className="text-sm font-medium">
                  {formatCurrency(transaction.fees, transaction.currency)}
                </p>
              </div>
            </>
          )}

          <Separator />

          {/* Type */}
          <div>
            <p className="text-xs text-muted-foreground mb-1">{t('type')}</p>
            <p className="text-sm font-medium">{t(transaction.type.toLowerCase())}</p>
          </div>
        </motion.div>

        {/* Actions */}
        <motion.div
          initial={{ opacity: 0, y: 20 }}
          animate={{ opacity: 1, y: 0 }}
          transition={{ delay: 0.2 }}
          className="mt-6 space-y-3"
        >
          <Button
            variant="outline"
            className="w-full"
            onClick={() => copyToClipboard(transaction.reference, t('reference'))}
          >
            <Copy className="w-4 h-4 mr-2" />
            {t('copy_reference')}
          </Button>

          {transaction.status === 'COMPLETED' && (
            <Button variant="outline" className="w-full">
              {t('download_receipt')}
            </Button>
          )}
        </motion.div>

        {/* Status Info */}
        {transaction.status === 'PENDING' && (
          <motion.div
            initial={{ opacity: 0, y: 20 }}
            animate={{ opacity: 1, y: 0 }}
            transition={{ delay: 0.3 }}
            className="mt-6 p-4 rounded-lg bg-yellow-50 dark:bg-yellow-950/30 border border-yellow-200 dark:border-yellow-900"
          >
            <p className="text-sm text-yellow-900 dark:text-yellow-200">
              {t('transaction_pending_info')}
            </p>
          </motion.div>
        )}

        {transaction.status === 'FAILED' && (
          <motion.div
            initial={{ opacity: 0, y: 20 }}
            animate={{ opacity: 1, y: 0 }}
            transition={{ delay: 0.3 }}
            className="mt-6 p-4 rounded-lg bg-red-50 dark:bg-red-950/30 border border-red-200 dark:border-red-900"
          >
            <p className="text-sm text-red-900 dark:text-red-200">
              {t('transaction_failed_info')}
            </p>
          </motion.div>
        )}
      </div>
    </div>
  );
}

export default TransactionDetails;
