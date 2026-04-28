import { Component, Inject, OnInit, inject } from '@angular/core';
import { ReactiveFormsModule, FormBuilder, Validators } from '@angular/forms';
import { MatDialogRef, MAT_DIALOG_DATA, MatDialogModule } from '@angular/material/dialog';
import { MatButtonModule } from '@angular/material/button';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { GroupService } from '../../../core/services/group.service';
import { GroupResponse } from '../../../core/models/group.model';
import { FieldComponent } from '../../../shared/components/field/field.component';

@Component({
  selector: 'app-group-form-dialog',
  standalone: true,
  imports: [ReactiveFormsModule, MatDialogModule, MatButtonModule, MatSnackBarModule, FieldComponent],
  templateUrl: './group-form-dialog.component.html',
  styleUrl: './group-form-dialog.component.scss'
})
export class GroupFormDialogComponent implements OnInit {
  private fb = inject(FormBuilder);
  form = this.fb.group({
    name: ['', [Validators.required, Validators.minLength(2)]],
    description: ['']
  });

  constructor(
    private groupService: GroupService,
    private snackBar: MatSnackBar,
    private dialogRef: MatDialogRef<GroupFormDialogComponent>,
    @Inject(MAT_DIALOG_DATA) public data: GroupResponse | null
  ) {}

  ngOnInit(): void {
    if (this.data) this.form.patchValue({ name: this.data.name, description: this.data.description });
  }

  save(): void {
    if (this.form.invalid) { this.form.markAllAsTouched(); return; }
    const v = this.form.getRawValue();
    const obs = this.data
      ? this.groupService.updateGroup(this.data.id, { name: v.name!, description: v.description! })
      : this.groupService.createGroup({ name: v.name!, description: v.description! });
    obs.subscribe({
      next: () => { this.snackBar.open('Saved', 'OK', { duration: 2000 }); this.dialogRef.close(true); },
      error: err => this.snackBar.open(err.error?.detail || 'Error saving group.', 'OK', { duration: 3000 })
    });
  }
}
