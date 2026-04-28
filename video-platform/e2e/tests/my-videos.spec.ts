import { test, expect } from '@playwright/test';
import { MyVideosPage } from '../pages/my-videos.page';
import { VideoDetailPage } from '../pages/video-detail.page';
import { HeaderPage } from '../pages/header.page';
import { createTestUser, setAuthInBrowser } from '../fixtures/api';
import { TEST_VIDEO_PATH, assertTestVideoExists } from '../fixtures/video-fixture';

/**
 * My Videos flows:
 *  1. Unauthenticated user visiting /my-videos is redirected to /login
 *  2. Authenticated user with no uploads sees the empty state
 *  3. Authenticated user sees only their own videos (isolation)
 *  4. After uploading a video it appears in My Videos
 *  5. My Videos page shows a count of videos
 *  6. User can navigate from My Videos to the Upload page via the button
 *  7. Clicking a video card in My Videos navigates to the detail page
 *  8. Owner sees edit/delete actions on the video detail page when reached via My Videos
 */

/**
 * Helper: upload a video via API on behalf of the given user.
 */
async function apiUploadVideo(
  request: import('@playwright/test').APIRequestContext,
  accessToken: string,
  title: string,
): Promise<{ id: number; title: string }> {
  assertTestVideoExists();

  const response = await request.post('http://localhost:8080/api/videos', {
    headers: { Authorization: `Bearer ${accessToken}` },
    multipart: {
      file: {
        name: 'test-video.mp4',
        mimeType: 'video/mp4',
        buffer: require('fs').readFileSync(TEST_VIDEO_PATH),
      },
      title,
      description: 'E2E my-videos test video',
    },
  });

  if (!response.ok()) {
    throw new Error(
      `Video upload failed (${response.status()}): ${await response.text()}`,
    );
  }

  const body = await response.json();
  return { id: body.id as number, title: body.title as string };
}

