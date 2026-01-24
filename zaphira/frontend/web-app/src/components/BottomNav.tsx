import React from 'react';
import { motion } from 'framer-motion';
import { Home, ArrowLeftRight, Clock, User } from 'lucide-react';
import { useNavigate, useLocation } from 'react-router-dom';
import { cn } from '@/lib/utils';

interface NavItem {
  id: string;
  icon: React.ElementType;
  label: string;
}

interface BottomNavProps {
  activeTab: string;
  onTabChange: (tab: string) => void;
}

const navItems: NavItem[] = [
  { id: 'home', icon: Home, label: 'Accueil' },
  { id: 'transactions', icon: ArrowLeftRight, label: 'Historique' },
  { id: 'scheduled', icon: Clock, label: 'Planifiés' },
  { id: 'profile', icon: User, label: 'Profil' },
];

export function BottomNav({ activeTab, onTabChange }: BottomNavProps) {
  const navigate = useNavigate();
  const location = useLocation();

  const handleNavigation = (tab: string) => {
    onTabChange(tab);
    // Navigation vers les pages correspondantes
    if (tab === 'home') navigate('/dashboard');
    if (tab === 'transactions') navigate('/transactions');
    if (tab === 'scheduled') navigate('/scheduled-transactions');
    if (tab === 'profile') navigate('/profile');
  };

  return (
    <nav 
      className="fixed bottom-0 left-0 right-0 w-full bg-card/95 backdrop-blur-lg border-t border-border z-50 bottom-nav-fixed" 
      style={{ 
        paddingBottom: 'max(env(safe-area-inset-bottom, 0px), 0px)',
        WebkitTransform: 'translateZ(0)',
        transform: 'translateZ(0)'
      }}
    >
      <div className="flex items-center justify-around h-16 max-w-lg mx-auto px-2">
        {navItems.map((item) => {
          const isActive = activeTab === item.id;
          return (
            <button
              key={item.id}
              onClick={() => handleNavigation(item.id)}
              className="relative flex flex-col items-center justify-center w-16 h-full touch-target"
              aria-label={item.label}
              aria-current={isActive ? 'page' : undefined}
            >
              {isActive && (
                <motion.div
                  layoutId="nav-indicator"
                  className="absolute top-0 left-1/2 -translate-x-1/2 w-8 h-1 bg-primary rounded-b-full"
                  transition={{ type: 'spring', stiffness: 500, damping: 30 }}
                />
              )}
              <item.icon
                className={cn(
                  'w-6 h-6 mb-0.5 transition-colors',
                  isActive ? 'text-primary' : 'text-muted-foreground'
                )}
              />
              <span
                className={cn(
                  'text-[10px] font-medium transition-colors',
                  isActive ? 'text-primary' : 'text-muted-foreground'
                )}
              >
                {item.label}
              </span>
            </button>
          );
        })}
      </div>
    </nav>
  );
}
