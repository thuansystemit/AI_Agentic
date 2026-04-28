import { Page, Locator, expect } from '@playwright/test';

/**
 * Page Object for /login (LoginComponent).
 *
 * Selectors target the `id` attributes already present in the template
 * (id="email", id="password"). The submit button is identified by its label text.
 *
 * Recommend adding to the template:
 *   data-testid="login-email"        on the email input
 *   data-testid="login-password"     on the password input
 *   data-testid="login-submit"       on the submit button
 *   data-testid="login-error"        on the error alert div
 */
export class LoginPage {
  readonly page: Page;
  readonly emailInput: Locator;
  readonly passwordInput: Locator;
  readonly submitButton: Locator;
  readonly errorAlert: Locator;
  readonly registerLink: Locator;

  constructor(page: Page) {
    this.page = page;
    this.emailInput = page.locator('#email');
    this.passwordInput = page.locator('#password');
    this.submitButton = page.getByRole('button', { name: /sign in/i });
    this.errorAlert = page.locator('.alert-error');
    this.registerLink = page.getByRole('link', { name: 'Register' });
  }

  async goto(): Promise<void> {
    await this.page.goto('/login');
    await expect(this.page.getByRole('heading', { name: 'Welcome Back' })).toBeVisible();
  }

  async fillEmail(email: string): Promise<void> {
    await this.emailInput.fill(email);
  }

  async fillPassword(password: string): Promise<void> {
    await this.passwordInput.fill(password);
  }

  async submit(): Promise<void> {
    await this.submitButton.click();
  }

  async login(email: string, password: string): Promise<void> {
    await this.fillEmail(email);
    await this.fillPassword(password);
    await this.submit();
  }

  async expectError(message?: string): Promise<void> {
    await expect(this.errorAlert).toBeVisible();
    if (message) {
      await expect(this.errorAlert).toContainText(message);
    }
  }

  async expectSubmitDisabled(): Promise<void> {
    await expect(this.submitButton).toBeDisabled();
  }
}
