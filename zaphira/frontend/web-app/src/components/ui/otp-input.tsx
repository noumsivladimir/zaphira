import React, { useRef, useState, useEffect } from 'react';
import { motion } from 'framer-motion';
import { cn } from '@/lib/utils';

interface OTPInputProps {
  length?: number;
  onComplete: (otp: string) => void;
  onChange?: (otp: string) => void;
  error?: string;
  loading?: boolean;
  autoFocus?: boolean;
  className?: string;
}

export function OTPInput({
  length = 6,
  onComplete,
  onChange,
  error,
  loading = false,
  autoFocus = true,
  className,
}: OTPInputProps) {
  const [values, setValues] = useState<string[]>(Array(length).fill(''));
  const inputRefs = useRef<(HTMLInputElement | null)[]>([]);

  useEffect(() => {
    if (autoFocus && inputRefs.current[0]) {
      inputRefs.current[0].focus();
    }
  }, [autoFocus]);

  // Clear on error
  useEffect(() => {
    if (error) {
      setValues(Array(length).fill(''));
      inputRefs.current[0]?.focus();
    }
  }, [error, length]);

  const handleChange = (index: number, value: string) => {
    if (loading) return;

    // Only allow digits
    const digit = value.replace(/\D/g, '').slice(-1);
    
    const newValues = [...values];
    newValues[index] = digit;
    setValues(newValues);

    // Move to next input
    if (digit && index < length - 1) {
      inputRefs.current[index + 1]?.focus();
    }

    // Check if complete
    const otp = newValues.join('');
    onChange?.(otp);
    if (otp.length === length && !otp.includes('')) {
      onComplete(otp);
    }
  };

  const handleKeyDown = (index: number, e: React.KeyboardEvent) => {
    if (e.key === 'Backspace' && !values[index] && index > 0) {
      inputRefs.current[index - 1]?.focus();
    }
  };

  const handlePaste = (e: React.ClipboardEvent) => {
    e.preventDefault();
    const pastedData = e.clipboardData.getData('text').replace(/\D/g, '').slice(0, length);
    
    if (pastedData) {
      const newValues = Array(length).fill('');
      pastedData.split('').forEach((digit, i) => {
        newValues[i] = digit;
      });
      setValues(newValues);

      // Focus last filled or first empty
      const focusIndex = Math.min(pastedData.length, length - 1);
      inputRefs.current[focusIndex]?.focus();

      if (pastedData.length === length) {
        onComplete(pastedData);
      }
      onChange?.(pastedData);
    }
  };

  return (
    <div className={cn('flex flex-col items-center', className)}>
      <div className="flex gap-2 sm:gap-3">
        {values.map((value, index) => (
          <motion.input
            key={index}
            ref={(el) => (inputRefs.current[index] = el)}
            type="text"
            inputMode="numeric"
            pattern="[0-9]*"
            maxLength={1}
            value={value}
            onChange={(e) => handleChange(index, e.target.value)}
            onKeyDown={(e) => handleKeyDown(index, e)}
            onPaste={handlePaste}
            disabled={loading}
            className={cn(
              'otp-input',
              error && 'border-destructive focus:border-destructive focus:ring-destructive/20'
            )}
            aria-label={`Digit ${index + 1}`}
            initial={false}
            animate={error ? { x: [-5, 5, -5, 5, 0] } : {}}
            transition={{ duration: 0.3 }}
          />
        ))}
      </div>

      {error && (
        <motion.p
          initial={{ opacity: 0, y: -10 }}
          animate={{ opacity: 1, y: 0 }}
          className="text-destructive text-sm mt-3"
        >
          {error}
        </motion.p>
      )}
    </div>
  );
}
