import { Component, computed, inject, signal } from '@angular/core';
import { rxResource, toObservable, takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { debounceTime, distinctUntilChanged } from 'rxjs';
import { MatTableModule } from '@angular/material/table';
import { MatPaginatorModule, PageEvent } from '@angular/material/paginator';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatDialog, MatDialogModule } from '@angular/material/dialog';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { MatTooltipModule } from '@angular/material/tooltip';
import { UserService } from '../../core/services/user.service';
import { UserResponse } from '../../core/models/user.model';
import { UserFormDialogComponent } from './user-form-dialog.component';
import { ResetPasswordDialogComponent } from './reset-password-dialog.component';
import { ConfirmDialogComponent } from '../../shared/components/confirm-dialog/confirm-dialog.component';

@Component({
  selector: 'app-user-list',
  standalone: true,
  imports: [
    MatTableModule, MatPaginatorModule, MatButtonModule, MatIconModule,
    MatInputModule, MatFormFieldModule,
    MatDialogModule, MatSnackBarModule, MatProgressBarModule, MatTooltipModule
  ],
  templateUrl: './user-list.component.html',
  styleUrl: './user-list.component.scss'
})
export class UserListComponent {
  private userService = inject(UserService);
  private dialog      = inject(MatDialog);
  private snackBar    = inject(MatSnackBar);

  readonly displayedColumns = ['fullName', 'globalRole', 'status', 'actions'];

  search    = signal('');
  pageIndex = signal(0);
  pageSize  = signal(10);

  private debouncedSearch = signal('');

  data = rxResource({
    params: () => ({
      search:   this.debouncedSearch(),
      page:     this.pageIndex(),
      pageSize: this.pageSize()
    }),
    stream: ({ params }) =>
      this.userService.listUsers(params.page, params.pageSize, params.search || undefined)
  });

  users         = computed(() => this.data.value()?.content ?? []);
  totalElements = computed(() => this.data.value()?.totalElements ?? 0);
  isLoading     = computed(() => this.data.isLoading());

  constructor() {
    toObservable(this.search).pipe(
      debounceTime(300), distinctUntilChanged(), takeUntilDestroyed()
    ).subscribe(s => {
      this.pageIndex.set(0);
      this.debouncedSearch.set(s);
    });
  }

  onPage(event: PageEvent): void {
    this.pageIndex.set(event.pageIndex);
    this.pageSize.set(event.pageSize);
  }

  openCreate(): void {
    this.dialog.open(UserFormDialogComponent, { width: '500px' })
      .afterClosed().subscribe(result => { if (result) this.data.reload(); });
  }

  openEdit(user: UserResponse): void {
    this.dialog.open(UserFormDialogComponent, { width: '500px', data: user })
      .afterClosed().subscribe(result => { if (result) this.data.reload(); });
  }

  openResetPassword(user: UserResponse): void {
    this.dialog.open(ResetPasswordDialogComponent, { width: '440px', data: user });
  }

  deleteUser(user: UserResponse): void {
    this.dialog.open(ConfirmDialogComponent, {
      width: '400px',
      data: {
        title:       'Delete user',
        message:     `${user.fullName} (${user.email}) will be permanently removed. All their permissions will be revoked.`,
        confirmText: 'Delete user'
      }
    }).afterClosed().subscribe(confirmed => {
      if (!confirmed) return;
      this.userService.deleteUser(user.id).subscribe({
        next: () => {
          this.snackBar.open(`${user.fullName} deleted`, 'Dismiss', { duration: 3000 });
          this.data.reload();
        },
        error: err =>
          this.snackBar.open(err.error?.detail || 'Failed to delete user.', 'Dismiss', { duration: 4000 })
      });
    });
  }
}
