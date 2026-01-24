import React, { useEffect, useState } from 'react';
import { motion, AnimatePresence } from 'framer-motion';
import { useTranslation } from 'react-i18next';
import { ChevronLeft, User, Mail, Phone, Shield, CalendarDays, Globe } from 'lucide-react';
import { Button } from '@/components/ui/button';
import { Input } from '@/components/ui/input';
import { SecureNumberPad } from '@/components/ui/secure-number-pad';
import { cn } from '@/lib/utils';
import { getSecurityQuestions, registerUser } from '@/api/user.api';
import type { SecurityQuestionResponse } from '@/api/user.api';

interface RegisterProps {
  onRegisterSuccess: (data: { userId: number; phoneNumber: string; email: string }) => void;
  onLogin: () => void;
}

type Step = 'info' | 'pin' | 'questions';
type PinStep = 'pin' | 'confirm';

interface SecurityQuestion {
  questionId: string;
  answer: string;
}

interface FormData {
  firstName: string;
  lastName: string;
  email: string;
  phoneNumber: string;
  dateOfBirth: string;
  country: string;
  pin: string;
  confirmPin: string;
  securityQuestions: SecurityQuestion[];
  acceptTerms: boolean;
}

export function Register({ onRegisterSuccess, onLogin }: RegisterProps) {
  const { t } = useTranslation();
  const [step, setStep] = useState<Step>('info');
  const [currentPinStep, setCurrentPinStep] = useState<PinStep>('pin');
  const [tempPin, setTempPin] = useState('');
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');
  const [formData, setFormData] = useState<FormData>({
    firstName: '',
    lastName: '',
    email: '',
    phoneNumber: '',
    dateOfBirth: '',
    country: '',
    pin: '',
    confirmPin: '',
    securityQuestions: [
      { questionId: '', answer: '' },
      { questionId: '', answer: '' },
      { questionId: '', answer: '' },
    ],
    acceptTerms: false,
  });

  const [securityQuestions, setSecurityQuestions] = useState<SecurityQuestionResponse[]>([]);
  const [questionsLoading, setQuestionsLoading] = useState(false);
  const [questionsError, setQuestionsError] = useState('');

  useEffect(() => {
    let mounted = true;
    const loadQuestions = async () => {
      setQuestionsLoading(true);
      setQuestionsError('');
      try {
        const response = await getSecurityQuestions();
        const questions = response?.data || [];
        if (mounted) {
          setSecurityQuestions(questions);
        }
      } catch (err: any) {
        if (mounted) {
          setQuestionsError(err?.message || 'Impossible de charger les questions');
        }
      } finally {
        if (mounted) {
          setQuestionsLoading(false);
        }
      }
    };

    loadQuestions();
    return () => {
      mounted = false;
    };
  }, []);

  const handleInputChange = (field: keyof FormData, value: string | boolean) => {
    setFormData((prev) => ({ ...prev, [field]: value }));
    setError('');
  };

  const handleSecurityQuestionChange = (
    index: number,
    field: keyof SecurityQuestion,
    value: string
  ) => {
    setFormData((prev) => {
      const updated = [...prev.securityQuestions];
      updated[index] = { ...updated[index], [field]: value };
      return { ...prev, securityQuestions: updated };
    });
    setError('');
  };

  const validatePersonalInfo = (): boolean => {
    if (!formData.firstName.trim()) {
      setError('Le prénom est requis');
      return false;
    }

    if (!formData.lastName.trim()) {
      setError('Le nom est requis');
      return false;
    }

    if (!formData.email.trim()) {
      setError('L’email est requis');
      return false;
    }

    const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
    if (!emailRegex.test(formData.email.trim())) {
      setError('Adresse email invalide');
      return false;
    }

    const phoneDigits = formData.phoneNumber.replace(/\D/g, '');
    if (phoneDigits.length < 9) {
      setError('Numéro de téléphone invalide');
      return false;
    }

    if (!formData.dateOfBirth) {
      setError('La date de naissance est requise');
      return false;
    }

    if (!formData.country.trim()) {
      setError('Le pays est requis');
      return false;
    }

    return true;
  };

  const validateSecurityQuestions = (): boolean => {
    const validQuestions = formData.securityQuestions.filter(
      (q) => q.questionId && q.answer.trim()
    );

    if (validQuestions.length < 3) {
      setError('Veuillez répondre aux 3 questions de sécurité');
      return false;
    }

    const uniqueQuestions = new Set(validQuestions.map((q) => q.questionId));
    if (uniqueQuestions.size !== validQuestions.length) {
      setError('Veuillez choisir des questions différentes');
      return false;
    }

    if (!formData.acceptTerms) {
      setError('Vous devez accepter les termes et conditions');
      return false;
    }

    return true;
  };

  const handleInfoSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    if (!validatePersonalInfo()) return;
    setError('');
    setStep('pin');
  };

  const handlePinComplete = (pin: string) => {
    if (currentPinStep === 'pin') {
      setTempPin(pin);
      setCurrentPinStep('confirm');
      setError('');
    } else {
      if (pin !== tempPin) {
        setError('Les codes PIN ne correspondent pas');
        setCurrentPinStep('pin');
        setTempPin('');
        return;
      }
      setFormData((prev) => ({ ...prev, pin, confirmPin: pin }));
      setTempPin('');
      setCurrentPinStep('pin');
      setError('');
      setStep('questions');
    }
  };

  const handleRegister = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!validateSecurityQuestions()) return;

    setLoading(true);
    setError('');

    try {
      const response = await registerUser({
        phoneNumber: formData.phoneNumber,
        pin: formData.pin,
        email: formData.email,
        firstName: formData.firstName,
        lastName: formData.lastName,
        dateOfBirth: formData.dateOfBirth,
        country: formData.country,
        securityAnswers: formData.securityQuestions.map((answer) => ({
          questionId: Number(answer.questionId),
          answer: answer.answer,
        })),
      });

      const userId = response?.data?.userId ?? response?.data?.data?.userId;
      const phoneNumber = response?.data?.phoneNumber ?? formData.phoneNumber;
      const email = response?.data?.email ?? formData.email;

      if (userId) {
        onRegisterSuccess({ userId, phoneNumber, email });
      } else {
        setError('Inscription terminée, mais identifiant utilisateur manquant');
      }
    } catch (err: any) {
      setError(err?.message || 'Erreur lors de l’inscription');
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="min-h-screen bg-background flex flex-col">
      <header className="safe-area-top px-4 py-4 flex items-center">
        <AnimatePresence mode="wait">
          {step !== 'info' && (
            <motion.button
              initial={{ opacity: 0, x: -10 }}
              animate={{ opacity: 1, x: 0 }}
              exit={{ opacity: 0, x: -10 }}
              onClick={() => {
                if (step === 'pin') {
                  setStep('info');
                  setCurrentPinStep('pin');
                  setTempPin('');
                } else {
                  setStep('pin');
                }
                setError('');
              }}
              className="w-10 h-10 rounded-full bg-secondary flex items-center justify-center touch-target"
            >
              <ChevronLeft className="w-5 h-5" />
            </motion.button>
          )}
        </AnimatePresence>

        <div className="flex-1 flex items-center justify-center gap-2 px-4">
          {['info', 'pin', 'questions'].map((s, i) => (
            <div
              key={s}
              className={cn(
                'h-1 flex-1 rounded-full transition-all',
                ['info', 'pin', 'questions'].indexOf(step) >= i
                  ? 'bg-primary'
                  : 'bg-secondary'
              )}
            />
          ))}
        </div>
      </header>

      <div className="flex-1 flex flex-col overflow-hidden">
        <AnimatePresence mode="wait">
          {step === 'info' && (
            <motion.div
              key="info"
              initial={{ opacity: 0, x: 20 }}
              animate={{ opacity: 1, x: 0 }}
              exit={{ opacity: 0, x: -20 }}
              className="flex-1 flex flex-col px-6 pt-8 overflow-y-auto"
            >
              <div className="mb-8">
                <motion.div
                  initial={{ scale: 0.8, opacity: 0 }}
                  animate={{ scale: 1, opacity: 1 }}
                  transition={{ delay: 0.1 }}
                  className="w-16 h-16 rounded-2xl bg-primary flex items-center justify-center mb-6 shadow-button"
                >
                  <User className="w-8 h-8 text-white" />
                </motion.div>
                <h1 className="text-2xl font-bold text-foreground font-display mb-2">
                  Créer un compte
                </h1>
                <p className="text-muted-foreground">
                  Commencez par vos informations personnelles
                </p>
              </div>

              <form onSubmit={handleInfoSubmit} className="flex-1 flex flex-col pb-8">
                <div className="space-y-4 flex-1">
                  <div className="grid grid-cols-2 gap-4">
                    <div>
                      <label className="block text-sm font-medium text-foreground mb-2">
                        Prénom
                      </label>
                      <Input
                        type="text"
                        value={formData.firstName}
                        onChange={(e) => handleInputChange('firstName', e.target.value)}
                        placeholder="Votre prénom"
                        className="h-12 rounded-xl"
                        required
                      />
                    </div>
                    <div>
                      <label className="block text-sm font-medium text-foreground mb-2">
                        Nom
                      </label>
                      <Input
                        type="text"
                        value={formData.lastName}
                        onChange={(e) => handleInputChange('lastName', e.target.value)}
                        placeholder="Votre nom"
                        className="h-12 rounded-xl"
                        required
                      />
                    </div>
                  </div>

                  <div>
                    <label className="block text-sm font-medium text-foreground mb-2">
                      Email
                    </label>
                    <div className="relative">
                      <Mail className="absolute left-4 top-1/2 -translate-y-1/2 w-5 h-5 text-muted-foreground" />
                      <Input
                        type="email"
                        value={formData.email}
                        onChange={(e) => handleInputChange('email', e.target.value)}
                        placeholder="exemple@email.com"
                        className="pl-12 h-12 rounded-xl"
                        required
                      />
                    </div>
                  </div>

                  <div>
                    <label className="block text-sm font-medium text-foreground mb-2">
                      Numéro de téléphone
                    </label>
                    <div className="relative">
                      <Phone className="absolute left-4 top-1/2 -translate-y-1/2 w-5 h-5 text-muted-foreground" />
                      <Input
                        type="tel"
                        value={formData.phoneNumber}
                        onChange={(e) => handleInputChange('phoneNumber', e.target.value)}
                        placeholder="+237XXXXXXXXX"
                        className="pl-12 h-12 rounded-xl"
                        required
                      />
                    </div>
                  </div>

                  <div className="grid grid-cols-2 gap-4">
                    <div>
                      <label className="block text-sm font-medium text-foreground mb-2">
                        Date de naissance
                      </label>
                      <div className="relative">
                        <CalendarDays className="absolute left-4 top-1/2 -translate-y-1/2 w-5 h-5 text-muted-foreground" />
                        <Input
                          type="date"
                          value={formData.dateOfBirth}
                          onChange={(e) => handleInputChange('dateOfBirth', e.target.value)}
                          className="pl-12 h-12 rounded-xl"
                          required
                        />
                      </div>
                    </div>
                    <div>
                      <label className="block text-sm font-medium text-foreground mb-2">
                        Pays
                      </label>
                      <div className="relative">
                        <Globe className="absolute left-4 top-1/2 -translate-y-1/2 w-5 h-5 text-muted-foreground" />
                        <Input
                          type="text"
                          value={formData.country}
                          onChange={(e) => handleInputChange('country', e.target.value)}
                          placeholder="Cameroun"
                          className="pl-12 h-12 rounded-xl"
                          required
                        />
                      </div>
                    </div>
                  </div>

                  {error && (
                    <motion.p
                      initial={{ opacity: 0, y: -10 }}
                      animate={{ opacity: 1, y: 0 }}
                      className="text-destructive text-sm"
                    >
                      {error}
                    </motion.p>
                  )}
                </div>

                <div className="mt-6">
                  <Button
                    type="submit"
                    className="w-full h-14 text-lg rounded-xl shadow-button"
                  >
                    Continuer
                  </Button>

                  <p className="text-center mt-4 text-muted-foreground text-sm">
                    Vous avez déjà un compte ?{' '}
                    <button
                      type="button"
                      onClick={onLogin}
                      className="text-primary font-medium hover:underline"
                    >
                      Se connecter
                    </button>
                  </p>
                </div>
              </form>
            </motion.div>
          )}

          {/* Step 2: PIN */}
          {step === 'pin' && (
            <motion.div
              key="pin"
              initial={{ opacity: 0, x: 20 }}
              animate={{ opacity: 1, x: 0 }}
              exit={{ opacity: 0, x: -20 }}
              className="flex-1 flex flex-col items-center justify-center px-6"
            >
              <SecureNumberPad
                key={`pin-${currentPinStep}`}
                length={6}
                title={
                  currentPinStep === 'pin'
                    ? 'Créer votre code PIN'
                    : 'Confirmer votre code PIN'
                }
                subtitle="6 chiffres"
                onComplete={handlePinComplete}
                error={error}
                loading={loading}
                onCancel={() => {
                  if (currentPinStep === 'confirm') {
                    setCurrentPinStep('pin');
                    setTempPin('');
                    setError('');
                  } else {
                    setStep('info');
                    setError('');
                  }
                }}
              />
            </motion.div>
          )}

          {/* Step 3: Security Questions */}
          {step === 'questions' && (
            <motion.div
              key="questions"
              initial={{ opacity: 0, x: 20 }}
              animate={{ opacity: 1, x: 0 }}
              exit={{ opacity: 0, x: -20 }}
              className="flex-1 flex flex-col px-6 pt-8 overflow-y-auto"
            >
              <div className="mb-8">
                <motion.div
                  initial={{ scale: 0.8, opacity: 0 }}
                  animate={{ scale: 1, opacity: 1 }}
                  transition={{ delay: 0.1 }}
                  className="w-16 h-16 rounded-2xl bg-primary flex items-center justify-center mb-6 shadow-button"
                >
                  <Shield className="w-8 h-8 text-white" />
                </motion.div>
                <h1 className="text-2xl font-bold text-foreground font-display mb-2">
                  Questions de sécurité
                </h1>
                <p className="text-muted-foreground">
                  Pour récupérer votre compte en cas d'oubli
                </p>
              </div>

              <form onSubmit={handleRegister} className="flex-1 flex flex-col pb-8">
                <div className="space-y-6 flex-1">
                  {questionsLoading && (
                    <p className="text-sm text-muted-foreground">
                      Chargement des questions de sécurité...
                    </p>
                  )}
                  {questionsError && (
                    <p className="text-sm text-destructive">
                      {questionsError}
                    </p>
                  )}
                  {[0, 1, 2].map((index) => (
                    <div key={index} className="space-y-3">
                      <label className="block text-sm font-medium text-foreground">
                        Question {index + 1}
                      </label>
                      <select
                        value={formData.securityQuestions[index].questionId}
                        onChange={(e) =>
                          handleSecurityQuestionChange(index, 'questionId', e.target.value)
                        }
                        className="w-full h-12 px-4 rounded-xl border border-input bg-background"
                        required
                      >
                        <option value="">Sélectionnez une question</option>
                        {securityQuestions.map((q) => (
                          <option
                            key={q.id}
                            value={String(q.id)}
                            disabled={formData.securityQuestions.some(
                              (sq, i) => i !== index && sq.questionId === String(q.id)
                            )}
                          >
                            {q.question}
                          </option>
                        ))}
                      </select>
                      <Input
                        type="text"
                        value={formData.securityQuestions[index].answer}
                        onChange={(e) =>
                          handleSecurityQuestionChange(index, 'answer', e.target.value)
                        }
                        placeholder="Votre réponse"
                        className="h-12 rounded-xl"
                        required
                      />
                    </div>
                  ))}

                  <div className="flex items-start gap-3 pt-4">
                    <input
                      type="checkbox"
                      id="terms"
                      checked={formData.acceptTerms}
                      onChange={(e) => handleInputChange('acceptTerms', e.target.checked)}
                      className="mt-1 w-5 h-5 rounded border-input"
                      required
                    />
                    <label htmlFor="terms" className="text-sm text-muted-foreground">
                      J'accepte les{' '}
                      <a href="/terms" className="text-primary hover:underline">
                        termes et conditions
                      </a>{' '}
                      et la{' '}
                      <a href="/privacy" className="text-primary hover:underline">
                        politique de confidentialité
                      </a>
                    </label>
                  </div>

                  {error && (
                    <motion.p
                      initial={{ opacity: 0, y: -10 }}
                      animate={{ opacity: 1, y: 0 }}
                      className="text-destructive text-sm"
                    >
                      {error}
                    </motion.p>
                  )}
                </div>

                <div className="mt-6">
                  <Button
                    type="submit"
                    className="w-full h-14 text-lg rounded-xl shadow-button"
                    disabled={loading}
                  >
                    {loading ? (
                      <span className="flex items-center gap-2">
                        <span className="w-5 h-5 border-2 border-white/30 border-t-white rounded-full animate-spin" />
                        Inscription en cours...
                      </span>
                    ) : (
                      'Créer mon compte'
                    )}
                  </Button>
                </div>
              </form>
            </motion.div>
          )}
        </AnimatePresence>
      </div>
    </div>
  );
}

export default Register;
