import { Component, OnInit, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ReactiveFormsModule, FormControl } from '@angular/forms';
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
import { debounceTime, distinctUntilChanged } from 'rxjs';
import { UserService } from '../../core/services/user.service';
import { UserResponse } from '../../core/models/user.model';
import { UserFormDialogComponent } from './user-form-dialog.component';
import { ConfirmDialogComponent } from '../../shared/components/confirm-dialog/confirm-dialog.component';

@Component({
  selector: 'app-user-list',
  standalone: true,
  imports: [
    CommonModule, ReactiveFormsModule,
    MatTableModule, MatPaginatorModule, MatButtonModule, MatIconModule,
    MatInputModule, MatFormFieldModule,
    MatDialogModule, MatSnackBarModule, MatProgressBarModule, MatTooltipModule
  ],
  templateUrl: './user-list.component.html',
  styleUrl: './user-list.component.scss'
})
export class UserListComponent implements OnInit {
  displayedColumns = ['fullName', 'globalRole', 'status', 'actions'];
  users = signal<UserResponse[]>([]);
  totalElements = signal(0);
  pageSize = 10;
  currentPage = 0;
  isLoading = signal(false);
  searchCtrl = new FormControl('');

  constructor(
    private userService: UserService,
    private dialog: MatDialog,
    private snackBar: MatSnackBar
  ) {}

  ngOnInit(): void {
    this.loadUsers();
    this.searchCtrl.valueChanges.pipe(debounceTime(300), distinctUntilChanged())
      .subscribe(() => { this.currentPage = 0; this.loadUsers(); });
  }

  loadUsers(): void {
    this.isLoading.set(true);
    this.userService.listUsers(this.currentPage, this.pageSize, this.searchCtrl.value || undefined)
      .subscribe({
        next: page => { this.users.set(page.content); this.totalElements.set(page.totalElements); this.isLoading.set(false); },
        error: () => { this.isLoading.set(false); }
      });
  }

  onPage(event: PageEvent): void {
    this.currentPage = event.pageIndex;
    this.pageSize = event.pageSize;
    this.loadUsers();
  }

  openCreate(): void {
    this.dialog.open(UserFormDialogComponent, { width: '500px' })
      .afterClosed().subscribe(result => { if (result) this.loadUsers(); });
  }

  openEdit(user: UserResponse): void {
    this.dialog.open(UserFormDialogComponent, { width: '500px', data: user })
      .afterClosed().subscribe(result => { if (result) this.loadUsers(); });
  }

  deleteUser(user: UserResponse): void {
    this.dialog.open(ConfirmDialogComponent, {
      width: '400px',
      data: {
        title: 'Delete user',
        message: `${user.fullName} (${user.email}) will be permanently removed. All their permissions will be revoked.`,
        confirmText: 'Delete user'
      }
    }).afterClosed().subscribe(confirmed => {
      if (!confirmed) return;
      this.userService.deleteUser(user.id).subscribe({
        next: () => { this.snackBar.open(`${user.fullName} deleted`, 'Dismiss', { duration: 3000 }); this.loadUsers(); },
        error: err => this.snackBar.open(err.error?.detail || 'Failed to delete user.', 'Dismiss', { duration: 4000 })
      });
    });
  }
}
