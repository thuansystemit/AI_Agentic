import { test, expect } from '@playwright/test';
import { UploadPage } from '../pages/upload.page';
import { VideoDetailPage } from '../pages/video-detail.page';
import { LoginPage } from '../pages/login.page';
import { HeaderPage } from '../pages/header.page';
import { createTestUser, setAuthInBrowser } from '../fixtures/api';
import { TEST_VIDEO_PATH, assertTestVideoExists } from '../fixtures/video-fixture';

/**
 * Video upload flows:
 *  1. Unauthenticated user visiting /upload is redirected to /login (authGuard)
 *  2. Authenticated user sees the Upload Video page
 *  3. Submit button is disabled when no file is selected
 *  4. Authenticated user can attach a file and see the file name in the UI
 *  5. Authenticated user can upload a video (file + title + description)
 *     and is redirected to the new video's detail page
 *  6. Upload without a title shows a validation error
 *  7. Invalid file type shows a validation error
 */

test.describe('Video Upload', () => {
  // ------------------------------------------------------------------
  // Guard: unauthenticated redirect
  // ------------------------------------------------------------------
  test('unauthenticated user visiting /upload is redirected to /login', async ({ page }) => {
    // Navigate without any auth tokens in localStorage
    await page.goto('/upload');

    await expect(page).toHaveURL('/login');
    await expect(page.getByRole('heading', { name: 'Welcome Back' })).toBeVisible();
  });

  test('Upload link is not visible in the header for guests', async ({ page }) => {
    await page.goto('/');
    const header = new HeaderPage(page);
    await expect(header.uploadLinkLocator).not.toBeVisible();
  });

  // ------------------------------------------------------------------
  // Authenticated upload flows
  // ------------------------------------------------------------------
  test.describe('Authenticated upload', () => {
    /**
     * Log in via UI in beforeEach to ensure the Angular app's auth state
     * is populated through its normal flow (localStorage + signals).
     */
    test.beforeEach(async ({ page, request }) => {
      const user = await createTestUser(request);

      // Seed auth via localStorage so we skip the login UI form
      // Navigate first to establish the origin
      await page.goto('/');
      await setAuthInBrowser(page, user);

      // Reload so Angular picks up the stored tokens
      await page.reload();

      // Store the user on the test context for assertions
      (test.info() as any)._user = user;
    });

    test('authenticated user can navigate to /upload from the header', async ({ page }) => {
      const header = new HeaderPage(page);
      await header.clickUpload();
      await expect(page).toHaveURL('/upload');
      await expect(page.getByRole('heading', { name: 'Upload Video' })).toBeVisible();
    });

    test('upload submit button is disabled when no file is selected', async ({ page }) => {
      const uploadPage = new UploadPage(page);
      await uploadPage.goto();
      await uploadPage.expectSubmitDisabled();
    });

    test('selecting a valid video file shows its name in the drop-zone', async ({ page }) => {
      assertTestVideoExists();

      const uploadPage = new UploadPage(page);
      await uploadPage.goto();

      await uploadPage.attachFile(TEST_VIDEO_PATH);

      // The component shows the file name inside .file-name once a file is chosen
      await expect(uploadPage.fileName).toBeVisible();
      await expect(uploadPage.fileName).toContainText('test-video');
    });

    test('removing a selected file hides the file-info and shows drop-zone again', async ({
      page,
    }) => {
      assertTestVideoExists();

      const uploadPage = new UploadPage(page);
      await uploadPage.goto();

      await uploadPage.attachFile(TEST_VIDEO_PATH);
      await expect(uploadPage.fileName).toBeVisible();

      await uploadPage.removeFileButton.click();

      await expect(uploadPage.fileName).not.toBeVisible();
      await expect(page.locator('.drop-zone-content')).toBeVisible();
    });

    test('successful upload redirects to the new video detail page', async ({
      page,
    }) => {
      assertTestVideoExists();

      const uploadPage = new UploadPage(page);
      await uploadPage.goto();

      const title = `E2E Upload Test ${Date.now()}`;
      const description = 'Automated E2E upload test description';

      // Wait for the navigation to the video detail page
      await Promise.all([
        page.waitForURL(/\/videos\/\d+/),
        uploadPage.uploadVideo(TEST_VIDEO_PATH, title, description),
      ]);

      await expect(page).toHaveURL(/\/videos\/\d+/);

      const videoDetailPage = new VideoDetailPage(page);
      await videoDetailPage.waitForVideoToLoad();
      await expect(videoDetailPage.videoTitle).toHaveText(title);
    });

    test('uploading without a title shows a validation error', async ({ page }) => {
      assertTestVideoExists();

      const uploadPage = new UploadPage(page);
      await uploadPage.goto();

      // Attach a file — the component auto-fills the title from the filename
      await uploadPage.attachFile(TEST_VIDEO_PATH);

      // Clear the auto-filled title
      await uploadPage.titleInput.clear();

      await uploadPage.submit();

      await uploadPage.expectError('Please enter a title');
    });

    test('selecting an invalid file type shows a validation error', async ({ page }) => {
      const uploadPage = new UploadPage(page);
      await uploadPage.goto();

      // Create a fake text file in memory and attach it
      await uploadPage.fileInput.setInputFiles({
        name: 'document.txt',
        mimeType: 'text/plain',
        buffer: Buffer.from('this is not a video'),
      });

      await uploadPage.expectError('Invalid file type');
    });

    test('file auto-fills the title input from the filename', async ({ page }) => {
      assertTestVideoExists();

      const uploadPage = new UploadPage(page);
      await uploadPage.goto();

      // Clear any existing title first
      await uploadPage.titleInput.clear();

      await uploadPage.attachFile(TEST_VIDEO_PATH);

      // The component strips the extension and replaces hyphens/underscores with spaces
      const titleValue = await uploadPage.titleInput.inputValue();
      // "test-video.mp4" → "test video"
      expect(titleValue.toLowerCase()).toContain('test');
    });
  });
});
