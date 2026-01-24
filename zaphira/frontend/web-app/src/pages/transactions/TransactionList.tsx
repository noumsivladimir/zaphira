import React, { useState, useEffect } from 'react';
import { motion } from 'framer-motion';
import { useTranslation } from 'react-i18next';
import { ChevronLeft, Filter, Search, Calendar } from 'lucide-react';
import { Button } from '@/components/ui/button';
import { Input } from '@/components/ui/input';
import { TransactionItem, Transaction } from '@/components/TransactionItem';
import { EmptyState } from '@/components/ui/empty-state';
import { TransactionListSkeleton } from '@/components/ui/loading-skeleton';
import { Tabs, TabsList, TabsTrigger } from '@/components/ui/tabs';
import { Sheet, SheetContent, SheetHeader, SheetTitle, SheetTrigger } from '@/components/ui/sheet';
import { Label } from '@/components/ui/label';
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from '@/components/ui/select';
import { Receipt } from 'lucide-react';
import { BottomNav } from '@/components/BottomNav';

interface TransactionListProps {
  walletNumber: string;
  onBack: () => void;
  onTransactionClick: (tx: Transaction) => void;
  onNavigate?: (tab: string) => void;
}

type TransactionFilter = 'all' | 'sent' | 'received' | 'pending' | 'failed';

