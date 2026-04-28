import { Component, OnInit, signal } from '@angular/core';
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
import { GroupService } from '../../../core/services/group.service';
import { GroupResponse } from '../../../core/models/group.model';
import { GroupFormDialogComponent } from '../group-form/group-form-dialog.component';
import { GroupMembersDialogComponent } from '../group-members/group-members-dialog.component';
import { ConfirmDialogComponent } from '../../../shared/components/confirm-dialog/confirm-dialog.component';

@Component({
  selector: 'app-group-list',
  standalone: true,
  imports: [
    ReactiveFormsModule,
    MatTableModule, MatPaginatorModule, MatButtonModule, MatIconModule,
    MatInputModule, MatFormFieldModule,
    MatDialogModule, MatSnackBarModule, MatProgressBarModule, MatTooltipModule
  ],
  templateUrl: './group-list.component.html',
  styleUrl: './group-list.component.scss'
})
export class GroupListComponent implements OnInit {
  displayedColumns = ['name', 'description', 'actions'];
  groups = signal<GroupResponse[]>([]);
  totalElements = signal(0);
  pageSize = 10;
  currentPage = 0;
  isLoading = signal(false);
  searchCtrl = new FormControl('');

  constructor(
    private groupService: GroupService,
    private dialog: MatDialog,
    private snackBar: MatSnackBar
  ) {}

  ngOnInit(): void {
    this.loadGroups();
    this.searchCtrl.valueChanges.pipe(debounceTime(300), distinctUntilChanged())
      .subscribe(() => { this.currentPage = 0; this.loadGroups(); });
  }

  loadGroups(): void {
    this.isLoading.set(true);
    this.groupService.listGroups(this.currentPage, this.pageSize, this.searchCtrl.value || undefined)
      .subscribe({
        next: page => { this.groups.set(page.content); this.totalElements.set(page.totalElements); this.isLoading.set(false); },
        error: () => { this.isLoading.set(false); }
      });
  }

  onPage(event: PageEvent): void {
    this.currentPage = event.pageIndex;
    this.pageSize = event.pageSize;
    this.loadGroups();
  }

  openCreate(): void {
    this.dialog.open(GroupFormDialogComponent, { width: '440px' })
      .afterClosed().subscribe(result => { if (result) this.loadGroups(); });
  }

  openEdit(group: GroupResponse): void {
    this.dialog.open(GroupFormDialogComponent, { width: '440px', data: group })
      .afterClosed().subscribe(result => { if (result) this.loadGroups(); });
  }

  openMembers(group: GroupResponse): void {
    this.dialog.open(GroupMembersDialogComponent, { width: '580px', data: group });
  }

  deleteGroup(group: GroupResponse): void {
    this.dialog.open(ConfirmDialogComponent, {
      width: '400px',
      data: {
        title: 'Delete group',
        message: `"${group.name}" will be deleted and all its category permissions will be revoked immediately.`,
        confirmText: 'Delete group'
      }
    }).afterClosed().subscribe(confirmed => {
      if (!confirmed) return;
      this.groupService.deleteGroup(group.id).subscribe({
        next: () => { this.snackBar.open(`"${group.name}" deleted`, 'Dismiss', { duration: 3000 }); this.loadGroups(); },
        error: () => this.snackBar.open('Failed to delete group. Please try again.', 'Dismiss', { duration: 4000 })
      });
    });
  }
}
