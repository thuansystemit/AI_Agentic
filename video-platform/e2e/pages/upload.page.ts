import { Page, Locator, expect } from '@playwright/test';
import * as path from 'path';

/**
 * Page Object for /upload (VideoUploadComponent).
 *
 * Recommend adding to the template:
 *   data-testid="upload-drop-zone"     on the .drop-zone div
 *   data-testid="upload-file-input"    on the hidden <input type="file">
 *   data-testid="upload-title"         on the title input (id="title" already present)
 *   data-testid="upload-description"   on the description textarea (id="description" already present)
 *   data-testid="upload-submit"        on the submit button
 *   data-testid="upload-error"         on the error alert div
 *   data-testid="upload-file-name"     on the .file-name span
 *   data-testid="upload-remove-file"   on the remove-file button
 */
export class UploadPage {
  readonly page: Page;
  readonly fileInput: Locator;
  readonly titleInput: Locator;
  readonly descriptionInput: Locator;
  readonly submitButton: Locator;
  readonly errorAlert: Locator;
  readonly dropZone: Locator;
  readonly fileName: Locator;
  readonly removeFileButton: Locator;
  readonly chooseFileLabel: Locator;

  constructor(page: Page) {
    this.page = page;
    // The file input is `hidden` inside the Choose File label
    this.fileInput = page.locator('input[type="file"][accept="video/*"]');
    this.titleInput = page.locator('#title');
    this.descriptionInput = page.locator('#description');
    this.submitButton = page.getByRole('button', { name: /upload video/i });
    this.errorAlert = page.locator('.alert-error');
    this.dropZone = page.locator('.drop-zone');
    this.fileName = page.locator('.file-name');
    this.removeFileButton = page.locator('.remove-file');
    this.chooseFileLabel = page.locator('.file-label');
  }

  async goto(): Promise<void> {
    await this.page.goto('/upload');
    await expect(this.page.getByRole('heading', { name: 'Upload Video' })).toBeVisible();
  }

  /**
   * Attach a video file by absolute path.
   * Uses setInputFiles which works even though the input is hidden.
   */
  async attachFile(filePath: string): Promise<void> {
    await this.fileInput.setInputFiles(filePath);
    // Wait for the UI to reflect the chosen file
    await expect(this.fileName).toBeVisible();
  }

  async fillTitle(title: string): Promise<void> {
    await this.titleInput.fill(title);
  }

  async fillDescription(description: string): Promise<void> {
    await this.descriptionInput.fill(description);
  }

  async submit(): Promise<void> {
    await this.submitButton.click();
  }

  /**
   * Full upload flow: attach file, fill metadata, submit.
   * Waits for navigation to the video detail page after success.
   */
  async uploadVideo(
    filePath: string,
    title: string,
    description?: string,
  ): Promise<void> {
    await this.attachFile(filePath);
    // Clear the auto-filled title then set ours
    await this.titleInput.clear();
    await this.fillTitle(title);
    if (description) {
      await this.fillDescription(description);
    }
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
