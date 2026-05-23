import { Component, computed, ElementRef, inject, signal, ViewChild } from '@angular/core';
import { MatDialogRef, MAT_DIALOG_DATA, MatDialogModule } from '@angular/material/dialog';
import { MatButtonModule } from '@angular/material/button';
import { MatMenuModule } from '@angular/material/menu';
import { MatIconModule } from '@angular/material/icon';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { UserService } from '../../core/services/user.service';
import { UserResponse } from '../../core/models/user.model';
import { FieldComponent } from '../../shared/components/field/field.component';
import {
  composeValidators, emailFormat, minLength, required, textField
} from '../../shared/signal-form';

@Component({
  selector: 'app-user-form-dialog',
  standalone: true,
  imports: [
    MatDialogModule, MatButtonModule, MatSnackBarModule,
    MatMenuModule, MatIconModule, FieldComponent
  ],
  templateUrl: './user-form-dialog.component.html',
  styleUrl: './user-form-dialog.component.scss'
})
export class UserFormDialogComponent {
  @ViewChild('roleTriggerBtn') roleTriggerBtn!: ElementRef<HTMLButtonElement>;

  private userService = inject(UserService);
  private snackBar    = inject(MatSnackBar);
  private dialogRef   = inject(MatDialogRef<UserFormDialogComponent>);
  readonly data       = inject<UserResponse | null>(MAT_DIALOG_DATA);

  readonly isEdit = !!this.data;

  fullName   = textField(this.data?.fullName  ?? '', required('Full name'));
  email      = textField(this.data?.email     ?? '', composeValidators(required('Email address'), emailFormat));
  password   = textField('', this.isEdit ? () => null : composeValidators(required('Password'), minLength(8)));
  globalRole = signal<'ADMIN' | 'EDITOR' | 'VIEWER'>((this.data?.globalRole ?? 'VIEWER') as 'ADMIN' | 'EDITOR' | 'VIEWER');
  isActive   = signal(this.data?.isActive ?? true);
  loading    = signal(false);

  formValid = computed(() => {
    if (this.isEdit) return !this.fullName.error();
    return !this.email.error() && !this.password.error() && !this.fullName.error();
  });

  onRoleMenuOpened(): void {
    const width = this.roleTriggerBtn.nativeElement.offsetWidth;
    requestAnimationFrame(() => {
      const panel = document.querySelector('.mat-mdc-menu-panel.role-menu') as HTMLElement | null;
      if (panel) panel.style.minWidth = `${width}px`;
    });
  }

  save(): void {
    this.fullName.touched.set(true);
    if (!this.isEdit) {
      this.email.touched.set(true);
      this.password.touched.set(true);
    }
    if (!this.formValid()) return;

    this.loading.set(true);
    const obs = this.isEdit
      ? this.userService.updateUser(this.data!.id, {
          fullName:   this.fullName.value(),
          globalRole: this.globalRole() as any,
          isActive:   this.isActive()
        })
      : this.userService.createUser({
          email:      this.email.value(),
          password:   this.password.value(),
          fullName:   this.fullName.value(),
          globalRole: this.globalRole() as any
        });

    obs.subscribe({
      next:  () => { this.snackBar.open('Saved', 'OK', { duration: 2000 }); this.dialogRef.close(true); },
      error: err => {
        this.loading.set(false);
        this.snackBar.open(err.error?.detail || 'Error saving user.', 'OK', { duration: 3000 });
      }
    });
  }
}
