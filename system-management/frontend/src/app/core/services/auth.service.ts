import { Injectable, signal, computed } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Router } from '@angular/router';
import { Observable, tap } from 'rxjs';
import { environment } from '../../../environments/environment';
import { AuthResponse, CurrentUser, LoginRequest } from '../models/auth.model';

@Injectable({ providedIn: 'root' })
export class AuthService {
  private readonly apiUrl = `${environment.apiUrl}/auth`;

  private _currentUser = signal<CurrentUser | null>(null);

  readonly currentUser = this._currentUser.asReadonly();
  readonly isLoggedIn = computed(() => this._currentUser() !== null);
  readonly isAdmin = computed(() => this._currentUser()?.globalRole === 'ADMIN');

  constructor(private http: HttpClient, private router: Router) {
    const stored = sessionStorage.getItem('currentUser');
    if (stored) {
      this._currentUser.set(JSON.parse(stored));
    }
  }

  login(request: LoginRequest): Observable<AuthResponse> {
    return this.http.post<AuthResponse>(`${this.apiUrl}/login`, request, { withCredentials: true })
      .pipe(tap(resp => this.setUser(resp)));
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
    const user: CurrentUser = {
      userId: resp.userId,
      email: resp.email,
      fullName: resp.fullName,
      globalRole: resp.globalRole
    };
    this._currentUser.set(user);
    sessionStorage.setItem('currentUser', JSON.stringify(user));
  }
}
