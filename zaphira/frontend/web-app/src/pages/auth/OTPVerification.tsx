import React, { useState, useEffect } from 'react';
import { motion } from 'framer-motion';
import { useTranslation } from 'react-i18next';
import { ChevronLeft } from 'lucide-react';
import { Button } from '@/components/ui/button';
import { OTPInput } from '@/components/ui/otp-input';

interface OTPVerificationProps {
  phoneNumber: string;
  onVerify: (otp: string) => Promise<void>;
  onResend: () => Promise<void>;
  onBack: () => void;
}

export function OTPVerification({
  phoneNumber,
  onVerify,
  onResend,
  onBack,
}: OTPVerificationProps) {
  const { t } = useTranslation();
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');
  const [countdown, setCountdown] = useState(60);
  const [canResend, setCanResend] = useState(false);
  const [otp, setOtp] = useState('');

  useEffect(() => {
    if (countdown > 0) {
      const timer = setTimeout(() => setCountdown(countdown - 1), 1000);
      return () => clearTimeout(timer);
    } else {
      setCanResend(true);
    }
  }, [countdown]);

  const handleComplete = async (value: string) => {
    setOtp(value);
    setLoading(true);
    setError('');

    try {
      await onVerify(value);
    } catch (err: any) {
      setError(err.message || 'Code invalide');
    } finally {
      setLoading(false);
    }
  };

  const handleSubmit = async () => {
    if (otp.length !== 6 || loading) return;

    setLoading(true);
    setError('');

    try {
      await onVerify(otp);
    } catch (err: any) {
      setError(err.message || 'Code invalide');
    } finally {
      setLoading(false);
    }
  };

  const handleResend = async () => {
    if (!canResend) return;
    
    setCanResend(false);
    setCountdown(60);
    setError('');

    try {
      await onResend();
    } catch (err) {
      setError('Impossible de renvoyer le code');
    }
  };

  return (
    <div className="min-h-screen bg-background flex flex-col">
      {/* Header */}
      <header className="safe-area-top px-4 py-4 flex items-center">
        <button
          onClick={onBack}
          className="w-10 h-10 rounded-full bg-secondary flex items-center justify-center touch-target"
        >
          <ChevronLeft className="w-5 h-5" />
        </button>
      </header>

      <div className="flex-1 flex flex-col items-center px-6 pt-12">
        {/* Icon */}
        <motion.div
          initial={{ scale: 0.8, opacity: 0 }}
          animate={{ scale: 1, opacity: 1 }}
          className="w-20 h-20 rounded-3xl bg-primary/10 flex items-center justify-center mb-8"
        >
          <span className="text-4xl">📱</span>
        </motion.div>

        {/* Title */}
        <motion.h1
          initial={{ opacity: 0, y: 20 }}
          animate={{ opacity: 1, y: 0 }}
          transition={{ delay: 0.1 }}
          className="text-2xl font-bold font-display text-center mb-2"
        >
          {t('verify_otp')}
        </motion.h1>

        <motion.p
          initial={{ opacity: 0, y: 20 }}
          animate={{ opacity: 1, y: 0 }}
          transition={{ delay: 0.2 }}
          className="text-muted-foreground text-center mb-8"
        >
          {t('otp_sent')}
          <br />
          <span className="font-medium text-foreground">{phoneNumber}</span>
        </motion.p>

        {/* OTP Input */}
        <motion.div
          initial={{ opacity: 0, y: 20 }}
          animate={{ opacity: 1, y: 0 }}
          transition={{ delay: 0.3 }}
          className="mb-8"
        >
          <OTPInput
            length={6}
            onComplete={handleComplete}
            onChange={(value) => {
              setOtp(value);
              if (error) {
                setError('');
              }
            }}
            error={error}
            loading={loading}
            autoFocus
          />
        </motion.div>

        <Button
          onClick={handleSubmit}
          disabled={otp.length !== 6 || loading}
          className="w-full max-w-xs h-12 rounded-xl shadow-button mb-6"
        >
          Vérifier
        </Button>

        {/* Resend */}
        <motion.div
          initial={{ opacity: 0 }}
          animate={{ opacity: 1 }}
          transition={{ delay: 0.4 }}
          className="text-center"
        >
          {canResend ? (
            <Button
              variant="ghost"
              onClick={handleResend}
              className="text-primary"
            >
              {t('resend_otp')}
            </Button>
          ) : (
            <p className="text-muted-foreground">
              {t('resend_in')}{' '}
              <span className="font-semibold text-foreground">{countdown}s</span>
            </p>
          )}
        </motion.div>

        {/* Loading overlay */}
        {loading && (
          <motion.div
            initial={{ opacity: 0 }}
            animate={{ opacity: 1 }}
            className="absolute inset-0 bg-background/80 backdrop-blur-sm flex items-center justify-center"
          >
            <div className="w-12 h-12 border-4 border-primary border-t-transparent rounded-full animate-spin" />
          </motion.div>
        )}
      </div>
    </div>
  );
}

export default OTPVerification;
