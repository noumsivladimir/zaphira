/**
 * Zaphira Backend Types
 * Generated from Spring Boot DTOs
 */

// ============================================
// Authentication Types
// ============================================

export interface LoginRequest {
  phoneNumber: string;
  pin: string;
}

export interface LoginResponse {
  accessToken: string;
  refreshToken: string;
  tokenType: string;
  expiresIn: number;
  userId: number;
  phoneNumber: string;
  firstName?: string;
  lastName?: string;
}

export interface RefreshTokenRequest {
  refreshToken: string;
}

export interface RefreshTokenResponse {
  accessToken: string;
  refreshToken: string;
  tokenType: string;
  expiresIn: number;
}

// ============================================
// User Registration Types
// ============================================

export interface UserRegistrationRequest {
  phoneNumber: string;
  pin: string;
  email: string;
  firstName: string;
  lastName: string;
  dateOfBirth?: string;
  country?: string;
  city?: string;
  neighborhood?: string;
  region?: string;
  preferredLanguage?: string;
}

export interface UserRegistrationResponse {
  userId: number;
  phoneNumber: string;
  email: string;
  firstName: string;
  lastName: string;
  walletId?: string;
  registrationDate: string;
  accountStatus: AccountStatus;
  roleType: RoleType;
  dateOfBirth?: string;
  country?: string;
  preferredCurrency?: string;
}

export interface EmailVerificationRequest {
  userId: number;
  verificationCode: string;
}

export interface EmailVerificationResponse {
  success: boolean;
  message: string;
  user?: UserResponse;
  wallet?: WalletResponse;
}

// ============================================
// User Types
// ============================================

export interface UserResponse {
  userId: number;
  phoneNumber: string;
  email?: string;
  firstName: string;
  lastName: string;
  walletId?: string;
  accountStatus: AccountStatus;
  roleType: RoleType;
  registrationDate: string;
  dateOfBirth?: string;
  country?: string;
  city?: string;
  neighborhood?: string;
  region?: string;
  preferredLanguage?: string;
  preferredCurrency?: string;
}

export interface UpdateUserProfileRequest {
  firstName?: string;
  lastName?: string;
  email?: string;
  dateOfBirth?: string;
  country?: string;
  city?: string;
  neighborhood?: string;
  region?: string;
  preferredLanguage?: string;
}

// ============================================
// Wallet Types
// ============================================

export interface WalletResponse {
  walletId: number;
  walletNumber: string;
  userId: number;
  balance: number;
  currency: string;
  status: WalletStatus;
  isPrimary: boolean;
  createdAt: string;
  updatedAt: string;
}

export interface CreateWalletRequest {
  userId: number;
  currency?: string;
}

// ============================================
// Transaction Types
// ============================================

export interface TransactionRequest {
  senderWalletNumber: string;
  receiverWalletNumber: string;
  amount: number;
  currency: string;
  description?: string;
  pin: string;
}

export interface TransactionResponse {
  transactionId: number;
  reference: string;
  senderWalletNumber: string;
  receiverWalletNumber: string;
  amount: number;
  currency: string;
  fees: number;
  totalAmount: number;
  status: TransactionStatus;
  description?: string;
  createdAt: string;
  completedAt?: string;
}

export interface TransactionHistoryRequest {
  walletNumber: string;
  page?: number;
  size?: number;
  startDate?: string;
  endDate?: string;
}

export interface TransactionHistoryResponse {
  transactions: TransactionResponse[];
  totalElements: number;
  totalPages: number;
  currentPage: number;
  pageSize: number;
}

// ============================================
// Enums
// ============================================

export enum AccountStatus {
  ACTIVE = 'ACTIVE',
  PENDING_VERIFICATION = 'PENDING_VERIFICATION',
  SUSPENDED = 'SUSPENDED',
  BLOCKED = 'BLOCKED',
  INACTIVE = 'INACTIVE'
}

export enum RoleType {
  REGULAR_USER = 'REGULAR_USER',
  MERCHANT_USER = 'MERCHANT_USER',
  ADMIN_USER = 'ADMIN_USER'
}

export enum WalletStatus {
  ACTIVE = 'ACTIVE',
  FROZEN = 'FROZEN',
  CLOSED = 'CLOSED'
}

export enum TransactionStatus {
  PENDING = 'PENDING',
  COMPLETED = 'COMPLETED',
  FAILED = 'FAILED',
  REVERSED = 'REVERSED',
  REFUNDED = 'REFUNDED'
}

// ============================================
// API Response Wrapper
// ============================================

export interface ApiSuccessResponse<T> {
  data: T;
  success: true;
  timestamp: string;
}

export interface ApiErrorResponse {
  success: false;
  error: {
    message: string;
    code: string;
    details?: Record<string, string[]>;
  };
  timestamp: string;
}

export type ApiResponse<T> = ApiSuccessResponse<T> | ApiErrorResponse;

// ============================================
// OTP Types
// ============================================

export interface SendOtpRequest {
  phoneNumber: string;
  purpose: OtpPurpose;
}

export interface VerifyOtpRequest {
  phoneNumber: string;
  code: string;
  purpose: OtpPurpose;
}

export enum OtpPurpose {
  REGISTRATION = 'REGISTRATION',
  LOGIN = 'LOGIN',
  PIN_RESET = 'PIN_RESET',
  TRANSACTION = 'TRANSACTION'
}

// ============================================
// Pagination Types
// ============================================

export interface PageRequest {
  page?: number;
  size?: number;
  sort?: string;
}

export interface PageResponse<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  currentPage: number;
  pageSize: number;
  first: boolean;
  last: boolean;
}
