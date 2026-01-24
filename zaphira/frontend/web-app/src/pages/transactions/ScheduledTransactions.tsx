import React, { useState, useEffect } from 'react';
import { motion } from 'framer-motion';
import { useTranslation } from 'react-i18next';
import { ChevronLeft, Plus, Calendar, Repeat, Pause, Play, Trash2 } from 'lucide-react';
import { Button } from '@/components/ui/button';
import { Badge } from '@/components/ui/badge';
import { EmptyState } from '@/components/ui/empty-state';
import { Skeleton } from '@/components/ui/skeleton';
import {
  AlertDialog,
  AlertDialogAction,
  AlertDialogCancel,
  AlertDialogContent,
  AlertDialogDescription,
  AlertDialogFooter,
  AlertDialogHeader,
  AlertDialogTitle,
} from '@/components/ui/alert-dialog';
import { formatCurrency } from '@/lib/currency';
import { useToast } from '@/hooks/use-toast';
import { cn } from '@/lib/utils';
import { BottomNav } from '@/components/BottomNav';

interface ScheduledTransaction {
  id: string;
  walletNumber: string;
  receiverWalletNumber: string;
  receiverName: string;
  amount: number;
  currency: string;
  frequency: 'DAILY' | 'WEEKLY' | 'MONTHLY';
  nextExecutionDate: string;
  status: 'ACTIVE' | 'PAUSED' | 'CANCELLED' | 'COMPLETED';
  description?: string;
  createdAt: string;
}

interface ScheduledTransactionsProps {
  walletNumber: string;
  onBack: () => void;
  onCreate: () => void;
  onNavigate?: (tab: string) => void;
}

