import { test, expect } from '@playwright/test';
import { VideoListPage } from '../pages/video-list.page';
import { VideoDetailPage } from '../pages/video-detail.page';
import { HeaderPage } from '../pages/header.page';
import { createTestUser, setAuthInBrowser } from '../fixtures/api';
import { TEST_VIDEO_PATH, assertTestVideoExists } from '../fixtures/video-fixture';
import * as path from 'path';

/**
 * Browse flows (guest and authenticated):
 *  1. Guest sees the Browse Videos page without logging in
 *  2. Guest can see video cards when videos exist
 *  3. Guest can search for videos by title
 *  4. Guest can clear a search and return to the full listing
 *  5. Guest can navigate to a video detail page and see the player
 *  6. Guest sees owner actions hidden on the video detail page
 *  7. Logo click returns to home from any page
 *
 * NOTE: Tests that require at least one video in the DB (cases 2, 3, 5)
 * seed the data by uploading a video via the API using a pre-created user.
 */

test.describe('Browse Videos', () => {
  // ------------------------------------------------------------------
  // Guest state — no auth required
  // ------------------------------------------------------------------
  test.describe('Guest browsing', () => {
    test('home page is accessible to guests and shows Browse Videos heading', async ({ page }) => {
      const videoListPage = new VideoListPage(page);
      const header = new HeaderPage(page);

      await videoListPage.goto();

      await expect(page.getByRole('heading', { name: 'Browse Videos' })).toBeVisible();
      await header.expectGuestState();
    });

    test('guest sees login and register links in the header', async ({ page }) => {
      const header = new HeaderPage(page);

      await page.goto('/');

      await expect(header.loginButtonLocator).toBeVisible();
      await expect(header.registerButtonLocator).toBeVisible();
    });

    test('guest can navigate from home to login via header Login button', async ({ page }) => {
      const header = new HeaderPage(page);

      await page.goto('/');
      await header.clickLogin();

      await expect(page).toHaveURL('/login');
    });

    test('guest can navigate from home to register via header Register button', async ({
      page,
    }) => {
      const header = new HeaderPage(page);

      await page.goto('/');
      await header.clickRegister();

      await expect(page).toHaveURL('/register');
    });
  });

  // ------------------------------------------------------------------
  // Video listing — requires at least one video in the system
  // ------------------------------------------------------------------
  test.describe('Video listing', () => {
    /**
     * Upload a video via multipart/form-data API call so the DB has content
     * for listing tests. Returns the uploaded video's id and title.
     */
    async function seedVideo(
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
          description: 'E2E seeded video',
        },
      });

      if (!response.ok()) {
        const body = await response.text();
        throw new Error(`Seed video upload failed (${response.status()}): ${body}`);
      }

      const body = await response.json();
      return { id: body.id as number, title: body.title as string };
    }

    test('video grid shows video cards when videos exist', async ({ page, request }) => {
      const user = await createTestUser(request);
      const videoTitle = `Browse E2E ${Date.now()}`;
      await seedVideo(request, user.accessToken!, videoTitle);

      const videoListPage = new VideoListPage(page);
      await videoListPage.goto();
      await videoListPage.waitForVideosToLoad();

      await videoListPage.expectVideosVisible();
    });

    test('search returns matching videos and hides non-matching ones', async ({
      page,
      request,
    }) => {
      const user = await createTestUser(request);
      const uniqueTitle = `UniqueSearchTerm_${Date.now()}`;
      await seedVideo(request, user.accessToken!, uniqueTitle);

      const videoListPage = new VideoListPage(page);
      await videoListPage.goto();
      await videoListPage.waitForVideosToLoad();

      await videoListPage.search(uniqueTitle);

      const count = await videoListPage.getVideoCardCount();
      expect(count).toBeGreaterThanOrEqual(1);

      // All returned cards should contain the search term in their title
      const titles = await page.locator('.video-card .video-title').allTextContents();
      for (const t of titles) {
        expect(t.toLowerCase()).toContain(uniqueTitle.toLowerCase());
      }
    });

    test('clearing search restores full video listing', async ({ page, request }) => {
      const user = await createTestUser(request);
      await seedVideo(request, user.accessToken!, `ClearSearchTest_${Date.now()}`);

      const videoListPage = new VideoListPage(page);
      await videoListPage.goto();
      await videoListPage.waitForVideosToLoad();
      const initialCount = await videoListPage.getVideoCardCount();

      await videoListPage.search('zzz_no_such_video_xyz');
      await videoListPage.clearSearch();

      const afterClearCount = await videoListPage.getVideoCardCount();
      expect(afterClearCount).toBeGreaterThanOrEqual(initialCount);
    });

    test('search with no results shows empty state', async ({ page }) => {
      const videoListPage = new VideoListPage(page);
      await videoListPage.goto();
      await videoListPage.waitForVideosToLoad();

      await videoListPage.search('zzz_absolutely_no_match_xyz_9999');

      await videoListPage.expectEmptyState();
    });
  });

  // ------------------------------------------------------------------
  // Video detail — guest can watch without logging in
  // ------------------------------------------------------------------
  test.describe('Video watch (guest)', () => {
    async function seedVideo(
      request: import('@playwright/test').APIRequestContext,
      accessToken: string,
      title: string,
    ): Promise<{ id: number }> {
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
          description: 'Guest watch E2E video',
        },
      });

      if (!response.ok()) {
        throw new Error(`Seed failed (${response.status()}): ${await response.text()}`);
      }

      const body = await response.json();
      return { id: body.id as number };
    }

    test('guest can open a video detail page and see the video player', async ({
      page,
      request,
    }) => {
      const user = await createTestUser(request);
      const title = `GuestWatch_${Date.now()}`;
      const { id } = await seedVideo(request, user.accessToken!, title);

      const videoDetailPage = new VideoDetailPage(page);
      await videoDetailPage.gotoById(id);

      await expect(videoDetailPage.videoPlayer).toBeVisible();
      await expect(videoDetailPage.videoTitle).toHaveText(title);
    });

    test('clicking a video card on the home page navigates to the detail page', async ({
      page,
      request,
    }) => {
      const user = await createTestUser(request);
      const title = `ClickCard_${Date.now()}`;
      await seedVideo(request, user.accessToken!, title);

      const videoListPage = new VideoListPage(page);
      await videoListPage.goto();
      await videoListPage.waitForVideosToLoad();

      // Click the specific card we seeded
      await videoListPage.clickVideoByTitle(title);

      await expect(page).toHaveURL(/\/videos\/\d+/);
      await expect(page.locator('video.video-player')).toBeVisible();
    });

    test('guest does not see edit/delete actions on the video detail page', async ({
      page,
      request,
    }) => {
      const user = await createTestUser(request);
      const { id } = await seedVideo(request, user.accessToken!, `OwnerActions_${Date.now()}`);

      const videoDetailPage = new VideoDetailPage(page);
      await videoDetailPage.gotoById(id);

      await videoDetailPage.expectOwnerActionsHidden();
    });

    test('logo click from video detail page navigates back to home', async ({
      page,
      request,
    }) => {
      const user = await createTestUser(request);
      const { id } = await seedVideo(request, user.accessToken!, `LogoBack_${Date.now()}`);

      const videoDetailPage = new VideoDetailPage(page);
      await videoDetailPage.gotoById(id);

      const header = new HeaderPage(page);
      await header.clickLogo();

      await expect(page).toHaveURL('/');
    });

    test('navigating to a non-existent video ID shows an error', async ({ page }) => {
      const videoDetailPage = new VideoDetailPage(page);
      await videoDetailPage.gotoById(999999999);

      await expect(videoDetailPage.errorAlert).toBeVisible();
      await expect(videoDetailPage.backToBrowseLink).toBeVisible();
    });
  });
});
