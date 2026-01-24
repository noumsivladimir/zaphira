    import React, { useState, useEffect } from 'react';
import { motion } from 'framer-motion';
import { useTranslation } from 'react-i18next';
import { 
  ChevronLeft,
  Plus,
  Wallet,
  Wallet2,
  PiggyBank,
  ShoppingCart,
  TrendingUp,
  Briefcase,
  Gift,
  Home,
  MoreVertical,
  Lock,
  Unlock,
  Edit,
  Trash2,
  ArrowUpRight,
  Check,
  ArrowRightLeft
} from 'lucide-react';
import { Button } from '@/components/ui/button';
import { Card } from '@/components/ui/card';
import { Badge } from '@/components/ui/badge';
import { EmptyState } from '@/components/ui/empty-state';
import { Skeleton } from '@/components/ui/skeleton';
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuTrigger,
} from '@/components/ui/dropdown-menu';
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
} from '@/components/ui/dialog';
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from '@/components/ui/select';
import { Input } from '@/components/ui/input';
import { Label } from '@/components/ui/label';
import { CurrencyInput } from '@/components/ui/currency-input';
import { formatCurrency, maskWalletNumber } from '@/lib/currency';
import { useToast } from '@/hooks/use-toast';
import { cn } from '@/lib/utils';
import { createSubWallet, getSubWalletsByWalletId, type SubWalletType } from '@/api/wallet.api';

type WalletType = 'SAVINGS' | 'EXPENSES' | 'INVESTMENT' | 'BUSINESS' | 'PERSONAL' | 'FAMILY' | 'CUSTOM';

interface WalletTypeConfig {
  type: WalletType;
  label: string;
  description: string;
  icon: React.ElementType;
  color: string;
  bgColor: string;
}

const WALLET_TYPES: WalletTypeConfig[] = [
  {
    type: 'SAVINGS',
    label: 'Épargne',
    description: 'Pour économiser et atteindre vos objectifs',
    icon: PiggyBank,
    color: 'text-green-600',
    bgColor: 'bg-green-100 dark:bg-green-900/30',
  },
  {
    type: 'EXPENSES',
    label: 'Dépenses',
    description: 'Pour vos achats quotidiens',
    icon: ShoppingCart,
    color: 'text-blue-600',
    bgColor: 'bg-blue-100 dark:bg-blue-900/30',
  },
  {
    type: 'INVESTMENT',
    label: 'Investissement',
    description: 'Pour faire fructifier votre argent',
    icon: TrendingUp,
    color: 'text-purple-600',
    bgColor: 'bg-purple-100 dark:bg-purple-900/30',
  },
  {
    type: 'BUSINESS',
    label: 'Business',
    description: 'Pour vos activités professionnelles',
    icon: Briefcase,
    color: 'text-orange-600',
    bgColor: 'bg-orange-100 dark:bg-orange-900/30',
  },
  {
    type: 'FAMILY',
    label: 'Famille',
    description: 'Pour les dépenses familiales',
    icon: Home,
    color: 'text-pink-600',
    bgColor: 'bg-pink-100 dark:bg-pink-900/30',
  },
  {
    type: 'PERSONAL',
    label: 'Personnel',
    description: 'Pour vos besoins personnels',
    icon: Wallet2,
    color: 'text-indigo-600',
    bgColor: 'bg-indigo-100 dark:bg-indigo-900/30',
  },
  {
    type: 'CUSTOM',
    label: 'Personnalisé',
    description: 'Créez votre propre catégorie',
    icon: Gift,
    color: 'text-gray-600',
    bgColor: 'bg-gray-100 dark:bg-gray-900/30',
  },
];

interface Wallet {
  id: string;
  name: string;
  walletNumber: string;
  balance: number;
  currency: string;
  type: WalletType;
  status: 'ACTIVE' | 'FROZEN';
  createdAt: string;
}

interface WalletsManagementProps {
  mainWalletNumber: string;
  mainBalance: number;
  onBack: () => void;
  onViewDetails?: (walletNumber: string) => void;
  onTransferBetween?: () => void;
}

