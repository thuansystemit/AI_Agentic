import { Page, Locator, expect } from '@playwright/test';

/**
 * Page Object for the global <app-header> component.
 *
 * The current header HTML does not use data-testid attributes.
 * Selectors fall back to ARIA roles, element types, and stable text content.
 * Corresponding data-testid attributes to add are listed in comments.
 */
export class HeaderPage {
  readonly page: Page;

  // Nav links
  // Recommend adding: data-testid="nav-browse", data-testid="nav-upload",
  //                   data-testid="nav-my-videos"
  readonly browseLinkLocator: Locator;
  readonly uploadLinkLocator: Locator;
  readonly myVideosLinkLocator: Locator;

  // Auth section
  // Recommend adding: data-testid="btn-logout", data-testid="btn-login",
  //                   data-testid="btn-register", data-testid="header-username"
  readonly loginButtonLocator: Locator;
  readonly registerButtonLocator: Locator;
  readonly logoutButtonLocator: Locator;
  readonly usernameLocator: Locator;
  readonly logoLocator: Locator;

  constructor(page: Page) {
    this.page = page;
    this.browseLinkLocator = page.getByRole('link', { name: 'Browse' });
    this.uploadLinkLocator = page.getByRole('link', { name: 'Upload' });
    this.myVideosLinkLocator = page.getByRole('link', { name: 'My Videos' });
    this.loginButtonLocator = page.getByRole('link', { name: 'Login' });
    this.registerButtonLocator = page.getByRole('link', { name: 'Register' });
    this.logoutButtonLocator = page.getByRole('button', { name: 'Logout' });
    this.usernameLocator = page.locator('header .username');
    this.logoLocator = page.getByRole('link', { name: 'VideoPlatform' });
  }

  async expectGuestState(): Promise<void> {
    await expect(this.loginButtonLocator).toBeVisible();
    await expect(this.registerButtonLocator).toBeVisible();
    await expect(this.logoutButtonLocator).not.toBeVisible();
    await expect(this.uploadLinkLocator).not.toBeVisible();
    await expect(this.myVideosLinkLocator).not.toBeVisible();
  }

  async expectAuthenticatedState(username: string): Promise<void> {
    await expect(this.logoutButtonLocator).toBeVisible();
    await expect(this.usernameLocator).toContainText(username);
    await expect(this.uploadLinkLocator).toBeVisible();
    await expect(this.myVideosLinkLocator).toBeVisible();
    await expect(this.loginButtonLocator).not.toBeVisible();
    await expect(this.registerButtonLocator).not.toBeVisible();
  }

  async clickLogout(): Promise<void> {
    await this.logoutButtonLocator.click();
  }

  async clickLogin(): Promise<void> {
    await this.loginButtonLocator.click();
  }

  async clickRegister(): Promise<void> {
    await this.registerButtonLocator.click();
  }

  async clickUpload(): Promise<void> {
    await this.uploadLinkLocator.click();
  }

  async clickMyVideos(): Promise<void> {
    await this.myVideosLinkLocator.click();
  }

  async clickLogo(): Promise<void> {
    await this.logoLocator.click();
  }
}
