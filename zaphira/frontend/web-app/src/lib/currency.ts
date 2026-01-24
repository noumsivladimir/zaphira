/**
 * Currency formatting utilities for XAF
 */

const XAF_LOCALE = 'fr-CM';

export function formatCurrency(
  amount: number,
  currency: string = 'XAF',
  options?: Intl.NumberFormatOptions
): string {
  const formatter = new Intl.NumberFormat(XAF_LOCALE, {
    style: 'currency',
    currency,
    minimumFractionDigits: 0,
    maximumFractionDigits: 0,
    ...options,
  });
  
  return formatter.format(amount);
}

export function formatAmount(amount: number): string {
  return new Intl.NumberFormat(XAF_LOCALE, {
    minimumFractionDigits: 0,
    maximumFractionDigits: 0,
  }).format(amount);
}

export function parseCurrencyInput(value: string): number {
  // Remove all non-numeric characters except decimal point
  const cleaned = value.replace(/[^\d]/g, '');
  return parseInt(cleaned, 10) || 0;
}

export function maskWalletNumber(walletNumber: string): string {
  if (walletNumber.length <= 4) return walletNumber;
  const lastFour = walletNumber.slice(-4);
  const masked = '•'.repeat(walletNumber.length - 4);
  return `${masked}${lastFour}`;
}

export function formatWalletNumber(walletNumber: string): string {
  // Format as groups of 4 for readability
  return walletNumber.replace(/(.{4})/g, '$1 ').trim();
}

export function formatPhoneNumber(phone: string): string {
  // Cameroon format: +237 6XX XXX XXX
  const cleaned = phone.replace(/\D/g, '');
  if (cleaned.startsWith('237')) {
    const number = cleaned.slice(3);
    return `+237 ${number.slice(0, 3)} ${number.slice(3, 6)} ${number.slice(6)}`;
  }
  return phone;
}
