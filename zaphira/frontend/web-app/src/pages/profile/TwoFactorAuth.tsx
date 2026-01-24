import React, { useState } from 'react';
import { motion } from 'framer-motion';
import { useTranslation } from 'react-i18next';
import { ChevronLeft, Shield, Smartphone, Mail, Key, CheckCircle2, XCircle } from 'lucide-react';
import { Button } from '@/components/ui/button';
import { Switch } from '@/components/ui/switch';
import { Label } from '@/components/ui/label';
import { Input } from '@/components/ui/input';
import { OTPInput } from '@/components/ui/otp-input';
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card';
import { Separator } from '@/components/ui/separator';
import { Alert, AlertDescription, AlertTitle } from '@/components/ui/alert';
import { SecureNumberPad } from '@/components/ui/secure-number-pad';
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
import { cn } from '@/lib/utils';

interface TwoFactorAuthProps {
  user: {
    phoneNumber: string;
    email?: string;
  };
  twoFactorEnabled?: boolean;
  onBack: () => void;
  onEnableTwoFactor: (method: '2fa-sms' | '2fa-email', pin: string) => Promise<void>;
  onDisableTwoFactor: (pin: string) => Promise<void>;
  onVerifyOTP: (otp: string) => Promise<boolean>;
}

type TwoFactorMethod = '2fa-sms' | '2fa-email';
type Step = 'main' | 'verify-pin' | 'verify-otp' | 'success';

