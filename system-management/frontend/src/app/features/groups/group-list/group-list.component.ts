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
import { GroupService } from '../../../core/services/group.service';
import { GroupResponse } from '../../../core/models/group.model';
import { GroupFormDialogComponent } from '../group-form/group-form-dialog.component';
import { GroupMembersDialogComponent } from '../group-members/group-members-dialog.component';
import { ConfirmDialogComponent } from '../../../shared/components/confirm-dialog/confirm-dialog.component';

@Component({
  selector: 'app-group-list',
  standalone: true,
  imports: [
    MatTableModule, MatPaginatorModule, MatButtonModule, MatIconModule,
    MatInputModule, MatFormFieldModule,
    MatDialogModule, MatSnackBarModule, MatProgressBarModule, MatTooltipModule
  ],
  templateUrl: './group-list.component.html',
  styleUrl: './group-list.component.scss'
})
export class GroupListComponent {
  private groupService = inject(GroupService);
  private dialog       = inject(MatDialog);
  private snackBar     = inject(MatSnackBar);

  readonly displayedColumns = ['name', 'description', 'actions'];

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
      this.groupService.listGroups(params.page, params.pageSize, params.search || undefined)
  });

  groups        = computed(() => this.data.value()?.content ?? []);
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
    this.dialog.open(GroupFormDialogComponent, { width: '440px' })
      .afterClosed().subscribe(result => { if (result) this.data.reload(); });
  }

  openEdit(group: GroupResponse): void {
    this.dialog.open(GroupFormDialogComponent, { width: '440px', data: group })
      .afterClosed().subscribe(result => { if (result) this.data.reload(); });
  }

  openMembers(group: GroupResponse): void {
    this.dialog.open(GroupMembersDialogComponent, { width: '580px', data: group });
  }

  deleteGroup(group: GroupResponse): void {
    this.dialog.open(ConfirmDialogComponent, {
      width: '400px',
      data: {
        title:       'Delete group',
        message:     `"${group.name}" will be deleted and all its category permissions will be revoked immediately.`,
        confirmText: 'Delete group'
      }
    }).afterClosed().subscribe(confirmed => {
      if (!confirmed) return;
      this.groupService.deleteGroup(group.id).subscribe({
        next:  () => { this.snackBar.open(`"${group.name}" deleted`, 'Dismiss', { duration: 3000 }); this.data.reload(); },
        error: () => this.snackBar.open('Failed to delete group. Please try again.', 'Dismiss', { duration: 4000 })
      });
    });
  }
}