test.describe('My Videos', () => {
  // ------------------------------------------------------------------
  // Guard: unauthenticated redirect
  // ------------------------------------------------------------------
  test('unauthenticated user visiting /my-videos is redirected to /login', async ({ page }) => {
    await page.goto('/my-videos');
    await expect(page).toHaveURL('/login');
  });

  test('My Videos link is hidden in the header for guests', async ({ page }) => {
    await page.goto('/');
    const header = new HeaderPage(page);
    await expect(header.myVideosLinkLocator).not.toBeVisible();
  });

  // ------------------------------------------------------------------
  // Authenticated flows
  // ------------------------------------------------------------------
  test.describe('Authenticated', () => {
    // Store the test user and an auth-seeded page on each test
    let testUser: Awaited<ReturnType<typeof createTestUser>>;

    test.beforeEach(async ({ page, request }) => {
      testUser = await createTestUser(request);

      // Seed auth via localStorage (avoids going through the login UI each time)
      await page.goto('/');
      await setAuthInBrowser(page, testUser);
      await page.reload();
    });

    test('new user with no uploads sees the empty state', async ({ page }) => {
      const myVideosPage = new MyVideosPage(page);
      await myVideosPage.goto();
      await myVideosPage.waitForLoad();

      await myVideosPage.expectEmptyState();
    });

    test('empty state shows a link to the upload page', async ({ page }) => {
      const myVideosPage = new MyVideosPage(page);
      await myVideosPage.goto();
      await myVideosPage.waitForLoad();

      // There are two upload links in the empty state template; use the first
      await expect(myVideosPage.uploadNewButton).toBeVisible();
    });

    test('uploaded video appears in My Videos', async ({ page, request }) => {
      const title = `MyVideo_${Date.now()}`;
      await apiUploadVideo(request, testUser.accessToken!, title);

      const myVideosPage = new MyVideosPage(page);
      await myVideosPage.goto();
      await myVideosPage.waitForLoad();

      await myVideosPage.expectVideosVisible();
      await myVideosPage.expectVideoWithTitle(title);
    });

    test('My Videos shows the correct video count', async ({ page, request }) => {
      const title1 = `MyVideo1_${Date.now()}`;
      const title2 = `MyVideo2_${Date.now()}`;
      await apiUploadVideo(request, testUser.accessToken!, title1);
      await apiUploadVideo(request, testUser.accessToken!, title2);

      const myVideosPage = new MyVideosPage(page);
      await myVideosPage.goto();
      await myVideosPage.waitForLoad();

      const count = await myVideosPage.getVideoCount();
      expect(count).toBeGreaterThanOrEqual(2);

      // The results-info span should mention the number of videos
      await expect(myVideosPage.resultsInfo).toContainText(/\d+ video/);
    });

    test('My Videos only shows the current user\'s own videos', async ({ page, request }) => {
      // Another user uploads a video
      const otherUser = await createTestUser(request);
      const otherTitle = `OtherUser_${Date.now()}`;
      await apiUploadVideo(request, otherUser.accessToken!, otherTitle);

      // Our test user uploads their own video
      const myTitle = `MyOwn_${Date.now()}`;
      await apiUploadVideo(request, testUser.accessToken!, myTitle);

      const myVideosPage = new MyVideosPage(page);
      await myVideosPage.goto();
      await myVideosPage.waitForLoad();

      // Our video must be visible
      await myVideosPage.expectVideoWithTitle(myTitle);

      // The other user's video must NOT be in the list
      const otherCount = await page
        .locator('.video-card .video-title', { hasText: otherTitle })
        .count();
      expect(otherCount).toBe(0);
    });

    test('"Upload New Video" button navigates to /upload', async ({ page }) => {
      const myVideosPage = new MyVideosPage(page);
      await myVideosPage.goto();
      await myVideosPage.waitForLoad();

      await myVideosPage.uploadNewButton.click();
      await expect(page).toHaveURL('/upload');
    });

    test('clicking a video card navigates to the video detail page', async ({
      page,
      request,
    }) => {
      const title = `ClickCard_${Date.now()}`;
      await apiUploadVideo(request, testUser.accessToken!, title);

      const myVideosPage = new MyVideosPage(page);
      await myVideosPage.goto();
      await myVideosPage.waitForLoad();

      await myVideosPage.clickVideoByTitle(title);

      await expect(page).toHaveURL(/\/videos\/\d+/);
    });

    test('owner sees edit and delete actions on their video detail page', async ({
      page,
      request,
    }) => {
      const title = `OwnerActions_${Date.now()}`;
      const { id } = await apiUploadVideo(request, testUser.accessToken!, title);

      const videoDetailPage = new VideoDetailPage(page);
      await videoDetailPage.gotoById(id);
      await videoDetailPage.waitForVideoToLoad();

      await videoDetailPage.expectOwnerActionsVisible();
    });

    test('owner can edit the video title from the detail page', async ({ page, request }) => {
      const originalTitle = `EditMe_${Date.now()}`;
      const { id } = await apiUploadVideo(request, testUser.accessToken!, originalTitle);

      const videoDetailPage = new VideoDetailPage(page);
      await videoDetailPage.gotoById(id);
      await videoDetailPage.waitForVideoToLoad();

      const updatedTitle = `Edited_${Date.now()}`;
      await videoDetailPage.startEdit();
      await videoDetailPage.saveEdit(updatedTitle, 'Updated description');

      await expect(videoDetailPage.videoTitle).toHaveText(updatedTitle);
    });

    test('owner can delete their video and is redirected to My Videos', async ({
      page,
      request,
    }) => {
      const title = `DeleteMe_${Date.now()}`;
      const { id } = await apiUploadVideo(request, testUser.accessToken!, title);

      const videoDetailPage = new VideoDetailPage(page);
      await videoDetailPage.gotoById(id);
      await videoDetailPage.waitForVideoToLoad();

      await Promise.all([
        page.waitForURL('/my-videos'),
        videoDetailPage.deleteVideo(),
      ]);

      await expect(page).toHaveURL('/my-videos');
    });
  });
});