export function TwoFactorAuth({
  user,
  twoFactorEnabled = false,
  onBack,
  onEnableTwoFactor,
  onDisableTwoFactor,
  onVerifyOTP,
}: TwoFactorAuthProps) {
  const { t } = useTranslation();
  const [isEnabled, setIsEnabled] = useState(twoFactorEnabled);
  const [selectedMethod, setSelectedMethod] = useState<TwoFactorMethod>('2fa-sms');
  const [currentStep, setCurrentStep] = useState<Step>('main');
  const [pin, setPin] = useState('');
  const [otp, setOtp] = useState('');
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');
  const [showDisableDialog, setShowDisableDialog] = useState(false);

  const handleToggle2FA = async (enabled: boolean) => {
    if (enabled) {
      setCurrentStep('verify-pin');
    } else {
      setShowDisableDialog(true);
    }
  };

  const handlePinComplete = async (enteredPin: string) => {
    setPin(enteredPin);
    setLoading(true);
    setError('');

    try {
      // Simuler l'envoi du code OTP
      await new Promise(resolve => setTimeout(resolve, 1000));
      setCurrentStep('verify-otp');
    } catch (err) {
      setError('PIN incorrect. Veuillez réessayer.');
    } finally {
      setLoading(false);
    }
  };

  const handleOTPComplete = async (enteredOtp: string) => {
    setOtp(enteredOtp);
    setLoading(true);
    setError('');

    try {
      const isValid = await onVerifyOTP(enteredOtp);
      if (isValid) {
        await onEnableTwoFactor(selectedMethod, pin);
        setIsEnabled(true);
        setCurrentStep('success');
        setTimeout(() => {
          setCurrentStep('main');
        }, 2000);
      } else {
        setError('Code OTP invalide. Veuillez réessayer.');
        setOtp('');
      }
    } catch (err) {
      setError('Une erreur est survenue. Veuillez réessayer.');
      setOtp('');
    } finally {
      setLoading(false);
    }
  };

  const handleDisable2FA = async () => {
    setLoading(true);
    setError('');

    try {
      await onDisableTwoFactor(pin);
      setIsEnabled(false);
      setShowDisableDialog(false);
    } catch (err) {
      setError('Erreur lors de la désactivation du 2FA.');
    } finally {
      setLoading(false);
    }
  };

  const handleBackToMain = () => {
    setCurrentStep('main');
    setPin('');
    setOtp('');
    setError('');
  };

  if (currentStep === 'verify-pin') {
    return (
      <div className="min-h-screen bg-background">
        <header className="safe-area-top px-4 py-4 flex items-center border-b bg-background sticky top-0 z-10">
          <button
            onClick={handleBackToMain}
            className="w-10 h-10 rounded-full bg-secondary flex items-center justify-center touch-target mr-3"
          >
            <ChevronLeft className="w-5 h-5" />
          </button>
          <h1 className="text-lg font-semibold">Vérification du PIN</h1>
        </header>

        <div className="max-w-lg mx-auto px-4 py-8">
          <motion.div
            initial={{ opacity: 0, y: 20 }}
            animate={{ opacity: 1, y: 0 }}
            className="text-center mb-8"
          >
            <div className="w-16 h-16 rounded-full bg-primary/10 flex items-center justify-center mx-auto mb-4">
              <Shield className="w-8 h-8 text-primary" />
            </div>
            <h2 className="text-xl font-bold mb-2">Entrez votre PIN</h2>
            <p className="text-muted-foreground">
              Pour activer l'authentification à deux facteurs
            </p>
          </motion.div>

          {error && (
            <Alert variant="destructive" className="mb-6">
              <XCircle className="h-4 w-4" />
              <AlertTitle>Erreur</AlertTitle>
              <AlertDescription>{error}</AlertDescription>
            </Alert>
          )}

          <SecureNumberPad
            onComplete={handlePinComplete}
            loading={loading}
            error={error}
            length={4}
          />
        </div>
      </div>
    );
  }

  if (currentStep === 'verify-otp') {
    return (
      <div className="min-h-screen bg-background">
        <header className="safe-area-top px-4 py-4 flex items-center border-b bg-background sticky top-0 z-10">
          <button
            onClick={handleBackToMain}
            className="w-10 h-10 rounded-full bg-secondary flex items-center justify-center touch-target mr-3"
          >
            <ChevronLeft className="w-5 h-5" />
          </button>
          <h1 className="text-lg font-semibold">Vérification OTP</h1>
        </header>

        <div className="max-w-lg mx-auto px-4 py-8">
          <motion.div
            initial={{ opacity: 0, y: 20 }}
            animate={{ opacity: 1, y: 0 }}
            className="text-center mb-8"
          >
            <div className="w-16 h-16 rounded-full bg-primary/10 flex items-center justify-center mx-auto mb-4">
              {selectedMethod === '2fa-sms' ? (
                <Smartphone className="w-8 h-8 text-primary" />
              ) : (
                <Mail className="w-8 h-8 text-primary" />
              )}
            </div>
            <h2 className="text-xl font-bold mb-2">Code de vérification</h2>
            <p className="text-muted-foreground">
              Un code a été envoyé à{' '}
              {selectedMethod === '2fa-sms' ? user.phoneNumber : user.email}
            </p>
          </motion.div>

          {error && (
            <Alert variant="destructive" className="mb-6">
              <XCircle className="h-4 w-4" />
              <AlertTitle>Erreur</AlertTitle>
              <AlertDescription>{error}</AlertDescription>
            </Alert>
          )}

          <OTPInput
            length={6}
            onComplete={handleOTPComplete}
            loading={loading}
            error={error}
          />

          <Button
            variant="link"
            className="w-full mt-6"
            onClick={() => {
              // Resend OTP logic
            }}
          >
            Renvoyer le code
          </Button>
        </div>
      </div>
    );
  }

  if (currentStep === 'success') {
    return (
      <div className="min-h-screen bg-background flex items-center justify-center">
        <motion.div
          initial={{ scale: 0 }}
          animate={{ scale: 1 }}
          className="text-center px-4"
        >
          <div className="w-20 h-20 rounded-full bg-success/10 flex items-center justify-center mx-auto mb-4">
            <CheckCircle2 className="w-10 h-10 text-success" />
          </div>
          <h2 className="text-2xl font-bold mb-2">Activé avec succès!</h2>
          <p className="text-muted-foreground">
            L'authentification à deux facteurs est maintenant active
          </p>
        </motion.div>
      </div>
    );
  }

  return (
    <div className="min-h-screen bg-background pb-20">
      <header className="safe-area-top px-4 py-4 flex items-center border-b bg-background sticky top-0 z-10">
        <button
          onClick={onBack}
          className="w-10 h-10 rounded-full bg-secondary flex items-center justify-center touch-target mr-3"
        >
          <ChevronLeft className="w-5 h-5" />
        </button>
        <h1 className="text-lg font-semibold">Authentification à deux facteurs</h1>
      </header>

      <div className="max-w-lg mx-auto px-4 py-6">
        {/* Status Card */}
        <motion.div
          initial={{ opacity: 0, y: 20 }}
          animate={{ opacity: 1, y: 0 }}
          className="mb-6"
        >
          <Card>
            <CardHeader>
              <div className="flex items-center justify-between">
                <div className="flex items-center gap-3">
                  <div className={cn(
                    "w-12 h-12 rounded-full flex items-center justify-center",
                    isEnabled ? "bg-success/10" : "bg-secondary"
                  )}>
                    <Shield className={cn(
                      "w-6 h-6",
                      isEnabled ? "text-success" : "text-muted-foreground"
                    )} />
                  </div>
                  <div>
                    <CardTitle className="text-base">2FA</CardTitle>
                    <CardDescription>
                      {isEnabled ? 'Activée' : 'Désactivée'}
                    </CardDescription>
                  </div>
                </div>
                <Switch
                  checked={isEnabled}
                  onCheckedChange={handleToggle2FA}
                  disabled={loading}
                />
              </div>
            </CardHeader>
          </Card>
        </motion.div>

        {/* Information Alert */}
        <motion.div
          initial={{ opacity: 0, y: 20 }}
          animate={{ opacity: 1, y: 0 }}
          transition={{ delay: 0.1 }}
          className="mb-6"
        >
          <Alert>
            <Key className="h-4 w-4" />
            <AlertTitle>Qu'est-ce que le 2FA?</AlertTitle>
            <AlertDescription>
              L'authentification à deux facteurs ajoute une couche de sécurité supplémentaire
              en demandant un code de vérification à chaque connexion.
            </AlertDescription>
          </Alert>
        </motion.div>

        {/* Method Selection */}
        {!isEnabled && (
          <motion.div
            initial={{ opacity: 0, y: 20 }}
            animate={{ opacity: 1, y: 0 }}
            transition={{ delay: 0.2 }}
          >
            <h3 className="text-sm font-medium text-muted-foreground mb-3">
              Choisir la méthode de vérification
            </h3>
            
            <div className="space-y-3">
              <Card
                className={cn(
                  "cursor-pointer transition-all",
                  selectedMethod === '2fa-sms' && "border-primary bg-primary/5"
                )}
                onClick={() => setSelectedMethod('2fa-sms')}
              >
                <CardContent className="p-4">
                  <div className="flex items-center gap-3">
                    <div className="w-10 h-10 rounded-full bg-secondary flex items-center justify-center">
                      <Smartphone className="w-5 h-5 text-primary" />
                    </div>
                    <div className="flex-1">
                      <h4 className="font-medium">SMS</h4>
                      <p className="text-sm text-muted-foreground">{user.phoneNumber}</p>
                    </div>
                    <div className={cn(
                      "w-5 h-5 rounded-full border-2",
                      selectedMethod === '2fa-sms' 
                        ? "border-primary bg-primary" 
                        : "border-muted-foreground"
                    )}>
                      {selectedMethod === '2fa-sms' && (
                        <div className="w-full h-full flex items-center justify-center">
                          <div className="w-2 h-2 rounded-full bg-white" />
                        </div>
                      )}
                    </div>
                  </div>
                </CardContent>
              </Card>

              {user.email && (
                <Card
                  className={cn(
                    "cursor-pointer transition-all",
                    selectedMethod === '2fa-email' && "border-primary bg-primary/5"
                  )}
                  onClick={() => setSelectedMethod('2fa-email')}
                >
                  <CardContent className="p-4">
                    <div className="flex items-center gap-3">
                      <div className="w-10 h-10 rounded-full bg-secondary flex items-center justify-center">
                        <Mail className="w-5 h-5 text-primary" />
                      </div>
                      <div className="flex-1">
                        <h4 className="font-medium">Email</h4>
                        <p className="text-sm text-muted-foreground">{user.email}</p>
                      </div>
                      <div className={cn(
                        "w-5 h-5 rounded-full border-2",
                        selectedMethod === '2fa-email' 
                          ? "border-primary bg-primary" 
                          : "border-muted-foreground"
                      )}>
                        {selectedMethod === '2fa-email' && (
                          <div className="w-full h-full flex items-center justify-center">
                            <div className="w-2 h-2 rounded-full bg-white" />
                          </div>
                        )}
                      </div>
                    </div>
                  </CardContent>
                </Card>
              )}
            </div>
          </motion.div>
        )}

        {/* Current Method (when enabled) */}
        {isEnabled && (
          <motion.div
            initial={{ opacity: 0, y: 20 }}
            animate={{ opacity: 1, y: 0 }}
            transition={{ delay: 0.2 }}
          >
            <h3 className="text-sm font-medium text-muted-foreground mb-3">
              Méthode active
            </h3>
            
            <Card>
              <CardContent className="p-4">
                <div className="flex items-center gap-3">
                  <div className="w-10 h-10 rounded-full bg-success/10 flex items-center justify-center">
                    {selectedMethod === '2fa-sms' ? (
                      <Smartphone className="w-5 h-5 text-success" />
                    ) : (
                      <Mail className="w-5 h-5 text-success" />
                    )}
                  </div>
                  <div className="flex-1">
                    <h4 className="font-medium">
                      {selectedMethod === '2fa-sms' ? 'SMS' : 'Email'}
                    </h4>
                    <p className="text-sm text-muted-foreground">
                      {selectedMethod === '2fa-sms' ? user.phoneNumber : user.email}
                    </p>
                  </div>
                  <CheckCircle2 className="w-5 h-5 text-success" />
                </div>
              </CardContent>
            </Card>
          </motion.div>
        )}
      </div>

      {/* Disable 2FA Confirmation Dialog */}
      <AlertDialog open={showDisableDialog} onOpenChange={setShowDisableDialog}>
        <AlertDialogContent>
          <AlertDialogHeader>
            <AlertDialogTitle>Désactiver le 2FA?</AlertDialogTitle>
            <AlertDialogDescription>
              Cela réduira la sécurité de votre compte. Êtes-vous sûr de vouloir continuer?
            </AlertDialogDescription>
          </AlertDialogHeader>
          <AlertDialogFooter>
            <AlertDialogCancel>Annuler</AlertDialogCancel>
            <AlertDialogAction
              onClick={handleDisable2FA}
              className="bg-destructive text-destructive-foreground"
            >
              Désactiver
            </AlertDialogAction>
          </AlertDialogFooter>
        </AlertDialogContent>
      </AlertDialog>
    </div>
  );
}

export default TwoFactorAuth;
