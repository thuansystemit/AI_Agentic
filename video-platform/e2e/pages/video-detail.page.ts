import { Page, Locator, expect } from '@playwright/test';

/**
 * Page Object for /videos/:id (VideoDetailComponent).
 *
 * Recommend adding to the template:
 *   data-testid="video-player"        on the <video> element
 *   data-testid="video-title"         on the h1.video-title
 *   data-testid="video-owner"         on the .video-owner span
 *   data-testid="video-description"   on the .video-description p
 *   data-testid="btn-edit"            on the Edit button
 *   data-testid="btn-delete"          on the Delete button
 *   data-testid="btn-confirm-delete"  on the "Yes, Delete" button
 *   data-testid="btn-cancel-delete"   on the Cancel (delete) button
 *   data-testid="edit-title-input"    on the editTitle input
 *   data-testid="edit-description-input" on the editDescription textarea
 *   data-testid="btn-save-edit"       on the Save button
 *   data-testid="btn-cancel-edit"     on the Cancel (edit) button
 */
export class VideoDetailPage {
  readonly page: Page;
  readonly videoPlayer: Locator;
  readonly videoTitle: Locator;
  readonly videoOwner: Locator;
  readonly videoDescription: Locator;
  readonly editButton: Locator;
  readonly deleteButton: Locator;
  readonly confirmDeleteButton: Locator;
  readonly cancelDeleteButton: Locator;
  readonly editTitleInput: Locator;
  readonly editDescriptionInput: Locator;
  readonly saveEditButton: Locator;
  readonly cancelEditButton: Locator;
  readonly loadingIndicator: Locator;
  readonly errorAlert: Locator;
  readonly backToBrowseLink: Locator;

  constructor(page: Page) {
    this.page = page;
    this.videoPlayer = page.locator('video.video-player');
    this.videoTitle = page.locator('h1.video-title');
    this.videoOwner = page.locator('.video-meta .video-owner');
    this.videoDescription = page.locator('.video-description');
    this.editButton = page.getByRole('button', { name: 'Edit' });
    this.deleteButton = page.getByRole('button', { name: 'Delete' }).first();
    this.confirmDeleteButton = page.getByRole('button', { name: 'Yes, Delete' });
    this.cancelDeleteButton = page
      .locator('.delete-confirm')
      .getByRole('button', { name: 'Cancel' });
    this.editTitleInput = page.locator('#editTitle');
    this.editDescriptionInput = page.locator('#editDescription');
    this.saveEditButton = page.getByRole('button', { name: 'Save' });
    this.cancelEditButton = page
      .locator('.edit-actions')
      .getByRole('button', { name: 'Cancel' });
    this.loadingIndicator = page.locator('.loading');
    this.errorAlert = page.locator('.alert-error');
    this.backToBrowseLink = page.getByRole('link', { name: 'Back to Browse' });
  }

  async gotoById(id: number): Promise<void> {
    await this.page.goto(`/videos/${id}`);
    await this.loadingIndicator.waitFor({ state: 'hidden' });
  }

  async waitForVideoToLoad(): Promise<void> {
    await this.loadingIndicator.waitFor({ state: 'hidden' });
    await expect(this.videoPlayer).toBeVisible();
  }

  async getTitleText(): Promise<string> {
    return (await this.videoTitle.textContent()) ?? '';
  }

  async startEdit(): Promise<void> {
    await this.editButton.click();
    await expect(this.editTitleInput).toBeVisible();
  }

  async saveEdit(title: string, description?: string): Promise<void> {
    await this.editTitleInput.fill(title);
    if (description !== undefined) {
      await this.editDescriptionInput.fill(description);
    }
    await this.saveEditButton.click();
  }

  async deleteVideo(): Promise<void> {
    await this.deleteButton.click();
    await this.confirmDeleteButton.click();
  }

  async expectOwnerActionsVisible(): Promise<void> {
    await expect(this.editButton).toBeVisible();
    await expect(this.deleteButton).toBeVisible();
  }

  async expectOwnerActionsHidden(): Promise<void> {
    await expect(this.editButton).not.toBeVisible();
    await expect(this.deleteButton).not.toBeVisible();
  }
}
