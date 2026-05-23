import { Component, computed, input, output, signal } from '@angular/core';
import { MatIconModule } from '@angular/material/icon';

export type FieldType = 'text' | 'email' | 'password' | 'textarea';

@Component({
  selector: 'app-field',
  standalone: true,
  imports: [MatIconModule],
  templateUrl: './field.component.html',
  styleUrl: './field.component.scss'
})
export class FieldComponent {
  label        = input('');
  type         = input<FieldType>('text');
  value        = input('');
  touched      = input(false);
  error        = input<string | null>(null);
  placeholder  = input('');
  prefixIcon   = input('');
  hint         = input('');
  rows         = input(4);
  autocomplete = input('');
  disabled     = input(false);

  valueChange = output<string>();
  blur        = output<void>();

  showPassword = signal(false);

  readonly fieldId = `field-${Math.random().toString(36).slice(2, 8)}`;

  readonly hasError = computed(() => !!this.error() && this.touched());

  readonly inputType = computed(() =>
    this.type() === 'password'
      ? (this.showPassword() ? 'text' : 'password')
      : this.type()
  );

  onInput(event: Event): void {
    this.valueChange.emit((event.target as HTMLInputElement).value);
  }

  onBlur(): void {
    this.blur.emit();
  }
}
