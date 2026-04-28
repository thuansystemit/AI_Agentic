import { Component, Inject, OnInit, signal } from '@angular/core';
import { ReactiveFormsModule, FormControl } from '@angular/forms';
import { MatDialogRef, MAT_DIALOG_DATA, MatDialogModule } from '@angular/material/dialog';
import { MatTabsModule } from '@angular/material/tabs';
import { MatTableModule } from '@angular/material/table';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatAutocompleteModule } from '@angular/material/autocomplete';
import { MatMenuModule } from '@angular/material/menu';
import { MatTooltipModule } from '@angular/material/tooltip';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { debounceTime, distinctUntilChanged, switchMap } from 'rxjs';
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
    MatAutocompleteModule, MatMenuModule, MatTooltipModule, MatSnackBarModule
  ],
  templateUrl: './category-permissions-dialog.component.html',
  styleUrl: './category-permissions-dialog.component.scss'
})
export class CategoryPermissionsDialogComponent implements OnInit {
  permColumns = ['name', 'permission', 'actions'];
  userPerms = signal<PermEntry[]>([]);
  groupPerms = signal<PermEntry[]>([]);
  filteredUsers = signal<UserResponse[]>([]);
  filteredGroups = signal<GroupResponse[]>([]);
  selectedUser = signal<UserResponse | null>(null);
  selectedGroup = signal<GroupResponse | null>(null);
  userSearchCtrl = new FormControl('');
  groupSearchCtrl = new FormControl('');
  newUserPermCtrl = new FormControl<Permission>('READ');
  newGroupPermCtrl = new FormControl<Permission>('READ');
  selectedTab = 0;

  constructor(
    private categoryService: CategoryService,
    private userService: UserService,
    private groupService: GroupService,
    private snackBar: MatSnackBar,
    @Inject(MAT_DIALOG_DATA) public data: CategoryResponse
  ) {}

  ngOnInit(): void {
    this.userSearchCtrl.valueChanges.pipe(debounceTime(300), distinctUntilChanged(),
      switchMap(s => this.userService.listUsers(0, 10, s || undefined)))
      .subscribe(p => this.filteredUsers.set(p.content));

    this.groupSearchCtrl.valueChanges.pipe(debounceTime(300), distinctUntilChanged(),
      switchMap(s => this.groupService.listGroups(0, 10, s || undefined)))
      .subscribe(p => this.filteredGroups.set(p.content));
  }

  displayUser = (u: UserResponse) => u ? `${u.fullName} (${u.email})` : '';
  displayGroup = (g: GroupResponse) => g ? g.name : '';

  get canAdd(): boolean {
    return this.selectedTab === 0 ? !!this.selectedUser() : !!this.selectedGroup();
  }

  addPermission(): void {
    if (this.selectedTab === 0) {
      this.addUserPermission();
    } else {
      this.addGroupPermission();
    }
  }

  addUserPermission(): void {
    if (!this.selectedUser() || !this.newUserPermCtrl.value) return;
    const u = this.selectedUser()!;
    this.categoryService.setUserPermission(this.data.id, u.id, { permission: this.newUserPermCtrl.value }).subscribe({
      next: () => {
        const idx = this.userPerms().findIndex(e => e.id === u.id);
        if (idx >= 0) this.userPerms.update(p => p.map((e, i) => i === idx ? { ...e, permission: this.newUserPermCtrl.value! } : e));
        else this.userPerms.update(p => [...p, { id: u.id, name: u.fullName, permission: this.newUserPermCtrl.value! }]);
        this.userSearchCtrl.reset(); this.selectedUser.set(null);
        this.snackBar.open('Permission set', 'OK', { duration: 2000 });
      },
      error: () => this.snackBar.open('Failed to set permission.', 'OK', { duration: 3000 })
    });
  }

  removeUserPermission(entry: PermEntry): void {
    this.categoryService.removeUserPermission(this.data.id, entry.id).subscribe({
      next: () => { this.userPerms.update(p => p.filter(e => e.id !== entry.id)); this.snackBar.open('Permission removed', 'OK', { duration: 2000 }); },
      error: () => this.snackBar.open('Failed to remove permission.', 'OK', { duration: 3000 })
    });
  }

  addGroupPermission(): void {
    if (!this.selectedGroup() || !this.newGroupPermCtrl.value) return;
    const g = this.selectedGroup()!;
    this.categoryService.setGroupPermission(this.data.id, g.id, { permission: this.newGroupPermCtrl.value }).subscribe({
      next: () => {
        const idx = this.groupPerms().findIndex(e => e.id === g.id);
        if (idx >= 0) this.groupPerms.update(p => p.map((e, i) => i === idx ? { ...e, permission: this.newGroupPermCtrl.value! } : e));
        else this.groupPerms.update(p => [...p, { id: g.id, name: g.name, permission: this.newGroupPermCtrl.value! }]);
        this.groupSearchCtrl.reset(); this.selectedGroup.set(null);
        this.snackBar.open('Permission set', 'OK', { duration: 2000 });
      },
      error: () => this.snackBar.open('Failed to set permission.', 'OK', { duration: 3000 })
    });
  }

  removeGroupPermission(entry: PermEntry): void {
    this.categoryService.removeGroupPermission(this.data.id, entry.id).subscribe({
      next: () => { this.groupPerms.update(p => p.filter(e => e.id !== entry.id)); this.snackBar.open('Permission removed', 'OK', { duration: 2000 }); },
      error: () => this.snackBar.open('Failed to remove permission.', 'OK', { duration: 3000 })
    });
  }
}