export function WalletsManagement({ 
  mainWalletNumber,
  mainBalance,
  onBack,
  onViewDetails,
  onTransferBetween
}: WalletsManagementProps) {
  const { t } = useTranslation();
  const { toast } = useToast();
  const [wallets, setWallets] = useState<Wallet[]>([]);
  const [loading, setLoading] = useState(true);
  const [showCreateDialog, setShowCreateDialog] = useState(false);
  const [creating, setCreating] = useState(false);
  const [selectedType, setSelectedType] = useState<WalletType | null>(null);
  const [newWallet, setNewWallet] = useState({
    name: '',
    type: 'SAVINGS' as WalletType,
    initialBalance: 0,
  });

  useEffect(() => {
    loadWallets();
  }, [mainWalletNumber]);

  const loadWallets = async () => {
    setLoading(true);
    try {
      if (!mainWalletNumber) {
        setWallets([]);
        return;
      }
      const subWallets = await getSubWalletsByWalletId(mainWalletNumber);
      const mapSubWalletTypeToUi = (type?: SubWalletType): WalletType => {
        switch (type) {
          case 'SAVINGS':
            return 'SAVINGS';
          case 'SPENDING':
            return 'EXPENSES';
          case 'ESCROW':
            return 'INVESTMENT';
          case 'BONUS':
            return 'CUSTOM';
          case 'FEES':
            return 'CUSTOM';
          default:
            return 'CUSTOM';
        }
      };
      const mapped: Wallet[] = subWallets.map((subWallet) => ({
        id: subWallet.id.toString(),
        name: subWallet.subWalletName || 'Sous-wallet',
        walletNumber: subWallet.id.toString(),
        balance: Number(subWallet.totalBalance ?? subWallet.availableBalance ?? 0),
        currency: subWallet.currency || 'XAF',
        type: mapSubWalletTypeToUi(subWallet.type),
        status: subWallet.status === 'FROZEN' ? 'FROZEN' : 'ACTIVE',
        createdAt: subWallet.createdAt || new Date().toISOString(),
      }));
      setWallets(mapped);
    } catch (err) {
      console.error('Failed to load wallets', err);
      toast({
        title: 'Erreur',
        description: 'Impossible de charger vos wallets',
        variant: 'destructive',
      });
    } finally {
      setLoading(false);
    }
  };

  const mapWalletTypeToSubWalletType = (type: WalletType): SubWalletType => {
    switch (type) {
      case 'SAVINGS':
        return 'SAVINGS';
      case 'EXPENSES':
        return 'SPENDING';
      case 'INVESTMENT':
        return 'ESCROW';
      case 'BUSINESS':
        return 'SPENDING';
      case 'FAMILY':
        return 'SPENDING';
      case 'PERSONAL':
        return 'SPENDING';
      default:
        return 'SPENDING';
    }
  };

  const getWalletTypeConfig = (type: WalletType): WalletTypeConfig => {
    return WALLET_TYPES.find(wt => wt.type === type) || WALLET_TYPES[0];
  };

  const handleCreateWallet = async () => {
    if (!newWallet.name.trim()) {
      toast({
        title: 'Erreur',
        description: 'Veuillez entrer un nom pour le wallet',
        variant: 'destructive',
      });
      return;
    }

    if (newWallet.initialBalance > mainBalance) {
      toast({
        title: 'Erreur',
        description: 'Solde insuffisant dans le portefeuille principal',
        variant: 'destructive',
      });
      return;
    }

    setCreating(true);
    try {
      await createSubWallet({
        subWalletName: newWallet.name.trim(),
        type: mapWalletTypeToSubWalletType(newWallet.type),
        walletNumberWhichCanManage: [mainWalletNumber],
      });
      
      toast({
        title: 'Succès',
        description: 'Wallet créé avec succès',
      });

      setShowCreateDialog(false);
      setSelectedType(null);
      setNewWallet({ name: '', type: 'SAVINGS', initialBalance: 0 });
      loadWallets();
    } catch (err) {
      toast({
        title: 'Erreur',
        description: 'Impossible de créer le wallet',
        variant: 'destructive',
      });
    } finally {
      setCreating(false);
    }
  };

  const handleToggleStatus = async (id: string, currentStatus: string) => {
    toast({
      title: 'Fonctionnalité indisponible',
      description: 'Le gel des sous-wallets sera activé après le module de gel de compte.',
      variant: 'destructive',
    });
  };

  const totalWalletsBalance = wallets.reduce((sum, w) => sum + w.balance, 0);

  const handleSelectType = (type: WalletType) => {
    setSelectedType(type);
    const typeConfig = getWalletTypeConfig(type);
    setNewWallet({
      ...newWallet,
      type,
      name: type === 'CUSTOM' ? '' : typeConfig.label,
    });
  };

  const handleViewDetails = (walletNumber: string) => {
    if (walletNumber === mainWalletNumber) {
      onViewDetails?.(walletNumber);
      return;
    }
    toast({
      title: 'Détails indisponibles',
      description: 'Les détails des sous-wallets seront disponibles prochainement.',
    });
  };

  return (
    <div className="min-h-screen bg-background pb-20">
      {/* Header */}
      <header className="safe-area-top px-4 py-4 border-b bg-background sticky top-0 z-10">
        <div className="flex items-center justify-between mb-4">
          <div className="flex items-center">
            <Button variant="ghost" size="icon" onClick={onBack}>
              <ChevronLeft className="w-6 h-6" />
            </Button>
            <div className="ml-3">
              <h1 className="text-lg font-semibold">Mes Wallets</h1>
              <p className="text-xs text-muted-foreground">
                {wallets.length} wallet{wallets.length > 1 ? 's' : ''}
              </p>
            </div>
          </div>
          <Button onClick={() => setShowCreateDialog(true)} size="sm">
            <Plus className="w-4 h-4 mr-2" />
            Créer
          </Button>
        </div>

        {/* Summary Card */}
        <Card className="p-4 bg-gradient-to-r from-primary/10 to-primary/5">
          <div className="flex items-center justify-between">
            <div>
              <p className="text-xs text-muted-foreground mb-1">Total des wallets</p>
              <p className="text-2xl font-bold">{formatCurrency(totalWalletsBalance, 'XAF')}</p>
            </div>
            <div className="w-12 h-12 rounded-full bg-primary/20 flex items-center justify-center">
              <Wallet className="w-6 h-6 text-primary" />
            </div>
          </div>
        </Card>

        {/* Transfer Button */}
        {wallets.length >= 2 && onTransferBetween && (
          <Button 
            onClick={onTransferBetween}
            variant="outline" 
            className="w-full mt-3"
            size="sm"
          >
            <ArrowRightLeft className="w-4 h-4 mr-2" />
            Transférer entre wallets
          </Button>
        )}
      </header>

      <div className="max-w-lg mx-auto px-4 py-6">
        {loading ? (
          <div className="space-y-4">
            {[...Array(3)].map((_, i) => (
              <Skeleton key={i} className="h-28 w-full" />
            ))}
          </div>
        ) : wallets.length > 0 ? (
          <motion.div
            initial={{ opacity: 0 }}
            animate={{ opacity: 1 }}
            className="space-y-3"
          >
            {wallets.map((wallet, index) => {
              const typeConfig = getWalletTypeConfig(wallet.type);
              const Icon = typeConfig.icon;
              
              return (
                <motion.div
                  key={wallet.id}
                  initial={{ opacity: 0, y: 20 }}
                  animate={{ opacity: 1, y: 0 }}
                  transition={{ delay: index * 0.05 }}
                >
                  <Card className={cn(
                    "p-4 cursor-pointer hover:bg-accent transition-colors",
                    wallet.status === 'FROZEN' && "opacity-60"
                  )}
                  onClick={() => handleViewDetails(wallet.walletNumber)}
                  >
                    <div className="flex items-start justify-between mb-3">
                      <div className="flex items-center gap-3">
                        <div className={cn(
                          "w-12 h-12 rounded-full flex items-center justify-center",
                          typeConfig.bgColor
                        )}>
                          <Icon className={cn("w-6 h-6", typeConfig.color)} />
                        </div>
                        <div>
                          <h3 className="font-semibold">{wallet.name}</h3>
                          <p className="text-xs text-muted-foreground font-mono">
                            {maskWalletNumber(wallet.walletNumber)}
                          </p>
                          <Badge variant="outline" className="mt-1 text-xs">
                            {typeConfig.label}
                          </Badge>
                        </div>
                      </div>
                      <DropdownMenu>
                        <DropdownMenuTrigger asChild onClick={(e) => e.stopPropagation()}>
                          <Button variant="ghost" size="icon" className="h-8 w-8">
                            <MoreVertical className="w-4 h-4" />
                          </Button>
                        </DropdownMenuTrigger>
                        <DropdownMenuContent align="end">
                          <DropdownMenuItem onClick={(e) => {
                            e.stopPropagation();
                            handleToggleStatus(wallet.id, wallet.status);
                          }}>
                            {wallet.status === 'ACTIVE' ? (
                              <>
                                <Lock className="w-4 h-4 mr-2" />
                                Geler
                              </>
                            ) : (
                              <>
                                <Unlock className="w-4 h-4 mr-2" />
                                Dégeler
                              </>
                            )}
                          </DropdownMenuItem>
                          <DropdownMenuItem onClick={(e) => e.stopPropagation()}>
                            <Edit className="w-4 h-4 mr-2" />
                            Modifier
                          </DropdownMenuItem>
                          <DropdownMenuItem 
                            onClick={(e) => e.stopPropagation()}
                            className="text-destructive"
                          >
                            <Trash2 className="w-4 h-4 mr-2" />
                            Supprimer
                          </DropdownMenuItem>
                        </DropdownMenuContent>
                      </DropdownMenu>
                    </div>

                    <div className="flex items-center justify-between">
                      <div>
                        <p className="text-2xl font-bold">
                          {formatCurrency(wallet.balance, wallet.currency)}
                        </p>
                        <p className="text-xs text-muted-foreground mt-1">
                          Créé le {new Date(wallet.createdAt).toLocaleDateString('fr-FR')}
                        </p>
                      </div>
                      <Badge 
                        variant={wallet.status === 'ACTIVE' ? 'default' : 'secondary'}
                        className={cn(
                          wallet.status === 'ACTIVE' 
                            ? "bg-green-100 text-green-700 dark:bg-green-900/30"
                            : "bg-gray-100 text-gray-700 dark:bg-gray-900/30"
                        )}
                      >
                        {wallet.status === 'ACTIVE' ? 'Actif' : 'Gelé'}
                      </Badge>
                    </div>
                  </Card>
                </motion.div>
              );
            })}
          </motion.div>
        ) : (
          <EmptyState
            icon={Wallet}
            title="Aucun wallet"
            description="Créez des wallets pour mieux organiser votre argent par catégorie"
            action={{
              label: 'Créer un wallet',
              onClick: () => setShowCreateDialog(true),
            }}
          />
        )}

        {/* Info Card */}
        <Card className="p-4 mt-6 bg-blue-50 dark:bg-blue-900/20 border-blue-200 dark:border-blue-800">
          <h3 className="font-semibold mb-2 text-sm text-blue-900 dark:text-blue-100">
            💡 À propos des wallets
          </h3>
          <ul className="space-y-1 text-xs text-blue-800 dark:text-blue-200">
            <li>• Organisez votre argent par catégorie (épargne, dépenses, etc.)</li>
            <li>• Transférez facilement entre vos wallets</li>
            <li>• Gelez un wallet pour bloquer les transactions</li>
            <li>• Aucun frais pour les transferts entre vos wallets</li>
          </ul>
        </Card>
      </div>

      {/* Create Dialog */}
      <Dialog open={showCreateDialog} onOpenChange={(open) => {
        setShowCreateDialog(open);
        if (!open) {
          setSelectedType(null);
          setNewWallet({ name: '', type: 'SAVINGS', initialBalance: 0 });
        }
      }}>
        <DialogContent className="max-w-2xl max-h-[90vh] overflow-y-auto">
          <DialogHeader>
            <DialogTitle>Créer un nouveau wallet</DialogTitle>
            <DialogDescription>
              Choisissez un type et organisez mieux votre argent
            </DialogDescription>
          </DialogHeader>
          
          {!selectedType ? (
            <div className="py-4">
              <h3 className="text-sm font-medium mb-3">Sélectionnez un type de wallet</h3>
              <div className="grid grid-cols-2 gap-3">
                {WALLET_TYPES.map((typeConfig) => {
                  const Icon = typeConfig.icon;
                  return (
                    <Card
                      key={typeConfig.type}
                      className="p-4 cursor-pointer hover:border-primary transition-colors"
                      onClick={() => handleSelectType(typeConfig.type)}
                    >
                      <div className={cn(
                        "w-10 h-10 rounded-full flex items-center justify-center mb-3",
                        typeConfig.bgColor
                      )}>
                        <Icon className={cn("w-5 h-5", typeConfig.color)} />
                      </div>
                      <h4 className="font-semibold text-sm mb-1">{typeConfig.label}</h4>
                      <p className="text-xs text-muted-foreground">
                        {typeConfig.description}
                      </p>
                    </Card>
                  );
                })}
              </div>
            </div>
          ) : (
            <div className="space-y-4 py-4">
              <div className="flex items-center gap-3 p-3 bg-accent rounded-lg">
                {(() => {
                  const typeConfig = getWalletTypeConfig(selectedType);
                  const Icon = typeConfig.icon;
                  return (
                    <>
                      <div className={cn(
                        "w-10 h-10 rounded-full flex items-center justify-center",
                        typeConfig.bgColor
                      )}>
                        <Icon className={cn("w-5 h-5", typeConfig.color)} />
                      </div>
                      <div className="flex-1">
                        <p className="font-semibold text-sm">{typeConfig.label}</p>
                        <p className="text-xs text-muted-foreground">{typeConfig.description}</p>
                      </div>
                      <Button
                        variant="ghost"
                        size="sm"
                        onClick={() => setSelectedType(null)}
                      >
                        Changer
                      </Button>
                    </>
                  );
                })()}
              </div>

              <div className="space-y-2">
                <Label>Nom du wallet</Label>
                <Input
                  placeholder="Ex: Épargne, Courses, Vacances..."
                  value={newWallet.name}
                  onChange={(e) => setNewWallet({ ...newWallet, name: e.target.value })}
                />
              </div>

              <div className="space-y-2">
                <Label>Solde initial (optionnel)</Label>
                <CurrencyInput
                  value={newWallet.initialBalance}
                  onChange={(value) => setNewWallet({ ...newWallet, initialBalance: value })}
                  currency="XAF"
                  placeholder="0"
                  max={mainBalance}
                />
                <p className="text-xs text-muted-foreground">
                  Disponible: {formatCurrency(mainBalance, 'XAF')}
                </p>
              </div>
            </div>
          )}

          <DialogFooter>
            <Button variant="outline" onClick={() => {
              setShowCreateDialog(false);
              setSelectedType(null);
              setNewWallet({ name: '', type: 'SAVINGS', initialBalance: 0 });
            }}>
              Annuler
            </Button>
            {selectedType && (
              <Button onClick={handleCreateWallet} disabled={creating}>
                {creating ? 'Création...' : 'Créer'}
              </Button>
            )}
          </DialogFooter>
        </DialogContent>
      </Dialog>
    </div>
  );
}

export default WalletsManagement;
