import React, { useState, useCallback, useEffect } from 'react';
import { motion, AnimatePresence } from 'framer-motion';
import { Delete, Fingerprint } from 'lucide-react';
import { cn } from '@/lib/utils';

interface SecureNumberPadProps {
  length?: number;
  onComplete: (value: string) => void;
  onCancel?: () => void;
  title?: string;
  subtitle?: string;
  error?: string;
  loading?: boolean;
  showBiometric?: boolean;
  onBiometric?: () => void;
  className?: string;
}

export function SecureNumberPad({
  length = 4,
  onComplete,
  onCancel,
  title,
  subtitle,
  error,
  loading = false,
  showBiometric = false,
  onBiometric,
  className,
}: SecureNumberPadProps) {
  const [value, setValue] = useState('');
  const [shake, setShake] = useState(false);

  // Clear value on error
  useEffect(() => {
    if (error) {
      setShake(true);
      setTimeout(() => {
        setShake(false);
        setValue('');
      }, 500);
    }
  }, [error]);

  const handleKeyPress = useCallback(
    (key: string) => {
      if (loading) return;
      
      if (key === 'delete') {
        setValue((prev) => prev.slice(0, -1));
      } else if (value.length < length) {
        const newValue = value + key;
        setValue(newValue);
        
        if (newValue.length === length) {
          // Small delay for visual feedback
          setTimeout(() => {
            onComplete(newValue);
          }, 150);
        }
      }
    },
    [value, length, loading, onComplete]
  );

  const keys = [
    ['1', '2', '3'],
    ['4', '5', '6'],
    ['7', '8', '9'],
    [showBiometric ? 'biometric' : '', '0', 'delete'],
  ];

  return (
    <div className={cn('flex flex-col items-center justify-center px-6', className)}>
      {/* Title */}
      {title && (
        <h2 className="text-xl font-semibold text-foreground mb-2">{title}</h2>
      )}
      {subtitle && (
        <p className="text-sm text-muted-foreground mb-6">{subtitle}</p>
      )}

      {/* PIN dots */}
      <motion.div
        className="flex gap-4 mb-8"
        animate={shake ? { x: [-10, 10, -10, 10, 0] } : {}}
        transition={{ duration: 0.4 }}
      >
        {Array.from({ length }).map((_, i) => (
          <motion.div
            key={i}
            className={cn(
              'w-4 h-4 rounded-full border-2 transition-all duration-200',
              i < value.length
                ? 'bg-primary border-primary'
                : 'border-muted-foreground/30'
            )}
            initial={false}
            animate={
              i === value.length - 1 && value.length > 0
                ? { scale: [1, 1.3, 1] }
                : {}
            }
            transition={{ duration: 0.15 }}
          />
        ))}
      </motion.div>

      {/* Error message */}
      <AnimatePresence>
        {error && (
          <motion.p
            initial={{ opacity: 0, y: -10 }}
            animate={{ opacity: 1, y: 0 }}
            exit={{ opacity: 0, y: -10 }}
            className="text-destructive text-sm mb-4"
          >
            {error}
          </motion.p>
        )}
      </AnimatePresence>

      {/* Number pad */}
      <div className="grid grid-cols-3 gap-4">
        {keys.flat().map((key, index) => {
          if (key === '') {
            return <div key={index} className="w-16 h-16" />;
          }

          if (key === 'biometric') {
            return (
              <button
                key={index}
                onClick={onBiometric}
                disabled={loading}
                className="number-pad-key"
                aria-label="Use biometric authentication"
              >
                <Fingerprint className="w-6 h-6" />
              </button>
            );
          }

          if (key === 'delete') {
            return (
              <button
                key={index}
                onClick={() => handleKeyPress('delete')}
                disabled={loading || value.length === 0}
                className="number-pad-key"
                aria-label="Delete"
              >
                <Delete className="w-6 h-6" />
              </button>
            );
          }

          return (
            <motion.button
              key={index}
              onClick={() => handleKeyPress(key)}
              disabled={loading}
              className="number-pad-key"
              whileTap={{ scale: 0.95 }}
              aria-label={key}
            >
              {key}
            </motion.button>
          );
        })}
      </div>

      {/* Cancel button */}
      {onCancel && (
        <button
          onClick={onCancel}
          className="mt-8 text-muted-foreground hover:text-foreground transition-colors touch-target"
        >
          Annuler
        </button>
      )}

      {/* Loading overlay */}
      <AnimatePresence>
        {loading && (
          <motion.div
            initial={{ opacity: 0 }}
            animate={{ opacity: 1 }}
            exit={{ opacity: 0 }}
            className="absolute inset-0 bg-background/80 backdrop-blur-sm flex items-center justify-center"
          >
            <div className="w-12 h-12 border-4 border-primary border-t-transparent rounded-full animate-spin" />
          </motion.div>
        )}
      </AnimatePresence>
    </div>
  );
}
