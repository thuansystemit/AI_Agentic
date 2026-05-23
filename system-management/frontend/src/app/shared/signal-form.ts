import { computed, signal, Signal, WritableSignal } from '@angular/core';

export interface FieldState<T> {
  value:   WritableSignal<T>;
  touched: WritableSignal<boolean>;
  error:   Signal<string | null>;
}

export function textField(
  initial: string,
  validate: (v: string) => string | null = () => null
): FieldState<string> {
  const value   = signal(initial);
  const touched = signal(false);
  return { value, touched, error: computed(() => validate(value())) };
}

export function required(label: string): (v: string) => string | null {
  return v => v.trim() ? null : `${label} is required`;
}

export function minLength(n: number): (v: string) => string | null {
  return v => v.length >= n ? null : `Minimum ${n} characters required`;
}

export function emailFormat(v: string): string | null {
  return /^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(v) ? null : 'Enter a valid email address';
}

export function composeValidators(
  ...fns: Array<(v: string) => string | null>
): (v: string) => string | null {
  return v => fns.reduce<string | null>((err, fn) => err ?? fn(v), null);
}
