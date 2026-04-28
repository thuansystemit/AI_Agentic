import { APIRequestContext } from '@playwright/test';

const API_BASE = 'http://localhost:8080/api';

export interface TestUser {
  username: string;
  email: string;
  password: string;
  accessToken?: string;
  userId?: number;
}

/**
 * Register a new user via the REST API and return their credentials + access token.
 * Each test generates a unique email using a timestamp + random suffix so tests
 * never collide even when running in parallel.
 */
export async function createTestUser(
  request: APIRequestContext,
  overrides: Partial<TestUser> = {},
): Promise<TestUser> {
  const suffix = `${Date.now()}_${Math.random().toString(36).slice(2, 7)}`;
  const user: TestUser = {
    username: overrides.username ?? `testuser_${suffix}`,
    email: overrides.email ?? `test_${suffix}@example.com`,
    password: overrides.password ?? 'Password123!',
  };

  const response = await request.post(`${API_BASE}/auth/register`, {
    data: {
      username: user.username,
      email: user.email,
      password: user.password,
    },
  });

  if (!response.ok()) {
    const body = await response.text();
    throw new Error(
      `Failed to create test user (${response.status()}): ${body}`,
    );
  }

  const body = await response.json();
  user.accessToken = body.accessToken;
  user.userId = body.user?.id;

  return user;
}

/**
 * Log in an existing user via the REST API and return their access token.
 */
export async function loginTestUser(
  request: APIRequestContext,
  email: string,
  password: string,
): Promise<string> {
  const response = await request.post(`${API_BASE}/auth/login`, {
    data: { email, password },
  });

  if (!response.ok()) {
    const body = await response.text();
    throw new Error(`Login failed (${response.status()}): ${body}`);
  }

  const body = await response.json();
  return body.accessToken as string;
}

/**
 * Inject auth tokens into the browser's localStorage so the Angular app
 * treats the current browser context as already logged in. Call this
 * *after* navigating to any page (so the origin is set) but *before*
 * doing any page interaction that requires auth.
 */
export async function setAuthInBrowser(
  page: import('@playwright/test').Page,
  user: TestUser,
): Promise<void> {
  if (!user.accessToken) {
    throw new Error('User has no accessToken — call createTestUser first');
  }

  await page.evaluate(
    ({ accessToken, refreshToken, userJson }: { accessToken: string; refreshToken: string; userJson: string }) => {
      localStorage.setItem('vp_access_token', accessToken);
      localStorage.setItem('vp_refresh_token', refreshToken);
      localStorage.setItem('vp_user', userJson);
    },
    {
      accessToken: user.accessToken,
      // The registration response also returns a refresh token; fall back to
      // a placeholder so the interceptor can at least attempt a refresh.
      refreshToken: (user as any).refreshToken ?? '',
      userJson: JSON.stringify({
        id: user.userId,
        username: user.username,
        email: user.email,
      }),
    },
  );
}

/**
 * Clear all auth-related localStorage keys, effectively logging out.
 */
export async function clearAuthInBrowser(
  page: import('@playwright/test').Page,
): Promise<void> {
  await page.evaluate(() => {
    localStorage.removeItem('vp_access_token');
    localStorage.removeItem('vp_refresh_token');
    localStorage.removeItem('vp_user');
  });
}
