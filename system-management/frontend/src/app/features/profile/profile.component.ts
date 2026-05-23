import { Component, computed, effect, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { rxResource } from '@angular/core/rxjs-interop';
import { MatButtonModule } from '@angular/material/button';
import { MatCardModule } from '@angular/material/card';
import { MatDividerModule } from '@angular/material/divider';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { AuthService } from '../../core/services/auth.service';
import { FieldComponent } from '../../shared/components/field/field.component';
import {
  composeValidators, minLength, required, textField
} from '../../shared/signal-form';

@Component({
  selector: 'app-profile',
  standalone: true,
  imports: [
    FormsModule,
    MatButtonModule, MatCardModule, MatDividerModule,
    MatIconModule, MatProgressSpinnerModule, MatSnackBarModule,
    FieldComponent
  ],
  templateUrl: './profile.component.html',
  styleUrl: './profile.component.scss'
})
export class ProfileComponent {
  private authService = inject(AuthService);
  private snackBar    = inject(MatSnackBar);

  // ── Profile form ────────────────────────────────────────────────────────────
  fullName = textField('', required('Full name'));
  bio      = textField('');

  profileLoading = signal(false);
  profileValid   = computed(() => !this.fullName.error());

  // ── Password form ───────────────────────────────────────────────────────────
  currentPassword = textField('', required('Current password'));
  newPassword     = textField('', composeValidators(required('New password'), minLength(8)));
  confirmPassword = textField('', required('Confirm password'));

  readonly confirmError = computed(() => {
    const base = this.confirmPassword.error();
    if (base) return base;
    if (!this.confirmPassword.touched()) return null;
    return this.confirmPassword.value() !== this.newPassword.value()
      ? 'Passwords do not match' : null;
  });

  passwordLoading = signal(false);
  passwordValid   = computed(() =>
    !this.currentPassword.error() &&
    !this.newPassword.error() &&
    !this.confirmError() &&
    this.newPassword.value() === this.confirmPassword.value()
  );

  // ── Data ────────────────────────────────────────────────────────────────────
  readonly profile = rxResource({
    stream: () => this.authService.getProfile()
  });

  readonly email      = computed(() => this.profile.value()?.email ?? '');
  readonly globalRole = computed(() => this.profile.value()?.globalRole ?? '');
  readonly isLoading  = computed(() => this.profile.isLoading());

  constructor() {
    effect(() => {
      const p = this.profile.value();
      if (!p) return;
      this.fullName.value.set(p.fullName);
      this.bio.value.set(p.bio ?? '');
    });
  }

  saveProfile(): void {
    this.fullName.touched.set(true);
    if (!this.profileValid()) return;

    this.profileLoading.set(true);
    this.authService.updateProfile({
      fullName: this.fullName.value(),
      bio:      this.bio.value()
    }).subscribe({
      next: () => {
        this.profileLoading.set(false);
        this.snackBar.open('Profile updated', 'Dismiss', { duration: 3000 });
      },
      error: () => {
        this.profileLoading.set(false);
        this.snackBar.open('Failed to update profile. Please try again.', 'Dismiss', { duration: 4000 });
      }
    });
  }

  savePassword(): void {
    this.currentPassword.touched.set(true);
    this.newPassword.touched.set(true);
    this.confirmPassword.touched.set(true);
    if (!this.passwordValid()) return;

    this.passwordLoading.set(true);
    this.authService.changePassword({
      currentPassword: this.currentPassword.value(),
      newPassword:     this.newPassword.value()
    }).subscribe({
      next: () => {
        this.passwordLoading.set(false);
        this.currentPassword.value.set('');
        this.newPassword.value.set('');
        this.confirmPassword.value.set('');
        this.currentPassword.touched.set(false);
        this.newPassword.touched.set(false);
        this.confirmPassword.touched.set(false);
        this.snackBar.open('Password changed successfully', 'Dismiss', { duration: 3000 });
      },
      error: err => {
        this.passwordLoading.set(false);
        const msg = err.status === 403
          ? (err.error?.detail || 'Current password is incorrect.')
          : 'Failed to change password. Please try again.';
        this.snackBar.open(msg, 'Dismiss', { duration: 4000 });
      }
    });
  }
}
