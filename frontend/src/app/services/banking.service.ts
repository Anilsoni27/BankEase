import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../environments/environment';
import {
  Account,
  ApiResponse,
  AuditLog,
  BillPayee,
  BillPayment,
  Investment,
  Loan,
  Transaction,
  TransferRequest,
  TransferResponse,
  UserSummary
} from '../models/bankease.models';

@Injectable({
  providedIn: 'root'
})
export class BankingService {
  private readonly baseUrl = environment.apiUrl;

  constructor(private http: HttpClient) {}

  // 1. Account operations
  getMyAccounts(): Observable<ApiResponse<Account[]>> {
    return this.http.get<ApiResponse<Account[]>>(`${this.baseUrl}/accounts/my-accounts`);
  }

  getAccountByNumber(accountNumber: string): Observable<ApiResponse<Account>> {
    return this.http.get<ApiResponse<Account>>(`${this.baseUrl}/accounts/${accountNumber}`);
  }

  // 2. Transfer operations
  transferFunds(req: TransferRequest): Observable<ApiResponse<TransferResponse>> {
    return this.http.post<ApiResponse<TransferResponse>>(`${this.baseUrl}/transfers`, req);
  }

  getAccountTransactions(accountNumber: string): Observable<ApiResponse<Transaction[]>> {
    return this.http.get<ApiResponse<Transaction[]>>(`${this.baseUrl}/transactions/account/${accountNumber}`);
  }

  // 3. Bill Pay
  getPayees(): Observable<ApiResponse<BillPayee[]>> {
    return this.http.get<ApiResponse<BillPayee[]>>(`${this.baseUrl}/bills/payees`);
  }

  addPayee(payee: { payeeName: string; category: string; consumerNumber: string; nickname?: string }): Observable<ApiResponse<BillPayee>> {
    return this.http.post<ApiResponse<BillPayee>>(`${this.baseUrl}/bills/payees`, payee);
  }

  payBill(data: { accountNumber: string; payeeName: string; category: string; consumerNumber: string; amount: number }): Observable<ApiResponse<BillPayment>> {
    return this.http.post<ApiResponse<BillPayment>>(`${this.baseUrl}/bills/pay`, data);
  }

  scheduleBill(data: { accountNumber: string; payeeName: string; category: string; consumerNumber: string; amount: number; scheduledDate: string }): Observable<ApiResponse<BillPayment>> {
    return this.http.post<ApiResponse<BillPayment>>(`${this.baseUrl}/bills/schedule`, data);
  }

  getBillHistory(): Observable<ApiResponse<BillPayment[]>> {
    return this.http.get<ApiResponse<BillPayment[]>>(`${this.baseUrl}/bills/history`);
  }

  // 4. Loans & EMI
  calculateEmi(data: { principal: number; annualRate: number; tenureMonths: number }): Observable<ApiResponse<any>> {
    return this.http.post<ApiResponse<any>>(`${this.baseUrl}/loans/calculate-emi`, data);
  }

  applyLoan(loan: { accountNumber: string; loanType: string; principal: number; annualRate: number; tenureMonths: number; purpose: string }): Observable<ApiResponse<Loan>> {
    return this.http.post<ApiResponse<Loan>>(`${this.baseUrl}/loans/apply`, loan);
  }

  getMyLoans(): Observable<ApiResponse<Loan[]>> {
    return this.http.get<ApiResponse<Loan[]>>(`${this.baseUrl}/loans/my-loans`);
  }

  getAllLoans(): Observable<ApiResponse<Loan[]>> {
    return this.http.get<ApiResponse<Loan[]>>(`${this.baseUrl}/loans/all`);
  }

  approveLoan(loanId: number, remarks: string = 'Approved'): Observable<ApiResponse<Loan>> {
    return this.http.put<ApiResponse<Loan>>(`${this.baseUrl}/loans/${loanId}/approve`, { remarks });
  }

  rejectLoan(loanId: number, remarks: string = 'Rejected'): Observable<ApiResponse<Loan>> {
    return this.http.put<ApiResponse<Loan>>(`${this.baseUrl}/loans/${loanId}/reject`, { remarks });
  }

  // 5. Investments (FD/RD/MF)
  calculateFd(data: { principal: number; rate: number; tenureMonths: number }): Observable<ApiResponse<any>> {
    return this.http.post<ApiResponse<any>>(`${this.baseUrl}/investments/calculate-fd`, data);
  }

  createInvestment(data: { accountNumber: string; type: string; planName: string; amount: number; returnRate: number; tenureMonths: number }): Observable<ApiResponse<Investment>> {
    return this.http.post<ApiResponse<Investment>>(`${this.baseUrl}/investments/create`, data);
  }

  getMyInvestments(): Observable<ApiResponse<Investment[]>> {
    return this.http.get<ApiResponse<Investment[]>>(`${this.baseUrl}/investments/my-investments`);
  }

  closeInvestment(id: number): Observable<ApiResponse<Investment>> {
    return this.http.post<ApiResponse<Investment>>(`${this.baseUrl}/investments/${id}/close`, {});
  }

  // 6. Compliance & Risk
  sendOtp(email: string, purpose: string): Observable<ApiResponse<any>> {
    return this.http.post<ApiResponse<any>>(`${this.baseUrl}/compliance/send-otp`, { email, purpose });
  }

  verifyOtp(email: string, otpCode: string, purpose: string): Observable<ApiResponse<boolean>> {
    return this.http.post<ApiResponse<boolean>>(`${this.baseUrl}/compliance/verify-otp`, { email, otpCode, purpose });
  }

  getKycStatus(): Observable<ApiResponse<any>> {
    return this.http.get<ApiResponse<any>>(`${this.baseUrl}/compliance/kyc`);
  }

  updateKyc(documentType: string, documentNumber: string): Observable<ApiResponse<any>> {
    return this.http.post<ApiResponse<any>>(`${this.baseUrl}/compliance/update-kyc`, { documentType, documentNumber });
  }

  updateDailyLimit(dailyLimit: number): Observable<ApiResponse<any>> {
    return this.http.post<ApiResponse<any>>(`${this.baseUrl}/compliance/update-limit`, { dailyLimit });
  }

  getAuditLogs(): Observable<ApiResponse<AuditLog[]>> {
    return this.http.get<ApiResponse<AuditLog[]>>(`${this.baseUrl}/compliance/audit-logs`);
  }

  unlockUser(userId: number): Observable<ApiResponse<any>> {
    return this.http.post<ApiResponse<any>>(`${this.baseUrl}/compliance/unlock-user/${userId}`, {});
  }

  adminVerifyKyc(userId: number, status: string): Observable<ApiResponse<any>> {
    return this.http.post<ApiResponse<any>>(`${this.baseUrl}/compliance/admin/verify-kyc/${userId}`, { status });
  }

  // 7. Admin Queries
  getAdminUsers(): Observable<ApiResponse<UserSummary[]>> {
    return this.http.get<ApiResponse<UserSummary[]>>(`${this.baseUrl}/admin/users`);
  }

  getAdminAccounts(): Observable<ApiResponse<Account[]>> {
    return this.http.get<ApiResponse<Account[]>>(`${this.baseUrl}/admin/accounts`);
  }

  getAdminTransactions(): Observable<ApiResponse<Transaction[]>> {
    return this.http.get<ApiResponse<Transaction[]>>(`${this.baseUrl}/admin/transactions`);
  }
}
