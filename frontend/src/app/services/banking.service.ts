import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../environments/environment';
import {
  Account,
  ApiResponse,
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

  // Account operations
  getMyAccounts(): Observable<ApiResponse<Account[]>> {
    return this.http.get<ApiResponse<Account[]>>(`${this.baseUrl}/accounts/my-accounts`);
  }

  getAccountByNumber(accountNumber: string): Observable<ApiResponse<Account>> {
    return this.http.get<ApiResponse<Account>>(`${this.baseUrl}/accounts/${accountNumber}`);
  }

  // Transfer operations
  transferFunds(req: TransferRequest): Observable<ApiResponse<TransferResponse>> {
    return this.http.post<ApiResponse<TransferResponse>>(`${this.baseUrl}/transfers`, req);
  }

  // Transaction history
  getAccountTransactions(accountNumber: string): Observable<ApiResponse<Transaction[]>> {
    return this.http.get<ApiResponse<Transaction[]>>(`${this.baseUrl}/transactions/account/${accountNumber}`);
  }

  // Admin operations
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

