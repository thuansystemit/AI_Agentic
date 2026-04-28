import { Component, Inject, OnInit, inject } from '@angular/core';
import { ReactiveFormsModule, FormBuilder, Validators } from '@angular/forms';
import { MatDialogRef, MAT_DIALOG_DATA, MatDialogModule } from '@angular/material/dialog';
import { MatButtonModule } from '@angular/material/button';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { CategoryService } from '../../../core/services/category.service';
import { CategoryResponse } from '../../../core/models/category.model';
import { FieldComponent } from '../../../shared/components/field/field.component';

@Component({
  selector: 'app-category-form-dialog',
  standalone: true,
  imports: [ReactiveFormsModule, MatDialogModule, MatButtonModule, MatSnackBarModule, FieldComponent],
  templateUrl: './category-form-dialog.component.html',
  styleUrl: './category-form-dialog.component.scss'
})
export class CategoryFormDialogComponent implements OnInit {
  private fb = inject(FormBuilder);
  form = this.fb.group({
    name: ['', [Validators.required, Validators.minLength(2)]],
    description: ['']
  });

  constructor(
    private categoryService: CategoryService,
    private snackBar: MatSnackBar,
    private dialogRef: MatDialogRef<CategoryFormDialogComponent>,
    @Inject(MAT_DIALOG_DATA) public data: CategoryResponse | null
  ) {}

  ngOnInit(): void {
    if (this.data) this.form.patchValue({ name: this.data.name, description: this.data.description });
  }

  save(): void {
    if (this.form.invalid) { this.form.markAllAsTouched(); return; }
    const v = this.form.getRawValue();
    const obs = this.data
      ? this.categoryService.updateCategory(this.data.id, { name: v.name!, description: v.description! })
      : this.categoryService.createCategory({ name: v.name!, description: v.description! });
    obs.subscribe({
      next: () => { this.snackBar.open('Saved', 'OK', { duration: 2000 }); this.dialogRef.close(true); },
      error: err => this.snackBar.open(err.error?.detail || 'Error saving category.', 'OK', { duration: 3000 })
    });
  }
}
