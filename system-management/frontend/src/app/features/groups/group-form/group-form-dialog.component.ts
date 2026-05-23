import { Component, computed, inject, signal } from '@angular/core';
import { MatDialogRef, MAT_DIALOG_DATA, MatDialogModule } from '@angular/material/dialog';
import { MatButtonModule } from '@angular/material/button';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { GroupService } from '../../../core/services/group.service';
import { GroupResponse } from '../../../core/models/group.model';
import { FieldComponent } from '../../../shared/components/field/field.component';
import { composeValidators, minLength, required, textField } from '../../../shared/signal-form';

@Component({
  selector: 'app-group-form-dialog',
  standalone: true,
  imports: [MatDialogModule, MatButtonModule, MatSnackBarModule, FieldComponent],
  templateUrl: './group-form-dialog.component.html',
  styleUrl: './group-form-dialog.component.scss'
})
export class GroupFormDialogComponent {
  private groupService = inject(GroupService);
  private snackBar     = inject(MatSnackBar);
  private dialogRef    = inject(MatDialogRef<GroupFormDialogComponent>);
  readonly data        = inject<GroupResponse | null>(MAT_DIALOG_DATA);

  name        = textField(this.data?.name ?? '', composeValidators(required('Group name'), minLength(2)));
  description = textField(this.data?.description ?? '');
  loading     = signal(false);

  formValid = computed(() => !this.name.error());

  save(): void {
    this.name.touched.set(true);
    if (!this.formValid()) return;

    this.loading.set(true);
    const obs = this.data
      ? this.groupService.updateGroup(this.data.id, {
          name:        this.name.value(),
          description: this.description.value()
        })
      : this.groupService.createGroup({
          name:        this.name.value(),
          description: this.description.value()
        });

    obs.subscribe({
      next:  () => { this.snackBar.open('Saved', 'OK', { duration: 2000 }); this.dialogRef.close(true); },
      error: err => {
        this.loading.set(false);
        this.snackBar.open(err.error?.detail || 'Error saving group.', 'OK', { duration: 3000 });
      }
    });
  }
}
