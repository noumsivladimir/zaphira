import React, { useState, useEffect } from 'react';
import { motion } from 'framer-motion';
import { useTranslation } from 'react-i18next';
import { ChevronLeft, Bell, BellOff, CheckCheck, Trash2, ArrowUpRight, ArrowDownLeft, Calendar } from 'lucide-react';
import { Button } from '@/components/ui/button';
import { EmptyState } from '@/components/ui/empty-state';
import { Skeleton } from '@/components/ui/skeleton';
import { Badge } from '@/components/ui/badge';
import { formatCurrency } from '@/lib/currency';
import { cn } from '@/lib/utils';

interface Notification {
  id: string;
  type: 'transaction' | 'security' | 'scheduled' | 'system';
  title: string;
  message: string;
  read: boolean;
  createdAt: string;
  data?: {
    amount?: number;
    reference?: string;
  };
}

interface NotificationsProps {
  onBack: () => void;
}

export function Notifications({ onBack }: NotificationsProps) {
  const { t } = useTranslation();
  const [notifications, setNotifications] = useState<Notification[]>([]);
  const [loading, setLoading] = useState(true);
  const [filter, setFilter] = useState<'all' | 'unread'>('all');

  useEffect(() => {
    loadNotifications();
  }, []);

  const loadNotifications = async () => {
    setLoading(true);
    try {
      // Mock data
      const mockNotifications: Notification[] = [
        {
          id: '1',
          type: 'transaction',
          title: 'Paiement reçu',
          message: 'Vous avez reçu 50,000 XAF de Jean Dupont',
          read: false,
          createdAt: new Date(Date.now() - 3600000).toISOString(),
          data: { amount: 50000, reference: 'TRX001' },
        },
        {
          id: '2',
          type: 'security',
          title: 'Nouvelle connexion',
          message: 'Connexion depuis un nouvel appareil détectée',
          read: false,
          createdAt: new Date(Date.now() - 7200000).toISOString(),
        },
        {
          id: '3',
          type: 'scheduled',
          title: 'Transaction planifiée exécutée',
          message: 'Votre paiement mensuel de 25,000 XAF a été effectué',
          read: true,
          createdAt: new Date(Date.now() - 86400000).toISOString(),
          data: { amount: 25000 },
        },
        {
          id: '4',
          type: 'system',
          title: 'Mise à jour disponible',
          message: 'Une nouvelle version de l\'application est disponible',
          read: true,
          createdAt: new Date(Date.now() - 172800000).toISOString(),
        },
      ];
      setNotifications(mockNotifications);
    } catch (err) {
      console.error('Failed to load notifications', err);
    } finally {
      setLoading(false);
    }
  };

  const markAsRead = (id: string) => {
    setNotifications(notifications.map(n => 
      n.id === id ? { ...n, read: true } : n
    ));
  };

  const markAllAsRead = () => {
    setNotifications(notifications.map(n => ({ ...n, read: true })));
  };

  const deleteNotification = (id: string) => {
    setNotifications(notifications.filter(n => n.id !== id));
  };

  const clearAll = () => {
    setNotifications([]);
  };

  const getNotificationIcon = (type: string) => {
    switch (type) {
      case 'transaction':
        return ArrowDownLeft;
      case 'scheduled':
        return Calendar;
      case 'security':
        return Bell;
      default:
        return Bell;
    }
  };

  const filteredNotifications = filter === 'unread' 
    ? notifications.filter(n => !n.read)
    : notifications;

  const unreadCount = notifications.filter(n => !n.read).length;

  const formatDate = (dateString: string) => {
    const date = new Date(dateString);
    const now = new Date();
    const diffMs = now.getTime() - date.getTime();
    const diffMins = Math.floor(diffMs / 60000);
    const diffHours = Math.floor(diffMs / 3600000);
    const diffDays = Math.floor(diffMs / 86400000);

    if (diffMins < 60) {
      return `Il y a ${diffMins} min`;
    } else if (diffHours < 24) {
      return `Il y a ${diffHours}h`;
    } else if (diffDays < 7) {
      return `Il y a ${diffDays}j`;
    } else {
      return new Intl.DateTimeFormat('fr-FR', {
        day: '2-digit',
        month: 'short',
      }).format(date);
    }
  };

  return (
    <div className="min-h-screen bg-background pb-20">
      {/* Header */}
      <header className="safe-area-top px-4 py-4 border-b bg-background sticky top-0 z-10">
        <div className="flex items-center justify-between mb-4">
          <div className="flex items-center">
            <button
              onClick={onBack}
              className="w-10 h-10 rounded-full bg-secondary flex items-center justify-center touch-target mr-3"
            >
              <ChevronLeft className="w-5 h-5" />
            </button>
            <div>
              <h1 className="text-lg font-semibold">{t('notifications')}</h1>
              {unreadCount > 0 && (
                <p className="text-xs text-muted-foreground">
                  {unreadCount} non lue{unreadCount > 1 ? 's' : ''}
                </p>
              )}
            </div>
          </div>
          {notifications.length > 0 && (
            <div className="flex gap-2">
              {unreadCount > 0 && (
                <Button
                  variant="ghost"
                  size="sm"
                  onClick={markAllAsRead}
                  className="text-xs"
                >
                  <CheckCheck className="w-4 h-4 mr-1" />
                  Tout lire
                </Button>
              )}
              <Button
                variant="ghost"
                size="sm"
                onClick={clearAll}
                className="text-xs text-destructive"
              >
                <Trash2 className="w-4 h-4" />
              </Button>
            </div>
          )}
        </div>

        {/* Filter Tabs */}
        {notifications.length > 0 && (
          <div className="flex gap-2">
            <button
              onClick={() => setFilter('all')}
              className={cn(
                "px-4 py-2 rounded-lg text-sm font-medium transition-colors",
                filter === 'all'
                  ? "bg-primary text-primary-foreground"
                  : "bg-secondary text-muted-foreground"
              )}
            >
              Toutes
            </button>
            <button
              onClick={() => setFilter('unread')}
              className={cn(
                "px-4 py-2 rounded-lg text-sm font-medium transition-colors",
                filter === 'unread'
                  ? "bg-primary text-primary-foreground"
                  : "bg-secondary text-muted-foreground"
              )}
            >
              Non lues {unreadCount > 0 && `(${unreadCount})`}
            </button>
          </div>
        )}
      </header>

      <div className="max-w-lg mx-auto px-4 py-4">
        {loading ? (
          <div className="space-y-4">
            {[...Array(5)].map((_, i) => (
              <Skeleton key={i} className="h-24 w-full" />
            ))}
          </div>
        ) : filteredNotifications.length > 0 ? (
          <div className="space-y-3">
            {filteredNotifications.map((notification, index) => {
              const NotificationIcon = getNotificationIcon(notification.type);
              return (
                <motion.div
                  key={notification.id}
                  initial={{ opacity: 0, y: 20 }}
                  animate={{ opacity: 1, y: 0 }}
                  transition={{ delay: index * 0.05 }}
                  onClick={() => markAsRead(notification.id)}
                  className={cn(
                    "bg-card rounded-lg p-4 cursor-pointer transition-colors hover:bg-accent relative",
                    !notification.read && "bg-primary/5 border-l-4 border-primary"
                  )}
                >
                  <div className="flex gap-3">
                    <div className={cn(
                      "w-10 h-10 rounded-full flex items-center justify-center flex-shrink-0",
                      notification.type === 'transaction' && "bg-green-100 dark:bg-green-900/30 text-green-600",
                      notification.type === 'security' && "bg-yellow-100 dark:bg-yellow-900/30 text-yellow-600",
                      notification.type === 'scheduled' && "bg-blue-100 dark:bg-blue-900/30 text-blue-600",
                      notification.type === 'system' && "bg-gray-100 dark:bg-gray-900/30 text-gray-600"
                    )}>
                      <NotificationIcon className="w-5 h-5" />
                    </div>
                    <div className="flex-1 min-w-0">
                      <div className="flex items-start justify-between gap-2 mb-1">
                        <h3 className="font-semibold text-sm">{notification.title}</h3>
                        <span className="text-xs text-muted-foreground whitespace-nowrap">
                          {formatDate(notification.createdAt)}
                        </span>
                      </div>
                      <p className="text-sm text-muted-foreground">{notification.message}</p>
                      {notification.data?.amount && (
                        <p className="text-sm font-semibold text-primary mt-1">
                          {formatCurrency(notification.data.amount, 'XAF')}
                        </p>
                      )}
                    </div>
                    <button
                      onClick={(e) => {
                        e.stopPropagation();
                        deleteNotification(notification.id);
                      }}
                      className="absolute top-4 right-4 opacity-0 group-hover:opacity-100 transition-opacity"
                    >
                      <Trash2 className="w-4 h-4 text-muted-foreground hover:text-destructive" />
                    </button>
                  </div>
                </motion.div>
              );
            })}
          </div>
        ) : (
          <EmptyState
            icon={filter === 'unread' ? CheckCheck : BellOff}
            title={filter === 'unread' ? 'Aucune notification non lue' : 'Aucune notification'}
            description={
              filter === 'unread' 
                ? 'Toutes vos notifications ont été lues'
                : 'Vous n\'avez aucune notification pour le moment'
            }
          />
        )}
      </div>
    </div>
  );
}

export default Notifications;
