import React, { useState, useEffect } from 'react';
import { motion, AnimatePresence } from 'framer-motion';
import { useTranslation } from 'react-i18next';
import { ChevronLeft, Phone, Shield, Lock, CheckCircle2 } from 'lucide-react';
import { Button } from '@/components/ui/button';
import { Input } from '@/components/ui/input';
import { SecureNumberPad } from '@/components/ui/secure-number-pad';
import { OTPInput } from '@/components/ui/otp-input';
import { cn } from '@/lib/utils';

interface ForgotPinProps {
  onComplete: () => void;
  onBackToLogin: () => void;
}

type Step = 'phone' | 'otp' | 'security' | 'new-pin' | 'success';

interface SecurityQuestion {
  id: string;
  question: string;
}

export function ForgotPin({ onComplete, onBackToLogin }: ForgotPinProps) {
  const { t } = useTranslation();
  const [step, setStep] = useState<Step>('phone');
  const [phoneNumber, setPhoneNumber] = useState('');
  const [otp, setOtp] = useState('');
  const [tempToken, setTempToken] = useState('');
  const [resetToken, setResetToken] = useState('');
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');
  const [resendTimer, setResendTimer] = useState(0);

  // Mock security questions - should come from API
  const [securityQuestions] = useState<SecurityQuestion[]>([
    { id: '1', question: t('security_q_birth_city') },
    { id: '2', question: t('security_q_pet_name') },
  ]);
  const [securityAnswers, setSecurityAnswers] = useState<Record<string, string>>({});

  useEffect(() => {
    if (resendTimer > 0) {
      const timer = setTimeout(() => setResendTimer(resendTimer - 1), 1000);
      return () => clearTimeout(timer);
    }
  }, [resendTimer]);

  const validatePhone = (value: string): boolean => {
    const cleaned = value.replace(/\D/g, '');
    return cleaned.length >= 9;
  };

  const handlePhoneSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!validatePhone(phoneNumber)) {
      setError(t('error_invalid_phone'));
      return;
    }

    setLoading(true);
    setError('');

    try {
      // API call to request PIN reset
      // await requestPinReset({ phoneNumber });
      setResendTimer(60);
      setStep('otp');
    } catch (err: unknown) {
      setError(err instanceof Error ? err.message : t('error_server'));
    } finally {
      setLoading(false);
    }
  };

  const handleOtpComplete = async (otpValue: string) => {
    setLoading(true);
    setError('');

    try {
      // API call to verify OTP
      // const { tempToken } = await verifyPinResetOtp({ phoneNumber, otp: otpValue });
      setTempToken('mock-temp-token');
      setStep('security');
    } catch (err: unknown) {
      setError(err instanceof Error ? err.message : t('error_invalid_otp'));
    } finally {
      setLoading(false);
    }
  };

  const handleResendOtp = async () => {
    if (resendTimer > 0) return;

    setLoading(true);
    setError('');

    try {
      // await requestPinReset({ phoneNumber });
      setResendTimer(60);
    } catch (err: unknown) {
      setError(err instanceof Error ? err.message : t('error_server'));
    } finally {
      setLoading(false);
    }
  };

  const handleSecuritySubmit = async (e: React.FormEvent) => {
    e.preventDefault();

    // Validate all questions are answered
    const allAnswered = securityQuestions.every((q) => securityAnswers[q.id]?.trim());
    if (!allAnswered) {
      setError(t('error_answer_all_questions'));
      return;
    }

    setLoading(true);
    setError('');

    try {
      // API call to verify security questions
      // const { resetToken } = await answerSecurityQuestions({
      //   tempToken,
      //   answers: Object.entries(securityAnswers).map(([questionId, answer]) => ({
      //     questionId,
      //     answer,
      //   })),
      // });
      setResetToken('mock-reset-token');
      setStep('new-pin');
    } catch (err: unknown) {
      setError(err instanceof Error ? err.message : t('error_incorrect_answers'));
    } finally {
      setLoading(false);
    }
  };

  const handleNewPinComplete = async (newPin: string) => {
    setLoading(true);
    setError('');

    try {
      // API call to set new PIN
      // await setNewPin({ tempToken: resetToken, newPin });
      setStep('success');
      setTimeout(() => onComplete(), 2000);
    } catch (err: unknown) {
      setError(err instanceof Error ? err.message : t('error_server'));
    } finally {
      setLoading(false);
    }
  };

  const goBack = () => {
    setError('');
    if (step === 'otp') setStep('phone');
    else if (step === 'security') setStep('otp');
    else if (step === 'new-pin') setStep('security');
    else onBackToLogin();
  };

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
          {/* Phone Number Step */}
          {step === 'phone' && (
            <motion.div
              key="phone"
              initial={{ opacity: 0, x: 20 }}
              animate={{ opacity: 1, x: 0 }}
              exit={{ opacity: 0, x: -20 }}
              className="flex-1 flex flex-col px-6 pt-8"
            >
              <div className="mb-12">
                <div className="w-16 h-16 rounded-2xl bg-primary/10 flex items-center justify-center mb-6">
                  <Lock className="w-8 h-8 text-primary" />
                </div>
                <h1 className="text-2xl font-bold text-foreground font-display mb-2">
                  {t('forgot_pin_title')}
                </h1>
                <p className="text-muted-foreground">{t('forgot_pin_subtitle')}</p>
              </div>

              <form onSubmit={handlePhoneSubmit} className="flex-1 flex flex-col">
                <div className="mb-6">
                  <label className="block text-sm font-medium text-foreground mb-2">
                    {t('phone_number')}
                  </label>
                  <div className="relative">
                    <Phone className="absolute left-3 top-1/2 -translate-y-1/2 w-5 h-5 text-muted-foreground" />
                    <Input
                      type="tel"
                      value={phoneNumber}
                      onChange={(e) => setPhoneNumber(e.target.value)}
                      placeholder={t('phone_placeholder')}
                      className="pl-11"
                      autoFocus
                    />
                  </div>
                </div>

                {error && (
                  <motion.p
                    initial={{ opacity: 0, y: -10 }}
                    animate={{ opacity: 1, y: 0 }}
                    className="text-sm text-destructive mb-4"
                  >
                    {error}
                  </motion.p>
                )}

                <div className="mt-auto">
                  <Button
                    type="submit"
                    size="lg"
                    className="w-full"
                    disabled={!phoneNumber || loading}
                  >
                    {loading ? t('loading') : t('send_otp')}
                  </Button>
                </div>
              </form>
            </motion.div>
          )}

          {/* OTP Step */}
          {step === 'otp' && (
            <motion.div
              key="otp"
              initial={{ opacity: 0, x: 20 }}
              animate={{ opacity: 1, x: 0 }}
              exit={{ opacity: 0, x: -20 }}
              className="flex-1 flex flex-col px-6 pt-8"
            >
              <div className="mb-12">
                <div className="w-16 h-16 rounded-2xl bg-primary/10 flex items-center justify-center mb-6">
                  <Shield className="w-8 h-8 text-primary" />
                </div>
                <h1 className="text-2xl font-bold text-foreground font-display mb-2">
                  {t('verify_otp')}
                </h1>
                <p className="text-muted-foreground">
                  {t('otp_sent')} {phoneNumber}
                </p>
              </div>

              <div className="mb-8">
                <OTPInput
                  length={6}
                  onComplete={handleOtpComplete}
                  loading={loading}
                />
              </div>

              {error && (
                <motion.p
                  initial={{ opacity: 0, y: -10 }}
                  animate={{ opacity: 1, y: 0 }}
                  className="text-sm text-destructive mb-4"
                >
                  {error}
                </motion.p>
              )}

              <div className="text-center">
                <button
                  onClick={handleResendOtp}
                  disabled={resendTimer > 0 || loading}
                  className={cn(
                    'text-sm font-medium touch-target',
                    resendTimer > 0 || loading
                      ? 'text-muted-foreground'
                      : 'text-primary'
                  )}
                >
                  {resendTimer > 0
                    ? `${t('resend_in')} ${resendTimer}s`
                    : t('resend_otp')}
                </button>
              </div>
            </motion.div>
          )}

          {/* Security Questions Step */}
          {step === 'security' && (
            <motion.div
              key="security"
              initial={{ opacity: 0, x: 20 }}
              animate={{ opacity: 1, x: 0 }}
              exit={{ opacity: 0, x: -20 }}
              className="flex-1 flex flex-col px-6 pt-8"
            >
              <div className="mb-8">
                <div className="w-16 h-16 rounded-2xl bg-primary/10 flex items-center justify-center mb-6">
                  <Shield className="w-8 h-8 text-primary" />
                </div>
                <h1 className="text-2xl font-bold text-foreground font-display mb-2">
                  {t('security_questions')}
                </h1>
                <p className="text-muted-foreground">{t('security_questions_subtitle')}</p>
              </div>

              <form onSubmit={handleSecuritySubmit} className="flex-1 flex flex-col">
                <div className="space-y-4 mb-6">
                  {securityQuestions.map((question) => (
                    <div key={question.id}>
                      <label className="block text-sm font-medium text-foreground mb-2">
                        {question.question}
                      </label>
                      <Input
                        type="text"
                        value={securityAnswers[question.id] || ''}
                        onChange={(e) =>
                          setSecurityAnswers({
                            ...securityAnswers,
                            [question.id]: e.target.value,
                          })
                        }
                        placeholder={t('enter_answer')}
                      />
                    </div>
                  ))}
                </div>

                {error && (
                  <motion.p
                    initial={{ opacity: 0, y: -10 }}
                    animate={{ opacity: 1, y: 0 }}
                    className="text-sm text-destructive mb-4"
                  >
                    {error}
                  </motion.p>
                )}

                <div className="mt-auto">
                  <Button
                    type="submit"
                    size="lg"
                    className="w-full"
                    disabled={loading}
                  >
                    {loading ? t('loading') : t('verify')}
                  </Button>
                </div>
              </form>
            </motion.div>
          )}

          {/* New PIN Step */}
          {step === 'new-pin' && (
            <motion.div
              key="new-pin"
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
                  {t('create_new_pin')}
                </h1>
                <p className="text-muted-foreground">{t('create_new_pin_subtitle')}</p>
              </div>

              <div className="flex-1 flex flex-col items-center justify-center">
                <SecureNumberPad
                  length={4}
                  onComplete={handleNewPinComplete}
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
                <CheckCircle2 className="w-12 h-12 text-green-600 dark:text-green-500" />
              </motion.div>
              <h1 className="text-2xl font-bold text-foreground font-display mb-2 text-center">
                {t('pin_reset_success')}
              </h1>
              <p className="text-muted-foreground text-center">
                {t('pin_reset_success_subtitle')}
              </p>
            </motion.div>
          )}
        </AnimatePresence>
      </div>
    </div>
  );
}

export default ForgotPin;