export function TransactionList({
  walletNumber,
  onBack,
  onTransactionClick,
  onNavigate,
}: TransactionListProps) {
  const { t } = useTranslation();
  const [transactions, setTransactions] = useState<Transaction[]>([]);
  const [filteredTransactions, setFilteredTransactions] = useState<Transaction[]>([]);
  const [loading, setLoading] = useState(true);
  const [filter, setFilter] = useState<TransactionFilter>('all');
  const [searchQuery, setSearchQuery] = useState('');
  const [showFilters, setShowFilters] = useState(false);
  const [dateRange, setDateRange] = useState<'7days' | '30days' | '90days' | 'all'>('all');
  const [minAmount, setMinAmount] = useState('');
  const [maxAmount, setMaxAmount] = useState('');
  const [activeTab, setActiveTab] = useState('transactions');

  const handleTabChange = (tab: string) => {
    setActiveTab(tab);
    if (onNavigate) {
      onNavigate(tab);
    }
  };

  useEffect(() => {
    loadTransactions();
  }, [walletNumber]);

  useEffect(() => {
    applyFilters();
  }, [transactions, filter, searchQuery, dateRange, minAmount, maxAmount]);

  const loadTransactions = async () => {
    setLoading(true);
    try {
      // Calculate 24h ago timestamp
      const twentyFourHoursAgo = Date.now() - (24 * 60 * 60 * 1000);
      
      // API call to load transactions from last 24 hours
      // const data = await getTransactions(walletNumber, { since: twentyFourHoursAgo });
      // Mock data for now - showing transactions from last 24h
      const mockTransactions: Transaction[] = [
        {
          id: '1',
          reference: 'TRX001',
          type: 'SEND',
          direction: 'out' as const,
          amount: 50000,
          currency: 'XAF',
          status: 'COMPLETED',
          description: 'Lunch money',
          createdAt: new Date(Date.now() - 2 * 60 * 60 * 1000).toISOString(), // 2 hours ago
          counterparty: { name: 'Jean Dupont', walletNumber: '12345678' },
        },
        {
          id: '2',
          reference: 'TRX002',
          type: 'RECEIVE',
          direction: 'in' as const,
          amount: 100000,
          currency: 'XAF',
          status: 'COMPLETED',
          createdAt: new Date(Date.now() - 5 * 60 * 60 * 1000).toISOString(), // 5 hours ago
          counterparty: { name: 'Marie Claire', walletNumber: '87654321' },
        },
        {
          id: '3',
          reference: 'TRX003',
          type: 'SEND',
          direction: 'out' as const,
          amount: 25000,
          currency: 'XAF',
          status: 'PENDING',
          createdAt: new Date(Date.now() - 8 * 60 * 60 * 1000).toISOString(), // 8 hours ago
          counterparty: { name: 'Paul Martin', walletNumber: '11223344' },
        },
        {
          id: '4',
          reference: 'TRX004',
          type: 'RECEIVE',
          direction: 'in' as const,
          amount: 75000,
          currency: 'XAF',
          status: 'COMPLETED',
          createdAt: new Date(Date.now() - 12 * 60 * 60 * 1000).toISOString(), // 12 hours ago
          counterparty: { name: 'Sophie Durand', walletNumber: '55667788' },
        },
        {
          id: '5',
          reference: 'TRX005',
          type: 'SEND',
          direction: 'out' as const,
          amount: 30000,
          currency: 'XAF',
          status: 'COMPLETED',
          description: 'Recharge téléphone',
          createdAt: new Date(Date.now() - 20 * 60 * 60 * 1000).toISOString(), // 20 hours ago
          counterparty: { name: 'Orange Money', walletNumber: '99887766' },
        },
      ];
      
      // Filter only transactions from last 24 hours
      const recentTransactions = mockTransactions.filter(tx => {
        const txDate = new Date(tx.createdAt).getTime();
        return txDate >= twentyFourHoursAgo;
      });
      
      setTransactions(recentTransactions);
    } catch (err) {
      console.error('Failed to load transactions', err);
    } finally {
      setLoading(false);
    }
  };

  const applyFilters = () => {
    let filtered = [...transactions];

    // Type filter
    if (filter !== 'all') {
      if (filter === 'sent') {
        filtered = filtered.filter((tx) => tx.type === 'SEND');
      } else if (filter === 'received') {
        filtered = filtered.filter((tx) => tx.type === 'RECEIVE');
      } else if (filter === 'pending') {
        filtered = filtered.filter((tx) => tx.status === 'PENDING');
      } else if (filter === 'failed') {
        filtered = filtered.filter((tx) => tx.status === 'FAILED');
      }
    }

    // Search filter
    if (searchQuery) {
      const query = searchQuery.toLowerCase();
      filtered = filtered.filter(
        (tx) =>
          tx.reference.toLowerCase().includes(query) ||
          tx.counterparty.name.toLowerCase().includes(query) ||
          tx.description?.toLowerCase().includes(query)
      );
    }

    // Date range filter
    if (dateRange !== 'all') {
      const now = Date.now();
      const days = dateRange === '7days' ? 7 : dateRange === '30days' ? 30 : 90;
      const cutoff = now - days * 24 * 60 * 60 * 1000;
      filtered = filtered.filter((tx) => new Date(tx.createdAt).getTime() > cutoff);
    }

    // Amount range filter
    if (minAmount) {
      filtered = filtered.filter((tx) => tx.amount >= parseFloat(minAmount));
    }
    if (maxAmount) {
      filtered = filtered.filter((tx) => tx.amount <= parseFloat(maxAmount));
    }

    setFilteredTransactions(filtered);
  };

  const clearFilters = () => {
    setDateRange('all');
    setMinAmount('');
    setMaxAmount('');
    setSearchQuery('');
  };

  const hasActiveFilters = dateRange !== 'all' || minAmount || maxAmount || searchQuery;

  return (
    <div className="min-h-screen bg-background pb-20">
      {/* Header */}
      <header className="safe-area-top px-4 py-4 flex items-center justify-between border-b bg-background sticky top-0 z-10">
        <div className="flex items-center flex-1">
          <button
            onClick={onBack}
            className="w-10 h-10 rounded-full bg-secondary flex items-center justify-center touch-target mr-3"
          >
            <ChevronLeft className="w-5 h-5" />
          </button>
          <div>
            <h1 className="text-lg font-semibold">{t('transactions')}</h1>
            <p className="text-xs text-muted-foreground">Dernières 24h</p>
          </div>
        </div>
        <Sheet open={showFilters} onOpenChange={setShowFilters}>
          <SheetTrigger asChild>
            <Button variant="ghost" size="icon" className="relative">
              <Filter className="w-5 h-5" />
              {hasActiveFilters && (
                <span className="absolute top-1 right-1 w-2 h-2 bg-primary rounded-full" />
              )}
            </Button>
          </SheetTrigger>
          <SheetContent side="right">
            <SheetHeader>
              <SheetTitle>{t('filters')}</SheetTitle>
            </SheetHeader>
            <div className="space-y-6 mt-6">
              <div className="space-y-2">
                <Label>{t('date_range')}</Label>
                <Select value={dateRange} onValueChange={(v) => setDateRange(v as typeof dateRange)}>
                  <SelectTrigger>
                    <SelectValue />
                  </SelectTrigger>
                  <SelectContent>
                    <SelectItem value="all">{t('all_time')}</SelectItem>
                    <SelectItem value="7days">{t('last_7_days')}</SelectItem>
                    <SelectItem value="30days">{t('last_30_days')}</SelectItem>
                    <SelectItem value="90days">{t('last_90_days')}</SelectItem>
                  </SelectContent>
                </Select>
              </div>

              <div className="space-y-2">
                <Label>{t('amount_range')}</Label>
                <div className="space-y-2">
                  <Input
                    type="number"
                    placeholder={t('min_amount')}
                    value={minAmount}
                    onChange={(e) => setMinAmount(e.target.value)}
                  />
                  <Input
                    type="number"
                    placeholder={t('max_amount')}
                    value={maxAmount}
                    onChange={(e) => setMaxAmount(e.target.value)}
                  />
                </div>
              </div>

              <Button onClick={clearFilters} variant="outline" className="w-full">
                {t('clear_filters')}
              </Button>
            </div>
          </SheetContent>
        </Sheet>
      </header>

      <div className="max-w-lg mx-auto px-4 py-4">
        {/* Search */}
        <div className="mb-4">
          <div className="relative">
            <Search className="absolute left-3 top-1/2 -translate-y-1/2 w-5 h-5 text-muted-foreground" />
            <Input
              type="text"
              placeholder={t('search_transactions')}
              value={searchQuery}
              onChange={(e) => setSearchQuery(e.target.value)}
              className="pl-11"
            />
          </div>
        </div>

        {/* Filter Tabs */}
        <Tabs value={filter} onValueChange={(v) => setFilter(v as TransactionFilter)} className="mb-6">
          <TabsList className="w-full grid grid-cols-5">
            <TabsTrigger value="all">{t('all')}</TabsTrigger>
            <TabsTrigger value="sent">{t('sent')}</TabsTrigger>
            <TabsTrigger value="received">{t('received')}</TabsTrigger>
            <TabsTrigger value="pending">{t('pending')}</TabsTrigger>
            <TabsTrigger value="failed">{t('failed')}</TabsTrigger>
          </TabsList>
        </Tabs>

        {/* Transaction List */}
        {loading ? (
          <TransactionListSkeleton count={8} />
        ) : filteredTransactions.length > 0 ? (
          <motion.div
            initial={{ opacity: 0 }}
            animate={{ opacity: 1 }}
            className="space-y-2"
          >
            {filteredTransactions.map((tx, index) => (
              <TransactionItem
                key={tx.id}
                transaction={tx}
                onClick={() => onTransactionClick(tx)}
                index={index}
              />
            ))}
          </motion.div>
        ) : (
          <EmptyState
            icon={Receipt}
            title={t('no_transactions_found')}
            description={t('try_different_filters')}
          />
        )}
      </div>
    </div>
  );
}

export default TransactionList;
