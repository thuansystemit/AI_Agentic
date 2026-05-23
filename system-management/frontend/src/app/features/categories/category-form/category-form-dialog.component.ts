import { Component, computed, inject, signal } from '@angular/core';
import { MatDialogRef, MAT_DIALOG_DATA, MatDialogModule } from '@angular/material/dialog';
import { MatButtonModule } from '@angular/material/button';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { CategoryService } from '../../../core/services/category.service';
import { CategoryResponse } from '../../../core/models/category.model';
import { FieldComponent } from '../../../shared/components/field/field.component';
import { composeValidators, minLength, required, textField } from '../../../shared/signal-form';

@Component({
  selector: 'app-category-form-dialog',
  standalone: true,
  imports: [MatDialogModule, MatButtonModule, MatSnackBarModule, FieldComponent],
  templateUrl: './category-form-dialog.component.html',
  styleUrl: './category-form-dialog.component.scss'
})
export class CategoryFormDialogComponent {
  private categoryService = inject(CategoryService);
  private snackBar        = inject(MatSnackBar);
  private dialogRef       = inject(MatDialogRef<CategoryFormDialogComponent>);
  readonly data           = inject<CategoryResponse | null>(MAT_DIALOG_DATA);

  name        = textField(this.data?.name ?? '', composeValidators(required('Category name'), minLength(2)));
  description = textField(this.data?.description ?? '');
  loading     = signal(false);

  formValid = computed(() => !this.name.error());

  save(): void {
    this.name.touched.set(true);
    if (!this.formValid()) return;

    this.loading.set(true);
    const obs = this.data
      ? this.categoryService.updateCategory(this.data.id, {
          name:        this.name.value(),
          description: this.description.value()
        })
      : this.categoryService.createCategory({
          name:        this.name.value(),
          description: this.description.value()
        });

    obs.subscribe({
      next:  () => { this.snackBar.open('Saved', 'OK', { duration: 2000 }); this.dialogRef.close(true); },
      error: err => {
        this.loading.set(false);
        this.snackBar.open(err.error?.detail || 'Error saving category.', 'OK', { duration: 3000 });
      }
    });
  }
}
