import React, { useState, useEffect, useRef } from 'react';
import { cn } from '@/lib/utils';
import { formatAmount, parseCurrencyInput } from '@/lib/currency';

interface CurrencyInputProps {
  value: number;
  onChange: (value: number) => void;
  currency?: string;
  max?: number;
  min?: number;
  placeholder?: string;
  error?: string;
  disabled?: boolean;
  autoFocus?: boolean;
  className?: string;
}

export function CurrencyInput({
  value,
  onChange,
  currency = 'XAF',
  max,
  min = 0,
  placeholder = '0',
  error,
  disabled = false,
  autoFocus = false,
  className,
}: CurrencyInputProps) {
  const [displayValue, setDisplayValue] = useState(value > 0 ? formatAmount(value) : '');
  const inputRef = useRef<HTMLInputElement>(null);

  useEffect(() => {
    if (autoFocus && inputRef.current) {
      inputRef.current.focus();
    }
  }, [autoFocus]);

  useEffect(() => {
    // Sync external value changes
    if (value === 0 && displayValue !== '') {
      setDisplayValue('');
    } else if (value > 0) {
      const parsed = parseCurrencyInput(displayValue);
      if (parsed !== value) {
        setDisplayValue(formatAmount(value));
      }
    }
  }, [value]);

  const handleChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    const input = e.target.value;
    const numericValue = parseCurrencyInput(input);

    // Apply limits
    let finalValue = numericValue;
    if (max !== undefined && numericValue > max) {
      finalValue = max;
    }
    if (numericValue < min) {
      finalValue = min;
    }

    setDisplayValue(finalValue > 0 ? formatAmount(finalValue) : '');
    onChange(finalValue);
  };

  return (
    <div className={cn('flex flex-col', className)}>
      <div className="relative flex items-center justify-center">
        <input
          ref={inputRef}
          type="text"
          inputMode="numeric"
          value={displayValue}
          onChange={handleChange}
          placeholder={placeholder}
          disabled={disabled}
          className={cn(
            'currency-display text-4xl sm:text-5xl w-full text-center py-4',
            'bg-transparent border-none outline-none',
            'placeholder:text-muted-foreground/50',
            error && 'text-destructive',
            disabled && 'opacity-50 cursor-not-allowed'
          )}
          aria-label="Amount"
        />
      </div>
      
      <p className="text-center text-muted-foreground text-sm mt-1">
        {currency}
      </p>

      {error && (
        <p className="text-destructive text-sm text-center mt-2">{error}</p>
      )}
    </div>
  );
}
