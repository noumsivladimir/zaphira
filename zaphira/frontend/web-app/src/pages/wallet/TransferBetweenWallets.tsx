import React, { useState } from 'react';
import { motion } from 'framer-motion';
import { useTranslation } from 'react-i18next';
import { 
  ChevronLeft,
  ArrowRightLeft,
  Wallet,
  Check
} from 'lucide-react';
import { Button } from '@/components/ui/button';
import { Card } from '@/components/ui/card';
import { Badge } from '@/components/ui/badge';
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from '@/components/ui/select';
import { Label } from '@/components/ui/label';
import { CurrencyInput } from '@/components/ui/currency-input';
import { OTPInput } from '@/components/ui/otp-input';
import { formatCurrency, maskWalletNumber } from '@/lib/currency';
import { useToast } from '@/hooks/use-toast';
import { cn } from '@/lib/utils';

interface Wallet {
  id: string;
  name: string;
  walletNumber: string;
  balance: number;
  type: string;
  currency: string;
}

interface TransferBetweenWalletsProps {
  wallets: Wallet[];
  onBack: () => void;
  onTransfer: (data: { fromWallet: string; toWallet: string; amount: number; pin: string }) => Promise<void>;
}

export function TransferBetweenWallets({ 
  wallets,
  onBack,
  onTransfer 
}: TransferBetweenWalletsProps) {
  const { t } = useTranslation();
  const { toast } = useToast();
  const [step, setStep] = useState<'details' | 'confirm'>('details');
  const [fromWalletId, setFromWalletId] = useState<string>('');
  const [toWalletId, setToWalletId] = useState<string>('');
  const [amount, setAmount] = useState<number>(0);
  const [pin, setPin] = useState('');
  const [loading, setLoading] = useState(false);

  const fromWallet = wallets.find(w => w.id === fromWalletId);
  const toWallet = wallets.find(w => w.id === toWalletId);

  const handleContinue = () => {
    if (!fromWalletId || !toWalletId) {
      toast({
        title: 'Erreur',
        description: 'Veuillez sélectionner les wallets source et destination',
        variant: 'destructive',
      });
      return;
    }

    if (fromWalletId === toWalletId) {
      toast({
        title: 'Erreur',
        description: 'Les wallets source et destination doivent être différents',
        variant: 'destructive',
      });
      return;
    }

    if (amount <= 0) {
      toast({
        title: 'Erreur',
        description: 'Veuillez entrer un montant valide',
        variant: 'destructive',
      });
      return;
    }

    if (fromWallet && amount > fromWallet.balance) {
      toast({
        title: 'Solde insuffisant',
        description: 'Le montant dépasse le solde disponible',
        variant: 'destructive',
      });
      return;
    }

    setStep('confirm');
  };

  const handleConfirm = async () => {
    if (pin.length !== 4) {
      toast({
        title: 'Erreur',
        description: 'Veuillez entrer votre code PIN',
        variant: 'destructive',
      });
      return;
    }

    setLoading(true);
    try {
      await onTransfer({
        fromWallet: fromWalletId,
        toWallet: toWalletId,
        amount,
        pin,
      });

      toast({
        title: 'Transfert réussi',
        description: `${formatCurrency(amount, 'XAF')} transféré avec succès`,
      });

      onBack();
    } catch (err) {
      toast({
        title: 'Erreur',
        description: 'Le transfert a échoué. Veuillez réessayer.',
        variant: 'destructive',
      });
    } finally {
      setLoading(false);
    }
  };

  const handleSwapWallets = () => {
    const temp = fromWalletId;
    setFromWalletId(toWalletId);
    setToWalletId(temp);
  };

  return (
    <div className="min-h-screen bg-background">
      {/* Header */}
      <header className="safe-area-top px-4 py-4 flex items-center border-b bg-background sticky top-0 z-10">
        <Button variant="ghost" size="icon" onClick={onBack}>
          <ChevronLeft className="w-6 h-6" />
        </Button>
        <h1 className="text-lg font-semibold ml-3">Transfert entre wallets</h1>
      </header>

      <div className="max-w-lg mx-auto px-4 py-6">
        {step === 'details' ? (
          <motion.div
            initial={{ opacity: 0, y: 20 }}
            animate={{ opacity: 1, y: 0 }}
            className="space-y-6"
          >
            {/* Info Banner */}
            <Card className="p-4 bg-blue-50 dark:bg-blue-900/20 border-blue-200 dark:border-blue-800">
              <p className="text-sm text-blue-800 dark:text-blue-200">
                💳 Aucun frais pour les transferts entre vos wallets
              </p>
            </Card>

            {/* From Wallet */}
            <div className="space-y-2">
              <Label>Depuis le wallet</Label>
              <Select value={fromWalletId} onValueChange={setFromWalletId}>
                <SelectTrigger>
                  <SelectValue placeholder="Sélectionner le wallet source" />
                </SelectTrigger>
                <SelectContent>
                  {wallets.map((wallet) => (
                    <SelectItem key={wallet.id} value={wallet.id}>
                      <div className="flex items-center justify-between w-full">
                        <span>{wallet.name}</span>
                        <span className="text-xs text-muted-foreground ml-2">
                          {formatCurrency(wallet.balance, wallet.currency)}
                        </span>
                      </div>
                    </SelectItem>
                  ))}
                </SelectContent>
              </Select>
              {fromWallet && (
                <Card className="p-3 bg-accent">
                  <div className="flex items-center justify-between">
                    <div>
                      <p className="font-medium">{fromWallet.name}</p>
                      <p className="text-xs text-muted-foreground font-mono">
                        {maskWalletNumber(fromWallet.walletNumber)}
                      </p>
                    </div>
                    <div className="text-right">
                      <p className="font-semibold">
                        {formatCurrency(fromWallet.balance, fromWallet.currency)}
                      </p>
                      <Badge variant="outline" className="text-xs mt-1">
                        {fromWallet.type}
                      </Badge>
                    </div>
                  </div>
                </Card>
              )}
            </div>

            {/* Swap Button */}
            {fromWalletId && toWalletId && (
              <div className="flex justify-center">
                <Button
                  variant="outline"
                  size="icon"
                  className="rounded-full"
                  onClick={handleSwapWallets}
                >
                  <ArrowRightLeft className="w-4 h-4" />
                </Button>
              </div>
            )}

            {/* To Wallet */}
            <div className="space-y-2">
              <Label>Vers le wallet</Label>
              <Select value={toWalletId} onValueChange={setToWalletId}>
                <SelectTrigger>
                  <SelectValue placeholder="Sélectionner le wallet destination" />
                </SelectTrigger>
                <SelectContent>
                  {wallets
                    .filter(w => w.id !== fromWalletId)
                    .map((wallet) => (
                      <SelectItem key={wallet.id} value={wallet.id}>
                        <div className="flex items-center justify-between w-full">
                          <span>{wallet.name}</span>
                          <span className="text-xs text-muted-foreground ml-2">
                            {formatCurrency(wallet.balance, wallet.currency)}
                          </span>
                        </div>
                      </SelectItem>
                    ))}
                </SelectContent>
              </Select>
              {toWallet && (
                <Card className="p-3 bg-accent">
                  <div className="flex items-center justify-between">
                    <div>
                      <p className="font-medium">{toWallet.name}</p>
                      <p className="text-xs text-muted-foreground font-mono">
                        {maskWalletNumber(toWallet.walletNumber)}
                      </p>
                    </div>
                    <div className="text-right">
                      <p className="font-semibold">
                        {formatCurrency(toWallet.balance, toWallet.currency)}
                      </p>
                      <Badge variant="outline" className="text-xs mt-1">
                        {toWallet.type}
                      </Badge>
                    </div>
                  </div>
                </Card>
              )}
            </div>

            {/* Amount */}
            <div className="space-y-2">
              <Label>Montant à transférer</Label>
              <CurrencyInput
                value={amount}
                onChange={setAmount}
                currency="XAF"
                placeholder="0"
                max={fromWallet?.balance}
              />
              {fromWallet && (
                <p className="text-xs text-muted-foreground">
                  Disponible: {formatCurrency(fromWallet.balance, fromWallet.currency)}
                </p>
              )}
            </div>

            <Button 
              onClick={handleContinue} 
              className="w-full"
              size="lg"
              disabled={!fromWalletId || !toWalletId || amount <= 0}
            >
              Continuer
            </Button>
          </motion.div>
        ) : (
          <motion.div
            initial={{ opacity: 0, y: 20 }}
            animate={{ opacity: 1, y: 0 }}
            className="space-y-6"
          >
            <Card className="p-6">
              <div className="text-center mb-6">
                <div className="w-16 h-16 bg-primary/10 rounded-full flex items-center justify-center mx-auto mb-4">
                  <ArrowRightLeft className="w-8 h-8 text-primary" />
                </div>
                <h2 className="text-2xl font-bold mb-2">
                  {formatCurrency(amount, 'XAF')}
                </h2>
                <p className="text-sm text-muted-foreground">
                  Confirmer le transfert
                </p>
              </div>

              <div className="space-y-4">
                {/* From */}
                <div className="flex items-start justify-between p-3 bg-accent rounded-lg">
                  <div className="flex items-center gap-3">
                    <div className="w-10 h-10 bg-background rounded-full flex items-center justify-center">
                      <Wallet className="w-5 h-5 text-primary" />
                    </div>
                    <div>
                      <p className="text-xs text-muted-foreground">De</p>
                      <p className="font-semibold">{fromWallet?.name}</p>
                      <p className="text-xs text-muted-foreground font-mono">
                        {maskWalletNumber(fromWallet?.walletNumber || '')}
                      </p>
                    </div>
                  </div>
                </div>

                <div className="flex justify-center">
                  <div className="w-8 h-8 bg-accent rounded-full flex items-center justify-center">
                    <ArrowRightLeft className="w-4 h-4" />
                  </div>
                </div>

                {/* To */}
                <div className="flex items-start justify-between p-3 bg-accent rounded-lg">
                  <div className="flex items-center gap-3">
                    <div className="w-10 h-10 bg-background rounded-full flex items-center justify-center">
                      <Wallet className="w-5 h-5 text-primary" />
                    </div>
                    <div>
                      <p className="text-xs text-muted-foreground">Vers</p>
                      <p className="font-semibold">{toWallet?.name}</p>
                      <p className="text-xs text-muted-foreground font-mono">
                        {maskWalletNumber(toWallet?.walletNumber || '')}
                      </p>
                    </div>
                  </div>
                </div>
              </div>
            </Card>

            {/* PIN Input */}
            <div className="space-y-4">
              <Label className="text-center block">Entrez votre code PIN</Label>
              <OTPInput
                length={4}
                onComplete={setPin}
              />
            </div>

            <div className="flex gap-3">
              <Button 
                onClick={() => {
                  setStep('details');
                  setPin('');
                }} 
                variant="outline"
                className="flex-1"
                size="lg"
              >
                Retour
              </Button>
              <Button 
                onClick={handleConfirm} 
                className="flex-1"
                size="lg"
                disabled={pin.length !== 4 || loading}
              >
                {loading ? 'Transfert...' : 'Confirmer'}
              </Button>
            </div>
          </motion.div>
        )}
      </div>
    </div>
  );
}

export default TransferBetweenWallets;
