import React, { useState, useEffect } from 'react';
import { motion } from 'framer-motion';
import { useTranslation } from 'react-i18next';
import { 
  ChevronLeft,
  Plus,
  Wallet,
  TrendingUp,
  MoreVertical,
  Lock,
  Unlock,
  Edit,
  Trash2,
  ArrowUpRight
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
import { Input } from '@/components/ui/input';
import { Label } from '@/components/ui/label';
import { CurrencyInput } from '@/components/ui/currency-input';
import { formatCurrency, maskWalletNumber } from '@/lib/currency';
import { useToast } from '@/hooks/use-toast';
import { cn } from '@/lib/utils';

interface SubWallet {
  id: string;
  name: string;
  walletNumber: string;
  balance: number;
  currency: string;
  status: 'ACTIVE' | 'FROZEN';
  createdAt: string;
}

interface SubWalletsManagementProps {
  mainWalletNumber: string;
  mainBalance: number;
  onBack: () => void;
  onViewDetails?: (walletNumber: string) => void;
}

export function SubWalletsManagement({ 
  mainWalletNumber,
  mainBalance,
  onBack,
  onViewDetails 
}: SubWalletsManagementProps) {
  const { t } = useTranslation();
  const { toast } = useToast();
  const [subWallets, setSubWallets] = useState<SubWallet[]>([]);
  const [loading, setLoading] = useState(true);
  const [showCreateDialog, setShowCreateDialog] = useState(false);
  const [creating, setCreating] = useState(false);
  const [newSubWallet, setNewSubWallet] = useState({
    name: '',
    initialBalance: 0,
  });

  useEffect(() => {
    loadSubWallets();
  }, [mainWalletNumber]);

  const loadSubWallets = async () => {
    setLoading(true);
    try {
      // Mock data
      const mockSubWallets: SubWallet[] = [
        {
          id: '1',
          name: 'Épargne',
          walletNumber: '87654321',
          balance: 500000,
          currency: 'XAF',
          status: 'ACTIVE',
          createdAt: new Date().toISOString(),
        },
        {
          id: '2',
          name: 'Dépenses courses',
          walletNumber: '11223344',
          balance: 75000,
          currency: 'XAF',
          status: 'ACTIVE',
          createdAt: new Date(Date.now() - 86400000).toISOString(),
        },
        {
          id: '3',
          name: 'Économies enfants',
          walletNumber: '55667788',
          balance: 250000,
          currency: 'XAF',
          status: 'FROZEN',
          createdAt: new Date(Date.now() - 604800000).toISOString(),
        },
      ];
      setSubWallets(mockSubWallets);
    } catch (err) {
      console.error('Failed to load sub-wallets', err);
    } finally {
      setLoading(false);
    }
  };

  const handleCreateSubWallet = async () => {
    if (!newSubWallet.name.trim()) {
      toast({
        title: 'Erreur',
        description: 'Veuillez entrer un nom pour le sous-portefeuille',
        variant: 'destructive',
      });
      return;
    }

    if (newSubWallet.initialBalance > mainBalance) {
      toast({
        title: 'Erreur',
        description: 'Solde insuffisant dans le portefeuille principal',
        variant: 'destructive',
      });
      return;
    }

    setCreating(true);
    try {
      // API call would go here
      await new Promise(resolve => setTimeout(resolve, 1500));
      
      toast({
        title: 'Succès',
        description: 'Sous-portefeuille créé avec succès',
      });

      setShowCreateDialog(false);
      setNewSubWallet({ name: '', initialBalance: 0 });
      loadSubWallets();
    } catch (err) {
      toast({
        title: 'Erreur',
        description: 'Impossible de créer le sous-portefeuille',
        variant: 'destructive',
      });
    } finally {
      setCreating(false);
    }
  };

  const handleToggleStatus = async (id: string, currentStatus: string) => {
    const newStatus = currentStatus === 'ACTIVE' ? 'FROZEN' : 'ACTIVE';
    setSubWallets(subWallets.map(sw => 
      sw.id === id ? { ...sw, status: newStatus as 'ACTIVE' | 'FROZEN' } : sw
    ));
    
    toast({
      title: newStatus === 'FROZEN' ? 'Portefeuille gelé' : 'Portefeuille dégelé',
      description: newStatus === 'FROZEN' 
        ? 'Les transactions sont temporairement bloquées'
        : 'Les transactions sont à nouveau autorisées',
    });
  };

  const totalSubWalletsBalance = subWallets.reduce((sum, sw) => sum + sw.balance, 0);

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
              <h1 className="text-lg font-semibold">Sous-portefeuilles</h1>
              <p className="text-xs text-muted-foreground">
                {subWallets.length} portefeuille{subWallets.length > 1 ? 's' : ''}
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
              <p className="text-xs text-muted-foreground mb-1">Total des sous-portefeuilles</p>
              <p className="text-2xl font-bold">{formatCurrency(totalSubWalletsBalance, 'XAF')}</p>
            </div>
            <div className="w-12 h-12 rounded-full bg-primary/20 flex items-center justify-center">
              <Wallet className="w-6 h-6 text-primary" />
            </div>
          </div>
        </Card>
      </header>

      <div className="max-w-lg mx-auto px-4 py-6">
        {loading ? (
          <div className="space-y-4">
            {[...Array(3)].map((_, i) => (
              <Skeleton key={i} className="h-28 w-full" />
            ))}
          </div>
        ) : subWallets.length > 0 ? (
          <motion.div
            initial={{ opacity: 0 }}
            animate={{ opacity: 1 }}
            className="space-y-3"
          >
            {subWallets.map((subWallet, index) => (
              <motion.div
                key={subWallet.id}
                initial={{ opacity: 0, y: 20 }}
                animate={{ opacity: 1, y: 0 }}
                transition={{ delay: index * 0.05 }}
              >
                <Card className={cn(
                  "p-4 cursor-pointer hover:bg-accent transition-colors",
                  subWallet.status === 'FROZEN' && "opacity-60"
                )}
                onClick={() => onViewDetails?.(subWallet.walletNumber)}
                >
                  <div className="flex items-start justify-between mb-3">
                    <div className="flex items-center gap-3">
                      <div className={cn(
                        "w-12 h-12 rounded-full flex items-center justify-center",
                        subWallet.status === 'ACTIVE' 
                          ? "bg-green-100 dark:bg-green-900/30"
                          : "bg-gray-100 dark:bg-gray-900/30"
                      )}>
                        <Wallet className={cn(
                          "w-6 h-6",
                          subWallet.status === 'ACTIVE' ? "text-green-600" : "text-gray-600"
                        )} />
                      </div>
                      <div>
                        <h3 className="font-semibold">{subWallet.name}</h3>
                        <p className="text-xs text-muted-foreground font-mono">
                          {maskWalletNumber(subWallet.walletNumber)}
                        </p>
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
                          handleToggleStatus(subWallet.id, subWallet.status);
                        }}>
                          {subWallet.status === 'ACTIVE' ? (
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
                        {formatCurrency(subWallet.balance, subWallet.currency)}
                      </p>
                      <p className="text-xs text-muted-foreground mt-1">
                        Créé le {new Date(subWallet.createdAt).toLocaleDateString('fr-FR')}
                      </p>
                    </div>
                    <Badge 
                      variant={subWallet.status === 'ACTIVE' ? 'default' : 'secondary'}
                      className={cn(
                        subWallet.status === 'ACTIVE' 
                          ? "bg-green-100 text-green-700 dark:bg-green-900/30"
                          : "bg-gray-100 text-gray-700 dark:bg-gray-900/30"
                      )}
                    >
                      {subWallet.status === 'ACTIVE' ? 'Actif' : 'Gelé'}
                    </Badge>
                  </div>
                </Card>
              </motion.div>
            ))}
          </motion.div>
        ) : (
          <EmptyState
            icon={Wallet}
            title="Aucun sous-portefeuille"
            description="Créez des sous-portefeuilles pour mieux organiser votre argent"
            action={{
              label: 'Créer un sous-portefeuille',
              onClick: () => setShowCreateDialog(true),
            }}
          />
        )}

        {/* Info Card */}
        <Card className="p-4 mt-6 bg-blue-50 dark:bg-blue-900/20 border-blue-200 dark:border-blue-800">
          <h3 className="font-semibold mb-2 text-sm text-blue-900 dark:text-blue-100">
            💡 À propos des sous-portefeuilles
          </h3>
          <ul className="space-y-1 text-xs text-blue-800 dark:text-blue-200">
            <li>• Organisez votre argent par catégorie (épargne, dépenses, etc.)</li>
            <li>• Transférez facilement entre vos portefeuilles</li>
            <li>• Gelez un portefeuille pour bloquer les transactions</li>
            <li>• Aucun frais pour les transferts entre vos portefeuilles</li>
          </ul>
        </Card>
      </div>

      {/* Create Dialog */}
      <Dialog open={showCreateDialog} onOpenChange={setShowCreateDialog}>
        <DialogContent>
          <DialogHeader>
            <DialogTitle>Créer un sous-portefeuille</DialogTitle>
            <DialogDescription>
              Organisez mieux votre argent en créant des portefeuilles dédiés
            </DialogDescription>
          </DialogHeader>
          
          <div className="space-y-4 py-4">
            <div className="space-y-2">
              <Label>Nom du portefeuille</Label>
              <Input
                placeholder="Ex: Épargne, Courses, Vacances..."
                value={newSubWallet.name}
                onChange={(e) => setNewSubWallet({ ...newSubWallet, name: e.target.value })}
              />
            </div>

            <div className="space-y-2">
              <Label>Solde initial (optionnel)</Label>
              <CurrencyInput
                value={newSubWallet.initialBalance}
                onChange={(value) => setNewSubWallet({ ...newSubWallet, initialBalance: value })}
                currency="XAF"
                placeholder="0"
                max={mainBalance}
              />
              <p className="text-xs text-muted-foreground">
                Disponible: {formatCurrency(mainBalance, 'XAF')}
              </p>
            </div>
          </div>

          <DialogFooter>
            <Button variant="outline" onClick={() => setShowCreateDialog(false)}>
              Annuler
            </Button>
            <Button onClick={handleCreateSubWallet} disabled={creating}>
              {creating ? 'Création...' : 'Créer'}
            </Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>
    </div>
  );
}

export default SubWalletsManagement;
