import { Component, Input } from '@angular/core';
import { AbstractControl, FormControl, ReactiveFormsModule } from '@angular/forms';
import { MatIconModule } from '@angular/material/icon';
import { NgClass } from '@angular/common';

export type FieldType = 'text' | 'email' | 'password' | 'textarea';

@Component({
  selector: 'app-field',
  standalone: true,
  imports: [ReactiveFormsModule, MatIconModule, NgClass],
  templateUrl: './field.component.html',
  styleUrl: './field.component.scss'
})
export class FieldComponent {
  @Input({ required: true }) control!: AbstractControl;
  @Input() label = '';
  @Input() type: FieldType = 'text';
  @Input() placeholder = '';
  @Input() prefixIcon = '';
  @Input() hint = '';
  @Input() rows = 4;
  @Input() autocomplete = '';
  @Input() errors: Partial<Record<string, string>> = {};

  showPassword = false;
  readonly fieldId = `field-${Math.random().toString(36).slice(2, 8)}`;

  get fc(): FormControl {
    return this.control as FormControl;
  }

  get inputType(): string {
    return this.type === 'password' ? (this.showPassword ? 'text' : 'password') : this.type;
  }

  get errorMessage(): string {
    const ctrl = this.control;
    if (!ctrl?.errors || !ctrl.touched) return '';
    const key = Object.keys(ctrl.errors)[0];
    return this.errors[key] ?? '';
  }
}
