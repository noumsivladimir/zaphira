import React, { useState, useEffect } from 'react';
import { motion } from 'framer-motion';
import { useTranslation } from 'react-i18next';
import { ChevronLeft, Plus, Wallet, Lock, Unlock, AlertCircle } from 'lucide-react';
import { Button } from '@/components/ui/button';
import { Badge } from '@/components/ui/badge';
import { Separator } from '@/components/ui/separator';
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
import { formatCurrency, maskWalletNumber } from '@/lib/currency';
import { useToast } from '@/hooks/use-toast';
import { cn } from '@/lib/utils';
import { getSubWalletsByWalletId } from '@/api/wallet.api';

interface SubWallet {
  id: string;
  name: string;
  walletNumber: string;
  balance: number;
  currency: string;
  status: 'ACTIVE' | 'FROZEN';
}

interface WalletManagementProps {
  mainWallet: {
    walletNumber: string;
    balance: number;
    status: string;
  };
  onBack: () => void;
  onCreateSubWallet: () => void;
}

export function WalletManagement({
  mainWallet,
  onBack,
  onCreateSubWallet,
}: WalletManagementProps) {
  const { t } = useTranslation();
  const { toast } = useToast();
  const [subWallets, setSubWallets] = useState<SubWallet[]>([]);
  const [loading, setLoading] = useState(true);
  const [actionLoading, setActionLoading] = useState<string | null>(null);
  const [freezeDialog, setFreezeDialog] = useState<SubWallet | null>(null);

  useEffect(() => {
    loadSubWallets();
  }, [mainWallet.walletNumber]);

  const loadSubWallets = async () => {
    setLoading(true);
    try {
      if (!mainWallet.walletNumber) {
        setSubWallets([]);
        return;
      }
      const subWallets = (await getSubWalletsByWalletId(mainWallet.walletNumber)).map((subWallet) => ({
        id: subWallet.id.toString(),
        name: subWallet.subWalletName || 'Sous-wallet',
        walletNumber: subWallet.id.toString(),
        balance: Number(subWallet.totalBalance ?? subWallet.availableBalance ?? 0),
        currency: subWallet.currency || 'XAF',
        status: subWallet.status === 'FROZEN' ? 'FROZEN' : 'ACTIVE',
      }));
      setSubWallets(subWallets);
    } catch (err) {
      toast({
        title: t('error'),
        description: t('error_loading_wallets'),
        variant: 'destructive',
      });
    } finally {
      setLoading(false);
    }
  };

  const handleFreezeUnfreeze = async (wallet: SubWallet) => {
    setActionLoading(wallet.id);
    try {
      toast({
        title: t('error'),
        description: t('error_server'),
        variant: 'destructive',
      });
    } catch (err) {
      toast({
        title: t('error'),
        description: t('error_server'),
        variant: 'destructive',
      });
    } finally {
      setActionLoading(null);
      setFreezeDialog(null);
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
          <h1 className="text-lg font-semibold">{t('wallet_management')}</h1>
        </div>
      </header>

      <div className="max-w-lg mx-auto px-4 py-6">
        {/* Main Wallet */}
        <motion.div
          initial={{ opacity: 0, y: 20 }}
          animate={{ opacity: 1, y: 0 }}
          className="mb-8"
        >
          <h2 className="text-sm font-medium text-muted-foreground mb-3">
            {t('main_wallet')}
          </h2>
          <div className="bg-gradient-to-br from-primary to-primary/80 rounded-lg p-6 text-primary-foreground">
            <div className="flex items-center gap-2 mb-4">
              <Wallet className="w-5 h-5" />
              <span className="text-sm font-medium">{t('main_account')}</span>
              <Badge variant="secondary" className="ml-auto">
                {mainWallet.status}
              </Badge>
            </div>
            <p className="text-3xl font-bold mb-2">
              {formatCurrency(mainWallet.balance, 'XAF')}
            </p>
            <p className="text-sm opacity-80 font-mono">
              {maskWalletNumber(mainWallet.walletNumber)}
            </p>
          </div>
        </motion.div>

        {/* Sub-Wallets Section */}
        <div className="mb-6">
          <div className="flex items-center justify-between mb-4">
            <h2 className="text-sm font-medium text-muted-foreground">
              {t('sub_wallets')}
            </h2>
            <Button onClick={onCreateSubWallet} size="sm" className="gap-2">
              <Plus className="w-4 h-4" />
              {t('create')}
            </Button>
          </div>

          {loading ? (
            <div className="space-y-4">
              {[...Array(2)].map((_, i) => (
                <Skeleton key={i} className="h-40 w-full" />
              ))}
            </div>
          ) : subWallets.length > 0 ? (
            <div className="space-y-4">
              {subWallets.map((wallet, index) => (
                <motion.div
                  key={wallet.id}
                  initial={{ opacity: 0, y: 20 }}
                  animate={{ opacity: 1, y: 0 }}
                  transition={{ delay: index * 0.05 }}
                  className={cn(
                    'bg-card rounded-lg p-4 border-2',
                    wallet.status === 'FROZEN'
                      ? 'border-muted opacity-60'
                      : 'border-transparent'
                  )}
                >
                  <div className="flex items-start justify-between mb-3">
                    <div className="flex-1">
                      <div className="flex items-center gap-2 mb-1">
                        <h3 className="font-semibold">{wallet.name}</h3>
                        {wallet.status === 'FROZEN' && (
                          <Badge variant="secondary" className="text-xs">
                            <Lock className="w-3 h-3 mr-1" />
                            {t('frozen')}
                          </Badge>
                        )}
                      </div>
                      <p className="text-xs text-muted-foreground font-mono">
                        {maskWalletNumber(wallet.walletNumber)}
                      </p>
                    </div>
                  </div>

                  <Separator className="my-3" />

                  <div className="flex items-center justify-between">
                    <div>
                      <p className="text-xs text-muted-foreground mb-1">
                        {t('balance')}
                      </p>
                      <p className="text-xl font-bold">
                        {formatCurrency(wallet.balance, wallet.currency)}
                      </p>
                    </div>
                    <Button
                      variant={wallet.status === 'ACTIVE' ? 'outline' : 'default'}
                      size="sm"
                      onClick={() => setFreezeDialog(wallet)}
                      disabled={actionLoading === wallet.id}
                      className="gap-2"
                    >
                      {wallet.status === 'ACTIVE' ? (
                        <>
                          <Lock className="w-4 h-4" />
                          {t('freeze')}
                        </>
                      ) : (
                        <>
                          <Unlock className="w-4 h-4" />
                          {t('unfreeze')}
                        </>
                      )}
                    </Button>
                  </div>
                </motion.div>
              ))}
            </div>
          ) : (
            <EmptyState
              icon={Wallet}
              title={t('no_sub_wallets')}
              description={t('no_sub_wallets_desc')}
              action={{
                label: t('create_sub_wallet'),
                onClick: onCreateSubWallet,
              }}
            />
          )}
        </div>

        {/* Info Banner */}
        <motion.div
          initial={{ opacity: 0, y: 20 }}
          animate={{ opacity: 1, y: 0 }}
          transition={{ delay: 0.3 }}
          className="p-4 rounded-lg bg-blue-50 dark:bg-blue-950/30 border border-blue-200 dark:border-blue-900 flex gap-3"
        >
          <AlertCircle className="w-5 h-5 text-blue-600 dark:text-blue-400 flex-shrink-0 mt-0.5" />
          <div className="text-sm text-blue-900 dark:text-blue-200">
            <p className="font-medium mb-1">{t('wallet_management_info_title')}</p>
            <p>{t('wallet_management_info_desc')}</p>
          </div>
        </motion.div>
      </div>

      {/* Freeze/Unfreeze Confirmation Dialog */}
      <AlertDialog
        open={freezeDialog !== null}
        onOpenChange={() => setFreezeDialog(null)}
      >
        <AlertDialogContent>
          <AlertDialogHeader>
            <AlertDialogTitle>
              {freezeDialog?.status === 'ACTIVE'
                ? t('confirm_freeze_wallet')
                : t('confirm_unfreeze_wallet')}
            </AlertDialogTitle>
            <AlertDialogDescription>
              {freezeDialog?.status === 'ACTIVE'
                ? t('confirm_freeze_wallet_desc', { name: freezeDialog?.name })
                : t('confirm_unfreeze_wallet_desc', { name: freezeDialog?.name })}
            </AlertDialogDescription>
          </AlertDialogHeader>
          <AlertDialogFooter>
            <AlertDialogCancel>{t('cancel')}</AlertDialogCancel>
            <AlertDialogAction
              onClick={() => freezeDialog && handleFreezeUnfreeze(freezeDialog)}
            >
              {t('confirm')}
            </AlertDialogAction>
          </AlertDialogFooter>
        </AlertDialogContent>
      </AlertDialog>
    </div>
  );
}

export default WalletManagement;
