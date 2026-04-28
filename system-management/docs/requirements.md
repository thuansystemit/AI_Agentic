# Requirements Specification

**Agent:** `@requirements-analyst`
**Project:** System Management
**Date:** 2026-04-25
**Status:** Final — approved

---

## 1. Scope

A web-based system for document management with authentication, role-based authorization, and category-level permission control. Users belong to groups and carry global roles. Access to categories and their documents is resolved from the most permissive of three layers: global role, group permission, direct user override.

---

## 2. Assumptions & Decisions

| # | Decision | Rationale |
|---|----------|-----------|
| D-01 | Only ADMIN can create/update/delete categories | Prevents unauthorized category sprawl |
| D-02 | Group deletion cascades — permissions revoked immediately | Simplicity; no orphan permission risk |
| D-03 | Categories with no effective permission are hidden from listing | Least-privilege; no information leakage |
| D-04 | JWT stored in HTTP-only, Secure, SameSite=Strict cookies | Prevents XSS token theft |
| D-05 | Role NOT stored in JWT — resolved from DB on every request | Prevents stale-role privilege persistence |
| D-06 | Most permissive permission wins across all three layers | Explicit design goal from user |

---

## 3. Formal Requirements

### 3.1 Authentication

| ID | Requirement |
|----|-------------|
| AUTH-01 | The system SHALL authenticate users via email and password |
| AUTH-02 | On successful login the system SHALL issue an access token (JWT, 15 min TTL) in an HTTP-only, Secure, SameSite=Strict cookie |
| AUTH-03 | On successful login the system SHALL issue a refresh token (opaque, 7 day TTL) in an HTTP-only, Secure, SameSite=Strict cookie scoped to the refresh endpoint only |
| AUTH-04 | The system SHALL reject login with a generic error message for both wrong password and unknown email (no enumeration) |
| AUTH-05 | The system SHALL lock an account for 30 minutes after 5 consecutive failed login attempts |
| AUTH-06 | The system SHALL unlock an account automatically after the 30-minute lockout expires |
| AUTH-07 | The system SHALL provide a token refresh endpoint that rotates the refresh token atomically (revoke old, issue new) |
| AUTH-08 | The system SHALL detect refresh token reuse and immediately revoke all tokens in the same token family |
| AUTH-09 | The system SHALL provide a logout endpoint that revokes the current refresh token and clears both cookies |
| AUTH-10 | The system SHALL never store the raw refresh token — only its SHA-256 hash |
| AUTH-11 | The system SHALL resolve the user's role from the database on every authenticated request — never from the JWT payload |

### 3.2 User Management

| ID | Requirement |
|----|-------------|
| USR-01 | The system SHALL allow ADMIN to create users with: email (unique), full name, global role |
| USR-02 | The system SHALL allow ADMIN to update a user's full name, global role, and active status |
| USR-03 | The system SHALL NOT allow an ADMIN to modify their own role or deactivate themselves |
| USR-04 | The system SHALL allow ADMIN to deactivate a user, which immediately revokes all their refresh tokens |
| USR-05 | The system SHALL allow ADMIN to list and search users by name or email |
| USR-06 | Deactivated users SHALL NOT be able to log in |
| USR-07 | The system SHALL support three global roles: ADMIN, EDITOR, VIEWER |

### 3.3 Group Management

| ID | Requirement |
|----|-------------|
| GRP-01 | The system SHALL allow ADMIN to create, update, and delete groups |
| GRP-02 | The system SHALL allow ADMIN to add and remove users from groups |
| GRP-03 | A user MAY belong to zero or more groups |
| GRP-04 | Deleting a group SHALL immediately remove all category permissions granted to that group |
| GRP-05 | The system SHALL allow ADMIN to list all groups and their members |

### 3.4 Category Management

