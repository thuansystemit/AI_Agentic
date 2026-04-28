import { Injectable, signal, computed } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Router } from '@angular/router';
import { Observable, tap } from 'rxjs';
import { environment } from '../../../environments/environment';
import { AuthResponse, LoginRequest, RegisterRequest, UserInfo } from '../models/auth.model';

@Injectable({ providedIn: 'root' })
export class AuthService {
  private readonly apiUrl = `${environment.apiUrl}/auth`;
  private readonly USER_KEY = 'vp_user';

  private currentUserSignal = signal<UserInfo | null>(this.loadUserFromStorage());

  readonly currentUser = this.currentUserSignal.asReadonly();
  readonly isLoggedIn = computed(() => this.currentUserSignal() !== null);

  constructor(
    private http: HttpClient,
    private router: Router,
  ) {}

  register(request: RegisterRequest): Observable<AuthResponse> {
    return this.http.post<AuthResponse>(`${this.apiUrl}/register`, request, { withCredentials: true }).pipe(
      tap((response) => this.setUser(response.user)),
    );
  }

  login(request: LoginRequest): Observable<AuthResponse> {
    return this.http.post<AuthResponse>(`${this.apiUrl}/login`, request, { withCredentials: true }).pipe(
      tap((response) => this.setUser(response.user)),
    );
  }

  refresh(): Observable<AuthResponse> {
    return this.http.post<AuthResponse>(`${this.apiUrl}/refresh`, {}, { withCredentials: true }).pipe(
      tap((response) => this.setUser(response.user)),
    );
  }

  logout(): void {
    this.http.post(`${this.apiUrl}/logout`, {}, { withCredentials: true }).subscribe({
      complete: () => this.clearAndRedirect(),
      error: () => this.clearAndRedirect(),
    });
  }

  clearAndRedirect(): void {
    this.currentUserSignal.set(null);
    sessionStorage.removeItem(this.USER_KEY);
    this.router.navigate(['/login']);
  }

  private setUser(user: UserInfo): void {
    this.currentUserSignal.set(user);
    sessionStorage.setItem(this.USER_KEY, JSON.stringify(user));
  }

  private loadUserFromStorage(): UserInfo | null {
    const userJson = sessionStorage.getItem(this.USER_KEY);
    if (userJson) {
      try {
        return JSON.parse(userJson);
      } catch {
        return null;
      }
    }
    return null;
  }
}