export function ScheduledTransactions({
  walletNumber,
  onBack,
  onCreate,
  onNavigate,
}: ScheduledTransactionsProps) {
  const { t } = useTranslation();
  const { toast } = useToast();
  const [transactions, setTransactions] = useState<ScheduledTransaction[]>([]);
  const [loading, setLoading] = useState(true);
  const [actionLoading, setActionLoading] = useState<string | null>(null);
  const [cancelDialog, setCancelDialog] = useState<string | null>(null);
  const [activeTab, setActiveTab] = useState('scheduled');

  const handleTabChange = (tab: string) => {
    setActiveTab(tab);
    if (onNavigate) {
      onNavigate(tab);
    }
  };

  useEffect(() => {
    loadScheduledTransactions();
  }, [walletNumber]);

  const loadScheduledTransactions = async () => {
    setLoading(true);
    try {
      // API call to load scheduled transactions
      // const data = await getScheduledTransactions(walletNumber);
      // Mock data
      const mockData: ScheduledTransaction[] = [
        {
          id: '1',
          walletNumber,
          receiverWalletNumber: '12345678',
          receiverName: 'Jean Dupont',
          amount: 50000,
          currency: 'XAF',
          frequency: 'MONTHLY',
          nextExecutionDate: new Date(Date.now() + 5 * 86400000).toISOString(),
          status: 'ACTIVE',
          description: 'Monthly rent',
          createdAt: new Date().toISOString(),
        },
      ];
      setTransactions(mockData);
    } catch (err) {
      toast({
        title: t('error'),
        description: t('error_loading_scheduled_transactions'),
        variant: 'destructive',
      });
    } finally {
      setLoading(false);
    }
  };

  const handlePauseResume = async (id: string, currentStatus: string) => {
    setActionLoading(id);
    try {
      // API call to pause/resume
      // if (currentStatus === 'ACTIVE') {
      //   await pauseScheduledTransaction(id);
      // } else {
      //   await resumeScheduledTransaction(id);
      // }
      
      setTransactions(
        transactions.map((tx) =>
          tx.id === id
            ? { ...tx, status: (currentStatus === 'ACTIVE' ? 'PAUSED' : 'ACTIVE') as ScheduledTransaction['status'] }
            : tx
        )
      );

      toast({
        title: t('success'),
        description:
          currentStatus === 'ACTIVE'
            ? t('scheduled_transaction_paused')
            : t('scheduled_transaction_resumed'),
      });
    } catch (err) {
      toast({
        title: t('error'),
        description: t('error_server'),
        variant: 'destructive',
      });
    } finally {
      setActionLoading(null);
    }
  };

  const handleCancel = async (id: string) => {
    setActionLoading(id);
    try {
      // API call to cancel
      // await cancelScheduledTransaction(id);

      setTransactions(transactions.filter((tx) => tx.id !== id));

      toast({
        title: t('success'),
        description: t('scheduled_transaction_cancelled'),
      });
    } catch (err) {
      toast({
        title: t('error'),
        description: t('error_server'),
        variant: 'destructive',
      });
    } finally {
      setActionLoading(null);
      setCancelDialog(null);
    }
  };

  const formatNextDate = (dateString: string) => {
    const date = new Date(dateString);
    return new Intl.DateTimeFormat('fr-FR', {
      day: '2-digit',
      month: 'long',
      year: 'numeric',
    }).format(date);
  };

  const getFrequencyLabel = (frequency: string) => {
    switch (frequency) {
      case 'DAILY':
        return t('daily');
      case 'WEEKLY':
        return t('weekly');
      case 'MONTHLY':
        return t('monthly');
      default:
        return frequency;
    }
  };

  return (
    <div className="min-h-screen bg-background pb-20">
      {/* Header */}
      <header className="safe-area-top px-4 py-4 flex items-center justify-between border-b bg-background sticky top-0 z-10">
        <div className="flex items-center">
          <button
            onClick={onBack}
            className="w-10 h-10 rounded-full bg-secondary flex items-center justify-center touch-target mr-3"
          >
            <ChevronLeft className="w-5 h-5" />
          </button>
          <h1 className="text-lg font-semibold">{t('scheduled_transactions')}</h1>
        </div>
        <Button onClick={onCreate} size="icon">
          <Plus className="w-5 h-5" />
        </Button>
      </header>

      <div className="max-w-lg mx-auto px-4 py-6">
        {loading ? (
          <div className="space-y-4">
            {[...Array(3)].map((_, i) => (
              <Skeleton key={i} className="h-32 w-full" />
            ))}
          </div>
        ) : transactions.length > 0 ? (
          <div className="space-y-4">
            {transactions.map((tx, index) => (
              <motion.div
                key={tx.id}
                initial={{ opacity: 0, y: 20 }}
                animate={{ opacity: 1, y: 0 }}
                transition={{ delay: index * 0.05 }}
                className="bg-card rounded-lg p-4 space-y-3"
              >
                <div className="flex items-start justify-between">
                  <div className="flex-1">
                    <div className="flex items-center gap-2 mb-1">
                      <h3 className="font-semibold">{tx.receiverName}</h3>
                      <Badge
                        variant={tx.status === 'ACTIVE' ? 'default' : 'secondary'}
                        className="text-xs"
                      >
                        {t(tx.status.toLowerCase())}
                      </Badge>
                    </div>
                    <p className="text-sm text-muted-foreground font-mono">
                      {tx.receiverWalletNumber}
                    </p>
                  </div>
                  <p className="text-lg font-bold">
                    {formatCurrency(tx.amount, tx.currency)}
                  </p>
                </div>

                {tx.description && (
                  <p className="text-sm text-muted-foreground">{tx.description}</p>
                )}

                <div className="flex items-center gap-4 text-sm">
                  <div className="flex items-center gap-1 text-muted-foreground">
                    <Repeat className="w-4 h-4" />
                    {getFrequencyLabel(tx.frequency)}
                  </div>
                  <div className="flex items-center gap-1 text-muted-foreground">
                    <Calendar className="w-4 h-4" />
                    {formatNextDate(tx.nextExecutionDate)}
                  </div>
                </div>

                <div className="flex gap-2 pt-2">
                  {tx.status !== 'CANCELLED' && (
                    <>
                      <Button
                        variant="outline"
                        size="sm"
                        onClick={() => handlePauseResume(tx.id, tx.status)}
                        disabled={actionLoading === tx.id}
                        className="flex-1"
                      >
                        {tx.status === 'ACTIVE' ? (
                          <>
                            <Pause className="w-4 h-4 mr-1" />
                            {t('pause')}
                          </>
                        ) : (
                          <>
                            <Play className="w-4 h-4 mr-1" />
                            {t('resume')}
                          </>
                        )}
                      </Button>
                      <Button
                        variant="outline"
                        size="sm"
                        onClick={() => setCancelDialog(tx.id)}
                        disabled={actionLoading === tx.id}
                        className="text-destructive"
                      >
                        <Trash2 className="w-4 h-4" />
                      </Button>
                    </>
                  )}
                </div>
              </motion.div>
            ))}
          </div>
        ) : (
          <EmptyState
            icon={Calendar}
            title={t('no_scheduled_transactions')}
            description={t('no_scheduled_transactions_desc')}
            action={{
              label: t('create_scheduled_transaction'),
              onClick: onCreate,
            }}
          />
        )}
      </div>

      {/* Cancel Confirmation Dialog */}
      <AlertDialog
        open={cancelDialog !== null}
        onOpenChange={() => setCancelDialog(null)}
      >
        <AlertDialogContent>
          <AlertDialogHeader>
            <AlertDialogTitle>{t('confirm_cancel_scheduled')}</AlertDialogTitle>
            <AlertDialogDescription>
              {t('confirm_cancel_scheduled_desc')}
            </AlertDialogDescription>
          </AlertDialogHeader>
          <AlertDialogFooter>
            <AlertDialogCancel>{t('cancel')}</AlertDialogCancel>
            <AlertDialogAction
              onClick={() => cancelDialog && handleCancel(cancelDialog)}
              className="bg-destructive text-destructive-foreground"
            >
              {t('confirm')}
            </AlertDialogAction>
          </AlertDialogFooter>
        </AlertDialogContent>
      </AlertDialog>

      {/* Bottom Navigation */}
      <BottomNav activeTab={activeTab} onTabChange={handleTabChange} />
    </div>
  );
}

export default ScheduledTransactions;