| ID | Requirement |
|----|-------------|
| CAT-01 | The system SHALL allow ADMIN to create categories with a unique name and optional description |
| CAT-02 | The system SHALL allow ADMIN to update and delete categories |
| CAT-03 | Deleting a category SHALL cascade-delete all its documents and all permissions on it |
| CAT-04 | Non-ADMIN users SHALL only see categories on which they have at least READ permission |
| CAT-05 | ADMIN users SHALL see all categories regardless of permissions |

### 3.5 Permission Model

| ID | Requirement |
|----|-------------|
| PERM-01 | The system SHALL support three permission levels per category: READ, WRITE, EDIT (ordered, each includes lower) |
| PERM-02 | READ allows: list and view documents |
| PERM-03 | WRITE allows: READ + create and update documents |
| PERM-04 | EDIT allows: WRITE + delete documents and manage category permissions |
| PERM-05 | Effective permission SHALL be the most permissive across three layers: global role baseline, group permissions, direct user override |
| PERM-06 | Global role baseline: ADMIN → EDIT (all categories); EDITOR → WRITE; VIEWER → READ |
| PERM-07 | ADMIN users SHALL always have EDIT on every category — no override can reduce this |
| PERM-08 | The system SHALL allow users with EDIT permission to assign READ/WRITE/EDIT to individual users on a category |
| PERM-09 | The system SHALL allow users with EDIT permission to assign READ/WRITE/EDIT to groups on a category |
| PERM-10 | Permission changes SHALL take effect immediately on the next request — no caching |
| PERM-11 | If a user's permission on a category is revoked mid-session, their next request SHALL return 403 |

### 3.6 Document Management

| ID | Requirement |
|----|-------------|
| DOC-01 | A category SHALL contain zero or more documents |
| DOC-02 | A document SHALL have: title (required), content (optional, defaults to empty string), category, creator, timestamps |
| DOC-03 | The system SHALL require READ permission to list or view documents in a category |
| DOC-04 | The system SHALL require WRITE permission to create or update a document |
| DOC-05 | The system SHALL require EDIT permission to delete a document |
| DOC-06 | Document access SHALL check permission against the document's category |
| DOC-07 | The system SHALL NEVER fetch a document and then check permission — permission check and fetch SHALL be atomic (prevent IDOR) |

---

## 4. Gherkin Acceptance Criteria

