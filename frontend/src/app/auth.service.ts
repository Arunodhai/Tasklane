import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { BehaviorSubject, catchError, Observable, of, tap } from 'rxjs';
import { AuthResponse, LoginRequest, RegisterRequest, UserAccount } from './model/user.model';

@Injectable({ providedIn: 'root' })
export class AuthService {
  private readonly apiUrl = '/api/auth';
  private readonly tokenKey = 'tasklane_token';
  private readonly userSubject = new BehaviorSubject<UserAccount | null>(null);

  readonly currentUser$ = this.userSubject.asObservable();

  constructor(private readonly http: HttpClient) {}

  get token(): string | null {
    return localStorage.getItem(this.tokenKey);
  }

  get currentUser(): UserAccount | null {
    return this.userSubject.value;
  }

  restoreSession(): Observable<UserAccount | null> {
    if (!this.token) {
      return of(null);
    }
    return this.http.get<UserAccount>(`${this.apiUrl}/me`).pipe(
      tap(user => this.userSubject.next(user)),
      catchError(() => {
        this.logout();
        return of(null);
      })
    );
  }

  login(request: LoginRequest): Observable<AuthResponse> {
    return this.http.post<AuthResponse>(`${this.apiUrl}/login`, request).pipe(
      tap(response => this.storeSession(response))
    );
  }

  register(request: RegisterRequest): Observable<AuthResponse> {
    return this.http.post<AuthResponse>(`${this.apiUrl}/register`, request).pipe(
      tap(response => this.storeSession(response))
    );
  }

  logout(): void {
    localStorage.removeItem(this.tokenKey);
    this.userSubject.next(null);
  }

  private storeSession(response: AuthResponse): void {
    localStorage.setItem(this.tokenKey, response.token);
    this.userSubject.next(response.user);
  }
}
