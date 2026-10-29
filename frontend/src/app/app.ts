import { Component, OnInit, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { AuthService } from './services/auth.service';
import { BankingService } from './services/banking.service';
import {
  Account,
  AuditLog,
  BillPayee,
  BillPayment,
  Investment,
  Loan,
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
  activeNavTab: 'banking' | 'billpay' | 'loans' | 'investments' | 'compliance' = 'banking';
  adminNavTab: 'overview' | 'loans' | 'audit' = 'overview';
  
  // Auth Form Models
  loginData = { email: '', password: '' };
  registerData = { fullName: '', email: '', password: '' };

  // Core Banking State
  accounts = signal<Account[]>([]);
  activeAccount = signal<Account | null>(null);
  transactions = signal<Transaction[]>([]);

  // Transfer Form Model
  transferModel: TransferRequest = {
    fromAccountNumber: '',
    toAccountNumber: '',
    amount: 500,
    description: '',
    otpCode: ''
  };
  isTransferring = signal<boolean>(false);
  showOtpInput = signal<boolean>(false);
  simulatedOtp = signal<string | null>(null);

  // Bill Pay State
  payees = signal<BillPayee[]>([]);
  billHistory = signal<BillPayment[]>([]);
  newPayee = { payeeName: '', category: 'ELECTRICITY', consumerNumber: '', nickname: '' };
  billPayModel = { accountNumber: '', payeeName: '', category: 'ELECTRICITY', consumerNumber: '', amount: 750, isScheduled: false, scheduledDate: '' };

  // Loans State
  myLoans = signal<Loan[]>([]);
  allLoans = signal<Loan[]>([]);
  loanCalculator = { principal: 50000, annualRate: 10.5, tenureMonths: 24, emi: 0, totalPay: 0, totalInterest: 0 };
  applyLoanModel = { accountNumber: '', loanType: 'PERSONAL', principal: 50000, annualRate: 10.5, tenureMonths: 24, purpose: '' };

  // Investments State
  myInvestments = signal<Investment[]>([]);
  fdCalculator = { principal: 25000, rate: 7.5, tenureMonths: 12, maturity: 0, interest: 0 };
  createInvModel = { accountNumber: '', type: 'FIXED_DEPOSIT', planName: '1-Year High Yield FD', amount: 10000, returnRate: 7.5, tenureMonths: 12 };

  // Compliance State
  kycDetails = signal<any>(null);
  dailyLimitInput = 50000;
  auditLogs = signal<AuditLog[]>([]);

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
    this.updateLoanEmiPreview();
    this.updateFdPreview();
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
        this.showToast(`Welcome back, ${res.data.fullName}! Authenticated via JWT.`, 'success');
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
    this.payees.set([]);
    this.billHistory.set([]);
    this.myLoans.set([]);
    this.allLoans.set([]);
    this.myInvestments.set([]);
    this.auditLogs.set([]);
    this.adminUsers.set([]);
    this.adminAccounts.set([]);
    this.adminTransactions.set([]);
  }

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
  // Dashboard & Module Data Loaders
  // --------------------------------------------------------------------------
  loadDashboardData(): void {
    if (this.auth.isAdmin()) {
      this.loadAdminData();
    } else {
      this.loadCustomerAccounts();
      this.loadBillPayData();
      this.loadLoansData();
      this.loadInvestmentsData();
      this.loadComplianceData();
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
          this.billPayModel.accountNumber = first.accountNumber;
          this.applyLoanModel.accountNumber = first.accountNumber;
          this.createInvModel.accountNumber = first.accountNumber;
          this.loadTransactions(first.accountNumber);
        }
      },
      error: (err) => this.showToast(err.error?.message || 'Failed to load accounts.', 'error')
    });
  }

  loadTransactions(accountNumber: string): void {
    this.banking.getAccountTransactions(accountNumber).subscribe({
      next: (res) => this.transactions.set(res.data || []),
      error: (err) => this.showToast(err.error?.message || 'Failed to load transactions.', 'error')
    });
  }

  // --------------------------------------------------------------------------
  // Fund Transfer & OTP
  // --------------------------------------------------------------------------
  onTransfer(): void {
    if (!this.transferModel.toAccountNumber) {
      this.showToast('Please enter a beneficiary account number.', 'error');
      return;
    }
    if (!this.transferModel.amount || this.transferModel.amount <= 0) {
      this.showToast('Please enter a valid amount greater than 0.', 'error');
      return;
    }

    // High value transfer check (> ₹ 25,000)
    if (this.transferModel.amount >= 25000 && !this.transferModel.otpCode) {
      this.showOtpInput.set(true);
      this.requestTransferOtp();
      return;
    }

    this.isTransferring.set(true);
    this.banking.transferFunds(this.transferModel).subscribe({
      next: () => {
        this.isTransferring.set(false);
        this.showOtpInput.set(false);
        this.showToast(`Transfer of ₹${this.transferModel.amount} succeeded!`, 'success');
        this.transferModel.toAccountNumber = '';
        this.transferModel.description = '';
        this.transferModel.otpCode = '';
        this.loadCustomerAccounts();
      },
      error: (err) => {
        this.isTransferring.set(false);
        const msg = err.error?.message || 'Transfer failed.';
        this.showToast(msg, 'error');
        this.loadCustomerAccounts();
      }
    });
  }

  requestTransferOtp(): void {
    const email = this.auth.currentUser()?.email || '';
    this.banking.sendOtp(email, 'HIGH_VALUE_TRANSFER').subscribe({
      next: (res) => {
        this.simulatedOtp.set(res.data?.otpCode || '123456');
        this.showToast(`Security OTP generated: ${res.data?.otpCode} (Valid for 5 mins)`, 'info');
      },
      error: (err) => this.showToast('Failed to generate OTP.', 'error')
    });
  }

  fillRecipient(accountNumber: string): void {
    this.transferModel.toAccountNumber = accountNumber;
  }

  // --------------------------------------------------------------------------
  // Bill Pay Methods
  // --------------------------------------------------------------------------
  loadBillPayData(): void {
    this.banking.getPayees().subscribe({
      next: (res) => this.payees.set(res.data || []),
      error: () => {}
    });
    this.banking.getBillHistory().subscribe({
      next: (res) => this.billHistory.set(res.data || []),
      error: () => {}
    });
  }

  onAddPayee(): void {
    if (!this.newPayee.payeeName || !this.newPayee.consumerNumber) {
      this.showToast('Please enter Payee Name and Consumer Number.', 'error');
      return;
    }
    this.banking.addPayee(this.newPayee).subscribe({
      next: () => {
        this.showToast('Payee added successfully!', 'success');
        this.newPayee = { payeeName: '', category: 'ELECTRICITY', consumerNumber: '', nickname: '' };
        this.loadBillPayData();
      },
      error: (err) => this.showToast(err.error?.message || 'Failed to add payee.', 'error')
    });
  }

  selectPayeeForPayment(p: BillPayee): void {
    this.billPayModel.payeeName = p.payeeName;
    this.billPayModel.category = p.category;
    this.billPayModel.consumerNumber = p.consumerNumber;
  }

  onPayBill(): void {
    if (!this.billPayModel.payeeName || !this.billPayModel.consumerNumber || this.billPayModel.amount <= 0) {
      this.showToast('Please select payee and provide valid amount.', 'error');
      return;
    }

    if (this.billPayModel.isScheduled && this.billPayModel.scheduledDate) {
      this.banking.scheduleBill(this.billPayModel as any).subscribe({
        next: () => {
          this.showToast(`Bill scheduled for ${this.billPayModel.scheduledDate}!`, 'success');
          this.loadBillPayData();
        },
        error: (err) => this.showToast(err.error?.message || 'Failed to schedule bill.', 'error')
      });
    } else {
      this.banking.payBill(this.billPayModel).subscribe({
        next: () => {
          this.showToast(`Bill payment of ₹${this.billPayModel.amount} completed!`, 'success');
          this.loadCustomerAccounts();
          this.loadBillPayData();
        },
        error: (err) => this.showToast(err.error?.message || 'Failed to pay bill.', 'error')
      });
    }
  }

  // --------------------------------------------------------------------------
  // Loans & EMI Methods
  // --------------------------------------------------------------------------
  loadLoansData(): void {
    this.banking.getMyLoans().subscribe({
      next: (res) => this.myLoans.set(res.data || []),
      error: () => {}
    });
  }

  updateLoanEmiPreview(): void {
    this.banking.calculateEmi({
      principal: this.loanCalculator.principal,
      annualRate: this.loanCalculator.annualRate,
      tenureMonths: this.loanCalculator.tenureMonths
    }).subscribe({
      next: (res) => {
        this.loanCalculator.emi = res.data?.monthlyEmi || 0;
        this.loanCalculator.totalPay = res.data?.totalPayment || 0;
        this.loanCalculator.totalInterest = res.data?.totalInterest || 0;
      }
    });
  }

  onApplyLoan(): void {
    if (!this.applyLoanModel.purpose) {
      this.showToast('Please enter the loan purpose.', 'error');
      return;
    }
    this.banking.applyLoan(this.applyLoanModel).subscribe({
      next: () => {
        this.showToast('Loan application submitted for administrator review!', 'success');
        this.applyLoanModel.purpose = '';
        this.loadLoansData();
      },
      error: (err) => this.showToast(err.error?.message || 'Loan application failed.', 'error')
    });
  }

  // --------------------------------------------------------------------------
  // Investments (FD/RD/MF) Methods
  // --------------------------------------------------------------------------
  loadInvestmentsData(): void {
    this.banking.getMyInvestments().subscribe({
      next: (res) => this.myInvestments.set(res.data || []),
      error: () => {}
    });
  }

  updateFdPreview(): void {
    this.banking.calculateFd({
      principal: this.fdCalculator.principal,
      rate: this.fdCalculator.rate,
      tenureMonths: this.fdCalculator.tenureMonths
    }).subscribe({
      next: (res) => {
        this.fdCalculator.maturity = res.data?.maturityAmount || 0;
        this.fdCalculator.interest = res.data?.interestEarned || 0;
      }
    });
  }

  onCreateInvestment(): void {
    if (this.createInvModel.amount <= 0) {
      this.showToast('Please enter an investment amount > 0.', 'error');
      return;
    }
    this.banking.createInvestment(this.createInvModel).subscribe({
      next: () => {
        this.showToast('Investment initiated successfully!', 'success');
        this.loadCustomerAccounts();
        this.loadInvestmentsData();
      },
      error: (err) => this.showToast(err.error?.message || 'Investment failed.', 'error')
    });
  }

  onCloseInvestment(inv: Investment): void {
    if (!confirm(`Are you sure you want to close and redeem ${inv.planName}? Funds will be credited immediately.`)) {
      return;
    }
    this.banking.closeInvestment(inv.id).subscribe({
      next: () => {
        this.showToast(`Investment closed! ₹${inv.maturityAmount} credited back.`, 'success');
        this.loadCustomerAccounts();
        this.loadInvestmentsData();
      },
      error: (err) => this.showToast(err.error?.message || 'Closure failed.', 'error')
    });
  }

  // --------------------------------------------------------------------------
  // Compliance & Security Methods
  // --------------------------------------------------------------------------
  loadComplianceData(): void {
    this.banking.getKycStatus().subscribe({
      next: (res) => {
        this.kycDetails.set(res.data);
        if (res.data?.dailyLimit) {
          this.dailyLimitInput = res.data.dailyLimit;
        }
      }
    });
    this.banking.getAuditLogs().subscribe({
      next: (res) => this.auditLogs.set(res.data || [])
    });
  }

  onUpdateKyc(): void {
    this.banking.updateKyc('Aadhaar / Passport Card', 'AADHAAR-8839-2910-4491').subscribe({
      next: () => {
        this.showToast('KYC documents verified successfully!', 'success');
        this.loadComplianceData();
      },
      error: (err) => this.showToast(err.error?.message || 'KYC update failed.', 'error')
    });
  }

  onUpdateDailyLimit(): void {
    this.banking.updateDailyLimit(this.dailyLimitInput).subscribe({
      next: () => {
        this.showToast(`Daily transfer limit updated to ₹${this.dailyLimitInput}!`, 'success');
        this.loadComplianceData();
      },
      error: (err) => this.showToast(err.error?.message || 'Limit update failed.', 'error')
    });
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

    this.banking.getAllLoans().subscribe({
      next: (res) => this.allLoans.set(res.data || []),
      error: () => {}
    });

    this.banking.getAuditLogs().subscribe({
      next: (res) => this.auditLogs.set(res.data || []),
      error: () => {}
    });
  }

  onApproveLoan(loan: Loan): void {
    this.banking.approveLoan(loan.id, 'Approved by Chief Loan Officer.').subscribe({
      next: () => {
        this.showToast(`Loan #${loan.id} approved! ₹${loan.principalAmount} disbursed directly to user.`, 'success');
        this.loadAdminData();
      },
      error: (err) => this.showToast(err.error?.message || 'Approval failed.', 'error')
    });
  }

  onRejectLoan(loan: Loan): void {
    this.banking.rejectLoan(loan.id, 'Risk criteria not met.').subscribe({
      next: () => {
        this.showToast(`Loan #${loan.id} marked as REJECTED.`, 'info');
        this.loadAdminData();
      },
      error: (err) => this.showToast(err.error?.message || 'Rejection failed.', 'error')
    });
  }

  onUnlockUser(user: UserSummary): void {
    this.banking.unlockUser(user.id).subscribe({
      next: () => {
        this.showToast(`Account for ${user.email} has been UNLOCKED.`, 'success');
        this.loadAdminData();
      },
      error: (err) => this.showToast(err.error?.message || 'Unlock failed.', 'error')
    });
  }

  onVerifyKyc(user: UserSummary, status: 'VERIFIED' | 'REJECTED'): void {
    this.banking.adminVerifyKyc(user.id, status).subscribe({
      next: () => {
        this.showToast(`KYC status for ${user.fullName} updated to ${status}.`, 'success');
        this.loadAdminData();
      },
      error: (err) => this.showToast(err.error?.message || 'Failed to update KYC status.', 'error')
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
    }, 4500);
  }
}
