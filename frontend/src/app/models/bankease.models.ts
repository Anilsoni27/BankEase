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
  type: string;
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
  kycStatus: 'PENDING' | 'VERIFIED' | 'REJECTED';
  accountLocked: boolean;
  dailyLimit: number;
  createdAt: string;
  accounts: Account[];
}

export interface TransferRequest {
  fromAccountNumber: string;
  toAccountNumber: string;
  amount: number;
  description?: string;
  otpCode?: string;
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

// Bill Pay Models
export interface BillPayee {
  id: number;
  payeeName: string;
  category: 'ELECTRICITY' | 'MOBILE_RECHARGE' | 'WATER' | 'BROADBAND' | 'DTH' | 'CREDIT_CARD';
  consumerNumber: string;
  nickname?: string;
  createdAt: string;
}

export interface BillPayment {
  id: number;
  payeeName: string;
  category: string;
  consumerNumber: string;
  amount: number;
  status: 'PENDING' | 'PAID' | 'SCHEDULED' | 'CANCELLED';
  isScheduled: boolean;
  scheduledDate?: string;
  paidAt?: string;
  createdAt: string;
}

// Loan Models
export interface Loan {
  id: number;
  user?: { id: number; fullName: string; email: string };
  loanType: 'PERSONAL' | 'HOME' | 'EDUCATION' | 'VEHICLE';
  principalAmount: number;
  annualInterestRate: number;
  tenureMonths: number;
  monthlyEmi: number;
  status: 'PENDING' | 'APPROVED' | 'REJECTED';
  purpose: string;
  adminRemarks?: string;
  appliedAt: string;
  reviewedAt?: string;
}

// Investment Models
export interface Investment {
  id: number;
  investmentType: 'FIXED_DEPOSIT' | 'RECURRING_DEPOSIT' | 'MUTUAL_FUND';
  planName: string;
  investedAmount: number;
  interestOrReturnRate: number;
  tenureMonths: number;
  maturityAmount: number;
  currentValuation: number;
  status: 'ACTIVE' | 'MATURED' | 'CLOSED';
  startDate: string;
  maturityDate?: string;
}

// Compliance & Audit Models
export interface AuditLog {
  id: number;
  userEmail: string;
  action: string;
  ipAddress: string;
  details: string;
  timestamp: string;
}
