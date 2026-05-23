import { Injectable, signal, computed, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Router } from '@angular/router';
import { Observable, tap } from 'rxjs';
import { environment } from '../../../environments/environment';
import {
  AuthResponse, ChangePasswordRequest, CurrentUser, LoginRequest,
  ProfileResponse, UpdateProfileRequest
} from '../models/auth.model';

@Injectable({ providedIn: 'root' })
export class AuthService {
  private readonly http = inject(HttpClient);
  private readonly router = inject(Router);
  private readonly apiUrl = `${environment.apiUrl}/auth`;

  private _currentUser = signal<CurrentUser | null>(null);

  readonly currentUser = this._currentUser.asReadonly();
  readonly isLoggedIn  = computed(() => this._currentUser() !== null);
  readonly isAdmin     = computed(() => this._currentUser()?.globalRole === 'ADMIN');

  constructor() {
    const stored = sessionStorage.getItem('currentUser');
    if (stored) {
      this._currentUser.set(JSON.parse(stored));
    }
  }

  login(request: LoginRequest): Observable<AuthResponse> {
    return this.http.post<AuthResponse>(`${this.apiUrl}/login`, request, { withCredentials: true })
      .pipe(tap(resp => this.setUser(resp)));
  }

  getProfile(): Observable<ProfileResponse> {
    return this.http.get<ProfileResponse>(`${this.apiUrl}/me`, { withCredentials: true });
  }

  updateProfile(request: UpdateProfileRequest): Observable<ProfileResponse> {
    return this.http.patch<ProfileResponse>(`${this.apiUrl}/me`, request, { withCredentials: true })
      .pipe(tap(resp => this.setUserFromProfile(resp)));
  }

  changePassword(request: ChangePasswordRequest): Observable<void> {
    return this.http.post<void>(`${this.apiUrl}/me/password`, request, { withCredentials: true });
  }

  refresh(): Observable<AuthResponse> {
    return this.http.post<AuthResponse>(`${this.apiUrl}/refresh`, {}, { withCredentials: true })
      .pipe(tap(resp => this.setUser(resp)));
  }

  logout(): void {
    this.http.post(`${this.apiUrl}/logout`, {}, { withCredentials: true }).subscribe({
      complete: () => this.clearAndRedirect()
    });
  }

  clearAndRedirect(): void {
    this._currentUser.set(null);
    sessionStorage.removeItem('currentUser');
    this.router.navigate(['/login']);
  }

  private setUser(resp: AuthResponse): void {
    const existing = this._currentUser();
    const user: CurrentUser = {
      userId:     resp.userId,
      email:      resp.email,
      fullName:   resp.fullName,
      bio:        existing?.bio ?? null,
      globalRole: resp.globalRole
    };
    this._currentUser.set(user);
    sessionStorage.setItem('currentUser', JSON.stringify(user));
  }

  private setUserFromProfile(resp: ProfileResponse): void {
    const user: CurrentUser = {
      userId:     resp.userId,
      email:      resp.email,
      fullName:   resp.fullName,
      bio:        resp.bio,
      globalRole: resp.globalRole
    };
    this._currentUser.set(user);
    sessionStorage.setItem('currentUser', JSON.stringify(user));
  }
}
