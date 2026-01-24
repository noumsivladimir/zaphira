import React, { useState } from 'react';
import { motion } from 'framer-motion';
import { useTranslation } from 'react-i18next';
import { ChevronLeft, ArrowUpFromLine, AlertCircle, CreditCard } from 'lucide-react';
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

interface TopUpProps {
  balance: number;
  currency?: string;
  onBack: () => void;
  onTopUp: (data: TopUpData) => void;
}

export interface TopUpData {
  method: 'mobile_money' | 'card' | 'bank_transfer';
  amount: number;
  phoneNumber?: string;
  operator?: string;
  cardNumber?: string;
  cardExpiry?: string;
  cardCvv?: string;
  cardName?: string;
  reference?: string;
}

export function TopUp({
  balance,
  currency = 'XAF',
  onBack,
  onTopUp,
}: TopUpProps) {
  const { t } = useTranslation();
  const [method, setMethod] = useState<'mobile_money' | 'card' | 'bank_transfer'>('mobile_money');
  const [amount, setAmount] = useState<number | undefined>();
  const [phoneNumber, setPhoneNumber] = useState('');
  const [operator, setOperator] = useState<'mtn' | 'orange' | ''>('');
  const [cardNumber, setCardNumber] = useState('');
  const [cardExpiry, setCardExpiry] = useState('');
  const [cardCvv, setCardCvv] = useState('');
  const [cardName, setCardName] = useState('');
  const [showConfirmStep, setShowConfirmStep] = useState(false);
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(false);

  const topUpFee = 0; // Fee calculation based on amount
  const minTopUp = 500;
  const maxTopUp = 5000000;

  // Quick amounts
  const quickAmounts = [5000, 10000, 25000, 50000, 100000];

  const handleAmountChange = (value: number | undefined) => {
    setAmount(value);
    setError('');
  };

  const handleQuickAmount = (value: number) => {
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

    if (amount < minTopUp) {
      setError(`Le montant minimum est ${formatCurrency(minTopUp, currency)}`);
      return;
    }

    if (amount > maxTopUp) {
      setError(`Le montant maximum est ${formatCurrency(maxTopUp, currency)}`);
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
    } else if (method === 'card') {
      if (!cardNumber || !cardExpiry || !cardCvv || !cardName) {
        setError('Veuillez remplir toutes les informations de la carte');
        return;
      }
    }

    setShowConfirmStep(true);
  };

  const handleConfirm = async () => {
    setLoading(true);
    setError('');

    try {
      const data: TopUpData = {
        method,
        amount: amount!,
      };

      if (method === 'mobile_money') {
        data.phoneNumber = phoneNumber;
        data.operator = operator;
      } else if (method === 'card') {
        data.cardNumber = cardNumber;
        data.cardExpiry = cardExpiry;
        data.cardCvv = cardCvv;
        data.cardName = cardName;
      }

      await onTopUp(data);
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Échec de la recharge');
      setLoading(false);
    }
  };

  const handleCancel = () => {
    setShowConfirmStep(false);
    setError('');
  };

  if (showConfirmStep) {
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
            <h1 className="text-xl font-semibold">{t('confirm_topup')}</h1>
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
                  {method === 'mobile_money' && 'Mobile Money'}
                  {method === 'card' && 'Carte bancaire'}
                  {method === 'bank_transfer' && 'Virement bancaire'}
                </span>
              </div>
              {method === 'mobile_money' && (
                <div className="flex justify-between items-center">
                  <span className="text-muted-foreground">Numéro</span>
                  <span className="font-medium">{phoneNumber}</span>
                </div>
              )}
              {method === 'card' && (
                <div className="flex justify-between items-center">
                  <span className="text-muted-foreground">Carte</span>
                  <span className="font-medium">**** **** **** {cardNumber.slice(-4)}</span>
                </div>
              )}
              <div className="flex justify-between items-center pt-2 border-t">
                <span className="text-muted-foreground">Frais</span>
                <span>{formatCurrency(topUpFee, currency)}</span>
              </div>
              <div className="flex justify-between items-center font-semibold">
                <span>Total</span>
                <span>{formatCurrency(amount! + topUpFee, currency)}</span>
              </div>
            </Card>

            {/* Instructions for Mobile Money */}
            {method === 'mobile_money' && (
              <Alert>
                <AlertCircle className="h-4 w-4" />
                <AlertDescription>
                  Vous recevrez une notification sur votre téléphone ({phoneNumber}) pour confirmer le paiement. Entrez votre code PIN {operator === 'mtn' ? 'MTN' : 'Orange'} Money pour valider.
                </AlertDescription>
              </Alert>
            )}

            {/* Instructions for Bank Transfer */}
            {method === 'bank_transfer' && (
              <Alert>
                <AlertCircle className="h-4 w-4" />
                <AlertDescription>
                  Effectuez un virement vers le compte fourni ci-dessous. Votre portefeuille sera crédité après confirmation du paiement.
                </AlertDescription>
              </Alert>
            )}

            {error && (
              <Alert variant="destructive">
                <AlertCircle className="h-4 w-4" />
                <AlertDescription>{error}</AlertDescription>
              </Alert>
            )}

            <Button
              onClick={handleConfirm}
              disabled={loading}
              className="w-full h-12 text-base"
            >
              {loading ? t('processing') : t('confirm_topup')}
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
            <ArrowUpFromLine className="w-6 h-6 text-primary" />
            <h1 className="text-xl font-semibold">{t('topup')}</h1>
          </div>
        </div>

        <motion.div
          initial={{ opacity: 0, y: 20 }}
          animate={{ opacity: 1, y: 0 }}
          className="space-y-6"
        >
          {/* Balance Display */}
          <Card className="p-6">
            <div className="text-sm text-muted-foreground mb-2">Solde actuel</div>
            <div className="text-3xl font-bold">{formatCurrency(balance, currency)}</div>
          </Card>

          {/* Method Selection */}
          <Tabs value={method} onValueChange={(v) => setMethod(v as 'mobile_money' | 'card' | 'bank_transfer')}>
            <TabsList className="grid w-full grid-cols-3">
              <TabsTrigger value="mobile_money">Mobile</TabsTrigger>
              <TabsTrigger value="card">Carte</TabsTrigger>
              <TabsTrigger value="bank_transfer">Virement</TabsTrigger>
            </TabsList>

            <TabsContent value="mobile_money" className="space-y-4 mt-6">
              {/* Amount */}
              <div className="space-y-2">
                <Label>Montant à recharger</Label>
                <CurrencyInput
                  value={amount || 0}
                  onChange={handleAmountChange}
                  currency={currency}
                  placeholder="0"
                  className="text-xl h-14"
                />
                <p className="text-xs text-muted-foreground">
                  Min: {formatCurrency(minTopUp, currency)} • Max: {formatCurrency(maxTopUp, currency)}
                </p>
              </div>

              {/* Quick amounts */}
              <div className="space-y-2">
                <Label>Montants rapides</Label>
                <div className="grid grid-cols-3 gap-2">
                  {quickAmounts.map((value) => (
                    <button
                      key={value}
                      type="button"
                      onClick={() => handleQuickAmount(value)}
                      className={cn(
                        "py-3 px-2 rounded-lg border-2 transition-all text-sm font-medium touch-target",
                        amount === value
                          ? "border-primary bg-primary/5 text-primary"
                          : "border-border hover:border-primary/50"
                      )}
                    >
                      {formatCurrency(value, currency)}
                    </button>
                  ))}
                </div>
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

            <TabsContent value="card" className="space-y-4 mt-6">
              {/* Amount */}
              <div className="space-y-2">
                <Label>Montant à recharger</Label>
                <CurrencyInput
                  value={amount || 0}
                  onChange={handleAmountChange}
                  currency={currency}
                  placeholder="0"
                  className="text-xl h-14"
                />
                <p className="text-xs text-muted-foreground">
                  Min: {formatCurrency(minTopUp, currency)} • Max: {formatCurrency(maxTopUp, currency)}
                </p>
              </div>

              {/* Card Number */}
              <div className="space-y-2">
                <Label htmlFor="card-number">Numéro de carte</Label>
                <div className="relative">
                  <Input
                    id="card-number"
                    type="text"
                    placeholder="1234 5678 9012 3456"
                    value={cardNumber}
                    onChange={(e) => setCardNumber(e.target.value)}
                    maxLength={19}
                    className="h-12 pl-10"
                  />
                  <CreditCard className="absolute left-3 top-3.5 w-5 h-5 text-muted-foreground" />
                </div>
              </div>

              {/* Expiry and CVV */}
              <div className="grid grid-cols-2 gap-4">
                <div className="space-y-2">
                  <Label htmlFor="card-expiry">Expiration</Label>
                  <Input
                    id="card-expiry"
                    type="text"
                    placeholder="MM/AA"
                    value={cardExpiry}
                    onChange={(e) => setCardExpiry(e.target.value)}
                    maxLength={5}
                    className="h-12"
                  />
                </div>
                <div className="space-y-2">
                  <Label htmlFor="card-cvv">CVV</Label>
                  <Input
                    id="card-cvv"
                    type="text"
                    placeholder="123"
                    value={cardCvv}
                    onChange={(e) => setCardCvv(e.target.value)}
                    maxLength={3}
                    className="h-12"
                  />
                </div>
              </div>

              {/* Card Name */}
              <div className="space-y-2">
                <Label htmlFor="card-name">Nom sur la carte</Label>
                <Input
                  id="card-name"
                  type="text"
                  placeholder="JEAN DUPONT"
                  value={cardName}
                  onChange={(e) => setCardName(e.target.value.toUpperCase())}
                  className="h-12"
                />
              </div>
            </TabsContent>

            <TabsContent value="bank_transfer" className="space-y-4 mt-6">
              {/* Amount */}
              <div className="space-y-2">
                <Label>Montant à recharger</Label>
                <CurrencyInput
                  value={amount || 0}
                  onChange={handleAmountChange}
                  currency={currency}
                  placeholder="0"
                  className="text-xl h-14"
                />
              </div>

              {/* Bank Transfer Info */}
              <Card className="p-6 space-y-3">
                <h3 className="font-semibold">Informations de virement</h3>
                <div className="space-y-2 text-sm">
                  <div className="flex justify-between">
                    <span className="text-muted-foreground">Banque:</span>
                    <span className="font-medium">Afriland First Bank</span>
                  </div>
                  <div className="flex justify-between">
                    <span className="text-muted-foreground">Titulaire:</span>
                    <span className="font-medium">Zaphira SA</span>
                  </div>
                  <div className="flex justify-between">
                    <span className="text-muted-foreground">Compte:</span>
                    <span className="font-medium">10001234567890</span>
                  </div>
                  <div className="flex justify-between">
                    <span className="text-muted-foreground">Code SWIFT:</span>
                    <span className="font-medium">CCBACMCX</span>
                  </div>
                </div>
              </Card>

              <Alert>
                <AlertCircle className="h-4 w-4" />
                <AlertDescription>
                  Après votre virement, envoyez le justificatif par email à support@zaphira.app ou via WhatsApp au +237 6XX XX XX XX avec votre numéro de portefeuille.
                </AlertDescription>
              </Alert>
            </TabsContent>
          </Tabs>

          {error && (
            <Alert variant="destructive">
              <AlertCircle className="h-4 w-4" />
              <AlertDescription>{error}</AlertDescription>
            </Alert>
          )}

          {/* Submit Button */}
          <Button
            onClick={handleSubmit}
            disabled={!amount || method === 'bank_transfer'}
            className="w-full h-12 text-base"
          >
            {method === 'bank_transfer' ? 'Instructions envoyées par email' : 'Continuer'}
          </Button>
        </motion.div>
      </div>
    </div>
  );
}

export default TopUp;
