import React, { useState, useEffect } from 'react';
import { motion } from 'framer-motion';
import { useTranslation } from 'react-i18next';
import { ChevronLeft, User, Phone, Mail, Wallet, Edit, Shield, Bell, HelpCircle, LogOut, KeyRound } from 'lucide-react';
import { Button } from '@/components/ui/button';
import { Avatar, AvatarFallback } from '@/components/ui/avatar';
import { Separator } from '@/components/ui/separator';
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
import { maskWalletNumber } from '@/lib/currency';
import { cn } from '@/lib/utils';
import { BottomNav } from '@/components/BottomNav';

interface ProfileProps {
  user: {
    id: string;
    name: string;
    phoneNumber: string;
    email?: string;
    walletNumber: string;
  };
  onBack: () => void;
  onEdit: () => void;
  onSecurityQuestions: () => void;
  onChangePin: () => void;
  on2FA: () => void;
  onNotifications: () => void;
  onHelp: () => void;
  onLogout: () => void;
  onNavigate?: (tab: string) => void;
}

interface MenuItem {
  icon: React.ElementType;
  label: string;
  onClick: () => void;
  variant?: 'default' | 'danger';
}

export function Profile({
  user,
  onBack,
  onEdit,
  onSecurityQuestions,
  onChangePin,
  on2FA,
  onNotifications,
  onHelp,
  onLogout,
  onNavigate,
}: ProfileProps) {
  const { t } = useTranslation();
  const [showLogoutDialog, setShowLogoutDialog] = useState(false);
  const [activeTab, setActiveTab] = useState('profile');

  const handleTabChange = (tab: string) => {
    setActiveTab(tab);
    if (onNavigate) {
      onNavigate(tab);
    }
  };

  const getInitials = (name: string) => {
    return name
      .split(' ')
      .map((n) => n[0])
      .join('')
      .toUpperCase()
      .slice(0, 2);
  };

  const menuSections: MenuItem[][] = [
    [
      {
        icon: Edit,
        label: t('edit_profile'),
        onClick: onEdit,
      },
    ],
    [
      {
        icon: Shield,
        label: t('security_questions'),
        onClick: onSecurityQuestions,
      },
      {
        icon: Shield,
        label: t('change_pin'),
        onClick: onChangePin,
      },
      {
        icon: KeyRound,
        label: 'Authentification 2FA',
        onClick: on2FA,
      },
    ],
    [
      {
        icon: Bell,
        label: t('notifications'),
        onClick: onNotifications,
      },
      {
        icon: HelpCircle,
        label: t('help'),
        onClick: onHelp,
      },
    ],
    [
      {
        icon: LogOut,
        label: t('logout'),
        onClick: () => setShowLogoutDialog(true),
        variant: 'danger',
      },
    ],
  ];

  return (
    <div className="min-h-screen bg-background pb-24">
      {/* Header */}
      <header className="safe-area-top px-4 py-4 flex items-center border-b bg-background sticky top-0 z-10">
        <button
          onClick={onBack}
          className="w-10 h-10 rounded-full bg-secondary flex items-center justify-center touch-target mr-3"
        >
          <ChevronLeft className="w-5 h-5" />
        </button>
        <h1 className="text-lg font-semibold">{t('profile')}</h1>
      </header>

      <div className="max-w-lg mx-auto">
        {/* Profile Header */}
        <motion.div
          initial={{ opacity: 0, y: 20 }}
          animate={{ opacity: 1, y: 0 }}
          className="px-4 py-8 text-center"
        >
          <Avatar className="w-24 h-24 mx-auto mb-4">
            <AvatarFallback className="text-2xl font-bold bg-primary text-primary-foreground">
              {getInitials(user.name)}
            </AvatarFallback>
          </Avatar>
          <h2 className="text-2xl font-bold text-foreground mb-1">{user.name}</h2>
          <p className="text-muted-foreground">{user.phoneNumber}</p>
        </motion.div>

        {/* Profile Details */}
        <motion.div
          initial={{ opacity: 0, y: 20 }}
          animate={{ opacity: 1, y: 0 }}
          transition={{ delay: 0.1 }}
          className="px-4 mb-6"
        >
          <div className="bg-card rounded-lg p-4 space-y-4">
            <div className="flex items-center gap-3">
              <div className="w-10 h-10 rounded-full bg-secondary flex items-center justify-center">
                <Phone className="w-5 h-5 text-muted-foreground" />
              </div>
              <div className="flex-1">
                <p className="text-xs text-muted-foreground">{t('phone_number')}</p>
                <p className="text-sm font-medium">{user.phoneNumber}</p>
              </div>
            </div>

            {user.email && (
              <>
                <Separator />
                <div className="flex items-center gap-3">
                  <div className="w-10 h-10 rounded-full bg-secondary flex items-center justify-center">
                    <Mail className="w-5 h-5 text-muted-foreground" />
                  </div>
                  <div className="flex-1">
                    <p className="text-xs text-muted-foreground">{t('email')}</p>
                    <p className="text-sm font-medium">{user.email}</p>
                  </div>
                </div>
              </>
            )}

            <Separator />
            <div className="flex items-center gap-3">
              <div className="w-10 h-10 rounded-full bg-secondary flex items-center justify-center">
                <Wallet className="w-5 h-5 text-muted-foreground" />
              </div>
              <div className="flex-1">
                <p className="text-xs text-muted-foreground">{t('wallet_number')}</p>
                <p className="text-sm font-medium font-mono">{maskWalletNumber(user.walletNumber)}</p>
              </div>
            </div>
          </div>
        </motion.div>

        {/* Menu Sections */}
        <div className="px-4 space-y-4 pb-8">
          {menuSections.map((section, sectionIndex) => (
            <motion.div
              key={sectionIndex}
              initial={{ opacity: 0, y: 20 }}
              animate={{ opacity: 1, y: 0 }}
              transition={{ delay: 0.2 + sectionIndex * 0.05 }}
              className="bg-card rounded-lg overflow-hidden"
            >
              {section.map((item, itemIndex) => (
                <React.Fragment key={itemIndex}>
                  {itemIndex > 0 && <Separator />}
                  <button
                    onClick={item.onClick}
                    className={cn(
                      'w-full flex items-center gap-3 p-4 text-left touch-target transition-colors',
                      'hover:bg-accent',
                      item.variant === 'danger' && 'text-destructive'
                    )}
                  >
                    <div
                      className={cn(
                        'w-10 h-10 rounded-full flex items-center justify-center',
                        item.variant === 'danger'
                          ? 'bg-destructive/10'
                          : 'bg-secondary'
                      )}
                    >
                      <item.icon
                        className={cn(
                          'w-5 h-5',
                          item.variant === 'danger'
                            ? 'text-destructive'
                            : 'text-muted-foreground'
                        )}
                      />
                    </div>
                    <span className="flex-1 font-medium">{item.label}</span>
                    <ChevronLeft className="w-5 h-5 rotate-180 text-muted-foreground" />
                  </button>
                </React.Fragment>
              ))}
            </motion.div>
          ))}
        </div>
      </div>

      {/* Logout Confirmation Dialog */}
      <AlertDialog open={showLogoutDialog} onOpenChange={setShowLogoutDialog}>
        <AlertDialogContent>
          <AlertDialogHeader>
            <AlertDialogTitle>{t('confirm_logout')}</AlertDialogTitle>
            <AlertDialogDescription>
              {t('confirm_logout_message')}
            </AlertDialogDescription>
          </AlertDialogHeader>
          <AlertDialogFooter>
            <AlertDialogCancel>{t('cancel')}</AlertDialogCancel>
            <AlertDialogAction onClick={onLogout} className="bg-destructive text-destructive-foreground">
              {t('logout')}
            </AlertDialogAction>
          </AlertDialogFooter>
        </AlertDialogContent>
      </AlertDialog>

      {/* Bottom Navigation */}
      <BottomNav activeTab={activeTab} onTabChange={handleTabChange} />
    </div>
  );
}

export default Profile;
