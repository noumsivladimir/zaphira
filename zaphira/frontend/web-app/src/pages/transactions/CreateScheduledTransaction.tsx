import React, { useState } from 'react';
import { motion, AnimatePresence } from 'framer-motion';
import { useTranslation } from 'react-i18next';
import { ChevronLeft, User, Calendar, Repeat } from 'lucide-react';
import { Button } from '@/components/ui/button';
import { Input } from '@/components/ui/input';
import { CurrencyInput } from '@/components/ui/currency-input';
import { Label } from '@/components/ui/label';
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from '@/components/ui/select';
import { SecureNumberPad } from '@/components/ui/secure-number-pad';
import { useToast } from '@/hooks/use-toast';
import { formatCurrency } from '@/lib/currency';

type Step = 'details' | 'pin' | 'success';

interface CreateScheduledTransactionProps {
  walletNumber: string;
  balance: number;
  onCreate: (data: {
    receiverWalletNumber: string;
    amount: number;
    frequency: string;
    startDate: string;
    description?: string;
    pin: string;
  }) => Promise<void>;
  onBack: () => void;
}

export function CreateScheduledTransaction({
  walletNumber,
  balance,
  onCreate,
  onBack,
}: CreateScheduledTransactionProps) {
  const { t } = useTranslation();
  const { toast } = useToast();
  const [step, setStep] = useState<Step>('details');
  const [receiverWallet, setReceiverWallet] = useState('');
  const [receiverName, setReceiverName] = useState('Jean Dupont'); // Mock - would come from API
  const [amount, setAmount] = useState(0);
  const [frequency, setFrequency] = useState<'DAILY' | 'WEEKLY' | 'MONTHLY'>('MONTHLY');
  const [startDate, setStartDate] = useState(() => {
    const tomorrow = new Date();
    tomorrow.setDate(tomorrow.getDate() + 1);
    return tomorrow.toISOString().split('T')[0];
  });
  const [description, setDescription] = useState('');
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');

  const handleDetailsSubmit = (e: React.FormEvent) => {
    e.preventDefault();

    if (receiverWallet.length < 8) {
      setError(t('error_invalid_wallet'));
      return;
    }

    if (amount <= 0) {
      setError(t('error_invalid_amount'));
      return;
    }

    if (amount > balance) {
      setError(t('error_insufficient_balance'));
      return;
    }

    setError('');
    setStep('pin');
  };

  const handlePinComplete = async (pin: string) => {
    setLoading(true);
    setError('');

    try {
      await onCreate({
        receiverWalletNumber: receiverWallet,
        amount,
        frequency,
        startDate,
        description: description || undefined,
        pin,
      });

      setStep('success');
      setTimeout(() => onBack(), 2000);
    } catch (err: any) {
      setError(err.message || t('error_server'));
    } finally {
      setLoading(false);
    }
  };

  const goBack = () => {
    setError('');
    if (step === 'pin') setStep('details');
    else onBack();
  };

  return (
    <div className="min-h-screen bg-background">
      {/* Header */}
      <header className="safe-area-top px-4 py-4 flex items-center border-b bg-background sticky top-0 z-10">
        {step !== 'success' && (
          <button
            onClick={goBack}
            className="w-10 h-10 rounded-full bg-secondary flex items-center justify-center touch-target mr-3"
            disabled={loading}
          >
            <ChevronLeft className="w-5 h-5" />
          </button>
        )}
        <h1 className="text-lg font-semibold">{t('create_scheduled_transaction')}</h1>
      </header>

      <div className="flex-1 flex flex-col">
        <AnimatePresence mode="wait">
          {/* Details Step */}
          {step === 'details' && (
            <motion.div
              key="details"
              initial={{ opacity: 0, x: 20 }}
              animate={{ opacity: 1, x: 0 }}
              exit={{ opacity: 0, x: -20 }}
              className="max-w-lg mx-auto w-full px-4 py-6"
            >
              <form onSubmit={handleDetailsSubmit} className="space-y-6">
                {/* Receiver */}
                <div className="space-y-2">
                  <Label htmlFor="receiver">{t('recipient')}</Label>
                  <div className="relative">
                    <User className="absolute left-3 top-1/2 -translate-y-1/2 w-5 h-5 text-muted-foreground" />
                    <Input
                      id="receiver"
                      type="text"
                      value={receiverWallet}
                      onChange={(e) => setReceiverWallet(e.target.value)}
                      placeholder={t('enter_wallet_number')}
                      className="pl-11"
                    />
                  </div>
                  {receiverName && (
                    <p className="text-sm text-muted-foreground">{receiverName}</p>
                  )}
                </div>

                {/* Amount */}
                <div className="space-y-2">
                  <Label htmlFor="amount">{t('amount')}</Label>
                  <CurrencyInput
                    value={amount}
                    onChange={setAmount}
                    max={balance}
                    placeholder="0"
                  />
                  <p className="text-xs text-muted-foreground">
                    {t('available')}: {formatCurrency(balance, 'XAF')}
                  </p>
                </div>

                {/* Frequency */}
                <div className="space-y-2">
                  <Label htmlFor="frequency">{t('frequency')}</Label>
                  <div className="relative">
                    <Repeat className="absolute left-3 top-1/2 -translate-y-1/2 w-5 h-5 text-muted-foreground z-10" />
                    <Select value={frequency} onValueChange={(v: any) => setFrequency(v)}>
                      <SelectTrigger className="pl-11">
                        <SelectValue />
                      </SelectTrigger>
                      <SelectContent>
                        <SelectItem value="DAILY">{t('daily')}</SelectItem>
                        <SelectItem value="WEEKLY">{t('weekly')}</SelectItem>
                        <SelectItem value="MONTHLY">{t('monthly')}</SelectItem>
                      </SelectContent>
                    </Select>
                  </div>
                </div>

                {/* Start Date */}
                <div className="space-y-2">
                  <Label htmlFor="startDate">{t('start_date')}</Label>
                  <div className="relative">
                    <Calendar className="absolute left-3 top-1/2 -translate-y-1/2 w-5 h-5 text-muted-foreground" />
                    <Input
                      id="startDate"
                      type="date"
                      value={startDate}
                      onChange={(e) => setStartDate(e.target.value)}
                      min={new Date().toISOString().split('T')[0]}
                      className="pl-11"
                    />
                  </div>
                </div>

                {/* Description */}
                <div className="space-y-2">
                  <Label htmlFor="description">{t('description_optional')}</Label>
                  <Input
                    id="description"
                    type="text"
                    value={description}
                    onChange={(e) => setDescription(e.target.value)}
                    placeholder={t('add_description')}
                  />
                </div>

                {error && (
                  <motion.p
                    initial={{ opacity: 0, y: -10 }}
                    animate={{ opacity: 1, y: 0 }}
                    className="text-sm text-destructive"
                  >
                    {error}
                  </motion.p>
                )}

                <Button
                  type="submit"
                  size="lg"
                  className="w-full"
                  disabled={!receiverWallet || amount <= 0}
                >
                  {t('continue')}
                </Button>
              </form>
            </motion.div>
          )}

          {/* PIN Step */}
          {step === 'pin' && (
            <motion.div
              key="pin"
              initial={{ opacity: 0, x: 20 }}
              animate={{ opacity: 1, x: 0 }}
              exit={{ opacity: 0, x: -20 }}
              className="flex-1 flex flex-col px-6 pt-8"
            >
              <div className="mb-8">
                <h1 className="text-2xl font-bold text-foreground font-display mb-2">
                  {t('confirm_with_pin')}
                </h1>
                <p className="text-muted-foreground">{t('enter_pin_to_confirm')}</p>
              </div>

              <div className="flex-1 flex flex-col items-center justify-center">
                <SecureNumberPad
                  length={4}
                  onComplete={handlePinComplete}
                  loading={loading}
                  error={error}
                />
              </div>
            </motion.div>
          )}

          {/* Success Step */}
          {step === 'success' && (
            <motion.div
              key="success"
              initial={{ opacity: 0, scale: 0.9 }}
              animate={{ opacity: 1, scale: 1 }}
              className="flex-1 flex flex-col items-center justify-center px-6"
            >
              <motion.div
                initial={{ scale: 0 }}
                animate={{ scale: 1 }}
                transition={{ delay: 0.2, type: 'spring' }}
                className="w-20 h-20 rounded-full bg-green-100 dark:bg-green-900/30 flex items-center justify-center mb-6"
              >
                <Repeat className="w-12 h-12 text-green-600 dark:text-green-500" />
              </motion.div>
              <h1 className="text-2xl font-bold text-foreground font-display mb-2 text-center">
                {t('scheduled_transaction_created')}
              </h1>
              <p className="text-muted-foreground text-center">
                {t('scheduled_transaction_created_desc')}
              </p>
            </motion.div>
          )}
        </AnimatePresence>
      </div>
    </div>
  );
}

export default CreateScheduledTransaction;
