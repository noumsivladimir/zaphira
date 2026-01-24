import React, { useState } from 'react';
import { motion } from 'framer-motion';
import { useTranslation } from 'react-i18next';
import { ChevronLeft, User, Mail, Save } from 'lucide-react';
import { Button } from '@/components/ui/button';
import { Input } from '@/components/ui/input';
import { Label } from '@/components/ui/label';
import { useToast } from '@/hooks/use-toast';
import { cn } from '@/lib/utils';

interface EditProfileProps {
  user: {
    firstName: string;
    lastName: string;
    email?: string;
  };
  onSave: (data: { firstName: string; lastName: string; email?: string }) => Promise<void>;
  onBack: () => void;
}

export function EditProfile({ user, onSave, onBack }: EditProfileProps) {
  const { t } = useTranslation();
  const { toast } = useToast();
  const [firstName, setFirstName] = useState(user.firstName);
  const [lastName, setLastName] = useState(user.lastName);
  const [email, setEmail] = useState(user.email || '');
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');

  const hasChanges = firstName !== user.firstName || lastName !== user.lastName || email !== (user.email || '');

  const validateEmail = (value: string): boolean => {
    if (!value) return true;
    return /^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(value);
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();

    if (!firstName.trim() || !lastName.trim()) {
      setError(t('error_name_required'));
      return;
    }

    if (email && !validateEmail(email)) {
      setError(t('error_invalid_email'));
      return;
    }

    setLoading(true);
    setError('');

    try {
      await onSave({
        firstName: firstName.trim(),
        lastName: lastName.trim(),
        email: email.trim() || undefined,
      });

      toast({
        title: t('success'),
        description: t('profile_updated_success'),
      });

      onBack();
    } catch (err: any) {
      setError(err.message || t('error_server'));
      toast({
        title: t('error'),
        description: err.message || t('error_server'),
        variant: 'destructive',
      });
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="min-h-screen bg-background">
      {/* Header */}
      <header className="safe-area-top px-4 py-4 flex items-center justify-between border-b bg-background sticky top-0 z-10">
        <div className="flex items-center">
          <button
            onClick={onBack}
            className="w-10 h-10 rounded-full bg-secondary flex items-center justify-center touch-target mr-3"
          >
            <ChevronLeft className="w-5 h-5" />
          </button>
          <h1 className="text-lg font-semibold">{t('edit_profile')}</h1>
        </div>
        <Button
          onClick={handleSubmit}
          disabled={!hasChanges || loading}
          size="sm"
          className="gap-2"
        >
          <Save className="w-4 h-4" />
          {t('save')}
        </Button>
      </header>

      <motion.div
        initial={{ opacity: 0, y: 20 }}
        animate={{ opacity: 1, y: 0 }}
        className="max-w-lg mx-auto px-4 py-8"
      >
        <form onSubmit={handleSubmit} className="space-y-6">
          {/* Prénom */}
          <div className="space-y-2">
            <Label htmlFor="firstName">Prénom</Label>
            <div className="relative">
              <User className="absolute left-3 top-1/2 -translate-y-1/2 w-5 h-5 text-muted-foreground" />
              <Input
                id="firstName"
                type="text"
                value={firstName}
                onChange={(e) => setFirstName(e.target.value)}
                placeholder="Entrez votre prénom"
                className="pl-11"
                disabled={loading}
              />
            </div>
          </div>

          {/* Nom */}
          <div className="space-y-2">
            <Label htmlFor="lastName">Nom</Label>
            <div className="relative">
              <User className="absolute left-3 top-1/2 -translate-y-1/2 w-5 h-5 text-muted-foreground" />
              <Input
                id="lastName"
                type="text"
                value={lastName}
                onChange={(e) => setLastName(e.target.value)}
                placeholder="Entrez votre nom"
                className="pl-11"
                disabled={loading}
              />
            </div>
          </div>

          {/* Email */}
          <div className="space-y-2">
            <Label htmlFor="email">Email</Label>
            <div className="relative">
              <Mail className="absolute left-3 top-1/2 -translate-y-1/2 w-5 h-5 text-muted-foreground" />
              <Input
                id="email"
                type="email"
                value={email}
                onChange={(e) => setEmail(e.target.value)}
                placeholder="Entrez votre email"
                className="pl-11"
                disabled={loading}
              />
            </div>
            <p className="text-xs text-muted-foreground">
              {t('email_help_text')}
            </p>
          </div>

          {error && (
            <motion.div
              initial={{ opacity: 0, y: -10 }}
              animate={{ opacity: 1, y: 0 }}
              className="p-3 rounded-lg bg-destructive/10 border border-destructive/20"
            >
              <p className="text-sm text-destructive">{error}</p>
            </motion.div>
          )}

          {/* Info Card */}
          <div className="p-4 rounded-lg bg-blue-50 dark:bg-blue-950/30 border border-blue-200 dark:border-blue-900">
            <p className="text-sm text-blue-900 dark:text-blue-200">
              {t('profile_edit_info')}
            </p>
          </div>
        </form>
      </motion.div>
    </div>
  );
}

export default EditProfile;
