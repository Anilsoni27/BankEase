export interface ApiResponse<T> {
  success: boolean;
  message: string;
  data: T;
  timestamp?: string;
}

export interface Account {
  id: number;
  accountNumber: string;
  accountType: 'SAVINGS' | 'CURRENT';
  balance: number;
  ownerName: string;
  ownerEmail: string;
  createdAt: string;
}

export interface Transaction {
  id: number;
  fromAccountNumber: string;
  fromOwnerName: string;
  toAccountNumber: string;
  toOwnerName: string;
  amount: number;
  type: 'TRANSFER' | 'DEPOSIT';
  status: 'SUCCESS' | 'FAILED';
  description: string;
  createdAt: string;
}

export interface AuthResponse {
  token: string;
  userId: number;
  fullName: string;
  email: string;
  role: 'CUSTOMER' | 'ADMIN';
  accounts: Account[];
}

export interface UserSummary {
  id: number;
  fullName: string;
  email: string;
  role: 'CUSTOMER' | 'ADMIN';
  createdAt: string;
  accounts: Account[];
}

export interface TransferRequest {
  fromAccountNumber: string;
  toAccountNumber: string;
  amount: number;
  description?: string;
}

export interface TransferResponse {
  transactionId: number;
  status: string;
  message: string;
  fromAccountNumber: string;
  toAccountNumber: string;
  amount: number;
  remainingBalance: number;
  timestamp: string;
}

export interface LoginRequest {
  email: string;
  password: string;
}

export interface RegisterRequest {
  fullName: string;
  email: string;
  password: string;
}

