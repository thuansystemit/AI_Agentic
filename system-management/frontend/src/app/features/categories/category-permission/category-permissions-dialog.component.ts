import { Component, computed, inject, OnInit, signal } from '@angular/core';
import { ReactiveFormsModule, FormControl } from '@angular/forms';
import { MatDialogModule } from '@angular/material/dialog';
import { MatTabsModule } from '@angular/material/tabs';
import { MatTableModule } from '@angular/material/table';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatAutocompleteModule } from '@angular/material/autocomplete';
import { MatMenuModule } from '@angular/material/menu';
import { MatTooltipModule } from '@angular/material/tooltip';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { MAT_DIALOG_DATA } from '@angular/material/dialog';
import { debounceTime, distinctUntilChanged, forkJoin, switchMap } from 'rxjs';
import { CategoryService } from '../../../core/services/category.service';
import { UserService } from '../../../core/services/user.service';
import { GroupService } from '../../../core/services/group.service';
import { CategoryResponse } from '../../../core/models/category.model';
import { UserResponse } from '../../../core/models/user.model';
import { GroupResponse } from '../../../core/models/group.model';
import { Permission } from '../../../core/models/auth.model';

interface PermEntry { id: string; name: string; permission: Permission; }

@Component({
  selector: 'app-category-permissions-dialog',
  standalone: true,
  imports: [
    ReactiveFormsModule, MatDialogModule, MatTabsModule,
    MatTableModule, MatButtonModule, MatIconModule,
    MatAutocompleteModule, MatMenuModule, MatTooltipModule,
    MatProgressBarModule, MatSnackBarModule
  ],
  templateUrl: './category-permissions-dialog.component.html',
  styleUrl: './category-permissions-dialog.component.scss'
})
export class CategoryPermissionsDialogComponent implements OnInit {
  private categoryService = inject(CategoryService);
  private userService     = inject(UserService);
  private groupService    = inject(GroupService);
  private snackBar        = inject(MatSnackBar);
  readonly data           = inject<CategoryResponse>(MAT_DIALOG_DATA);

  readonly permColumns = ['name', 'permission', 'actions'];

  userPerms      = signal<PermEntry[]>([]);
  groupPerms     = signal<PermEntry[]>([]);
  filteredUsers  = signal<UserResponse[]>([]);
  filteredGroups = signal<GroupResponse[]>([]);
  selectedUser   = signal<UserResponse | null>(null);
  selectedGroup  = signal<GroupResponse | null>(null);
  selectedTab    = signal(0);
  newUserPerm    = signal<Permission>('READ');
  newGroupPerm   = signal<Permission>('READ');
  loading        = signal(false);

  // FormControl retained: valueChanges feeds a debounced switchMap stream
  userSearchCtrl  = new FormControl('');
  groupSearchCtrl = new FormControl('');

  canAdd = computed(() =>
    this.selectedTab() === 0 ? !!this.selectedUser() : !!this.selectedGroup()
  );

  ngOnInit(): void {
    this.loadPermissions();

    this.userSearchCtrl.valueChanges.pipe(
      debounceTime(300), distinctUntilChanged(),
      switchMap(s => this.userService.listUsers(0, 10, s || undefined))
    ).subscribe(p => this.filteredUsers.set(p.content));

    this.groupSearchCtrl.valueChanges.pipe(
      debounceTime(300), distinctUntilChanged(),
      switchMap(s => this.groupService.listGroups(0, 10, s || undefined))
    ).subscribe(p => this.filteredGroups.set(p.content));
  }

  private loadPermissions(): void {
    this.loading.set(true);
    forkJoin({
      users:  this.categoryService.getUserPermissions(this.data.id),
      groups: this.categoryService.getGroupPermissions(this.data.id)
    }).subscribe({
      next: ({ users, groups }) => {
        this.userPerms.set(users.map(e => ({ id: e.subjectId, name: e.subjectName, permission: e.permission })));
        this.groupPerms.set(groups.map(e => ({ id: e.subjectId, name: e.subjectName, permission: e.permission })));
        this.loading.set(false);
      },
      error: () => {
        this.snackBar.open('Failed to load permissions.', 'OK', { duration: 3000 });
        this.loading.set(false);
      }
    });
  }

  displayUser  = (u: UserResponse)  => u ? `${u.fullName} (${u.email})` : '';
  displayGroup = (g: GroupResponse) => g ? g.name : '';

  addPermission(): void {
    if (this.selectedTab() === 0) this.addUserPermission();
    else this.addGroupPermission();
  }

  addUserPermission(): void {
    if (!this.selectedUser()) return;
    const u = this.selectedUser()!;
    this.categoryService.setUserPermission(this.data.id, u.id, { permission: this.newUserPerm() }).subscribe({
      next: () => {
        const idx = this.userPerms().findIndex(e => e.id === u.id);
        if (idx >= 0) this.userPerms.update(p => p.map((e, i) => i === idx ? { ...e, permission: this.newUserPerm() } : e));
        else this.userPerms.update(p => [...p, { id: u.id, name: u.fullName, permission: this.newUserPerm() }]);
        this.userSearchCtrl.reset();
        this.selectedUser.set(null);
        this.snackBar.open('Permission set', 'OK', { duration: 2000 });
      },
      error: () => this.snackBar.open('Failed to set permission.', 'OK', { duration: 3000 })
    });
  }

  removeUserPermission(entry: PermEntry): void {
    this.categoryService.removeUserPermission(this.data.id, entry.id).subscribe({
      next:  () => { this.userPerms.update(p => p.filter(e => e.id !== entry.id)); this.snackBar.open('Permission removed', 'OK', { duration: 2000 }); },
      error: () => this.snackBar.open('Failed to remove permission.', 'OK', { duration: 3000 })
    });
  }

  addGroupPermission(): void {
    if (!this.selectedGroup()) return;
    const g = this.selectedGroup()!;
    this.categoryService.setGroupPermission(this.data.id, g.id, { permission: this.newGroupPerm() }).subscribe({
      next: () => {
        const idx = this.groupPerms().findIndex(e => e.id === g.id);
        if (idx >= 0) this.groupPerms.update(p => p.map((e, i) => i === idx ? { ...e, permission: this.newGroupPerm() } : e));
        else this.groupPerms.update(p => [...p, { id: g.id, name: g.name, permission: this.newGroupPerm() }]);
        this.groupSearchCtrl.reset();
        this.selectedGroup.set(null);
        this.snackBar.open('Permission set', 'OK', { duration: 2000 });
      },
      error: () => this.snackBar.open('Failed to set permission.', 'OK', { duration: 3000 })
    });
  }

  removeGroupPermission(entry: PermEntry): void {
    this.categoryService.removeGroupPermission(this.data.id, entry.id).subscribe({
      next:  () => { this.groupPerms.update(p => p.filter(e => e.id !== entry.id)); this.snackBar.open('Permission removed', 'OK', { duration: 2000 }); },
      error: () => this.snackBar.open('Failed to remove permission.', 'OK', { duration: 3000 })
    });
  }
}
