import { Page, Locator, expect } from '@playwright/test';

/**
 * Page Object for /my-videos (MyVideosComponent).
 *
 * Recommend adding to the template:
 *   data-testid="my-videos-grid"        on the .video-grid div
 *   data-testid="my-video-card"         on each <app-video-card> host element
 *   data-testid="my-videos-empty"       on the .empty-state div
 *   data-testid="my-videos-count"       on the .results-info span
 *   data-testid="btn-upload-new"        on the "Upload New Video" link
 *   data-testid="pagination-prev"       on the Previous button
 *   data-testid="pagination-next"       on the Next button
 */
export class MyVideosPage {
  readonly page: Page;
  readonly videoGrid: Locator;
  readonly videoCards: Locator;
  readonly emptyState: Locator;
  readonly resultsInfo: Locator;
  readonly uploadNewButton: Locator;
  readonly prevButton: Locator;
  readonly nextButton: Locator;
  readonly loadingIndicator: Locator;

  constructor(page: Page) {
    this.page = page;
    this.videoGrid = page.locator('.video-grid');
    this.videoCards = page.locator('.video-card');
    this.emptyState = page.locator('.empty-state');
    this.resultsInfo = page.locator('.results-info');
    this.uploadNewButton = page.getByRole('link', { name: 'Upload New Video' });
    this.prevButton = page.getByRole('button', { name: 'Previous' });
    this.nextButton = page.getByRole('button', { name: 'Next' });
    this.loadingIndicator = page.locator('.loading');
  }

  async goto(): Promise<void> {
    await this.page.goto('/my-videos');
    await expect(this.page.getByRole('heading', { name: 'My Videos' })).toBeVisible();
  }

  async waitForLoad(): Promise<void> {
    await this.loadingIndicator.waitFor({ state: 'hidden' });
  }

  async getVideoCount(): Promise<number> {
    return this.videoCards.count();
  }

  async expectVideosVisible(): Promise<void> {
    await expect(this.videoGrid).toBeVisible();
    const count = await this.getVideoCount();
    expect(count).toBeGreaterThan(0);
  }

  async expectEmptyState(): Promise<void> {
    await expect(this.emptyState).toBeVisible();
    await expect(
      this.emptyState.getByText('You have not uploaded any videos yet'),
    ).toBeVisible();
  }

  async expectVideoWithTitle(title: string): Promise<void> {
    await expect(
      this.videoCards.filter({ has: this.page.locator('.video-title', { hasText: title }) }),
    ).toBeVisible();
  }

  async clickVideoByTitle(title: string): Promise<void> {
    await this.videoCards
      .filter({ has: this.page.locator('.video-title', { hasText: title }) })
      .first()
      .click();
  }
}
