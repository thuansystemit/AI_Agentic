import { Component, OnInit, Input, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ReactiveFormsModule, FormControl } from '@angular/forms';
import { RouterLink } from '@angular/router';
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
import { DocumentService } from '../../../core/services/document.service';
import { CategoryService } from '../../../core/services/category.service';
import { DocumentResponse } from '../../../core/models/document.model';
import { CategoryResponse } from '../../../core/models/category.model';
import { DocumentFormDialogComponent } from '../document-form/document-form-dialog.component';
import { ConfirmDialogComponent } from '../../../shared/components/confirm-dialog/confirm-dialog.component';

@Component({
  selector: 'app-document-list',
  standalone: true,
  imports: [
    CommonModule, ReactiveFormsModule, RouterLink,
    MatTableModule, MatPaginatorModule, MatButtonModule, MatIconModule,
    MatInputModule, MatFormFieldModule,
    MatDialogModule, MatSnackBarModule, MatProgressBarModule, MatTooltipModule
  ],
  templateUrl: './document-list.component.html',
  styleUrl: './document-list.component.scss'
})
export class DocumentListComponent implements OnInit {
  @Input() id!: string;

  displayedColumns = ['title', 'updatedAt', 'actions'];
  documents = signal<DocumentResponse[]>([]);
  category = signal<CategoryResponse | null>(null);
  totalElements = signal(0);
  pageSize = 10;
  currentPage = 0;
  isLoading = signal(false);
  searchCtrl = new FormControl('');

  constructor(
    private documentService: DocumentService,
    private categoryService: CategoryService,
    private dialog: MatDialog,
    private snackBar: MatSnackBar
  ) {}

  ngOnInit(): void {
    this.categoryService.getCategory(this.id).subscribe(cat => this.category.set(cat));
    this.loadDocuments();
    this.searchCtrl.valueChanges.pipe(debounceTime(300), distinctUntilChanged())
      .subscribe(() => { this.currentPage = 0; this.loadDocuments(); });
  }

  loadDocuments(): void {
    this.isLoading.set(true);
    this.documentService.listDocuments(this.id, this.currentPage, this.pageSize, this.searchCtrl.value || undefined)
      .subscribe({
        next: page => { this.documents.set(page.content); this.totalElements.set(page.totalElements); this.isLoading.set(false); },
        error: () => { this.isLoading.set(false); }
      });
  }

  onPage(event: PageEvent): void {
    this.currentPage = event.pageIndex;
    this.pageSize = event.pageSize;
    this.loadDocuments();
  }

  openCreate(): void {
    this.dialog.open(DocumentFormDialogComponent, { width: '680px', data: { categoryId: this.id } })
      .afterClosed().subscribe(result => { if (result) this.loadDocuments(); });
  }

  openEdit(doc: DocumentResponse): void {
    this.dialog.open(DocumentFormDialogComponent, { width: '680px', data: doc })
      .afterClosed().subscribe(result => { if (result) this.loadDocuments(); });
  }

  deleteDocument(doc: DocumentResponse): void {
    this.dialog.open(ConfirmDialogComponent, {
      width: '400px',
      data: {
        title: 'Delete document',
        message: `"${doc.title}" will be permanently deleted and cannot be recovered.`,
        confirmText: 'Delete document'
      }
    }).afterClosed().subscribe(confirmed => {
      if (!confirmed) return;
      this.documentService.deleteDocument(doc.id).subscribe({
        next: () => { this.snackBar.open(`"${doc.title}" deleted`, 'Dismiss', { duration: 3000 }); this.loadDocuments(); },
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
