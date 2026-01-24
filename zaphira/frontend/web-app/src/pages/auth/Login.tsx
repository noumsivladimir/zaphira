import React, { useState } from 'react';
import { motion, AnimatePresence } from 'framer-motion';
import { useTranslation } from 'react-i18next';
import { ChevronLeft, Phone } from 'lucide-react';
import { Button } from '@/components/ui/button';
import { Input } from '@/components/ui/input';
import { SecureNumberPad } from '@/components/ui/secure-number-pad';
import { cn } from '@/lib/utils';
import { login } from '@/api/auth.api';
import type { AuthResponse } from '@/api/auth.api';

interface LoginProps {
  onLoginSuccess: (response: AuthResponse, phone: string) => Promise<void> | void;
  onForgotPin: () => void;
  onRegister: () => void;
}

type Step = 'phone' | 'pin';

export function Login({ onLoginSuccess, onForgotPin, onRegister }: LoginProps) {
  const { t } = useTranslation();
  const [step, setStep] = useState<Step>('phone');
  const [phone, setPhone] = useState('');
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');

  const validatePhone = (value: string): boolean => {
    const cleaned = value.replace(/\D/g, '');
    return cleaned.length >= 9;
  };

  const handlePhoneSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    if (!validatePhone(phone)) {
      setError(t('error_invalid_phone'));
      return;
    }
    setError('');
    setStep('pin');
  };

  const handlePinComplete = async (pin: string) => {
    setLoading(true);
    setError('');
    
    try {
      const response = await login({ phoneNumber: phone, pin });
      await onLoginSuccess(response, phone);
    } catch (err: any) {
      setError(err.message || t('error_invalid_pin'));
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="min-h-screen bg-background flex flex-col">
      {/* Header */}
      <header className="safe-area-top px-4 py-4 flex items-center">
        <AnimatePresence mode="wait">
          {step === 'pin' && (
            <motion.button
              initial={{ opacity: 0, x: -10 }}
              animate={{ opacity: 1, x: 0 }}
              exit={{ opacity: 0, x: -10 }}
              onClick={() => setStep('phone')}
              className="w-10 h-10 rounded-full bg-secondary flex items-center justify-center touch-target"
            >
              <ChevronLeft className="w-5 h-5" />
            </motion.button>
          )}
        </AnimatePresence>
      </header>

      <div className="flex-1 flex flex-col">
        <AnimatePresence mode="wait">
          {step === 'phone' ? (
            <motion.div
              key="phone"
              initial={{ opacity: 0, x: 20 }}
              animate={{ opacity: 1, x: 0 }}
              exit={{ opacity: 0, x: -20 }}
              className="flex-1 flex flex-col px-6 pt-8"
            >
              {/* Logo and title */}
              <div className="mb-12">
                <motion.div
                  initial={{ scale: 0.8, opacity: 0 }}
                  animate={{ scale: 1, opacity: 1 }}
                  transition={{ delay: 0.1 }}
                  className="w-16 h-16 rounded-2xl bg-primary flex items-center justify-center mb-6 shadow-button"
                >
                  <span className="text-2xl font-bold text-white font-display">Z</span>
                </motion.div>
                <h1 className="text-2xl font-bold text-foreground font-display mb-2">
                  {t('welcome')}
                </h1>
                <p className="text-muted-foreground">{t('welcome_subtitle')}</p>
              </div>

              {/* Phone form */}
              <form onSubmit={handlePhoneSubmit} className="flex-1 flex flex-col">
                <div className="mb-6">
                  <label className="block text-sm font-medium text-foreground mb-2">
                    {t('phone_number')}
                  </label>
                  <div className="relative">
                    <Phone className="absolute left-4 top-1/2 -translate-y-1/2 w-5 h-5 text-muted-foreground" />
                    <Input
                      type="tel"
                      value={phone}
                      onChange={(e) => {
                        setPhone(e.target.value);
                        setError('');
                      }}
                      placeholder={t('phone_placeholder')}
                      className={cn(
                        'pl-12 h-14 text-lg rounded-xl',
                        error && 'border-destructive focus-visible:ring-destructive'
                      )}
                      autoFocus
                    />
                  </div>
                  {error && (
                    <motion.p
                      initial={{ opacity: 0, y: -10 }}
                      animate={{ opacity: 1, y: 0 }}
                      className="text-destructive text-sm mt-2"
                    >
                      {error}
                    </motion.p>
                  )}
                </div>

                <div className="mt-auto pb-8">
                  <Button
                    type="submit"
                    className="w-full h-14 text-lg rounded-xl shadow-button"
                    disabled={!phone}
                  >
                    {t('continue')}
                  </Button>

                  <p className="text-center mt-6 text-muted-foreground">
                    Pas encore de compte ?{' '}
                    <button
                      type="button"
                      onClick={onRegister}
                      className="text-primary font-medium hover:underline"
                    >
                      {t('register')}
                    </button>
                  </p>
                </div>
              </form>
            </motion.div>
          ) : (
            <motion.div
              key="pin"
              initial={{ opacity: 0, x: 20 }}
              animate={{ opacity: 1, x: 0 }}
              exit={{ opacity: 0, x: -20 }}
              className="flex-1 flex flex-col items-center justify-center px-6"
            >
              <SecureNumberPad
                length={6}
                title={t('enter_pin')}
                subtitle={phone}
                onComplete={handlePinComplete}
                error={error}
                loading={loading}
                onCancel={() => setStep('phone')}
              />

              <button
                onClick={onForgotPin}
                className="mt-8 text-primary text-sm font-medium hover:underline touch-target"
              >
                {t('forgot_pin')}
              </button>
            </motion.div>
          )}
        </AnimatePresence>
      </div>
    </div>
  );
}

export default Login;
