# TEST PLAN: Video Platform MVP

**Feature:** Full MVP — Guest Browsing, User Auth, Video Upload, Streaming, My Videos
**Version:** Sprint 1 / v0.1.0-SNAPSHOT
**Date:** 2026-04-27
**Author:** QA Engineer
**Stack:** Spring Boot 3.4.5 (Java 17) + Angular 21 + PostgreSQL 16 + local filesystem storage

---

## Table of Contents

1. [Scope](#1-scope)
2. [Test Environment](#2-test-environment)
3. [Testing Pyramid](#3-testing-pyramid)
4. [Entry and Exit Criteria](#4-entry-and-exit-criteria)
5. [Feature Area: Authentication](#5-feature-area-authentication)
6. [Feature Area: Guest Browsing](#6-feature-area-guest-browsing)
7. [Feature Area: Video Upload](#7-feature-area-video-upload)
8. [Feature Area: Video Streaming](#8-feature-area-video-streaming)
9. [Feature Area: My Videos (Owner Management)](#9-feature-area-my-videos-owner-management)
10. [Feature Area: Access Control and Security](#10-feature-area-access-control-and-security)
11. [Regression Checklist](#11-regression-checklist)
12. [Identified Bugs and Gaps](#12-identified-bugs-and-gaps)
13. [Test Automation Gaps](#13-test-automation-gaps)
14. [Acceptance Criteria Summary](#14-acceptance-criteria-summary)

---

## 1. Scope

### What is being tested

- User registration, login, logout, and token refresh flows
- Guest access to the video list, video detail, and stream endpoints without authentication
- Authenticated video upload (file validation, metadata persistence, storage)
- Video streaming via `GET /api/videos/{id}/stream` (range request behavior, content-type accuracy)
- Owner-only operations: edit metadata, delete video
- Authorization enforcement: unauthenticated and cross-user access blocked on protected endpoints
- Frontend guard behavior (authGuard, guestGuard) and interceptor token injection
- Pagination and search on the public video listing
- Error handling and user-facing error messages

### What is NOT being tested this cycle

- Video transcoding or format conversion (explicitly out of MVP scope)
- Admin roles (post-MVP)
- Email verification or password reset
- Rate limiting
- File virus/malware scanning
- S3/object storage integration (local filesystem only for MVP)
- Thumbnails or preview images
- Video view counts, likes, or comments
- Accessibility (WCAG compliance) — deferred to post-MVP audit
- Performance under high concurrent load — basic benchmarks only

---

## 2. Test Environment

| Item | Value |
|------|-------|
| Environment | Local docker-compose (`docker-compose.yml` at repo root) |
| Backend URL | `http://localhost:8080` |
| Frontend URL | `http://localhost:4200` |
| Database | PostgreSQL 16 (port 5433 on host, 5432 inside Docker) |
| Upload volume | Docker volume `video_uploads` mounted at `/app/uploads/videos` |
| JWT access token TTL | 86400000 ms (24 hours) |
| JWT refresh token TTL | 604800000 ms (7 days) |
| Max file size | 500 MB (Spring multipart) + 510 MB request |
| Test seed data | Three user accounts; at least five uploaded videos of varied format and size |
| API test tool | curl or any HTTP client (Postman, HTTPie) |

### Test Accounts to Pre-create

| Account | Role | Purpose |
|---------|------|---------|
| `alice@example.com` / `Alice1234` | Registered user | Primary owner for upload and edit tests |
| `bob@example.com` / `Bob1234` | Registered user | Cross-user authorization tests |
| Guest (no account) | Unauthenticated | Guest browsing tests |

---

## 3. Testing Pyramid

```
          /\
         /E2E\         3-5 critical journeys only
        /------\
       /  API   \      All 9 REST endpoints, including negative paths
      /----------\
     / Integration\    Service + repository interaction (H2 in-memory)
    /--------------\
   /   Unit Tests   \  Business logic: AuthService, VideoService, StorageService
  /------------------\
```

| Layer | Who Owns | Current State | What Needs Adding |
|-------|----------|---------------|-------------------|
| Unit | Developer | 20 tests passing (AuthService: 7, VideoService: 9, AuthController: 3, Context: 1) | StorageService validation, JwtTokenProvider, VideoController layer |
| Integration | Developer | None exist | Service + DB round-trip with H2; StorageService with temp dir |
| API | QA / Dev | None exist | All endpoints, all error paths |
| E2E | QA | None exist | 3 critical user journeys (see Section 5–9) |
| Manual | QA | This document | Full execution per test cases below |

---

## 4. Entry and Exit Criteria

### Entry Criteria (testing can begin when)

- [ ] `docker-compose up --build` starts all three services (postgres, backend, frontend) without errors
- [ ] `GET http://localhost:8080/api/videos` returns HTTP 200 with a valid JSON body
- [ ] `http://localhost:4200` loads the Angular SPA in a browser
- [ ] All 20 existing unit tests pass (`mvn test` exits 0)
- [ ] Test accounts and seed videos created per Section 2

### Exit Criteria (MVP release is approved when)

- [ ] All P0 test cases pass
- [ ] All P1 test cases pass
- [ ] No open CRITICAL or HIGH severity defects
- [ ] Streaming plays in Chrome and Firefox without stalling on the first load
- [ ] Upload rejects files > 500 MB and non-video MIME types at both the UI and API layers
- [ ] Unauthenticated requests to protected endpoints return 401, not 403 or 500
- [ ] Cross-user mutation attempts (edit, delete) return 401, not 500

---

## 5. Feature Area: Authentication

### TC-AUTH-001: Successful registration
**Priority:** P0
**Type:** Functional

Preconditions:
- Clean database or email/username not yet registered
- Guest (no Authorization header)

Steps:
1. `POST /api/auth/register` with body `{"username":"alice","email":"alice@example.com","password":"Alice1234"}`

Expected result:
- HTTP 201 Created
- Response body contains `accessToken` (non-empty string), `refreshToken`, `tokenType: "Bearer"`, `expiresIn: 86400`, and `user.email: "alice@example.com"`
- Database has one row in `users` table for this email
- Password stored as BCrypt hash (starts with `$2a$`), never plaintext

---

### TC-AUTH-002: Registration — duplicate email rejected
**Priority:** P0
**Type:** Negative

Preconditions:
- `alice@example.com` already registered (run TC-AUTH-001 first)

Steps:
1. `POST /api/auth/register` with `{"username":"alice2","email":"alice@example.com","password":"Alice1234"}`

Expected result:
- HTTP 400 Bad Request
- Response body: `{"message":"Email is already registered"}`
- No new user row created

---

### TC-AUTH-003: Registration — duplicate username rejected
**Priority:** P1
**Type:** Negative

Steps:
1. `POST /api/auth/register` with `{"username":"alice","email":"alice2@example.com","password":"Alice1234"}`

Expected result:
- HTTP 400 Bad Request
- Response body: `{"message":"Username is already taken"}`

---

### TC-AUTH-004: Registration — bean validation enforcement
**Priority:** P1
**Type:** Negative / Edge

Test matrix (one request each):

| Scenario | Payload |
|----------|---------|
| Blank username | `{"username":"","email":"x@x.com","password":"valid123"}` |
| Username too short (< 3 chars) | `{"username":"ab","email":"x@x.com","password":"valid123"}` |
| Invalid email format | `{"username":"alice","email":"notanemail","password":"valid123"}` |
| Password too short (< 6 chars) | `{"username":"alice","email":"x@x.com","password":"12345"}` |
| Missing all fields | `{}` |

Expected result for all:
- HTTP 400 Bad Request
- Response body contains `fieldErrors` map with the relevant field name and message

---

### TC-AUTH-005: Successful login
**Priority:** P0
**Type:** Functional

Preconditions: `alice@example.com` registered with password `Alice1234`

Steps:
1. `POST /api/auth/login` with `{"email":"alice@example.com","password":"Alice1234"}`

Expected result:
- HTTP 200 OK
- `accessToken` and `refreshToken` are non-empty JWT strings
- `user.username` is `"alice"`
- Access token decodes (via jwt.io) to `type: "access"`, `sub: "<userId>"`
- Refresh token decodes to `type: "refresh"`, no `email` claim

---

### TC-AUTH-006: Login — wrong password
**Priority:** P0
**Type:** Negative

Steps:
1. `POST /api/auth/login` with `{"email":"alice@example.com","password":"wrongpassword"}`

Expected result:
- HTTP 401 Unauthorized
- Response body: `{"message":"Invalid email or password"}`
- Response does NOT distinguish "email not found" from "wrong password" (prevents enumeration)

---

### TC-AUTH-007: Login — non-existent email
**Priority:** P1
**Type:** Negative

Steps:
1. `POST /api/auth/login` with `{"email":"nobody@example.com","password":"anything"}`

Expected result:
- HTTP 401 Unauthorized
- Same generic message as TC-AUTH-006 (no enumeration leakage)

---

### TC-AUTH-008: Token refresh — happy path
**Priority:** P1
**Type:** Functional

Preconditions: Login as alice and capture `refreshToken`

Steps:
1. `POST /api/auth/refresh` with `{"refreshToken":"<captured_refresh_token>"}`

Expected result:
- HTTP 200 OK
- New `accessToken` and `refreshToken` issued
- New access token is different from the original

---

### TC-AUTH-009: Token refresh — access token rejected as refresh token
**Priority:** P1
**Type:** Security / Negative

Steps:
1. Login and capture `accessToken`
2. `POST /api/auth/refresh` with `{"refreshToken":"<access_token>"}`

Expected result:
- HTTP 401 Unauthorized
- `{"message":"Token is not a refresh token"}`
- A valid but wrong-type token must not produce a new token pair

---

### TC-AUTH-010: Token refresh — expired or tampered token
**Priority:** P1
**Type:** Security / Negative

Steps:
1. `POST /api/auth/refresh` with `{"refreshToken":"invalid.token.here"}`

Expected result:
- HTTP 401 Unauthorized
- `{"message":"Invalid or expired refresh token"}`

---

### TC-AUTH-011: Frontend — login redirects authenticated user away from /login
**Priority:** P1
**Type:** Functional / Guard

Preconditions: Alice is logged in (token in localStorage)

Steps:
1. Navigate to `http://localhost:4200/login` in the browser

Expected result:
- Browser is immediately redirected to `/` (home page)
- Login form is never shown
- (guestGuard fires and navigates away)

---

### TC-AUTH-012: Frontend — logout clears session and redirects
**Priority:** P0
**Type:** Functional

Preconditions: Alice is logged in

Steps:
1. Click "Logout" in the header
2. Observe `localStorage` in browser DevTools

Expected result:
- `vp_access_token`, `vp_refresh_token`, and `vp_user` keys are all removed from `localStorage`
- Browser navigates to `/login`
- "Upload" and "My Videos" nav links are no longer visible
- Refreshing the page keeps the user logged out

---

### TC-AUTH-013: Frontend — token attached to authenticated requests
**Priority:** P0
**Type:** Integration (Browser)

Preconditions: Alice is logged in

Steps:
1. Open browser DevTools Network tab
2. Navigate to `/my-videos`

Expected result:
- `GET /api/users/me/videos` request carries `Authorization: Bearer <token>` header
- Request succeeds with 200

---

### TC-AUTH-014: Frontend — 401 triggers token refresh then retry
**Priority:** P1
**Type:** Functional / Interceptor

Preconditions: Alice's access token has expired (manipulate `vp_access_token` in localStorage to an expired token, leave `vp_refresh_token` valid)

Steps:
1. Attempt any authenticated request (e.g., navigate to `/my-videos`)

Expected result:
- Interceptor detects 401 response
- `POST /api/auth/refresh` is called automatically
- Original request is retried with new token
- User sees the page content, not an error
- `localStorage` `vp_access_token` is updated

---

### TC-AUTH-015: Frontend — failed refresh forces logout
**Priority:** P1
**Type:** Functional / Interceptor

Preconditions: Both access token and refresh token are invalid/expired in localStorage

Steps:
1. Attempt any authenticated request

Expected result:
- Interceptor attempts refresh, gets 401
- `authService.logout()` is called: all tokens cleared, user navigated to `/login`

---

## 6. Feature Area: Guest Browsing

### TC-GUEST-001: List videos — unauthenticated
**Priority:** P0
**Type:** Functional

Steps:
1. `GET /api/videos` (no Authorization header)

Expected result:
- HTTP 200 OK
- Response body matches `PageResponse` shape: `{content:[], page:0, size:12, totalElements:N, totalPages:N, last:bool}`
- `content` array items each contain: `id`, `title`, `description`, `fileName`, `fileSize`, `contentType`, `streamUrl`, `owner.id`, `owner.username`, `createdAt`, `updatedAt`

---

### TC-GUEST-002: List videos — pagination
**Priority:** P1
**Type:** Functional

Preconditions: At least 13 videos exist in the database

Steps:
1. `GET /api/videos?page=0&size=12`
2. `GET /api/videos?page=1&size=12`

Expected result:
- Page 0: 12 items, `last: false`
- Page 1: remaining items, `last: true`
- Videos are ordered by `createdAt DESC` (newest first) on both pages

---

### TC-GUEST-003: List videos — search by title
**Priority:** P1
**Type:** Functional

Preconditions: A video with title "Spring Tutorial" exists

Steps:
1. `GET /api/videos?search=spring`

Expected result:
- HTTP 200 OK
- `content` contains the "Spring Tutorial" video (case-insensitive match)
- `content` does NOT contain videos whose titles don't contain "spring"

---

### TC-GUEST-004: List videos — search with no match
**Priority:** P2
**Type:** Edge

Steps:
1. `GET /api/videos?search=zzz_definitely_no_match`

Expected result:
- HTTP 200 OK
- `content: []`, `totalElements: 0`, `totalPages: 0`

---

### TC-GUEST-005: Get single video — unauthenticated
**Priority:** P0
**Type:** Functional

Steps:
1. `GET /api/videos/1` (no Authorization header, video ID 1 exists)

Expected result:
- HTTP 200 OK
- Full `VideoResponse` object returned

---

### TC-GUEST-006: Get single video — not found
**Priority:** P1
**Type:** Negative

Steps:
1. `GET /api/videos/99999`

Expected result:
- HTTP 404 Not Found
- `{"message":"Video not found with id: 99999"}`

---

### TC-GUEST-007: Frontend — home page renders video grid for guest
**Priority:** P0
**Type:** Functional (Manual)

Preconditions: At least one video uploaded; no user logged in

Steps:
1. Open `http://localhost:4200` in a private browser window

Expected result:
- Page loads without error
- Video cards are displayed with title, uploader username, upload date, file size
- Header shows "Login" and "Register" links; "Upload" and "My Videos" are NOT visible
- No JavaScript console errors

---

### TC-GUEST-008: Frontend — clicking a video card navigates to detail page
**Priority:** P0
**Type:** Functional (Manual)

Steps:
1. On the home page, click any video card

Expected result:
- Browser navigates to `/videos/<id>`
- Video player (`<video>` element) renders with controls
- Video title, uploader, and date are displayed
- Edit/Delete buttons are NOT shown (user is guest)

---

## 7. Feature Area: Video Upload

### TC-UPLOAD-001: Successful video upload — mp4
**Priority:** P0
**Type:** Functional

Preconditions: Alice is authenticated (valid Bearer token)

Steps:
1. `POST /api/videos` as multipart/form-data:
   - `file`: a valid `video/mp4` file <= 500 MB
   - `title`: "My Test Video"
   - `description`: "A description"

Expected result:
- HTTP 201 Created
- Response body: `VideoResponse` with `id` set, `title: "My Test Video"`, `owner.username: "alice"`, `streamUrl: "/api/videos/<id>/stream"`
- File stored on disk (inside the Docker volume or `./uploads/videos` locally)
- Stored filename is a UUID + original extension (e.g. `550e8400-e29b-41d4-a716-446655440000.mp4`) — NOT the original filename
- `Video` row created in database with correct `user_id`

---

### TC-UPLOAD-002: Upload without authentication
**Priority:** P0
**Type:** Security / Negative

Steps:
1. `POST /api/videos` (multipart) with no Authorization header, valid file

Expected result:
- HTTP 401 Unauthorized or 403 Forbidden (Spring Security blocks before the controller)
- No file written to disk
- No database row created

---

### TC-UPLOAD-003: Upload — unsupported file type rejected
**Priority:** P0
**Type:** Negative

Steps (each as a separate request, authenticated as Alice):
1. Upload a `.txt` file with Content-Type `text/plain`
2. Upload a `.pdf` file with Content-Type `application/pdf`
3. Upload an image file with Content-Type `image/jpeg`

Expected result for each:
- HTTP 500 (StorageException maps to 500) — **see Bug #1 in Section 12**
- File NOT written to disk
- Error message contains "Invalid file type"

---

### TC-UPLOAD-004: Upload — empty file rejected
**Priority:** P1
**Type:** Negative

Steps:
1. Authenticated POST with a zero-byte file (Content-Type: video/mp4)

Expected result:
- HTTP 500 (StorageException) — see Bug #1
- `{"message":"File storage error occurred"}` (generic; underlying message is "Cannot store empty file")

---

### TC-UPLOAD-005: Upload — file exceeds 500 MB
**Priority:** P1
**Type:** Edge

Steps:
1. Authenticated POST with a file > 500 MB

Expected result:
- HTTP 400 Bad Request
- `{"message":"File size exceeds maximum allowed size of 500MB"}`
- Spring's `MaxUploadSizeExceededException` handler fires before the service layer

---

### TC-UPLOAD-006: Upload — missing title
**Priority:** P1
**Type:** Negative

Steps:
1. Authenticated POST with valid file but omit the `title` form field entirely
2. Authenticated POST with `title` as blank string: `title=   `

Expected result:
- HTTP 400 Bad Request
- `{"message":"Title is required"}`
- File is written to disk in scenario 1 before the title check fails — **see Bug #2 in Section 12**

---

### TC-UPLOAD-007: Upload — description is optional
**Priority:** P1
**Type:** Edge

Steps:
1. Authenticated POST with valid file, title set, description field omitted

Expected result:
- HTTP 201 Created
- `description` field in response is `null`

---

### TC-UPLOAD-008: Upload — supported formats matrix
**Priority:** P1
**Type:** Functional

For each MIME type the server allows:

| MIME type | Extension |
|-----------|-----------|
| `video/mp4` | .mp4 |
| `video/webm` | .webm |
| `video/x-msvideo` | .avi |
| `video/quicktime` | .mov |
| `video/x-matroska` | .mkv |

Steps: Upload a valid small (< 1 MB) file of each type as Alice.

Expected result:
- HTTP 201 for each
- `contentType` in response matches what was sent

---

### TC-UPLOAD-009: Frontend — client-side file type validation
**Priority:** P1
**Type:** Functional (Manual)

Steps:
1. Navigate to `http://localhost:4200/upload` (logged in as Alice)
2. Use "Choose File" to select a `.txt` file

Expected result:
- Error message displayed: "Invalid file type. Supported formats: MP4, WebM, AVI, MOV, MKV"
- File is NOT submitted to the server
- Upload button remains disabled

---

### TC-UPLOAD-010: Frontend — client-side file size validation
**Priority:** P1
**Type:** Functional (Manual)

Steps:
1. Attempt to drop a file larger than 500 MB onto the drop zone

Expected result:
- Error message: "File too large. Maximum size is 500MB."
- File is NOT submitted

---

### TC-UPLOAD-011: Frontend — drag and drop
**Priority:** P2
**Type:** Functional (Manual)

Steps:
1. Drag a valid .mp4 file over the drop zone; observe the `drag-over` CSS class applied
2. Drop the file

Expected result:
- File info (name, size) appears in the drop zone
- Title field auto-populated from filename (with extension stripped and hyphens/underscores replaced by spaces)
- Drag-over styling is removed after drop

---

### TC-UPLOAD-012: Frontend — upload in progress state
**Priority:** P2
**Type:** Functional (Manual)

Steps:
1. Submit a large file upload
2. Observe the Upload button during the upload

Expected result:
- Button text changes to "Uploading..."
- Button is disabled while upload is in flight
- No duplicate submissions possible

---

### TC-UPLOAD-013: Frontend — successful upload redirects to video detail
**Priority:** P0
**Type:** Functional (Manual)

Steps:
1. Complete a successful upload

Expected result:
- Browser navigates to `/videos/<newId>`
- New video's player and metadata are displayed
- Edit/Delete buttons are visible (Alice is the owner)

---

### TC-UPLOAD-014: Frontend — /upload redirects unauthenticated user to /login
**Priority:** P0
**Type:** Security / Guard (Manual)

Preconditions: Guest session (not logged in)

Steps:
1. Navigate directly to `http://localhost:4200/upload`

Expected result:
- authGuard fires; browser redirects to `/login`
- Upload form is never rendered

---

## 8. Feature Area: Video Streaming

### TC-STREAM-001: Stream video — full file response
**Priority:** P0
**Type:** Functional

Preconditions: Video with ID 1 exists

Steps:
1. `GET /api/videos/1/stream` (no Authorization header)

Expected result:
- HTTP 200 OK
- `Content-Type` header matches the video's stored `contentType` (e.g. `video/mp4`)
- `Content-Disposition: inline`
- Response body is the full video binary
- No authentication required (endpoint is public)

---

### TC-STREAM-002: Stream video — range request (seek/resume)
**Priority:** P0
**Type:** Functional / Critical

Steps:
1. `GET /api/videos/1/stream` with header `Range: bytes=0-1048575` (first 1 MB)

Expected result:
- **BUG #3 (see Section 12):** The current implementation returns HTTP 200 (full file) instead of HTTP 206 Partial Content. Range requests are silently ignored because the controller returns a plain `UrlResource` without configuring `ResourceHttpRequestHandler` or manually parsing the `Range` header.
- Observed: HTTP 200 with full file body
- Expected: HTTP 206 Partial Content with `Content-Range: bytes 0-1048575/<total>` header and partial body

---

### TC-STREAM-003: Stream video — non-existent video
**Priority:** P1
**Type:** Negative

Steps:
1. `GET /api/videos/99999/stream`

Expected result:
- HTTP 404 Not Found
- `{"message":"Video not found with id: 99999"}`

---

### TC-STREAM-004: Stream video — file missing from disk (orphaned record)
**Priority:** P2
**Type:** Edge

Preconditions: Manually delete the physical file from the upload volume while the database record remains

Steps:
1. `GET /api/videos/<orphaned_id>/stream`

Expected result:
- HTTP 500 Internal Server Error
- `StorageException` ("File not found: <filename>") is caught and surfaced generically
- Client receives `{"message":"File storage error occurred"}`

---

### TC-STREAM-005: Frontend — video player loads and plays
**Priority:** P0
**Type:** Functional (Manual, two browsers)

Steps:
1. Navigate to `/videos/<id>` in Chrome and in Firefox
2. Press Play on the `<video>` element

Expected result:
- Video plays without buffering indefinitely on a local network
- Player controls (play/pause, seek bar, volume, fullscreen) are functional
- No browser console errors

---

### TC-STREAM-006: Frontend — video player seek behavior
**Priority:** P1
**Type:** Functional (Manual)

Steps:
1. Click a point midway through the seek bar

Expected result:
- Due to Bug #3 (no 206 support), seeking may require the browser to download the file from the beginning. Document observed behavior.
- Seeking will work if the browser caches the response but is unreliable for large files or on slow connections.

---

## 9. Feature Area: My Videos (Owner Management)

### TC-MYVIDEOS-001: List own videos — authenticated
**Priority:** P0
**Type:** Functional

Preconditions: Alice has uploaded 3 videos; Bob has uploaded 2 videos

Steps:
1. `GET /api/users/me/videos` with Alice's Bearer token

Expected result:
- HTTP 200 OK
- `totalElements: 3`; `content` contains only Alice's 3 videos
- Bob's videos are NOT included

---

### TC-MYVIDEOS-002: List own videos — unauthenticated
**Priority:** P0
**Type:** Security

Steps:
1. `GET /api/users/me/videos` with no Authorization header

Expected result:
- HTTP 401 Unauthorized

---

### TC-MYVIDEOS-003: Get current user profile
**Priority:** P1
**Type:** Functional

Steps:
1. `GET /api/users/me` with Alice's Bearer token

Expected result:
- HTTP 200 OK
- `{"id":<id>,"username":"alice","email":"alice@example.com"}`

---

### TC-MYVIDEOS-004: Update video metadata — title only
**Priority:** P0
**Type:** Functional

Preconditions: Alice uploaded video ID 5

Steps:
1. `PUT /api/videos/5` with Alice's token and body `{"title":"New Title"}`

Expected result:
- HTTP 200 OK
- Response `title: "New Title"`
- `description` is unchanged (not nulled out)

---

### TC-MYVIDEOS-005: Update video metadata — description only
**Priority:** P1
**Type:** Functional

Steps:
1. `PUT /api/videos/5` with Alice's token and body `{"description":"New Description"}`

Expected result:
- HTTP 200 OK
- `description: "New Description"`
- `title` is unchanged

---

### TC-MYVIDEOS-006: Update video — cross-user blocked
**Priority:** P0
**Type:** Security

Preconditions: Alice owns video ID 5

Steps:
1. `PUT /api/videos/5` with BOB's Bearer token and body `{"title":"Hacked"}`

Expected result:
- HTTP 401 Unauthorized
- `{"message":"You can only edit your own videos"}`
- Database record unchanged

---

### TC-MYVIDEOS-007: Update video — title validation
**Priority:** P1
**Type:** Negative

Steps:
1. `PUT /api/videos/5` with Alice's token and body `{"title":""}` (empty string)
2. `PUT /api/videos/5` with Alice's token and body `{"title":"A".repeat(256)}` (title > 255 chars)

Expected result:
- HTTP 400 Bad Request with `fieldErrors.title` message for both

Note: Sending `{"title":""}` with `@Size(min=1)` should fail validation. However, the current `VideoUpdateRequest` has nullable title (partial update semantics). An empty string `""` has length 0, which violates `@Size(min=1)`. Verify the actual response since a blank-but-non-null title may bypass the service-level blank check.

---

### TC-MYVIDEOS-008: Delete video — owner can delete
**Priority:** P0
**Type:** Functional

Preconditions: Alice owns video ID 5

Steps:
1. `DELETE /api/videos/5` with Alice's token

Expected result:
- HTTP 204 No Content
- Subsequent `GET /api/videos/5` returns 404
- Physical file removed from the upload volume (verify with `docker exec vp-backend ls /app/uploads/videos`)
- `videos` table row deleted

---

### TC-MYVIDEOS-009: Delete video — cross-user blocked
**Priority:** P0
**Type:** Security

Preconditions: Alice owns video ID 5

Steps:
1. `DELETE /api/videos/5` with BOB's Bearer token

Expected result:
- HTTP 401 Unauthorized
- `{"message":"You can only delete your own videos"}`
- Video record and physical file still exist

---

### TC-MYVIDEOS-010: Delete video — not found
**Priority:** P1
**Type:** Negative

Steps:
1. `DELETE /api/videos/99999` with Alice's token

Expected result:
- HTTP 404 Not Found

---

### TC-MYVIDEOS-011: Frontend — My Videos page shows only user's own videos
**Priority:** P0
**Type:** Functional (Manual)

Preconditions: Alice logged in; both Alice and Bob have uploaded videos

Steps:
1. Navigate to `http://localhost:4200/my-videos`

Expected result:
- Only Alice's videos are listed
- Each card links to its detail page

---

### TC-MYVIDEOS-012: Frontend — /my-videos redirects guest to /login
**Priority:** P0
**Type:** Security / Guard (Manual)

Preconditions: Guest session

Steps:
1. Navigate to `http://localhost:4200/my-videos`

Expected result:
- authGuard fires; browser redirects to `/login`

---

### TC-MYVIDEOS-013: Frontend — inline edit on video detail page
**Priority:** P1
**Type:** Functional (Manual)

Preconditions: Alice is on `/videos/<her_video_id>`

Steps:
1. Click "Edit"
2. Change title and description
3. Click "Save"

Expected result:
- Edit form replaces the display view
- On save, display view returns with updated title/description
- Page does not reload (SPA update via signal)

---

### TC-MYVIDEOS-014: Frontend — cancel edit restores original values
**Priority:** P2
**Type:** Functional (Manual)

Steps:
1. Click "Edit", change the title to something else
2. Click "Cancel"

Expected result:
- Original title is still displayed
- No PUT request sent to the server (verify in Network tab)

---

### TC-MYVIDEOS-015: Frontend — delete confirmation dialog
**Priority:** P1
**Type:** Functional (Manual)

Steps:
1. Click "Delete" on a video the user owns

Expected result:
- Confirmation inline prompt appears: "Are you sure? [Yes, Delete] [Cancel]"
2. Click "Cancel"
- Confirmation disappears; video is NOT deleted
3. Click "Delete" again, then "Yes, Delete"
- `DELETE /api/videos/<id>` is called
- Browser navigates to `/my-videos`

---

### TC-MYVIDEOS-016: Frontend — edit/delete controls hidden for non-owners
**Priority:** P0
**Type:** Security (Manual)

Preconditions: Alice is logged in; navigating to a video owned by Bob

Steps:
1. Navigate to `/videos/<bob_video_id>`

Expected result:
- "Edit" and "Delete" buttons are NOT rendered
- `isOwner()` returns false because `video.owner.id !== currentUser.id`

---

### TC-MYVIDEOS-017: Frontend — pagination on My Videos page
**Priority:** P2
**Type:** Functional (Manual)

Preconditions: Alice has uploaded > 12 videos

Steps:
1. Navigate to `/my-videos`
2. Click "Next" page

Expected result:
- Page 2 of Alice's videos loads
- "Prev" button becomes active; "Next" button becomes inactive when on the last page

---

## 10. Feature Area: Access Control and Security

### TC-SEC-001: Access protected endpoint with expired access token
**Priority:** P0
**Type:** Security

Steps:
1. Obtain a valid token pair, then wait for expiry (or manually craft an expired JWT)
2. `GET /api/users/me` with the expired access token

Expected result:
- HTTP 401 Unauthorized (filter rejects; `validateToken` returns false for expired token)

---

### TC-SEC-002: Access protected endpoint with tampered token signature
**Priority:** P0
**Type:** Security

Steps:
1. Take a valid access token and modify the signature portion (last segment)
2. `GET /api/users/me` with the tampered token

Expected result:
- HTTP 401 Unauthorized
- JwtException logged at WARN level; no authentication set

---

### TC-SEC-003: Use refresh token as access token
**Priority:** P1
**Type:** Security

Steps:
1. Obtain a valid refresh token from login
2. `GET /api/users/me` with `Authorization: Bearer <refresh_token>`

Expected result:
- HTTP 401 Unauthorized
- The JWT filter checks `tokenType == "access"` and skips setting authentication for refresh-type tokens (lines 37-38 of `JwtAuthenticationFilter`)
- No authentication set, Spring Security rejects the request

---

### TC-SEC-004: Path traversal attempt on stream endpoint
**Priority:** P1
**Type:** Security

Steps:
1. `GET /api/videos/1/stream` where the stored `filePath` for video 1 is a normal UUID filename
2. Attempt to directly call the API with crafted path parameters: not applicable at controller level since video is looked up by DB ID, then the stored filename is resolved

Verify that `StorageService.loadAsResource` uses `uploadPath.resolve(filename).normalize()`:
- The filename stored in DB is always a UUID + extension (generated by StorageService line 59)
- Direct path traversal via the REST API is not possible because the filename comes from the database, not user input at stream time

Expected result:
- This path is safe by design. Confirm no endpoint accepts a raw filename from the user.

---

### TC-SEC-005: CORS — preflight from allowed origin
**Priority:** P1
**Type:** Security

Steps:
1. `OPTIONS /api/videos` with `Origin: http://localhost:4200` and `Access-Control-Request-Method: POST`

Expected result:
- HTTP 200 OK (or 204)
- `Access-Control-Allow-Origin: http://localhost:4200`
- `Access-Control-Allow-Credentials: true`
- `Access-Control-Allow-Methods` includes POST

---

### TC-SEC-006: CORS — request from disallowed origin
**Priority:** P1
**Type:** Security

Steps:
1. `GET /api/videos` with `Origin: http://evil.example.com`

Expected result:
- No `Access-Control-Allow-Origin` header in response (browser would block)
- Response body still returned (CORS is a browser enforcement; the API itself responds, but the browser drops it)

---

### TC-SEC-007: Username uniqueness is case-sensitive (gap)
**Priority:** P2
**Type:** Edge

Steps:
1. Register with username `alice`
2. Register with username `Alice`

Expected result:
- Both registrations succeed (current code does `existsByUsername` which is case-sensitive via standard SQL)
- Two separate users exist: `alice` and `Alice`
- This may be surprising to end users — **see Gap #4 in Section 12**

---

## 11. Regression Checklist

Execute before any release candidate build:

| Area | Check | Method |
|------|-------|--------|
| Authentication | Login returns valid JWT | API |
| Authentication | Register with new email succeeds | API |
| Authentication | Duplicate email blocked | API |
| Authentication | Logout clears localStorage | Manual |
| Guest browsing | `/api/videos` returns 200 without auth | API |
| Guest browsing | `/api/videos/{id}` returns 200 without auth | API |
| Guest browsing | `/api/videos/{id}/stream` returns 200 without auth | API |
| Authorization | `/api/videos` POST without auth returns 401 | API |
| Authorization | `/api/users/me` without auth returns 401 | API |
| Authorization | PUT/DELETE another user's video returns 401 | API |
| Upload | Valid MP4 upload returns 201 | API |
| Upload | `.txt` upload returns error (not 201) | API |
| Upload | File > 500 MB returns 400 | API |
| Streaming | Stream returns video bytes with correct Content-Type | API |
| My Videos | GET /users/me/videos returns only current user's videos | API |
| Frontend | Home page loads and shows videos | Manual |
| Frontend | `/upload` redirects guest to login | Manual |
| Frontend | `/my-videos` redirects guest to login | Manual |
| Frontend | Token attached to authenticated API calls | DevTools |
| Frontend | Video player renders on detail page | Manual |
| Frontend | Edit/Delete hidden for non-owner | Manual |
| Pagination | Page 0 and page 1 return different content | API |
| Error handling | 404 on unknown video ID | API |
| Error handling | 500 errors return generic message, not stack trace | API |

---

## 12. Identified Bugs and Gaps

The following issues were identified during code review before test execution. They should be tracked as defects.

---

### Bug #1 — Storage errors returned as 500 instead of 400 for invalid uploads

**Severity:** HIGH
**Priority:** P1
**Endpoint:** `POST /api/videos`

**Description:**
`StorageException` is a runtime exception thrown by `StorageService.validateFile()` when the content type is invalid or the file is empty. `GlobalExceptionHandler` maps `StorageException` to HTTP 500. This means a client uploading a `.txt` file receives a 500 Internal Server Error, which is semantically incorrect — 400 Bad Request (client error) is the appropriate status for invalid input.

**File:** `GlobalExceptionHandler.java` line 41-45, `StorageService.java` lines 98-106

**Steps to reproduce:**
1. Authenticated POST to `/api/videos` with Content-Type `text/plain`
2. Observe HTTP 500 response

**Expected:** HTTP 400 Bad Request with `"Invalid file type: ..."` message
**Actual:** HTTP 500 Internal Server Error with `"File storage error occurred"` (message is swallowed)

**Fix:** Either change the `StorageException` handler to return 400, or throw `BadRequestException` from `validateFile()` for validation failures and reserve `StorageException` for I/O errors.

---

### Bug #2 — File written to disk before title validation in upload flow

**Severity:** MEDIUM
**Priority:** P2
**Endpoint:** `POST /api/videos`

**Description:**
In `VideoService.upload()`, `storageService.store(file)` is called on line 39 before the title null/blank check on line 35. Wait — the title check is actually at lines 35-37, which runs BEFORE `storageService.store()` at line 39. This ordering is correct for the blank title case. However, there is a secondary scenario: the `title` parameter is a raw `@RequestParam` with no `@NotBlank` annotation on the controller method signature (VideoController line 40). If the client sends `title=` (blank), the service layer catches it. But if `title` is sent as only whitespace (e.g. `title=   `), `title.isBlank()` returns true and `BadRequestException` is thrown — but only AFTER the multipart body has been parsed (the file bytes are buffered in memory by Spring's multipart resolver). No orphaned file is created in this path.

**Actual gap:** There is no `@NotBlank` validation on the `title` `@RequestParam` in `VideoController.uploadVideo()`. Validation happens inside the service, which is acceptable but means the file is fully received and buffered before the title check. For very large files, this wastes bandwidth. This is a design concern, not a critical bug.

**Recommendation:** Add `@RequestParam @NotBlank String title` or handle at the controller boundary.

---

### Bug #3 — Video streaming does not support HTTP Range requests (no 206 Partial Content)

**Severity:** HIGH
**Priority:** P1
**Endpoint:** `GET /api/videos/{id}/stream`

**Description:**
The stream endpoint in `VideoController.streamVideo()` returns `ResponseEntity.ok()` with a `UrlResource` body. Spring's default `ResourceHttpMessageConverter` will serve the full file (200 OK) regardless of any `Range` request header sent by the browser. Browser-native `<video>` elements rely on HTTP 206 Partial Content responses to implement seeking, resumable playback, and efficient buffering. Without range support:

- Large videos cannot be seeked to a position without the browser downloading from the start
- Playback may stall on slow connections
- Mobile browsers may fail to play videos at all (some require 206)
- Safari on macOS and iOS requires range support for `<video>` playback

**File:** `VideoController.java` lines 59-68

**Steps to reproduce:**
1. `curl -v -H "Range: bytes=0-1023" http://localhost:8080/api/videos/1/stream`
2. Observe: HTTP 200 with full file; no `Content-Range` header

**Expected:** HTTP 206 with `Content-Range: bytes 0-1023/<total>`

**Fix:** Replace the manual `ResponseEntity` return with Spring's `ResourceHttpRequestHandler`, or manually parse the `Range` header and use `StreamingResponseBody` with `RandomAccessFile` to serve partial content, or configure a `ResourceRegion`-based response using Spring's `ResourceRegionMessageConverter`. This is a P1 blocker for release because seeking is a basic video player expectation.

---

### Bug #4 — Access token expiry field type mismatch (Long vs number)

**Severity:** LOW
**Priority:** P3
**Endpoint:** `POST /api/auth/login`, `POST /api/auth/register`, `POST /api/auth/refresh`

**Description:**
`AuthResponse.expiresIn` is declared as `Long` in Java. The Angular model `AuthResponse` declares `expiresIn` as `number`. The actual value returned is `86400` (seconds). This is consistent. However, `AuthResponse.java` line 14 shows `private Long expiresIn`, while the Angular model `auth.model.ts` line 16 has `expiresIn: number`. Both are fine for current values, but it is worth noting the frontend does not use `expiresIn` to schedule proactive token refresh — it relies entirely on catching 401 errors reactively. A proactive refresh strategy would be more reliable.

---

### Gap #5 — No sign-out invalidation at the server (stateless JWT)

**Severity:** MEDIUM
**Priority:** P2

**Description:**
Logout (`AuthService.logout()` in Angular) only clears `localStorage`. The backend has no `/api/auth/logout` endpoint. The issued JWT remains cryptographically valid until it expires (24 hours). If an attacker extracts a token from localStorage before logout, they can use it for the full TTL. For an MVP this is an accepted trade-off of stateless JWT, but it should be documented and a token blocklist or short-lived tokens considered post-MVP.

---

### Gap #6 — Username uniqueness check is case-sensitive; email check is also case-sensitive

**Severity:** LOW
**Priority:** P3

**Description:**
`UserRepository.existsByUsername("alice")` uses a standard JPA equals query which is case-sensitive in PostgreSQL by default. A user can register both `alice` and `Alice` as separate accounts. Similarly, `existsByEmail` is case-sensitive: `ALICE@EXAMPLE.COM` and `alice@example.com` are treated as different emails. This will cause confusion and potential duplicate accounts.

**Fix:** Normalize username and email to lowercase before persistence and before lookup, or use `LOWER(email) = LOWER(:email)` queries.

---

### Gap #7 — No file size stored accurately at the API level vs. actual disk size

**Severity:** LOW
**Priority:** P3

**Description:**
`video.fileSize` is set from `file.getSize()` which returns the size of the `MultipartFile` as received. This is the correct file size. No issue here, but the `VideoResponse` exposes raw bytes which the frontend formats correctly. Confirm no off-by-one in the formatter (both `VideoCardComponent.formatFileSize` and `VideoDetailComponent.formatFileSize` handle `bytes === 0` slightly differently — one returns `"0 B"` and the other returns `"0 B"` only if `!bytes` which also catches `null`/`undefined`).

---

### Gap #8 — `pom.xml` declares `java.version` as 17 but the README and feature tracking mention Java 21

**Severity:** LOW
**Priority:** P3

**File:** `backend/pom.xml` line 21: `<java.version>17</java.version>`
**Feature tracking doc:** "Spring Boot 3 / Java 21"

**Description:**
The Maven property controlling the compiler source/target is set to Java 17, not Java 21. This means the project compiles targeting Java 17 bytecode even if running on a Java 21 JVM. No Java 21 features (virtual threads, record patterns, sequenced collections) are available. This is not a functional bug for the current MVP code but is a documentation/configuration inconsistency.

---

### Gap #9 — The `CorsConfig` registers a WebMvcConfigurer mapping but does not define a `CorsConfigurationSource` bean used by Spring Security

**Severity:** MEDIUM
**Priority:** P2

**Description:**
`CorsConfig.java` implements `WebMvcConfigurer.addCorsMappings()`. This configures CORS for Spring MVC handler dispatch. However, Spring Security processes requests before the MVC dispatcher. The `SecurityConfig` does not call `http.cors(...)` and does not reference a `CorsConfigurationSource` bean. The `CorsConfigurationSource` bean declared in `CorsConfig` (if one existed) would be needed for Security-layer CORS enforcement.

Looking at the code: `CorsConfig` only overrides `addCorsMappings` — it does NOT declare a `@Bean CorsConfigurationSource`. This means CORS headers may not be added to responses for requests rejected by Spring Security (e.g., a 401 response to an `OPTIONS` preflight will have no CORS headers, causing the browser to treat it as a CORS error rather than an auth error). This can cause confusing behavior in the Angular app where a 401 appears as a network error instead of an HTTP error.

**Fix:** Add `http.cors(Customizer.withDefaults())` to `SecurityConfig.securityFilterChain()` and declare a `@Bean CorsConfigurationSource` in `CorsConfig`, or apply both approaches together.

---

## 13. Test Automation Gaps

The following test types are missing and should be added:

### Missing Unit Tests

| Class | Missing Coverage |
|-------|-----------------|
| `StorageService` | `validateFile()` for each invalid type, empty file, and successful store/delete/load |
| `JwtTokenProvider` | `generateAccessToken`, `generateRefreshToken`, `validateToken` (expired, tampered, wrong type) |
| `VideoController` | Upload endpoint (multipart), stream endpoint (content-type header), update/delete authorization |
| `UserController` | `GET /api/users/me` returns correct user; `GET /api/users/me/videos` delegates to VideoService |

### Missing Integration Tests

| Scenario | What to Test |
|----------|-------------|
| Register + Login flow | Full round-trip with real BCrypt and JWT signing against H2 |
| Upload + Stream | Multipart upload stored to temp dir; then stream retrieves same bytes |
| Delete + Verify Gone | Delete removes both DB row and file |
| Cross-user edit blocked | Real Spring Security context rejects Bob editing Alice's video |

### Missing API Tests (Recommended: RestAssured or Testcontainers)

- All 9 endpoints with valid and invalid inputs
- CORS preflight responses
- Range request handling (currently failing — Bug #3)
- Pagination boundary conditions (page beyond last, negative page)

### Missing E2E Tests (Recommended: Playwright or Cypress)

| Journey | Priority |
|---------|---------|
| Guest browses home page, clicks a video, watches it | P0 |
| New user registers, uploads a video, watches it, deletes it | P0 |
| User tries to access /upload without login, is redirected, logs in, and can upload | P1 |

---

## 14. Acceptance Criteria Summary

The MVP is ready for release when all of the following are true:

| # | Criterion | Status |
|---|-----------|--------|
| AC-1 | An unauthenticated user can browse the video list and watch any video | To Test |
| AC-2 | A new user can register with email, username, and password (6-100 chars) | To Test |
| AC-3 | A registered user can log in and receive an access + refresh token pair | To Test |
| AC-4 | Logout clears all client-side session data | To Test |
| AC-5 | An authenticated user can upload MP4, WebM, AVI, MOV, or MKV files up to 500 MB | To Test |
| AC-6 | Files of unsupported types are rejected before being stored | Bug #1 (wrong status code) |
| AC-7 | Videos have a required title (1-255 chars) and optional description | To Test |
| AC-8 | Uploaded videos are immediately visible in the public listing | To Test |
| AC-9 | Videos play in the browser via the stream endpoint | To Test |
| AC-10 | Seeking within a video works reliably | Bug #3 (no Range support) |
| AC-11 | An authenticated user can only edit or delete their own videos | To Test |
| AC-12 | Cross-user edit/delete attempts return 401 (not 500) | To Test |
| AC-13 | Unauthenticated requests to protected endpoints return 401 | To Test |
| AC-14 | `/upload` and `/my-videos` routes redirect guests to `/login` | To Test |
| AC-15 | All 20 existing unit tests pass | PASSING |
| AC-16 | No stack traces or internal paths exposed in error responses | To Test |

**Release is blocked by:** Bug #3 (no HTTP 206) and Bug #1 (wrong status code for invalid file type). These should be fixed and re-tested before the MVP ships.
