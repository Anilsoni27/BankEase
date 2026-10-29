import { Injectable, signal, computed } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, tap } from 'rxjs';
import { environment } from '../../environments/environment';
import { ApiResponse, AuthResponse, LoginRequest, RegisterRequest } from '../models/bankease.models';

@Injectable({
  providedIn: 'root'
})
export class AuthService {
  private readonly baseUrl = `${environment.apiUrl}/auth`;

  // Angular Signals for state management
  readonly token = signal<string | null>(localStorage.getItem('bankease_token'));
  readonly currentUser = signal<AuthResponse | null>(null);

  readonly isLoggedIn = computed(() => !!this.token());
  readonly isAdmin = computed(() => this.currentUser()?.role === 'ADMIN');
  readonly isCustomer = computed(() => this.currentUser()?.role === 'CUSTOMER');

  constructor(private http: HttpClient) {
    if (this.token()) {
      this.verifySession().subscribe({
        error: () => this.clearSession()
      });
    }
  }

  register(req: RegisterRequest): Observable<ApiResponse<AuthResponse>> {
    return this.http.post<ApiResponse<AuthResponse>>(`${this.baseUrl}/register`, req).pipe(
      tap(res => {
        if (res.data) {
          this.setSession(res.data);
        }
      })
    );
  }

  login(req: LoginRequest): Observable<ApiResponse<AuthResponse>> {
    return this.http.post<ApiResponse<AuthResponse>>(`${this.baseUrl}/login`, req).pipe(
      tap(res => {
        if (res.data) {
          this.setSession(res.data);
        }
      })
    );
  }

  verifySession(): Observable<ApiResponse<AuthResponse>> {
    return this.http.get<ApiResponse<AuthResponse>>(`${this.baseUrl}/me`).pipe(
      tap(res => {
        if (res.data) {
          this.currentUser.set(res.data);
        }
      })
    );
  }

  logout(): Observable<ApiResponse<string>> {
    return this.http.post<ApiResponse<string>>(`${this.baseUrl}/logout`, {}).pipe(
      tap(() => this.clearSession())
    );
  }

  clearSession(): void {
    localStorage.removeItem('bankease_token');
    this.token.set(null);
    this.currentUser.set(null);
  }

  private setSession(authData: AuthResponse): void {
    localStorage.setItem('bankease_token', authData.token);
    this.token.set(authData.token);
    this.currentUser.set(authData);
  }
}
