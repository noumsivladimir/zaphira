import React, { useState } from 'react';
import { motion } from 'framer-motion';
import { useTranslation } from 'react-i18next';
import { ChevronLeft, Lock, Bell, Globe, Moon, Sun, Monitor } from 'lucide-react';
import { Button } from '@/components/ui/button';
import { Separator } from '@/components/ui/separator';
import { Switch } from '@/components/ui/switch';
import { Label } from '@/components/ui/label';
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from '@/components/ui/select';
import { useToast } from '@/hooks/use-toast';

interface SettingsProps {
  onBack: () => void;
  onChangePin: () => void;
}

type Theme = 'light' | 'dark' | 'system';
type Language = 'fr' | 'en';

export function Settings({ onBack, onChangePin }: SettingsProps) {
  const { t, i18n } = useTranslation();
  const { toast } = useToast();
  const [theme, setTheme] = useState<Theme>('system');
  const [language, setLanguage] = useState<Language>(i18n.language as Language);
  const [notifications, setNotifications] = useState({
    transactions: true,
    security: true,
    marketing: false,
  });
  const [saving, setSaving] = useState(false);

  const handleThemeChange = (newTheme: Theme) => {
    setTheme(newTheme);
    // Apply theme logic here
    if (newTheme === 'dark') {
      document.documentElement.classList.add('dark');
    } else if (newTheme === 'light') {
      document.documentElement.classList.remove('dark');
    } else {
      // System preference
      const prefersDark = window.matchMedia('(prefers-color-scheme: dark)').matches;
      if (prefersDark) {
        document.documentElement.classList.add('dark');
      } else {
        document.documentElement.classList.remove('dark');
      }
    }

    toast({
      title: t('theme_changed'),
      description: t('theme_changed_desc'),
    });
  };

  const handleLanguageChange = (newLanguage: Language) => {
    setLanguage(newLanguage);
    i18n.changeLanguage(newLanguage);

    toast({
      title: t('language_changed'),
      description: t('language_changed_desc'),
    });
  };

  const handleNotificationToggle = (key: keyof typeof notifications) => {
    setNotifications({
      ...notifications,
      [key]: !notifications[key],
    });

    toast({
      title: t('notification_updated'),
      description: t('notification_updated_desc'),
    });
  };

  const getThemeIcon = () => {
    switch (theme) {
      case 'light':
        return Sun;
      case 'dark':
        return Moon;
      default:
        return Monitor;
    }
  };

  const ThemeIcon = getThemeIcon();

  return (
    <div className="min-h-screen bg-background">
      {/* Header */}
      <header className="safe-area-top px-4 py-4 flex items-center border-b bg-background sticky top-0 z-10">
        <button
          onClick={onBack}
          className="w-10 h-10 rounded-full bg-secondary flex items-center justify-center touch-target mr-3"
        >
          <ChevronLeft className="w-5 h-5" />
        </button>
        <h1 className="text-lg font-semibold">{t('settings')}</h1>
      </header>

      <div className="max-w-lg mx-auto px-4 py-6 space-y-6">
        {/* Security Section */}
        <motion.div
          initial={{ opacity: 0, y: 20 }}
          animate={{ opacity: 1, y: 0 }}
        >
          <h2 className="text-sm font-medium text-muted-foreground mb-3">
            {t('security')}
          </h2>
          <div className="bg-card rounded-lg overflow-hidden">
            <button
              onClick={onChangePin}
              className="w-full flex items-center justify-between p-4 text-left touch-target hover:bg-accent transition-colors"
            >
              <div className="flex items-center gap-3">
                <div className="w-10 h-10 rounded-full bg-secondary flex items-center justify-center">
                  <Lock className="w-5 h-5 text-muted-foreground" />
                </div>
                <div>
                  <p className="font-medium">{t('change_pin')}</p>
                  <p className="text-xs text-muted-foreground">
                    {t('change_pin_desc')}
                  </p>
                </div>
              </div>
              <ChevronLeft className="w-5 h-5 rotate-180 text-muted-foreground" />
            </button>
          </div>
        </motion.div>

        {/* Notifications Section */}
        <motion.div
          initial={{ opacity: 0, y: 20 }}
          animate={{ opacity: 1, y: 0 }}
          transition={{ delay: 0.05 }}
        >
          <h2 className="text-sm font-medium text-muted-foreground mb-3">
            {t('notifications')}
          </h2>
          <div className="bg-card rounded-lg p-4 space-y-4">
            <div className="flex items-center justify-between">
              <div className="flex items-center gap-3">
                <div className="w-10 h-10 rounded-full bg-secondary flex items-center justify-center">
                  <Bell className="w-5 h-5 text-muted-foreground" />
                </div>
                <div>
                  <Label htmlFor="notifications-transactions" className="font-medium">
                    {t('transaction_notifications')}
                  </Label>
                  <p className="text-xs text-muted-foreground">
                    {t('transaction_notifications_desc')}
                  </p>
                </div>
              </div>
              <Switch
                id="notifications-transactions"
                checked={notifications.transactions}
                onCheckedChange={() => handleNotificationToggle('transactions')}
              />
            </div>

            <Separator />

            <div className="flex items-center justify-between">
              <div>
                <Label htmlFor="notifications-security" className="font-medium">
                  {t('security_notifications')}
                </Label>
                <p className="text-xs text-muted-foreground">
                  {t('security_notifications_desc')}
                </p>
              </div>
              <Switch
                id="notifications-security"
                checked={notifications.security}
                onCheckedChange={() => handleNotificationToggle('security')}
              />
            </div>

            <Separator />

            <div className="flex items-center justify-between">
              <div>
                <Label htmlFor="notifications-marketing" className="font-medium">
                  {t('marketing_notifications')}
                </Label>
                <p className="text-xs text-muted-foreground">
                  {t('marketing_notifications_desc')}
                </p>
              </div>
              <Switch
                id="notifications-marketing"
                checked={notifications.marketing}
                onCheckedChange={() => handleNotificationToggle('marketing')}
              />
            </div>
          </div>
        </motion.div>

        {/* Appearance Section */}
        <motion.div
          initial={{ opacity: 0, y: 20 }}
          animate={{ opacity: 1, y: 0 }}
          transition={{ delay: 0.1 }}
        >
          <h2 className="text-sm font-medium text-muted-foreground mb-3">
            {t('appearance')}
          </h2>
          <div className="bg-card rounded-lg p-4 space-y-4">
            <div className="flex items-center justify-between">
              <div className="flex items-center gap-3">
                <div className="w-10 h-10 rounded-full bg-secondary flex items-center justify-center">
                  <ThemeIcon className="w-5 h-5 text-muted-foreground" />
                </div>
                <div>
                  <Label htmlFor="theme" className="font-medium">
                    {t('theme')}
                  </Label>
                  <p className="text-xs text-muted-foreground">
                    {t('theme_desc')}
                  </p>
                </div>
              </div>
              <Select value={theme} onValueChange={(v: Theme) => handleThemeChange(v)}>
                <SelectTrigger className="w-32">
                  <SelectValue />
                </SelectTrigger>
                <SelectContent>
                  <SelectItem value="light">{t('light')}</SelectItem>
                  <SelectItem value="dark">{t('dark')}</SelectItem>
                  <SelectItem value="system">{t('system')}</SelectItem>
                </SelectContent>
              </Select>
            </div>

            <Separator />

            <div className="flex items-center justify-between">
              <div className="flex items-center gap-3">
                <div className="w-10 h-10 rounded-full bg-secondary flex items-center justify-center">
                  <Globe className="w-5 h-5 text-muted-foreground" />
                </div>
                <div>
                  <Label htmlFor="language" className="font-medium">
                    {t('language')}
                  </Label>
                  <p className="text-xs text-muted-foreground">
                    {t('language_desc')}
                  </p>
                </div>
              </div>
              <Select value={language} onValueChange={(v: Language) => handleLanguageChange(v)}>
                <SelectTrigger className="w-32">
                  <SelectValue />
                </SelectTrigger>
                <SelectContent>
                  <SelectItem value="fr">Français</SelectItem>
                  <SelectItem value="en">English</SelectItem>
                </SelectContent>
              </Select>
            </div>
          </div>
        </motion.div>

        {/* About Section */}
        <motion.div
          initial={{ opacity: 0, y: 20 }}
          animate={{ opacity: 1, y: 0 }}
          transition={{ delay: 0.15 }}
        >
          <h2 className="text-sm font-medium text-muted-foreground mb-3">
            {t('about')}
          </h2>
          <div className="bg-card rounded-lg p-4">
            <div className="text-center">
              <p className="font-semibold mb-1">{t('app_name')}</p>
              <p className="text-sm text-muted-foreground mb-4">
                {t('version')} 1.0.0
              </p>
              <p className="text-xs text-muted-foreground">
                © 2026 Zaphira. {t('all_rights_reserved')}
              </p>
            </div>
          </div>
        </motion.div>
      </div>
    </div>
  );
}

export default Settings;
