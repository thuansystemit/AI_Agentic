import { Component, computed, inject, signal } from '@angular/core';
import { Router } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatIconModule } from '@angular/material/icon';
import { AuthService } from '../../../core/services/auth.service';
import { FieldComponent } from '../../../shared/components/field/field.component';
import {
  composeValidators, emailFormat, required, textField
} from '../../../shared/signal-form';

@Component({
  selector: 'app-login',
  standalone: true,
  imports: [FormsModule, MatButtonModule, MatProgressSpinnerModule, MatIconModule, FieldComponent],
  templateUrl: './login.component.html',
  styleUrl: './login.component.scss'
})
export class LoginComponent {
  private authService = inject(AuthService);
  private router      = inject(Router);

  email    = textField('', composeValidators(required('Email address'), emailFormat));
  password = textField('', required('Password'));

  loading      = signal(false);
  errorMessage = signal('');

  formValid = computed(() => !this.email.error() && !this.password.error());

  onSubmit(): void {
    this.email.touched.set(true);
    this.password.touched.set(true);
    if (!this.formValid()) return;

    this.loading.set(true);
    this.errorMessage.set('');

    this.authService.login({ email: this.email.value(), password: this.password.value() }).subscribe({
      next: () => this.router.navigate(['/categories']),
      error: err => {
        this.loading.set(false);
        if (err.status === 423 || (err.status === 403 && err.error?.lockedUntil)) {
          this.errorMessage.set('Account locked after too many failed attempts. Please try again later.');
        } else if (err.status === 401) {
          this.errorMessage.set('Incorrect email or password. Please try again.');
        } else {
          this.errorMessage.set('Something went wrong. Please try again.');
        }
      }
    });
  }
}
