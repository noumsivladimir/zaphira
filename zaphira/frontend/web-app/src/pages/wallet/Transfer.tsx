import React, { useState } from 'react';
import { motion, AnimatePresence } from 'framer-motion';
import { useTranslation } from 'react-i18next';
import { ChevronLeft, User, CheckCircle2, ArrowRight } from 'lucide-react';
import { Button } from '@/components/ui/button';
import { Input } from '@/components/ui/input';
import { CurrencyInput } from '@/components/ui/currency-input';
import { SecureNumberPad } from '@/components/ui/secure-number-pad';
import { AvatarDisplay } from '@/components/ui/avatar-display';
import { formatCurrency } from '@/lib/currency';
import { cn } from '@/lib/utils';

type TransferStep = 'recipient' | 'amount' | 'confirm' | 'pin' | 'success';

interface Recipient {
  name: string;
  walletNumber: string;
}

interface TransferProps {
  balance: number;
  onTransfer: (data: {
    recipientWallet: string;
    amount: number;
    description?: string;
    pin: string;
  }) => Promise<{ reference: string }>;
  onClose: () => void;
}

export function Transfer({ balance, onTransfer, onClose }: TransferProps) {
  const { t } = useTranslation();
  const [step, setStep] = useState<TransferStep>('recipient');
  const [recipient, setRecipient] = useState<Recipient | null>(null);
  const [walletInput, setWalletInput] = useState('');
  const [amount, setAmount] = useState(0);
  const [description, setDescription] = useState('');
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');
  const [reference, setReference] = useState('');

  const fees = Math.round(amount * 0.01); // 1% fee example
  const total = amount + fees;

  const handleRecipientSubmit = () => {
    if (walletInput.length < 8) {
      setError('Numéro de portefeuille invalide');
      return;
    }
    // In real app, verify wallet and get recipient name
    setRecipient({
      name: 'Jean Dupont',
      walletNumber: walletInput,
    });
    setError('');
    setStep('amount');
  };

  const handleAmountSubmit = () => {
    if (amount <= 0) {
      setError(t('error_invalid_amount'));
      return;
    }
    if (total > balance) {
      setError(t('error_insufficient_balance'));
      return;
    }
    setError('');
    setStep('confirm');
  };

  const handleConfirm = () => {
    setStep('pin');
  };

  const handlePinComplete = async (pin: string) => {
    setLoading(true);
    setError('');

    try {
      const result = await onTransfer({
        recipientWallet: recipient!.walletNumber,
        amount,
        description,
        pin,
      });
      setReference(result.reference);
      setStep('success');
    } catch (err: any) {
      setError(err.message || t('error_server'));
    } finally {
      setLoading(false);
    }
  };

  const goBack = () => {
    setError('');
    if (step === 'amount') setStep('recipient');
    else if (step === 'confirm') setStep('amount');
    else if (step === 'pin') setStep('confirm');
    else onClose();
  };

  return (
    <div className="min-h-screen bg-background flex flex-col">
      {/* Header */}
      <header className="safe-area-top px-4 py-4 flex items-center gap-4 border-b border-border">
        {step !== 'success' && (
          <button
            onClick={goBack}
            className="w-10 h-10 rounded-full bg-secondary flex items-center justify-center touch-target"
          >
            <ChevronLeft className="w-5 h-5" />
          </button>
        )}
        <h1 className="text-lg font-semibold font-display">{t('send_money')}</h1>
      </header>

      <div className="flex-1 flex flex-col">
        <AnimatePresence mode="wait">
          {/* Step 1: Recipient */}
          {step === 'recipient' && (
            <motion.div
              key="recipient"
              initial={{ opacity: 0, x: 20 }}
              animate={{ opacity: 1, x: 0 }}
              exit={{ opacity: 0, x: -20 }}
              className="flex-1 flex flex-col p-6"
            >
              <div className="mb-6">
                <label className="block text-sm font-medium text-foreground mb-2">
                  {t('wallet_number')}
                </label>
                <div className="relative">
                  <User className="absolute left-4 top-1/2 -translate-y-1/2 w-5 h-5 text-muted-foreground" />
                  <Input
                    type="text"
                    inputMode="numeric"
                    value={walletInput}
                    onChange={(e) => {
                      setWalletInput(e.target.value.replace(/\D/g, ''));
                      setError('');
                    }}
                    placeholder={t('enter_wallet_number')}
                    className={cn(
                      'pl-12 h-14 text-lg rounded-xl font-mono',
                      error && 'border-destructive'
                    )}
                    autoFocus
                  />
                </div>
                {error && (
                  <p className="text-destructive text-sm mt-2">{error}</p>
                )}
              </div>

              <div className="mt-auto">
                <Button
                  onClick={handleRecipientSubmit}
                  className="w-full h-14 text-lg rounded-xl shadow-button"
                  disabled={!walletInput}
                >
                  {t('continue')}
                </Button>
              </div>
            </motion.div>
          )}

          {/* Step 2: Amount */}
          {step === 'amount' && (
            <motion.div
              key="amount"
              initial={{ opacity: 0, x: 20 }}
              animate={{ opacity: 1, x: 0 }}
              exit={{ opacity: 0, x: -20 }}
              className="flex-1 flex flex-col p-6"
            >
              {/* Recipient preview */}
              <div className="flex items-center gap-3 p-4 rounded-xl bg-secondary/50 mb-8">
                <AvatarDisplay name={recipient?.name} size="md" />
                <div>
                  <p className="font-medium">{recipient?.name}</p>
                  <p className="text-sm text-muted-foreground font-mono">
                    {recipient?.walletNumber}
                  </p>
                </div>
              </div>

              {/* Amount input */}
              <div className="flex-1 flex flex-col items-center justify-center">
                <CurrencyInput
                  value={amount}
                  onChange={setAmount}
                  max={balance}
                  error={error}
                  autoFocus
                />
                <p className="text-sm text-muted-foreground mt-4">
                  Solde disponible: {formatCurrency(balance)}
                </p>
              </div>

              {/* Description */}
              <div className="mb-6">
                <Input
                  value={description}
                  onChange={(e) => setDescription(e.target.value)}
                  placeholder={t('add_description')}
                  className="h-12 rounded-xl"
                />
              </div>

              <Button
                onClick={handleAmountSubmit}
                className="w-full h-14 text-lg rounded-xl shadow-button"
                disabled={amount <= 0}
              >
                {t('continue')}
              </Button>
            </motion.div>
          )}

          {/* Step 3: Confirm */}
          {step === 'confirm' && (
            <motion.div
              key="confirm"
              initial={{ opacity: 0, x: 20 }}
              animate={{ opacity: 1, x: 0 }}
              exit={{ opacity: 0, x: -20 }}
              className="flex-1 flex flex-col p-6"
            >
              <div className="trust-card mb-6">
                <p className="text-sm text-muted-foreground mb-1">{t('transfer_to')}</p>
                <div className="flex items-center gap-3 mb-6">
                  <AvatarDisplay name={recipient?.name} size="lg" />
                  <div>
                    <p className="font-semibold text-lg">{recipient?.name}</p>
                    <p className="text-sm text-muted-foreground font-mono">
                      {recipient?.walletNumber}
                    </p>
                  </div>
                </div>

                <div className="space-y-4 border-t border-border pt-4">
                  <div className="flex justify-between">
                    <span className="text-muted-foreground">{t('amount')}</span>
                    <span className="font-semibold">{formatCurrency(amount)}</span>
                  </div>
                  <div className="flex justify-between">
                    <span className="text-muted-foreground">{t('fees')}</span>
                    <span className="font-semibold">{formatCurrency(fees)}</span>
                  </div>
                  <div className="flex justify-between text-lg">
                    <span className="font-medium">{t('total')}</span>
                    <span className="font-bold text-primary">{formatCurrency(total)}</span>
                  </div>
                </div>

                {description && (
                  <div className="mt-4 pt-4 border-t border-border">
                    <p className="text-sm text-muted-foreground">{description}</p>
                  </div>
                )}
              </div>

              <div className="mt-auto">
                <Button
                  onClick={handleConfirm}
                  className="w-full h-14 text-lg rounded-xl shadow-button"
                >
                  {t('confirm_transfer')}
                  <ArrowRight className="w-5 h-5 ml-2" />
                </Button>
              </div>
            </motion.div>
          )}

          {/* Step 4: PIN */}
          {step === 'pin' && (
            <motion.div
              key="pin"
              initial={{ opacity: 0, x: 20 }}
              animate={{ opacity: 1, x: 0 }}
              exit={{ opacity: 0, x: -20 }}
              className="flex-1 flex flex-col items-center justify-center"
            >
              <SecureNumberPad
                length={4}
                title={t('confirm_with_pin')}
                subtitle={`Envoi de ${formatCurrency(total)}`}
                onComplete={handlePinComplete}
                error={error}
                loading={loading}
                onCancel={() => setStep('confirm')}
              />
            </motion.div>
          )}

          {/* Step 5: Success */}
          {step === 'success' && (
            <motion.div
              key="success"
              initial={{ opacity: 0, scale: 0.9 }}
              animate={{ opacity: 1, scale: 1 }}
              className="flex-1 flex flex-col items-center justify-center p-6 text-center"
            >
              <motion.div
                initial={{ scale: 0 }}
                animate={{ scale: 1 }}
                transition={{ type: 'spring', stiffness: 300, damping: 20, delay: 0.2 }}
                className="w-20 h-20 rounded-full bg-success flex items-center justify-center mb-6"
              >
                <CheckCircle2 className="w-10 h-10 text-white" />
              </motion.div>

              <motion.h2
                initial={{ opacity: 0, y: 20 }}
                animate={{ opacity: 1, y: 0 }}
                transition={{ delay: 0.3 }}
                className="text-2xl font-bold font-display mb-2"
              >
                {t('transfer_successful')}
              </motion.h2>

              <motion.p
                initial={{ opacity: 0, y: 20 }}
                animate={{ opacity: 1, y: 0 }}
                transition={{ delay: 0.4 }}
                className="text-muted-foreground mb-8"
              >
                <span className="font-semibold text-foreground">
                  {formatCurrency(amount)}
                </span>{' '}
                {t('transfer_amount_sent')} {recipient?.name}
              </motion.p>

              <motion.div
                initial={{ opacity: 0, y: 20 }}
                animate={{ opacity: 1, y: 0 }}
                transition={{ delay: 0.5 }}
                className="w-full p-4 rounded-xl bg-secondary/50 mb-8"
              >
                <p className="text-sm text-muted-foreground mb-1">
                  {t('transaction_reference')}
                </p>
                <p className="font-mono font-medium">{reference}</p>
              </motion.div>

              <Button onClick={onClose} className="w-full h-14 rounded-xl">
                {t('done')}
              </Button>
            </motion.div>
          )}
        </AnimatePresence>
      </div>
    </div>
  );
}

export default Transfer;
