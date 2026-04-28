import { Component, ElementRef, Inject, OnInit, ViewChild, inject, signal } from '@angular/core';
import { ReactiveFormsModule, FormBuilder, Validators } from '@angular/forms';
import { MatDialogRef, MAT_DIALOG_DATA, MatDialogModule } from '@angular/material/dialog';
import { MatButtonModule } from '@angular/material/button';
import { MatMenuModule } from '@angular/material/menu';
import { MatIconModule } from '@angular/material/icon';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { UserService } from '../../core/services/user.service';
import { UserResponse } from '../../core/models/user.model';
import { FieldComponent } from '../../shared/components/field/field.component';

@Component({
  selector: 'app-user-form-dialog',
  standalone: true,
  imports: [
    ReactiveFormsModule, MatDialogModule,
    MatButtonModule, MatSnackBarModule,
    MatMenuModule, MatIconModule,
    FieldComponent
  ],
  templateUrl: './user-form-dialog.component.html',
  styleUrl: './user-form-dialog.component.scss'
})
export class UserFormDialogComponent implements OnInit {
  @ViewChild('roleTriggerBtn') roleTriggerBtn!: ElementRef<HTMLButtonElement>;

  private fb = inject(FormBuilder);
  isEdit = false;
  loading = signal(false);
  form = this.fb.group({
    email: ['', [Validators.required, Validators.email]],
    password: ['', [Validators.required, Validators.minLength(8)]],
    fullName: ['', Validators.required],
    globalRole: ['VIEWER', Validators.required],
    isActive: [true]
  });

  constructor(
    private userService: UserService,
    private snackBar: MatSnackBar,
    private dialogRef: MatDialogRef<UserFormDialogComponent>,
    @Inject(MAT_DIALOG_DATA) public data: UserResponse | null
  ) {}

  ngOnInit(): void {
    if (this.data) {
      this.isEdit = true;
      this.form.patchValue({ fullName: this.data.fullName, globalRole: this.data.globalRole, isActive: this.data.isActive });
      this.form.get('email')?.disable();
      this.form.get('password')?.disable();
    }
  }

  onRoleMenuOpened(): void {
    const width = this.roleTriggerBtn.nativeElement.offsetWidth;
    requestAnimationFrame(() => {
      const panel = document.querySelector('.mat-mdc-menu-panel.role-menu') as HTMLElement | null;
      if (panel) panel.style.minWidth = `${width}px`;
    });
  }

  save(): void {
    if (this.form.invalid) { this.form.markAllAsTouched(); return; }
    this.loading.set(true);
    const v = this.form.getRawValue();
    const obs = this.isEdit
      ? this.userService.updateUser(this.data!.id, { fullName: v.fullName!, globalRole: v.globalRole as any, isActive: v.isActive! })
      : this.userService.createUser({ email: v.email!, password: v.password!, fullName: v.fullName!, globalRole: v.globalRole as any });
    obs.subscribe({
      next: () => { this.snackBar.open('Saved', 'OK', { duration: 2000 }); this.dialogRef.close(true); },
      error: err => { this.loading.set(false); this.snackBar.open(err.error?.detail || 'Error saving user.', 'OK', { duration: 3000 }); }
    });
  }
}
