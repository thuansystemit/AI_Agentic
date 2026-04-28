import { Component, Inject, OnInit, inject, signal, ElementRef, ViewChild } from '@angular/core';
import { ReactiveFormsModule, FormBuilder, Validators } from '@angular/forms';
import { MatDialogRef, MAT_DIALOG_DATA, MatDialogModule } from '@angular/material/dialog';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatTooltipModule } from '@angular/material/tooltip';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { DocumentService } from '../../../core/services/document.service';
import { DocumentResponse } from '../../../core/models/document.model';
import { FieldComponent } from '../../../shared/components/field/field.component';

const MAX_FILE_SIZE = 50 * 1024 * 1024; // 50 MB

const ALLOWED_MIME_TYPES = new Set([
  'text/plain',
  'text/markdown',
  'text/csv',
  'text/xml',
  'application/xml',
  'application/json',
]);

const ALLOWED_EXTENSIONS = new Set(['.txt', '.md', '.csv', '.json', '.xml']);

const ALLOWED_TYPES_LABEL = '.txt, .md, .csv, .json, .xml';

@Component({
  selector: 'app-document-form-dialog',
  standalone: true,
  imports: [
    ReactiveFormsModule,
    MatDialogModule,
    MatButtonModule,
    MatIconModule,
    MatSnackBarModule,
    MatTooltipModule,
    FieldComponent,
  ],
  templateUrl: './document-form-dialog.component.html',
  styleUrl: './document-form-dialog.component.scss'
})
export class DocumentFormDialogComponent implements OnInit {
  @ViewChild('fileInput') fileInput!: ElementRef<HTMLInputElement>;

  private fb = inject(FormBuilder);

  readonly allowedTypesLabel = ALLOWED_TYPES_LABEL;
  readonly acceptAttr = Array.from(ALLOWED_EXTENSIONS).join(',');

  isEdit = false;
  loading = signal(false);
  selectedFile = signal<File | null>(null);
  dragOver = signal(false);
  fileError = signal('');

  form = this.fb.group({
    title: ['', Validators.required],
  });

  constructor(
    private documentService: DocumentService,
    private snackBar: MatSnackBar,
    private dialogRef: MatDialogRef<DocumentFormDialogComponent>,
    @Inject(MAT_DIALOG_DATA) public data: DocumentResponse & { categoryId?: string }
  ) {}

  ngOnInit(): void {
    if (this.data?.id) {
      this.isEdit = true;
      this.form.patchValue({ title: this.data.title });
    }
  }

  onDragOver(event: DragEvent): void {
    event.preventDefault();
    event.stopPropagation();
    this.dragOver.set(true);
  }

  onDragLeave(event: DragEvent): void {
    event.preventDefault();
    event.stopPropagation();
    this.dragOver.set(false);
  }

  onDrop(event: DragEvent): void {
    event.preventDefault();
    event.stopPropagation();
    this.dragOver.set(false);
    const file = event.dataTransfer?.files?.[0] ?? null;
    this.applyFile(file);
  }

  onFileInputChange(event: Event): void {
    const input = event.target as HTMLInputElement;
    const file = input.files?.[0] ?? null;
    this.applyFile(file);
    input.value = '';
  }

  browseFiles(): void {
    this.fileInput.nativeElement.click();
  }

  clearFile(): void {
    this.selectedFile.set(null);
    this.fileError.set('');
  }

  formatFileSize(bytes: number): string {
    if (bytes < 1024) return `${bytes} B`;
    if (bytes < 1024 * 1024) return `${(bytes / 1024).toFixed(1)} KB`;
    return `${(bytes / (1024 * 1024)).toFixed(1)} MB`;
  }

  save(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }

    if (!this.isEdit && !this.selectedFile()) {
      this.fileError.set('Please select a file to upload.');
      return;
    }

    this.loading.set(true);
    const title = this.form.getRawValue().title!;
    const file = this.selectedFile();

    if (this.isEdit && !file) {
      this.documentService.updateDocument(this.data.id, { title }).subscribe({
        next: () => {
          this.snackBar.open('Saved', 'OK', { duration: 2000 });
          this.dialogRef.close(true);
        },
        error: (err: { status: number; error?: { detail?: string } }) => {
          this.loading.set(false);
          this.snackBar.open(
            err.status === 403
              ? "You don't have permission to save this document."
              : (err.error?.detail || 'Error saving document.'),
            'OK',
            { duration: 3000 }
          );
        }
      });
      return;
    }

    const formData = new FormData();
    formData.append('file', file!);
    formData.append('title', title);
    if (!this.isEdit) {
      formData.append('categoryId', this.data.categoryId!);
    }

    this.documentService.uploadDocument(formData).subscribe({
      next: () => {
        this.snackBar.open('Saved', 'OK', { duration: 2000 });
        this.dialogRef.close(true);
      },
      error: (err: { status: number; error?: { detail?: string } }) => {
        this.loading.set(false);
        this.snackBar.open(
          err.status === 403
            ? "You don't have permission to save this document."
            : (err.error?.detail || 'Error saving document.'),
          'OK',
          { duration: 3000 }
        );
      }
    });
  }

  private applyFile(file: File | null): void {
    this.fileError.set('');
    if (!file) return;

    const ext = '.' + file.name.split('.').pop()?.toLowerCase();
    const typeOk = ALLOWED_MIME_TYPES.has(file.type) || ALLOWED_EXTENSIONS.has(ext);
    if (!typeOk) {
      this.fileError.set(`File type not supported. Allowed types: ${ALLOWED_TYPES_LABEL}.`);
      return;
    }

    if (file.size > MAX_FILE_SIZE) {
      this.fileError.set(`File is too large (${this.formatFileSize(file.size)}). Maximum allowed size is 50 MB.`);
      return;
    }

    this.selectedFile.set(file);
  }
}
