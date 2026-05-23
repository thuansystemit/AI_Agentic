import { Component, computed, inject, signal } from '@angular/core';
import { MatDialogRef, MAT_DIALOG_DATA, MatDialogModule } from '@angular/material/dialog';
import { MatButtonModule } from '@angular/material/button';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { UserService } from '../../core/services/user.service';
import { UserResponse } from '../../core/models/user.model';
import { FieldComponent } from '../../shared/components/field/field.component';
import { composeValidators, minLength, required, textField } from '../../shared/signal-form';

@Component({
  selector: 'app-reset-password-dialog',
  standalone: true,
  imports: [MatDialogModule, MatButtonModule, MatSnackBarModule, FieldComponent],
  templateUrl: './reset-password-dialog.component.html',
  styleUrl: './reset-password-dialog.component.scss'
})
export class ResetPasswordDialogComponent {
  private userService = inject(UserService);
  private snackBar    = inject(MatSnackBar);
  private dialogRef   = inject(MatDialogRef<ResetPasswordDialogComponent>);
  readonly user       = inject<UserResponse>(MAT_DIALOG_DATA);

  newPassword     = textField('', composeValidators(required('New password'), minLength(8)));
  confirmPassword = textField('', required('Confirm password'));
  loading         = signal(false);

  confirmError = computed(() => {
    if (!this.confirmPassword.touched()) return null;
    return this.confirmPassword.value() !== this.newPassword.value()
      ? 'Passwords do not match'
      : null;
  });

  formValid = computed(() =>
    !this.newPassword.error() && !this.confirmError() && this.confirmPassword.value().length > 0
  );

  save(): void {
    this.newPassword.touched.set(true);
    this.confirmPassword.touched.set(true);
    if (!this.formValid()) return;

    this.loading.set(true);
    this.userService.resetPassword(this.user.id, this.newPassword.value()).subscribe({
      next: () => {
        this.snackBar.open(`Password reset for ${this.user.fullName}`, 'OK', { duration: 3000 });
        this.dialogRef.close(true);
      },
      error: err => {
        this.loading.set(false);
        this.snackBar.open(err.error?.detail || 'Failed to reset password.', 'OK', { duration: 3000 });
      }
    });
  }
}
