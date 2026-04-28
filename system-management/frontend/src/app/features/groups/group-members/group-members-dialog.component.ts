import { Component, Inject, OnInit, signal } from '@angular/core';
import { ReactiveFormsModule, FormControl } from '@angular/forms';
import { MatDialogRef, MAT_DIALOG_DATA, MatDialogModule } from '@angular/material/dialog';
import { MatListModule } from '@angular/material/list';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatAutocompleteModule } from '@angular/material/autocomplete';
import { MatTooltipModule } from '@angular/material/tooltip';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { debounceTime, distinctUntilChanged, switchMap } from 'rxjs';
import { GroupService } from '../../../core/services/group.service';
import { UserService } from '../../../core/services/user.service';
import { GroupResponse } from '../../../core/models/group.model';
import { UserResponse } from '../../../core/models/user.model';

@Component({
  selector: 'app-group-members-dialog',
  standalone: true,
  imports: [
    ReactiveFormsModule, MatDialogModule,
    MatListModule, MatButtonModule, MatIconModule,
    MatAutocompleteModule, MatTooltipModule, MatSnackBarModule
  ],
  templateUrl: './group-members-dialog.component.html',
  styleUrl: './group-members-dialog.component.scss'
})
export class GroupMembersDialogComponent implements OnInit {
  members = signal<UserResponse[]>([]);
  filteredUsers = signal<UserResponse[]>([]);
  userSearchCtrl = new FormControl('');

  constructor(
    private groupService: GroupService,
    private userService: UserService,
    private snackBar: MatSnackBar,
    @Inject(MAT_DIALOG_DATA) public data: GroupResponse
  ) {}

  ngOnInit(): void {
    this.userSearchCtrl.valueChanges.pipe(
      debounceTime(300), distinctUntilChanged(),
      switchMap(search => this.userService.listUsers(0, 10, search || undefined))
    ).subscribe(page => this.filteredUsers.set(page.content));
  }

  displayUser = (user: UserResponse) => user ? `${user.fullName} (${user.email})` : '';

  addMember(user: UserResponse): void {
    this.groupService.addMember(this.data.id, user.id).subscribe({
      next: () => {
        if (!this.members().find(m => m.id === user.id)) this.members.update(m => [...m, user]);
        this.userSearchCtrl.reset();
        this.snackBar.open(`${user.fullName} added to group`, 'OK', { duration: 2000 });
      },
      error: () => this.snackBar.open('Failed to add member.', 'OK', { duration: 3000 })
    });
  }

  removeMember(user: UserResponse): void {
    this.groupService.removeMember(this.data.id, user.id).subscribe({
      next: () => {
        this.members.update(m => m.filter(m2 => m2.id !== user.id));
        this.snackBar.open(`${user.fullName} removed from group`, 'OK', { duration: 2000 });
      },
      error: () => this.snackBar.open('Failed to remove member.', 'OK', { duration: 3000 })
    });
  }
}