```gherkin
Feature: Authentication

  Scenario: Successful login
    Given a user exists with email "alice@darkness.com" and valid password
    And the account is active and not locked
    When the user submits correct credentials
    Then the response status is 200
    And an HTTP-only access token cookie is set (maxAge 15 min)
    And an HTTP-only refresh token cookie is set scoped to /api/v1/auth/refresh

  Scenario: Login with wrong password
    Given a user exists with email "alice@darkness.com"
    When the user submits an incorrect password
    Then the response status is 401
    And the error message is "Invalid email or password"
    And no cookie is set
    And the failed attempt counter increments by 1

  Scenario: Login with unknown email
    When the user submits email "ghost@darkness.com"
    Then the response status is 401
    And the error message is "Invalid email or password"
    And the response time is indistinguishable from a wrong-password response

  Scenario: Account lockout after 5 failures
    Given "alice@darkness.com" has failed login 4 times
    When the user submits an incorrect password
    Then the response status is 423
    And the account is locked for 30 minutes
    And subsequent login attempts return 423 until lockout expires

  Scenario: Refresh token reuse detected
    Given a valid refresh token RT-1 has been used to obtain RT-2
    When the client attempts to use RT-1 again
    Then the response status is 401
    And both RT-1 and RT-2 are revoked (entire token family invalidated)

Feature: Permission Resolution

  Scenario: ADMIN always has EDIT
    Given a user with global role ADMIN
    When they access any category
    Then their effective permission is EDIT regardless of any group or direct override

  Scenario: Most permissive wins — group elevates VIEWER
    Given a user with global role VIEWER (baseline: READ)
    And the user belongs to group "editors" which has WRITE on category C1
    When the effective permission for C1 is resolved
    Then the result is WRITE

  Scenario: Direct override elevates above group
    Given a user with global role VIEWER (baseline: READ)
    And the user belongs to group "viewers" with READ on category C1
    And the user has a direct EDIT override on category C1
    When the effective permission for C1 is resolved
    Then the result is EDIT

  Scenario: Category hidden with no permission
    Given a user with global role VIEWER and no group or direct permissions on category C2
    When the user lists all categories
    Then category C2 does not appear in the response

  Scenario: Mid-session permission revocation
    Given a user is authenticated with READ on category C1
    When an ADMIN revokes all permissions for that user on C1
    And the user immediately requests documents from C1
    Then the response status is 403

Feature: Document Access Control

  Scenario: IDOR prevention on document fetch
    Given user A has no permission on the category containing document D1
    When user A requests document D1 by ID
    Then the response status is 403
    And document D1 content is NOT returned

  Scenario: Create document requires WRITE
    Given a user has READ permission on category C1
    When the user attempts to create a document in C1
    Then the response status is 403

  Scenario: Delete document requires EDIT
    Given a user has WRITE permission on category C1
    When the user attempts to delete a document in C1
    Then the response status is 403

Feature: User Management

  Scenario: Admin cannot deactivate themselves
    Given an ADMIN is authenticated
    When the ADMIN sends a request to set their own isActive to false
    Then the response status is 400
    And the ADMIN account remains active

  Scenario: Deactivation revokes all tokens
    Given user Bob is active with two active refresh tokens
    When an ADMIN deactivates Bob
    Then both refresh tokens are immediately revoked
    And Bob's next request with any token returns 401
```

---

## 5. Edge Cases

| # | Edge Case | Expected Behaviour |
|---|-----------|-------------------|
| EC-01 | User belongs to multiple groups with different permissions on same category | Most permissive group permission is used |
| EC-02 | User's group permission + direct override both present | Most permissive of the two is used |
| EC-03 | Access token expires mid-session | Client receives 401; must call refresh endpoint |
| EC-04 | Refresh token used after expiry (7 days) | 401; token is revoked; user must re-login |
| EC-05 | Two concurrent refresh requests with same token | SELECT FOR UPDATE ensures only one succeeds; second receives 401 |
| EC-06 | Category name collision (case-insensitive) | 409 Conflict |
| EC-07 | Creating a document in a non-existent category | 404 Not Found |
| EC-08 | Pagination on empty collection | Returns empty list, not 404 |
| EC-09 | User removed from group while session active | Next request re-resolves permission from DB; group permission no longer applies |
| EC-10 | ADMIN role assigned to existing EDITOR mid-session | Takes effect on next request (DB-resolved role) |

---

## 6. Out of Scope

- Password reset / forgot password flow
- Email verification on registration
- Multi-factor authentication (MFA)
- OAuth / SSO integration
- Document versioning or history
- Real-time collaboration
- File attachments (documents are text/content only)
- Audit log UI (logs exist in application logs only)
- Multi-tenancy

---

## 7. Traceability Matrix

| Req ID | User Story | Gherkin Scenario | Test ID |
|--------|------------|-----------------|---------|
| AUTH-01–11 | Login / token lifecycle | Successful login, wrong password, lockout, reuse | TC-AUTH-001–008 |
| USR-01–07 | User CRUD | Admin deactivates self, token revocation | TC-USR-001–005 |
| GRP-01–05 | Group CRUD + membership | — | TC-GRP-001–003 |
| CAT-01–05 | Category CRUD | Category hidden with no permission | TC-CAT-001–004 |
| PERM-01–11 | Permission resolution | ADMIN always EDIT, most permissive wins, mid-session revoke | TC-PERM-001–006 |
| DOC-01–07 | Document CRUD | IDOR prevention, WRITE/EDIT gates | TC-DOC-001–005 |
