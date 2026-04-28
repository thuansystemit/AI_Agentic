import { test, expect } from '@playwright/test';
import { LoginPage } from '../pages/login.page';
import { RegisterPage } from '../pages/register.page';
import { HeaderPage } from '../pages/header.page';
import { createTestUser } from '../fixtures/api';

/**
 * Auth flows:
 *  1. User can register with username, email, and password
 *  2. Duplicate email registration is rejected with an error
 *  3. User can sign in with valid credentials
 *  4. Invalid credentials show an error message
 *  5. Signed-in user can sign out and is redirected to /login
 *  6. After sign-out the header returns to guest state
 *  7. Authenticated user visiting /login is redirected to home (guestGuard)
 *  8. Authenticated user visiting /register is redirected to home (guestGuard)
 */

test.describe('Authentication', () => {
  // ------------------------------------------------------------------
  // Registration
  // ------------------------------------------------------------------
  test.describe('Sign up', () => {
    test('new user can register and is redirected to home', async ({ page, request }) => {
      const registerPage = new RegisterPage(page);
      const header = new HeaderPage(page);

      const suffix = `${Date.now()}`;
      const username = `newuser_${suffix}`;
      const email = `newuser_${suffix}@example.com`;
      const password = 'Password123!';

      await registerPage.goto();
      await registerPage.register(username, email, password);

      // After successful registration the app auto-logs in and redirects home
      await expect(page).toHaveURL('/');
      await header.expectAuthenticatedState(username);
    });

    test('registration fails when email is already taken', async ({ page, request }) => {
      // Pre-create the user via API so we know the email exists
      const existing = await createTestUser(request);

      const registerPage = new RegisterPage(page);
      await registerPage.goto();
      await registerPage.register('anotheruser', existing.email, 'Password123!');

      await registerPage.expectError();
      await expect(page).toHaveURL('/register');
    });

    test('registration fails when passwords do not match', async ({ page }) => {
      const registerPage = new RegisterPage(page);
      await registerPage.goto();

      await registerPage.fillUsername('testuser');
      await registerPage.fillEmail('mismatch@example.com');
      await registerPage.fillPassword('Password123!');
      await registerPage.fillConfirmPassword('DifferentPassword!');
      await registerPage.submit();

      // The Angular component should show an error before hitting the API
      await registerPage.expectError();
      await expect(page).toHaveURL('/register');
    });
  });

  // ------------------------------------------------------------------
  // Sign in
  // ------------------------------------------------------------------
  test.describe('Sign in', () => {
    test('existing user can sign in with correct credentials', async ({ page, request }) => {
      const user = await createTestUser(request);
      const loginPage = new LoginPage(page);
      const header = new HeaderPage(page);

      await loginPage.goto();
      await loginPage.login(user.email, user.password);

      await expect(page).toHaveURL('/');
      await header.expectAuthenticatedState(user.username);
    });

    test('sign in fails with wrong password and shows an error', async ({ page, request }) => {
      const user = await createTestUser(request);
      const loginPage = new LoginPage(page);

      await loginPage.goto();
      await loginPage.login(user.email, 'WrongPassword!');

      await loginPage.expectError();
      await expect(page).toHaveURL('/login');
    });

    test('sign in fails with unknown email and shows an error', async ({ page }) => {
      const loginPage = new LoginPage(page);

      await loginPage.goto();
      await loginPage.login('nobody@nowhere.invalid', 'SomePassword1!');

      await loginPage.expectError();
      await expect(page).toHaveURL('/login');
    });
  });

  // ------------------------------------------------------------------
  // Sign out
  // ------------------------------------------------------------------
  test.describe('Sign out', () => {
    test('authenticated user can sign out and is redirected to login', async ({
      page,
      request,
    }) => {
      const user = await createTestUser(request);
      const loginPage = new LoginPage(page);
      const header = new HeaderPage(page);

      // Log in first
      await loginPage.goto();
      await loginPage.login(user.email, user.password);
      await expect(page).toHaveURL('/');
      await header.expectAuthenticatedState(user.username);

      // Now sign out
      await header.clickLogout();

      await expect(page).toHaveURL('/login');
      await header.expectGuestState();
    });

    test('after sign-out the nav links for upload and my-videos are hidden', async ({
      page,
      request,
    }) => {
      const user = await createTestUser(request);
      const loginPage = new LoginPage(page);
      const header = new HeaderPage(page);

      await loginPage.goto();
      await loginPage.login(user.email, user.password);
      await expect(page).toHaveURL('/');

      await header.clickLogout();

      await expect(header.uploadLinkLocator).not.toBeVisible();
      await expect(header.myVideosLinkLocator).not.toBeVisible();
    });
  });

  // ------------------------------------------------------------------
  // Guest guards (already-authenticated redirects)
  // ------------------------------------------------------------------
  test.describe('Guest guards', () => {
    test('authenticated user visiting /login is redirected to home', async ({
      page,
      request,
    }) => {
      const user = await createTestUser(request);
      const loginPage = new LoginPage(page);

      // Sign in to get auth state
      await loginPage.goto();
      await loginPage.login(user.email, user.password);
      await expect(page).toHaveURL('/');

      // Now try to navigate to /login
      await page.goto('/login');
      await expect(page).toHaveURL('/');
    });

    test('authenticated user visiting /register is redirected to home', async ({
      page,
      request,
    }) => {
      const user = await createTestUser(request);
      const loginPage = new LoginPage(page);

      await loginPage.goto();
      await loginPage.login(user.email, user.password);
      await expect(page).toHaveURL('/');

      await page.goto('/register');
      await expect(page).toHaveURL('/');
    });
  });
});
