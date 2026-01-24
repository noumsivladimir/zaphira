import React, { useState } from 'react';
import { motion } from 'framer-motion';
import { useTranslation } from 'react-i18next';
import { ChevronLeft, ArrowDownToLine, AlertCircle } from 'lucide-react';
import { Button } from '@/components/ui/button';
import { CurrencyInput } from '@/components/ui/currency-input';
import { Input } from '@/components/ui/input';
import { Label } from '@/components/ui/label';
import { Alert, AlertDescription } from '@/components/ui/alert';
import { Tabs, TabsContent, TabsList, TabsTrigger } from '@/components/ui/tabs';
import { Card } from '@/components/ui/card';
import { OTPInput } from '@/components/ui/otp-input';
import { formatCurrency } from '@/lib/currency';
import { cn } from '@/lib/utils';

interface WithdrawProps {
  balance: number;
  currency?: string;
  onBack: () => void;
  onWithdraw: (data: WithdrawData) => void;
}

export interface WithdrawData {
  method: 'mobile_money' | 'bank';
  amount: number;
  phoneNumber?: string;
  bankAccount?: string;
  bankCode?: string;
  accountName?: string;
  pin: string;
}

export function Withdraw({
  balance,
  currency = 'XAF',
  onBack,
  onWithdraw,
}: WithdrawProps) {
  const { t } = useTranslation();
  const [method, setMethod] = useState<'mobile_money' | 'bank'>('mobile_money');
  const [amount, setAmount] = useState<number | undefined>();
  const [phoneNumber, setPhoneNumber] = useState('');
  const [bankAccount, setBankAccount] = useState('');
  const [bankCode, setBankCode] = useState('');
  const [accountName, setAccountName] = useState('');
  const [operator, setOperator] = useState<'mtn' | 'orange' | ''>('');
  const [pin, setPin] = useState('');
  const [showPinStep, setShowPinStep] = useState(false);
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(false);

  const withdrawalFee = 0; // Fee calculation based on amount
  const minWithdraw = 500;
  const maxWithdraw = 2000000;

  const handleAmountChange = (value: number | undefined) => {
    setAmount(value);
    setError('');
  };

  const handleSubmit = () => {
    setError('');

    // Validation
    if (!amount || amount <= 0) {
      setError('Veuillez entrer un montant valide');
      return;
    }

    if (amount < minWithdraw) {
      setError(`Le montant minimum est ${formatCurrency(minWithdraw, currency)}`);
      return;
    }

    if (amount > maxWithdraw) {
      setError(`Le montant maximum est ${formatCurrency(maxWithdraw, currency)}`);
      return;
    }

    if (amount > balance) {
      setError('Solde insuffisant');
      return;
    }

    if (method === 'mobile_money') {
      if (!phoneNumber || phoneNumber.length < 9) {
        setError('Veuillez entrer un numéro de téléphone valide');
        return;
      }
      if (!operator) {
        setError('Veuillez sélectionner un opérateur');
        return;
      }
    } else {
      if (!bankAccount || !bankCode || !accountName) {
        setError('Veuillez remplir tous les champs bancaires');
        return;
      }
    }

    setShowPinStep(true);
  };

  const handleConfirm = async () => {
    if (pin.length !== 4) {
      setError('Veuillez entrer votre code PIN');
      return;
    }

    setLoading(true);
    setError('');

    try {
      const data: WithdrawData = {
        method,
        amount: amount!,
        pin,
      };

      if (method === 'mobile_money') {
        data.phoneNumber = phoneNumber;
      } else {
        data.bankAccount = bankAccount;
        data.bankCode = bankCode;
        data.accountName = accountName;
      }

      await onWithdraw(data);
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Échec du retrait');
      setLoading(false);
    }
  };

  const handleCancel = () => {
    setShowPinStep(false);
    setPin('');
    setError('');
  };

  if (showPinStep) {
    return (
      <div className="min-h-screen bg-background">
        <div className="max-w-lg mx-auto px-4 py-6 safe-area-top">
          {/* Header */}
          <div className="flex items-center gap-4 mb-8">
            <Button
              variant="ghost"
              size="icon"
              onClick={handleCancel}
              className="touch-target"
            >
              <ChevronLeft className="w-6 h-6" />
            </Button>
            <h1 className="text-xl font-semibold">{t('confirm_withdrawal')}</h1>
          </div>

          <motion.div
            initial={{ opacity: 0, y: 20 }}
            animate={{ opacity: 1, y: 0 }}
            className="space-y-6"
          >
            {/* Summary */}
            <Card className="p-6 space-y-4">
              <div className="flex justify-between items-center">
                <span className="text-muted-foreground">Montant</span>
                <span className="text-xl font-bold">{formatCurrency(amount!, currency)}</span>
              </div>
              <div className="flex justify-between items-center">
                <span className="text-muted-foreground">Méthode</span>
                <span className="font-medium">
                  {method === 'mobile_money' ? 'Mobile Money' : 'Compte bancaire'}
                </span>
              </div>
              {method === 'mobile_money' ? (
                <div className="flex justify-between items-center">
                  <span className="text-muted-foreground">Numéro</span>
                  <span className="font-medium">{phoneNumber}</span>
                </div>
              ) : (
                <div className="flex justify-between items-center">
                  <span className="text-muted-foreground">Compte</span>
                  <span className="font-medium">{bankAccount}</span>
                </div>
              )}
              <div className="flex justify-between items-center pt-2 border-t">
                <span className="text-muted-foreground">Frais</span>
                <span>{formatCurrency(withdrawalFee, currency)}</span>
              </div>
              <div className="flex justify-between items-center font-semibold">
                <span>Total</span>
                <span>{formatCurrency(amount! + withdrawalFee, currency)}</span>
              </div>
            </Card>

            {/* PIN Input */}
            <div className="space-y-4">
              <Label>{t('enter_pin')}</Label>
              <OTPInput
                onComplete={(otp) => setPin(otp)}
                length={4}
              />
            </div>

            {error && (
              <Alert variant="destructive">
                <AlertCircle className="h-4 w-4" />
                <AlertDescription>{error}</AlertDescription>
              </Alert>
            )}

            <Button
              onClick={handleConfirm}
              disabled={loading || pin.length !== 4}
              className="w-full h-12 text-base"
            >
              {loading ? t('processing') : t('confirm_withdrawal')}
            </Button>
          </motion.div>
        </div>
      </div>
    );
  }

  return (
    <div className="min-h-screen bg-background">
      <div className="max-w-lg mx-auto px-4 py-6 safe-area-top">
        {/* Header */}
        <div className="flex items-center gap-4 mb-8">
          <Button
            variant="ghost"
            size="icon"
            onClick={onBack}
            className="touch-target"
          >
            <ChevronLeft className="w-6 h-6" />
          </Button>
          <div className="flex items-center gap-2">
            <ArrowDownToLine className="w-6 h-6 text-primary" />
            <h1 className="text-xl font-semibold">{t('withdraw')}</h1>
          </div>
        </div>

        <motion.div
          initial={{ opacity: 0, y: 20 }}
          animate={{ opacity: 1, y: 0 }}
          className="space-y-6"
        >
          {/* Balance Display */}
          <Card className="p-6">
            <div className="text-sm text-muted-foreground mb-2">Solde disponible</div>
            <div className="text-3xl font-bold">{formatCurrency(balance, currency)}</div>
          </Card>

          {/* Method Selection */}
          <Tabs value={method} onValueChange={(v) => setMethod(v as 'mobile_money' | 'bank')}>
            <TabsList className="grid w-full grid-cols-2">
              <TabsTrigger value="mobile_money">Mobile Money</TabsTrigger>
              <TabsTrigger value="bank">Compte bancaire</TabsTrigger>
            </TabsList>

            <TabsContent value="mobile_money" className="space-y-4 mt-6">
              {/* Amount */}
              <div className="space-y-2">
                <Label>Montant à retirer</Label>
                <CurrencyInput
                  value={amount || 0}
                  onChange={handleAmountChange}
                  currency={currency}
                  placeholder="0"
                  className="text-xl h-14"
                />
                <p className="text-xs text-muted-foreground">
                  Min: {formatCurrency(minWithdraw, currency)} • Max: {formatCurrency(maxWithdraw, currency)}
                </p>
              </div>

              {/* Operator Selection */}
              <div className="space-y-3">
                <Label>Opérateur Mobile Money</Label>
                <div className="grid grid-cols-2 gap-3">
                  <button
                    type="button"
                    onClick={() => setOperator('mtn')}
                    className={cn(
                      "p-4 rounded-lg border-2 transition-all touch-target",
                      operator === 'mtn'
                        ? "border-primary bg-primary/5"
                        : "border-border hover:border-primary/50"
                    )}
                  >
                    <div className="font-semibold text-yellow-500">MTN MoMo</div>
                  </button>
                  <button
                    type="button"
                    onClick={() => setOperator('orange')}
                    className={cn(
                      "p-4 rounded-lg border-2 transition-all touch-target",
                      operator === 'orange'
                        ? "border-primary bg-primary/5"
                        : "border-border hover:border-primary/50"
                    )}
                  >
                    <div className="font-semibold text-orange-500">Orange Money</div>
                  </button>
                </div>
              </div>

              {/* Phone Number */}
              <div className="space-y-2">
                <Label htmlFor="phone">Numéro de téléphone</Label>
                <Input
                  id="phone"
                  type="tel"
                  placeholder="6XXXXXXXX"
                  value={phoneNumber}
                  onChange={(e) => setPhoneNumber(e.target.value)}
                  className="h-12"
                />
              </div>
            </TabsContent>

            <TabsContent value="bank" className="space-y-4 mt-6">
              {/* Amount */}
              <div className="space-y-2">
                <Label>Montant à retirer</Label>
                <CurrencyInput
                  value={amount || 0}
                  onChange={handleAmountChange}
                  currency={currency}
                  placeholder="0"
                  className="text-xl h-14"
                />
                <p className="text-xs text-muted-foreground">
                  Min: {formatCurrency(minWithdraw, currency)} • Max: {formatCurrency(maxWithdraw, currency)}
                </p>
              </div>

              {/* Bank Code */}
              <div className="space-y-2">
                <Label htmlFor="bank-code">Code bancaire</Label>
                <Input
                  id="bank-code"
                  type="text"
                  placeholder="10001"
                  value={bankCode}
                  onChange={(e) => setBankCode(e.target.value)}
                  className="h-12"
                />
              </div>

              {/* Account Number */}
              <div className="space-y-2">
                <Label htmlFor="account">Numéro de compte</Label>
                <Input
                  id="account"
                  type="text"
                  placeholder="XXXXXXXXXXXXXXXX"
                  value={bankAccount}
                  onChange={(e) => setBankAccount(e.target.value)}
                  className="h-12"
                />
              </div>

              {/* Account Name */}
              <div className="space-y-2">
                <Label htmlFor="account-name">Nom du titulaire</Label>
                <Input
                  id="account-name"
                  type="text"
                  placeholder="Jean Dupont"
                  value={accountName}
                  onChange={(e) => setAccountName(e.target.value)}
                  className="h-12"
                />
              </div>
            </TabsContent>
          </Tabs>

          {error && (
            <Alert variant="destructive">
              <AlertCircle className="h-4 w-4" />
              <AlertDescription>{error}</AlertDescription>
            </Alert>
          )}

          {/* Info */}
          <Alert>
            <AlertCircle className="h-4 w-4" />
            <AlertDescription>
              Les retraits sont généralement traités en 24-48 heures. Des frais peuvent s'appliquer selon le montant et la méthode.
            </AlertDescription>
          </Alert>

          {/* Submit Button */}
          <Button
            onClick={handleSubmit}
            disabled={!amount}
            className="w-full h-12 text-base"
          >
            Continuer
          </Button>
        </motion.div>
      </div>
    </div>
  );
}

export default Withdraw;
