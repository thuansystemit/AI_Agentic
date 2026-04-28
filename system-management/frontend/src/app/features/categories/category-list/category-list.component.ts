import { Component, OnInit, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router } from '@angular/router';
import { MatCardModule } from '@angular/material/card';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatDialog, MatDialogModule } from '@angular/material/dialog';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { MatTooltipModule } from '@angular/material/tooltip';
import { CategoryService } from '../../../core/services/category.service';
import { CategoryResponse } from '../../../core/models/category.model';
import { AuthService } from '../../../core/services/auth.service';
import { CategoryFormDialogComponent } from '../category-form/category-form-dialog.component';
import { CategoryPermissionsDialogComponent } from '../category-permission/category-permissions-dialog.component';
import { ConfirmDialogComponent } from '../../../shared/components/confirm-dialog/confirm-dialog.component';

@Component({
  selector: 'app-category-list',
  standalone: true,
  imports: [
    CommonModule,
    MatCardModule, MatButtonModule, MatIconModule,
    MatDialogModule, MatSnackBarModule, MatProgressBarModule, MatTooltipModule
  ],
  templateUrl: './category-list.component.html',
  styleUrl: './category-list.component.scss'
})
export class CategoryListComponent implements OnInit {
  categories = signal<CategoryResponse[]>([]);
  isLoading = signal(false);

  constructor(
    private categoryService: CategoryService,
    readonly auth: AuthService,
    private router: Router,
    private dialog: MatDialog,
    private snackBar: MatSnackBar
  ) {}

  ngOnInit(): void { this.loadCategories(); }

  loadCategories(): void {
    this.isLoading.set(true);
    this.categoryService.listCategories().subscribe({
      next: page => { this.categories.set(page.content); this.isLoading.set(false); },
      error: () => { this.isLoading.set(false); }
    });
  }

  openDocuments(cat: CategoryResponse): void {
    this.router.navigate(['/categories', cat.id, 'documents']);
  }

  openCreate(): void {
    this.dialog.open(CategoryFormDialogComponent, { width: '440px' })
      .afterClosed().subscribe(result => { if (result) this.loadCategories(); });
  }

  openEdit(cat: CategoryResponse): void {
    this.dialog.open(CategoryFormDialogComponent, { width: '440px', data: cat })
      .afterClosed().subscribe(result => { if (result) this.loadCategories(); });
  }

  openPermissions(cat: CategoryResponse): void {
    this.dialog.open(CategoryPermissionsDialogComponent, { width: '680px', data: cat });
  }

  deleteCategory(cat: CategoryResponse): void {
    this.dialog.open(ConfirmDialogComponent, {
      width: '400px',
      data: {
        title: 'Delete category',
        message: `"${cat.name}" and all its documents will be permanently deleted. This cannot be undone.`,
        confirmText: 'Delete category'
      }
    }).afterClosed().subscribe(confirmed => {
      if (!confirmed) return;
      this.categoryService.deleteCategory(cat.id).subscribe({
        next: () => { this.snackBar.open('Category deleted', 'Dismiss', { duration: 3000 }); this.loadCategories(); },
        error: () => this.snackBar.open('Failed to delete category. Please try again.', 'Dismiss', { duration: 4000 })
      });
    });
  }
}
