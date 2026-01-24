import React, { useState } from 'react';
import { motion, AnimatePresence } from 'framer-motion';
import { useTranslation } from 'react-i18next';
import { ChevronLeft, Lock, CheckCircle2 } from 'lucide-react';
import { SecureNumberPad } from '@/components/ui/secure-number-pad';
import { changePin, verifyCurrentPin } from '@/api/user.api';
import { useToast } from '@/hooks/use-toast';

type Step = 'current' | 'new' | 'confirm' | 'success';

interface ChangePinProps {
  walletNumber: string;
  onComplete: () => void;
  onBack: () => void;
}

export function ChangePin({ walletNumber, onComplete, onBack }: ChangePinProps) {
  const { t } = useTranslation();
  const { toast } = useToast();
  const [step, setStep] = useState<Step>('current');
  const [currentPin, setCurrentPin] = useState('');
  const [newPin, setNewPin] = useState('');
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');

  const handleCurrentPinComplete = async (pin: string) => {
    setLoading(true);
    setError('');

    try {
      const response = await verifyCurrentPin(walletNumber, { oldPin: pin });
      if (!response.data?.success) {
        throw new Error(response.data?.message || t('error_invalid_pin'));
      }
      setCurrentPin(pin);
      setStep('new');
    } catch (err: any) {
      setError(err.message || t('error_invalid_pin'));
    } finally {
      setLoading(false);
    }
  };

  const handleNewPinComplete = (pin: string) => {
    if (pin === currentPin) {
      setError(t('error_pin_same_as_current'));
      return;
    }
    setNewPin(pin);
    setError('');
    setStep('confirm');
  };

  const handleConfirmPinComplete = async (pin: string) => {
    if (pin !== newPin) {
      setError(t('error_pin_mismatch'));
      return;
    }

    setLoading(true);
    setError('');

    try {
      const response = await changePin(walletNumber, {
        walletNumber,
        oldPin: currentPin,
        newPin,
        newPinConfirmation: pin,
      });
      if (!response.data?.success) {
        throw new Error(response.data?.message || t('error_server'));
      }
      setStep('success');
      setTimeout(() => {
        toast({
          title: t('success'),
          description: t('pin_changed_success'),
        });
        onComplete();
      }, 2000);
    } catch (err: any) {
      setError(err.message || t('error_server'));
    } finally {
      setLoading(false);
    }
  };

  const goBack = () => {
    setError('');
    if (step === 'new') setStep('current');
    else if (step === 'confirm') setStep('new');
    else onBack();
  };

  const getStepInfo = () => {
    switch (step) {
      case 'current':
        return {
          title: t('enter_current_pin'),
          subtitle: t('enter_current_pin_subtitle'),
          onComplete: handleCurrentPinComplete,
        };
      case 'new':
        return {
          title: t('enter_new_pin'),
          subtitle: t('enter_new_pin_subtitle'),
          onComplete: handleNewPinComplete,
        };
      case 'confirm':
        return {
          title: t('confirm_new_pin'),
          subtitle: t('confirm_new_pin_subtitle'),
          onComplete: handleConfirmPinComplete,
        };
      default:
        return { title: '', subtitle: '', onComplete: () => {} };
    }
  };

  const stepInfo = getStepInfo();

  return (
    <div className="min-h-screen bg-background flex flex-col">
      {/* Header */}
      <header className="safe-area-top px-4 py-4 flex items-center">
        {step !== 'success' && (
          <button
            onClick={goBack}
            className="w-10 h-10 rounded-full bg-secondary flex items-center justify-center touch-target"
            disabled={loading}
          >
            <ChevronLeft className="w-5 h-5" />
          </button>
        )}
      </header>

      <div className="flex-1 flex flex-col">
        <AnimatePresence mode="wait">
          {step !== 'success' ? (
            <motion.div
              key={step}
              initial={{ opacity: 0, x: 20 }}
              animate={{ opacity: 1, x: 0 }}
              exit={{ opacity: 0, x: -20 }}
              className="flex-1 flex flex-col px-6 pt-8"
            >
              <div className="mb-8">
                <div className="w-16 h-16 rounded-2xl bg-primary/10 flex items-center justify-center mb-6">
                  <Lock className="w-8 h-8 text-primary" />
                </div>
                <h1 className="text-2xl font-bold text-foreground font-display mb-2">
                  {stepInfo.title}
                </h1>
                <p className="text-muted-foreground">{stepInfo.subtitle}</p>
              </div>

              <div className="flex-1 flex flex-col items-center justify-center">
                <SecureNumberPad
                  length={6}
                  onComplete={stepInfo.onComplete}
                  loading={loading}
                  error={error}
                />
              </div>
            </motion.div>
          ) : (
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
                <CheckCircle2 className="w-12 h-12 text-green-600 dark:text-green-500" />
              </motion.div>
              <h1 className="text-2xl font-bold text-foreground font-display mb-2 text-center">
                {t('pin_changed')}
              </h1>
              <p className="text-muted-foreground text-center">
                {t('pin_changed_subtitle')}
              </p>
            </motion.div>
          )}
        </AnimatePresence>
      </div>
    </div>
  );
}

export default ChangePin;
