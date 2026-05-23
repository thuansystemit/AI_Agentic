import { Component, computed, inject, input, signal } from '@angular/core';
import { rxResource, toObservable, takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { debounceTime, distinctUntilChanged } from 'rxjs';
import { RouterLink } from '@angular/router';
import { DatePipe, SlicePipe } from '@angular/common';
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
import { DocumentService } from '../../../core/services/document.service';
import { CategoryService } from '../../../core/services/category.service';
import { DocumentResponse } from '../../../core/models/document.model';
import { DocumentFormDialogComponent } from '../document-form/document-form-dialog.component';
import { ConfirmDialogComponent } from '../../../shared/components/confirm-dialog/confirm-dialog.component';

@Component({
  selector: 'app-document-list',
  standalone: true,
  imports: [
    RouterLink, DatePipe, SlicePipe,
    MatTableModule, MatPaginatorModule, MatButtonModule, MatIconModule,
    MatInputModule, MatFormFieldModule,
    MatDialogModule, MatSnackBarModule, MatProgressBarModule, MatTooltipModule
  ],
  templateUrl: './document-list.component.html',
  styleUrl: './document-list.component.scss'
})
export class DocumentListComponent {
  private documentService = inject(DocumentService);
  private categoryService = inject(CategoryService);
  private dialog          = inject(MatDialog);
  private snackBar        = inject(MatSnackBar);

  // Route param bound automatically via withComponentInputBinding()
  id = input.required<string>();

  readonly displayedColumns = ['title', 'updatedAt', 'actions'];

  search    = signal('');
  pageIndex = signal(0);
  pageSize  = signal(10);

  private debouncedSearch = signal('');

  categoryData = rxResource({
    params: () => this.id(),
    stream: ({ params: id }) => this.categoryService.getCategory(id)
  });
  category = computed(() => this.categoryData.value() ?? null);

  data = rxResource({
    params: () => ({
      id:       this.id(),
      search:   this.debouncedSearch(),
      page:     this.pageIndex(),
      pageSize: this.pageSize()
    }),
    stream: ({ params }) =>
      this.documentService.listDocuments(params.id, params.page, params.pageSize, params.search || undefined)
  });

  documents     = computed(() => this.data.value()?.content ?? []);
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
    this.dialog.open(DocumentFormDialogComponent, { width: '680px', data: { categoryId: this.id() } })
      .afterClosed().subscribe(result => { if (result) this.data.reload(); });
  }

  openEdit(doc: DocumentResponse): void {
    this.dialog.open(DocumentFormDialogComponent, { width: '680px', data: doc })
      .afterClosed().subscribe(result => { if (result) this.data.reload(); });
  }

  deleteDocument(doc: DocumentResponse): void {
    this.dialog.open(ConfirmDialogComponent, {
      width: '400px',
      data: {
        title:       'Delete document',
        message:     `"${doc.title}" will be permanently deleted and cannot be recovered.`,
        confirmText: 'Delete document'
      }
    }).afterClosed().subscribe(confirmed => {
      if (!confirmed) return;
      this.documentService.deleteDocument(doc.id).subscribe({
        next: () => {
          this.snackBar.open(`"${doc.title}" deleted`, 'Dismiss', { duration: 3000 });
          this.data.reload();
        },
        error: err => {
          const msg = err.status === 403
            ? 'You don\'t have permission to delete this document.'
            : 'Failed to delete document. Please try again.';
          this.snackBar.open(msg, 'Dismiss', { duration: 4000 });
        }
      });
    });
  }
}
