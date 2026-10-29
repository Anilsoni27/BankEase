import { Component, OnInit, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { AuthService } from './services/auth.service';
import { BankingService } from './services/banking.service';
import {
  Account,
  Transaction,
  TransferRequest,
  UserSummary
} from './models/bankease.models';

interface Toast {
  id: number;
  message: string;
  type: 'success' | 'error' | 'info';
}

@Component({
  selector: 'app-root',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './app.html',
  styleUrl: './app.css'
})
export class App implements OnInit {
  // Navigation / Tabs
  authTab: 'login' | 'register' = 'login';
  
  // Auth Form Models
  loginData = { email: '', password: '' };
  registerData = { fullName: '', email: '', password: '' };

  // Customer State
  accounts = signal<Account[]>([]);
  activeAccount = signal<Account | null>(null);
  transactions = signal<Transaction[]>([]);

  // Transfer Form Model
  transferModel: TransferRequest = {
    fromAccountNumber: '',
    toAccountNumber: '',
    amount: 100,
    description: ''
  };
  isTransferring = signal<boolean>(false);

  // Admin State
  adminUsers = signal<UserSummary[]>([]);
  adminAccounts = signal<Account[]>([]);
  adminTransactions = signal<Transaction[]>([]);
  adminTotalLiquidity = signal<number>(0);

  // Toast Notification State
  toasts = signal<Toast[]>([]);
  private toastCounter = 0;

  constructor(
    public auth: AuthService,
    private banking: BankingService
  ) {}

  ngOnInit(): void {
    if (this.auth.isLoggedIn()) {
      this.loadDashboardData();
    }
  }

  // --------------------------------------------------------------------------
  // Auth Actions
  // --------------------------------------------------------------------------
  onLogin(): void {
    if (!this.loginData.email || !this.loginData.password) {
      this.showToast('Please enter your email and password.', 'error');
      return;
    }

    this.auth.login(this.loginData).subscribe({
      next: (res) => {
        this.showToast(`Welcome back, ${res.data.fullName}!`, 'success');
        this.loadDashboardData();
      },
      error: (err) => {
        const msg = err.error?.message || 'Login failed. Please verify credentials.';
        this.showToast(msg, 'error');
      }
    });
  }

  onRegister(): void {
    if (!this.registerData.fullName || !this.registerData.email || !this.registerData.password) {
      this.showToast('Please fill in all registration fields.', 'error');
      return;
    }

    this.auth.register(this.registerData).subscribe({
      next: (res) => {
        this.showToast(`Account created! Default SAVINGS account opened with ₹10,000 demo funds.`, 'success');
        this.loadDashboardData();
      },
      error: (err) => {
        const msg = err.error?.message || 'Registration failed.';
        this.showToast(msg, 'error');
      }
    });
  }

  onLogout(): void {
    this.auth.logout().subscribe({
      next: () => {
        this.clearLocalData();
        this.showToast('Logged out successfully.', 'info');
      },
      error: () => {
        this.clearLocalData();
        this.showToast('Session ended.', 'info');
      }
    });
  }

  private clearLocalData(): void {
    this.accounts.set([]);
    this.activeAccount.set(null);
    this.transactions.set([]);
    this.adminUsers.set([]);
    this.adminAccounts.set([]);
    this.adminTransactions.set([]);
  }

  // 1-Click Demo Fill
  fillDemo(userType: 'rahul' | 'priya' | 'admin'): void {
    this.authTab = 'login';
    if (userType === 'rahul') {
      this.loginData.email = 'rahul@bankease.com';
      this.loginData.password = 'rahul123';
    } else if (userType === 'priya') {
      this.loginData.email = 'priya@bankease.com';
      this.loginData.password = 'priya123';
    } else if (userType === 'admin') {
      this.loginData.email = 'admin@bankease.com';
      this.loginData.password = 'admin123';
    }
    this.showToast(`Demo credentials filled for ${userType.toUpperCase()}. Click 'Sign In'!`, 'info');
  }

  // --------------------------------------------------------------------------
  // Dashboard & Banking Actions
  // --------------------------------------------------------------------------
  loadDashboardData(): void {
    if (this.auth.isAdmin()) {
      this.loadAdminData();
    } else {
      this.loadCustomerAccounts();
    }
  }

  loadCustomerAccounts(): void {
    this.banking.getMyAccounts().subscribe({
      next: (res) => {
        const accs = res.data || [];
        this.accounts.set(accs);
        if (accs.length > 0) {
          const first = accs[0];
          this.activeAccount.set(first);
          this.transferModel.fromAccountNumber = first.accountNumber;
          this.loadTransactions(first.accountNumber);
        }
      },
      error: (err) => {
        this.showToast(err.error?.message || 'Failed to load accounts.', 'error');
      }
    });
  }

  loadTransactions(accountNumber: string): void {
    this.banking.getAccountTransactions(accountNumber).subscribe({
      next: (res) => {
        this.transactions.set(res.data || []);
      },
      error: (err) => {
        this.showToast(err.error?.message || 'Failed to load transactions.', 'error');
      }
    });
  }

  onTransfer(): void {
    if (!this.transferModel.toAccountNumber) {
      this.showToast('Please enter a beneficiary account number.', 'error');
      return;
    }
    if (!this.transferModel.amount || this.transferModel.amount <= 0) {
      this.showToast('Please enter a valid amount greater than 0.', 'error');
      return;
    }

    this.isTransferring.set(true);
    this.banking.transferFunds(this.transferModel).subscribe({
      next: (res) => {
        this.isTransferring.set(false);
        this.showToast(`Transfer of ₹${this.transferModel.amount} succeeded!`, 'success');
        this.transferModel.toAccountNumber = '';
        this.transferModel.description = '';
        this.loadCustomerAccounts();
      },
      error: (err) => {
        this.isTransferring.set(false);
        const msg = err.error?.message || 'Transfer failed.';
        this.showToast(msg, 'error');
        // Refresh anyway to display FAILED transaction audit row
        this.loadCustomerAccounts();
      }
    });
  }

  fillRecipient(accountNumber: string): void {
    this.transferModel.toAccountNumber = accountNumber;
  }

  // --------------------------------------------------------------------------
  // Admin Operations
  // --------------------------------------------------------------------------
  loadAdminData(): void {
    this.banking.getAdminUsers().subscribe({
      next: (res) => this.adminUsers.set(res.data || []),
      error: (err) => this.showToast(err.error?.message || 'Failed to load users.', 'error')
    });

    this.banking.getAdminAccounts().subscribe({
      next: (res) => {
        const accs = res.data || [];
        this.adminAccounts.set(accs);
        const total = accs.reduce((sum, a) => sum + (a.balance || 0), 0);
        this.adminTotalLiquidity.set(total);
      },
      error: (err) => this.showToast(err.error?.message || 'Failed to load accounts.', 'error')
    });

    this.banking.getAdminTransactions().subscribe({
      next: (res) => this.adminTransactions.set(res.data || []),
      error: (err) => this.showToast(err.error?.message || 'Failed to load audit transactions.', 'error')
    });
  }

  // --------------------------------------------------------------------------
  // Toast Helper
  // --------------------------------------------------------------------------
  showToast(message: string, type: 'success' | 'error' | 'info' = 'info'): void {
    const id = ++this.toastCounter;
    this.toasts.update(list => [...list, { id, message, type }]);

    setTimeout(() => {
      this.toasts.update(list => list.filter(t => t.id !== id));
    }, 4000);
  }
}
