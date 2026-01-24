import React from 'react';
import { motion } from 'framer-motion';
import { Send, ArrowDownToLine, Calendar, Wallet2 } from 'lucide-react';
import { useTranslation } from 'react-i18next';
import { useNavigate } from 'react-router-dom';
import { cn } from '@/lib/utils';

interface QuickAction {
  id: string;
  icon: React.ElementType;
  label: string;
  color?: string;
}

interface QuickActionsProps {
  onAction: (action: string) => void;
  className?: string;
}

export function QuickActions({ onAction, className }: QuickActionsProps) {
  const { t } = useTranslation();
  const navigate = useNavigate();

  const actions: QuickAction[] = [
    { id: 'send', icon: Send, label: 'Envoyer' },
    { id: 'withdraw', icon: ArrowDownToLine, label: 'Retirer' },
    { id: 'schedule', icon: Calendar, label: 'Prévoir' },
    { id: 'wallets', icon: Wallet2, label: 'Wallets' },
  ];

  return (
    <div className={cn('grid grid-cols-4 gap-3', className)}>
      {actions.map((action, index) => {
        const handleClick = () => {
          onAction(action.id);
          // Navigation vers les pages correspondantes
          if (action.id === 'send') navigate('/transfer');
          if (action.id === 'withdraw') navigate('/withdraw');
          if (action.id === 'schedule') navigate('/scheduled-transactions/create');
          if (action.id === 'wallets') navigate('/wallet-management');
        };

        return (
          <motion.button
            key={action.id}
            initial={{ opacity: 0, y: 20 }}
            animate={{ opacity: 1, y: 0 }}
            transition={{ delay: 0.1 + index * 0.05 }}
            onClick={handleClick}
            className="quick-action"
          >
            <div className="quick-action-icon">
              <action.icon className="w-5 h-5 text-white" />
            </div>
            <span className="text-xs font-medium text-foreground">{action.label}</span>
          </motion.button>
        );
      })}
    </div>
  );
}
