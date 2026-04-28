import { Page, Locator, expect } from '@playwright/test';

/**
 * Page Object for / (VideoListComponent — the Browse Videos home page).
 *
 * Recommend adding to the template:
 *   data-testid="search-input"          on the search text input
 *   data-testid="search-button"         on the Search button
 *   data-testid="clear-search-button"   on the Clear button
 *   data-testid="video-grid"            on the .video-grid div
 *   data-testid="video-card"            on each <app-video-card> host element (or its inner <a>)
 *   data-testid="empty-state"           on the .empty-state div
 *   data-testid="results-info"          on the .results-info span
 *   data-testid="pagination-prev"       on the Previous button
 *   data-testid="pagination-next"       on the Next button
 */
export class VideoListPage {
  readonly page: Page;
  readonly searchInput: Locator;
  readonly searchButton: Locator;
  readonly clearButton: Locator;
  readonly videoGrid: Locator;
  readonly videoCards: Locator;
  readonly emptyState: Locator;
  readonly resultsInfo: Locator;
  readonly prevButton: Locator;
  readonly nextButton: Locator;
  readonly loadingIndicator: Locator;

  constructor(page: Page) {
    this.page = page;
    this.searchInput = page.locator('.search-input');
    this.searchButton = page.getByRole('button', { name: 'Search' });
    this.clearButton = page.getByRole('button', { name: 'Clear' });
    this.videoGrid = page.locator('.video-grid');
    // Each video card renders as an <a> element with class .video-card
    this.videoCards = page.locator('.video-card');
    this.emptyState = page.locator('.empty-state');
    this.resultsInfo = page.locator('.results-info');
    this.prevButton = page.getByRole('button', { name: 'Previous' });
    this.nextButton = page.getByRole('button', { name: 'Next' });
    this.loadingIndicator = page.locator('.loading');
  }

  async goto(): Promise<void> {
    await this.page.goto('/');
    await expect(this.page.getByRole('heading', { name: 'Browse Videos' })).toBeVisible();
  }

  async waitForVideosToLoad(): Promise<void> {
    // Wait for the loading indicator to disappear
    await this.loadingIndicator.waitFor({ state: 'hidden' });
  }

  async search(query: string): Promise<void> {
    await this.searchInput.fill(query);
    await this.searchButton.click();
    await this.waitForVideosToLoad();
  }

  async clearSearch(): Promise<void> {
    await this.clearButton.click();
    await this.waitForVideosToLoad();
  }

  async getVideoCardCount(): Promise<number> {
    return this.videoCards.count();
  }

  async clickFirstVideo(): Promise<void> {
    await this.videoCards.first().click();
  }

  async clickVideoByTitle(title: string): Promise<void> {
    await this.videoCards
      .filter({ has: this.page.locator('.video-title', { hasText: title }) })
      .first()
      .click();
  }

  async expectVideosVisible(): Promise<void> {
    await expect(this.videoGrid).toBeVisible();
    const count = await this.getVideoCardCount();
    expect(count).toBeGreaterThan(0);
  }

  async expectEmptyState(): Promise<void> {
    await expect(this.emptyState).toBeVisible();
  }
}
