import React, { useState, useEffect } from 'react';
import { motion } from 'framer-motion';
import { useTranslation } from 'react-i18next';
import { 
  ChevronLeft, 
  Eye, 
  EyeOff,
  TrendingUp, 
  TrendingDown, 
  Lock,
  Wallet,
  ArrowUpRight,
  ArrowDownLeft,
  RefreshCw
} from 'lucide-react';
import { Button } from '@/components/ui/button';
import { Card } from '@/components/ui/card';
import { Separator } from '@/components/ui/separator';
import { Badge } from '@/components/ui/badge';
import { formatCurrency, formatWalletNumber } from '@/lib/currency';
import { cn } from '@/lib/utils';
import { Skeleton } from '@/components/ui/skeleton';
import { getWalletByNumber } from '@/api/wallet.api';

interface WalletDetailsProps {
  walletNumber: string;
  onBack: () => void;
  onTopUp?: () => void;
  onWithdraw?: () => void;
}

interface WalletStats {
  availableBalance: number;
  blockedBalance: number;
  totalSpent: number;
  totalReceived: number;
  totalBalance: number;
  currency: string;
}

export function WalletDetails({ 
  walletNumber, 
  onBack,
  onTopUp,
  onWithdraw 
}: WalletDetailsProps) {
  const { t } = useTranslation();
  const [showBalance, setShowBalance] = useState(true);
  const [loading, setLoading] = useState(true);
  const [refreshing, setRefreshing] = useState(false);
  const [stats, setStats] = useState<WalletStats>({
    availableBalance: 0,
    blockedBalance: 0,
    totalSpent: 0,
    totalReceived: 0,
    totalBalance: 0,
    currency: 'XAF',
  });

  useEffect(() => {
    loadWalletStats();
  }, [walletNumber]);

  const loadWalletStats = async () => {
    setLoading(true);
    try {
      if (!walletNumber) {
        setStats((prev) => ({
          ...prev,
          availableBalance: 0,
          blockedBalance: 0,
          totalBalance: 0,
        }));
        return;
      }
      const wallet = await getWalletByNumber(walletNumber);
      setStats((prev) => ({
        ...prev,
        availableBalance: Number(wallet.availableBalance ?? 0),
        blockedBalance: Number(wallet.blockedBalance ?? 0),
        totalBalance: Number(wallet.totalBalance ?? wallet.availableBalance ?? 0),
        currency: wallet.currency ?? prev.currency,
      }));
    } catch (err) {
      console.error('Failed to load wallet stats', err);
    } finally {
      setLoading(false);
    }
  };

  const handleRefresh = async () => {
    setRefreshing(true);
    await loadWalletStats();
    setRefreshing(false);
  };

  const StatCard = ({ 
    icon: Icon, 
    label, 
    value, 
    trend, 
    trendValue,
    variant = 'default'
  }: { 
    icon: React.ElementType; 
    label: string; 
    value: number; 
    trend?: 'up' | 'down';
    trendValue?: string;
    variant?: 'default' | 'success' | 'warning' | 'info';
  }) => {
    const variantClasses = {
      default: 'bg-primary/10 text-primary',
      success: 'bg-green-100 dark:bg-green-900/30 text-green-600',
      warning: 'bg-orange-100 dark:bg-orange-900/30 text-orange-600',
      info: 'bg-blue-100 dark:bg-blue-900/30 text-blue-600',
    };

    return (
      <Card className="p-4">
        <div className="flex items-start justify-between mb-3">
          <div className={cn(
            "w-10 h-10 rounded-full flex items-center justify-center",
            variantClasses[variant]
          )}>
            <Icon className="w-5 h-5" />
          </div>
          {trend && trendValue && (
            <Badge variant={trend === 'up' ? 'default' : 'secondary'} className="text-xs">
              {trend === 'up' ? <TrendingUp className="w-3 h-3 mr-1" /> : <TrendingDown className="w-3 h-3 mr-1" />}
              {trendValue}
            </Badge>
          )}
        </div>
        <p className="text-xs text-muted-foreground mb-1">{label}</p>
        <p className="text-xl font-bold">
          {showBalance ? formatCurrency(value, stats.currency) : '••••••••'}
        </p>
      </Card>
    );
  };

  if (loading) {
    return (
      <div className="min-h-screen bg-background">
        <header className="safe-area-top px-4 py-4 flex items-center border-b bg-background">
          <Button variant="ghost" size="icon" onClick={onBack}>
            <ChevronLeft className="w-6 h-6" />
          </Button>
          <h1 className="text-lg font-semibold ml-3">Détails du portefeuille</h1>
        </header>
        <div className="max-w-lg mx-auto px-4 py-6 space-y-4">
          <Skeleton className="h-32 w-full" />
          <Skeleton className="h-24 w-full" />
          <Skeleton className="h-24 w-full" />
        </div>
      </div>
    );
  }

  return (
    <div className="min-h-screen bg-background pb-20">
      {/* Header */}
      <header className="safe-area-top px-4 py-4 flex items-center justify-between border-b bg-background sticky top-0 z-10">
        <div className="flex items-center">
          <Button variant="ghost" size="icon" onClick={onBack}>
            <ChevronLeft className="w-6 h-6" />
          </Button>
          <div className="ml-3">
            <h1 className="text-lg font-semibold">Détails du portefeuille</h1>
            <p className="text-xs text-muted-foreground font-mono">
              {formatWalletNumber(walletNumber)}
            </p>
          </div>
        </div>
        <div className="flex gap-2">
          <Button
            variant="ghost"
            size="icon"
            onClick={() => setShowBalance(!showBalance)}
          >
            {showBalance ? <EyeOff className="w-5 h-5" /> : <Eye className="w-5 h-5" />}
          </Button>
          <Button
            variant="ghost"
            size="icon"
            onClick={handleRefresh}
            disabled={refreshing}
          >
            <RefreshCw className={cn("w-5 h-5", refreshing && "animate-spin")} />
          </Button>
        </div>
      </header>

      <motion.div
        initial={{ opacity: 0, y: 20 }}
        animate={{ opacity: 1, y: 0 }}
        className="max-w-lg mx-auto px-4 py-6 space-y-6"
      >
        {/* Main Balance Card */}
        <Card className="p-6 bg-gradient-to-br from-primary to-primary/80 text-white">
          <div className="flex items-center justify-between mb-4">
            <div className="flex items-center gap-2">
              <Wallet className="w-5 h-5" />
              <span className="text-sm opacity-90">Solde total</span>
            </div>
            <Badge variant="secondary" className="bg-white/20 text-white border-none">
              Principal
            </Badge>
          </div>
          <p className="text-4xl font-bold mb-6">
            {showBalance ? formatCurrency(stats.totalBalance, stats.currency) : '••••••••'}
          </p>
          <div className="flex gap-3">
            {onTopUp && (
              <Button 
                onClick={onTopUp}
                className="flex-1 bg-white text-primary hover:bg-white/90"
              >
                <ArrowDownLeft className="w-4 h-4 mr-2" />
                Recharger
              </Button>
            )}
            {onWithdraw && (
              <Button 
                onClick={onWithdraw}
                variant="outline"
                className="flex-1 border-white/30 text-white hover:bg-white/10"
              >
                <ArrowUpRight className="w-4 h-4 mr-2" />
                Retirer
              </Button>
            )}
          </div>
        </Card>

        {/* Balance Breakdown */}
        <div>
          <h2 className="text-sm font-semibold mb-3 text-muted-foreground">Répartition du solde</h2>
          <div className="grid grid-cols-2 gap-3">
            <StatCard
              icon={Wallet}
              label="Solde disponible"
              value={stats.availableBalance}
              variant="success"
            />
            <StatCard
              icon={Lock}
              label="Solde bloqué"
              value={stats.blockedBalance}
              variant="warning"
            />
          </div>
        </div>

        <Separator />

        {/* Transaction Stats */}
        <div>
          <h2 className="text-sm font-semibold mb-3 text-muted-foreground">Statistiques</h2>
          <div className="grid grid-cols-2 gap-3">
            <StatCard
              icon={ArrowDownLeft}
              label="Total reçu"
              value={stats.totalReceived}
              trend="up"
              trendValue="+12%"
              variant="success"
            />
            <StatCard
              icon={ArrowUpRight}
              label="Total dépensé"
              value={stats.totalSpent}
              trend="down"
              trendValue="-5%"
              variant="info"
            />
          </div>
        </div>

        {/* Additional Info */}
        <Card className="p-4">
          <h3 className="font-semibold mb-3 text-sm">Informations</h3>
          <div className="space-y-3 text-sm">
            <div className="flex justify-between">
              <span className="text-muted-foreground">Type de compte</span>
              <span className="font-medium">Principal</span>
            </div>
            <Separator />
            <div className="flex justify-between">
              <span className="text-muted-foreground">Statut</span>
              <Badge variant="default" className="bg-green-100 text-green-700 dark:bg-green-900/30">
                Actif
              </Badge>
            </div>
            <Separator />
            <div className="flex justify-between">
              <span className="text-muted-foreground">Devise</span>
              <span className="font-medium">{stats.currency}</span>
            </div>
            <Separator />
            <div className="flex justify-between">
              <span className="text-muted-foreground">Limite quotidienne</span>
              <span className="font-medium">{formatCurrency(2000000, stats.currency)}</span>
            </div>
          </div>
        </Card>

        {/* Info Banner */}
        <Card className="p-4 bg-blue-50 dark:bg-blue-900/20 border-blue-200 dark:border-blue-800">
          <p className="text-sm text-blue-900 dark:text-blue-100">
            💡 Le solde bloqué correspond aux fonds réservés pour les transactions planifiées en attente.
          </p>
        </Card>
      </motion.div>
    </div>
  );
}

export default WalletDetails;
