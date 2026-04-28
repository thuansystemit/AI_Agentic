import { Page, Locator, expect } from '@playwright/test';

/**
 * Page Object for /register (RegisterComponent).
 *
 * Selectors use the `id` attributes already present in the template.
 *
 * Recommend adding to the template:
 *   data-testid="register-username"        on the username input
 *   data-testid="register-email"           on the email input
 *   data-testid="register-password"        on the password input
 *   data-testid="register-confirm-pw"      on the confirmPassword input
 *   data-testid="register-submit"          on the submit button
 *   data-testid="register-error"           on the error alert div
 */
export class RegisterPage {
  readonly page: Page;
  readonly usernameInput: Locator;
  readonly emailInput: Locator;
  readonly passwordInput: Locator;
  readonly confirmPasswordInput: Locator;
  readonly submitButton: Locator;
  readonly errorAlert: Locator;
  readonly loginLink: Locator;

  constructor(page: Page) {
    this.page = page;
    this.usernameInput = page.locator('#username');
    this.emailInput = page.locator('#email');
    this.passwordInput = page.locator('#password');
    this.confirmPasswordInput = page.locator('#confirmPassword');
    this.submitButton = page.getByRole('button', { name: /create account/i });
    this.errorAlert = page.locator('.alert-error');
    this.loginLink = page.getByRole('link', { name: 'Sign In' });
  }

  async goto(): Promise<void> {
    await this.page.goto('/register');
    await expect(this.page.getByRole('heading', { name: 'Create Account' })).toBeVisible();
  }

  async fillUsername(username: string): Promise<void> {
    await this.usernameInput.fill(username);
  }

  async fillEmail(email: string): Promise<void> {
    await this.emailInput.fill(email);
  }

  async fillPassword(password: string): Promise<void> {
    await this.passwordInput.fill(password);
  }

  async fillConfirmPassword(password: string): Promise<void> {
    await this.confirmPasswordInput.fill(password);
  }

  async submit(): Promise<void> {
    await this.submitButton.click();
  }

  async register(username: string, email: string, password: string): Promise<void> {
    await this.fillUsername(username);
    await this.fillEmail(email);
    await this.fillPassword(password);
    await this.fillConfirmPassword(password);
    await this.submit();
  }

  async expectError(message?: string): Promise<void> {
    await expect(this.errorAlert).toBeVisible();
    if (message) {
      await expect(this.errorAlert).toContainText(message);
    }
  }
}
