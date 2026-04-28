# Implementation Plan

**Agents:** `@estimator` + `@planner`
**Project:** System Management
**Date:** 2026-04-25
**Input:** requirements.md, architecture.md, api-spec.md, data-model.md, security-review.md

---

## Estimation Summary

| User Story | Layer breakdown | Points | Risk |
|------------|----------------|--------|------|
| US-001 Authentication | DB + Domain + Security + Service + Controller + Tests | 8 | HIGH — BCrypt order, token rotation, cookie scoping |
| US-002 User Management | Service + Controller + Tests | 5 | LOW |
| US-003 Group Management | Service + Controller + Tests | 5 | LOW |
| US-004 Category Management | Service + Controller + Tests | 3 | LOW |
| US-005 Permission Management | Service (3-layer resolution) + Controller + Tests | 8 | HIGH — most-permissive logic, SELECT FOR UPDATE |
| US-006 Document Management | Service + Controller + Tests | 5 | MEDIUM — IDOR prevention |
| Frontend — Core | Setup + Auth + Interceptor + Guards | 8 | MEDIUM — Angular 21 patterns |
| Frontend — Features | All feature modules | 13 | MEDIUM |
| DevOps | Dockerfile + docker-compose + CI/CD | 5 | LOW |
| **Total** | | **60** | |

**Confidence range:**
- Optimistic: 52 points (~2.5 sprints at 20 pts/sprint)
- Likely: 60 points (~3 sprints)
- Pessimistic: 75 points (~4 sprints, if security review findings require rework)

---

## Risk Register

| Risk | Likelihood | Impact | Mitigation |
|------|------------|--------|------------|
| BCrypt timing attack (FINDING-007) | Medium | HIGH | Enforce exact check order in AuthService from day 1 |
| IDOR on document fetch (FINDING-001) | High | HIGH | Permission check before data return — enforced in TDD test |
| Refresh token race condition (FINDING-014) | Low | HIGH | SELECT FOR UPDATE in repository — write test for concurrent refresh |
| Permission resolution incorrect | Medium | HIGH | Unit test all 3-layer combinations before integration |
| Angular 21 breaking changes | Low | Medium | Pin versions, test interceptor/guard pattern early |
| DB migration failure on team machines | Low | Medium | Flyway baseline + Docker Compose for local DB |

---

## Critical Path

```
B1 (Setup) → B2 (Migrations) → B3 (Domain) → B4 (Repositories)
    → B5 (Security) → B6-AUTH (AuthService) → B7-AUTH (AuthController)
        → B6-PERM (PermissionService)  ← BLOCKS all other services
            → B6-USR/GRP/CAT/DOC (remaining services)
                → B7 (remaining controllers)
                    → F1 (Frontend setup) → F2 (Auth) → F3 (Features)
```

**`PermissionService` is the critical blocker** — every other service depends on it for access control. It must be completed and tested before any other service can be fully implemented.

---

## Sprint Plan

### Sprint 1 — Foundation + Auth + Permissions (20 pts)

Goal: A working, secured API with authentication and permission resolution. No business features yet — just the security backbone.

---

### Sprint 2 — Business Features Backend (20 pts)

Goal: All backend endpoints working. Full API coverage with tests.

---

### Sprint 3 — Frontend + DevOps + Hardening (20 pts)

Goal: Working Angular SPA + Docker + CI/CD. Ready for release.

---

## Ordered Task Breakdown

### PHASE B1 — Project Setup [3 pts]

**B1-01** — Initialize Spring Boot project `[1 pt] [low risk]`
- Create Maven project with Java 21
- Dependencies: spring-boot-starter-web, spring-boot-starter-data-jpa, spring-boot-starter-security, spring-boot-starter-validation, postgresql, flyway-core, jjwt, lombok
- Add OWASP dependency-check plugin (fail on CVSS ≥ 7)
- Files: `pom.xml`
- Depends on: nothing

**B1-02** — Configure application properties `[1 pt] [low risk]`
- `application.yml`: server port, datasource, JPA, Flyway, JWT config, CORS origins
- Set `server.error.include-stacktrace: never` (FINDING-005)
- Set `management.endpoints.web.exposure.include: health,info` (FINDING-012)
- Files: `src/main/resources/application.yml`, `application-test.yml`
- Depends on: B1-01

**B1-03** — Create package structure + base exception classes `[1 pt] [low risk]`
- Create all packages per architecture.md
- Exception classes: `ResourceNotFoundException`, `AccessDeniedException`, `EmailAlreadyExistsException`, `AccountLockedException`, `InvalidTokenException`, `CannotModifySelfException`, `DuplicateNameException`
- `GlobalExceptionHandler` — safe messages only (FINDING-005)
- Files: all exception classes, `GlobalExceptionHandler.java`
- Depends on: B1-01

---

### PHASE B2 — Database Migrations [2 pts]

**B2-01** — Flyway migrations V1–V7 `[2 pt] [low risk]`
- V1: PostgreSQL enums (`global_role`, `permission_level`)
- V2: `users` table
- V3: `groups` + `group_members` tables
- V4: `categories` table
- V5: `documents` table (ON DELETE CASCADE from categories)
- V6: `category_user_permissions` + `category_group_permissions` tables
- V7: `refresh_tokens` table + all indexes (including partial index on active tokens, family_id index)
- Files: `src/main/resources/db/migration/V1__*.sql` through `V7__*.sql`
- Depends on: B1-02

**B2-02** — Flyway V8 seed data `[0 pt — included in B2-01]`
- V8: Seed default ADMIN user for development (BCrypt hash of known password)
- Files: `V8__seed_admin.sql`

---

### PHASE B3 — Domain Models [2 pts]

**B3-01** — Enums + domain entities `[2 pt] [low risk]`
- `GlobalRole.java` enum: ADMIN, EDITOR, VIEWER
- `Permission.java` enum: READ(1), WRITE(2), EDIT(3) with `getLevel()` and `isAtLeast()` methods
- `User.java` JPA entity with `isLocked()` helper (checks `lockedUntil` vs `Instant.now()`)
- `Group.java`, `GroupMember.java` (composite PK)
- `Category.java`, `Document.java`
- `CategoryUserPermission.java`, `CategoryGroupPermission.java` (composite PKs)
- `RefreshToken.java` with `familyId` UUID field and `isExpired()` helper
- Files: all domain classes
- Depends on: B2-01

---

### PHASE B4 — Repositories [2 pts]

**B4-01** — JPA Repositories `[2 pt] [low risk]`
- `UserRepository`: `findByEmailIgnoreCase`, `findRoleById`, `existsById`, search by name/email
- `GroupRepository`: `findByNameIgnoreCase`
- `GroupMemberRepository`: `findGroupIdsByUserId`, `existsByIdUserIdAndIdGroupId`, `deleteByIdUserIdAndIdGroupId`
- `CategoryRepository`: `findByNameIgnoreCase`, `findAccessibleByUserId` (JOIN through permissions)
- `DocumentRepository`: `findByCategoryId`
- `CategoryUserPermissionRepository`: `findByCategoryIdAndUserId`, `findByCategoryId`
- `CategoryGroupPermissionRepository`: `findByCategoryIdAndGroupIdIn`, `findByCategoryId`
- `RefreshTokenRepository`:
  - `@Lock(PESSIMISTIC_WRITE) findByTokenHashAndIsRevokedFalse` (FINDING-014)
  - `findByTokenHash`
  - `revokeAllByFamilyId` (bulk UPDATE)
  - `revokeAllByUserId` (bulk UPDATE)
- Files: all repository interfaces
- Depends on: B3-01

---

### PHASE B5 — Security Layer [5 pts] ⚠️ HIGH RISK

**B5-01** — JWT configuration + provider `[2 pt] [medium risk]`
- `JwtConfig.java`: secret (min 32 chars validated at startup), access TTL (15 min), refresh TTL (7 days)
- `JwtTokenProvider.java`:
  - `generateAccessToken(UUID userId)` — payload: `sub=userId`, `iat`, `exp` ONLY (no role — FINDING-003 from architecture)
  - `validateAndExtractUserId(String token)` → UUID
- CORS configuration: explicit origins, no wildcards, validate at startup (FINDING-006)
- Files: `JwtConfig.java`, `JwtTokenProvider.java`, `CorsConfig.java`
- Depends on: B1-03

**B5-02** — Authentication filter + security config `[3 pt] [high risk]`
- `UserPrincipal.java`: wraps User, implements UserDetails, exposes `userId`, `role`
- `JwtAuthenticationFilter.java`:
  - Reads JWT from `access_token` cookie
  - Validates token → extracts userId
  - Loads fresh User from DB (FINDING-003: role from DB, not JWT)
  - Sets SecurityContext
  - Skips `/auth/login` and `/auth/refresh`
- `SecurityConfig.java`:
  - CSRF disabled (SameSite=Strict handles it)
  - Stateless session
  - JWT filter before `UsernamePasswordAuthenticationFilter`
  - Permit: `/auth/login`, `/auth/refresh`
  - Add security response headers (FINDING-003)
- Files: `UserPrincipal.java`, `JwtAuthenticationFilter.java`, `SecurityConfig.java`
- Depends on: B5-01

---

### PHASE B6 — Services [13 pts] ⚠️ CRITICAL PATH

**B6-01** — PermissionService `[5 pt] [high risk]` ← MUST COMPLETE FIRST
- Three-layer resolution: role baseline → group max → direct override → most permissive
- ADMIN short-circuit → always returns EDIT
- `resolvePermission(userId, categoryId)` → Permission
- `requirePermission(userId, categoryId, required)` → throws AccessDeniedException with WARN log
- No caching — always reads from DB (PERM-10)
- Unit tests: all 3-layer combinations, ADMIN short-circuit, most-permissive-wins, mid-session revoke
- Files: `PermissionService.java`, `PermissionServiceTest.java`
- Depends on: B4-01

**B6-02** — AuthService `[5 pt] [high risk]`
- `login()`:
  - BCrypt always runs first — even for null user (FINDING-007)
  - Order: fetch → BCrypt → null/mismatch check → active check → lock check → reset counter
  - Account lockout: 5 attempts → 30 min lock
- `generateAndPersistRefreshToken()`: raw UUID → SHA-256 → store hash + familyId
- `refresh()`:
  - SELECT FOR UPDATE on token lookup (FINDING-014)
  - Reuse detection: revoked token found → revokeAllByFamilyId → throw 401
  - Atomic rotation: revoke old → insert new (same familyId) in one @Transactional
- `logout()`: revoke current refresh token
- Cookie builders: access (Path=/), refresh (Path=/api/v1/auth/refresh — FINDING-002)
- Files: `AuthService.java`, `AuthServiceTest.java`
- Depends on: B4-01, B5-01

**B6-03** — UserService `[1 pt] [low risk]`
- `create()`: email dedup, BCrypt random password (user sets password via separate flow)
- `update()`: cannot modify own role or deactivate self (USR-03)
- `deactivate()`: calls `revokeAllByUserId` immediately (AUTH-04)
- Files: `UserService.java`, `UserServiceTest.java`
- Depends on: B4-01, B6-01

**B6-04** — GroupService `[1 pt] [low risk]`
- CRUD with name dedup
- `addMember()`: idempotent
- `removeMember()`: cascades via DB FK (group delete removes permissions)
- Files: `GroupService.java`, `GroupServiceTest.java`
- Depends on: B4-01

**B6-05** — CategoryService `[1 pt] [low risk]`
- `findAccessible()`: ADMIN → all; others → filtered by permission (CAT-04/05)
- Files: `CategoryService.java`, `CategoryServiceTest.java`
- Depends on: B4-01, B6-01

**B6-06** — DocumentService `[2 pt] [medium risk]`
- `findById()`: fetch first → permission check → return (FINDING-001 — never reverse this order)
- `create()`: WRITE required
- `update()`: WRITE required
- `delete()`: EDIT required
- Files: `DocumentService.java`, `DocumentServiceTest.java`
- Depends on: B4-01, B6-01

---

### PHASE B7 — Controllers [8 pts]

**B7-01** — AuthController `[2 pt] [medium risk]`
- `POST /auth/login` → set both cookies in response
- `POST /auth/refresh` → extract cookie → rotate → set new cookies (204)
- `POST /auth/logout` → revoke → clear cookies (204)
- `GET /auth/me` → return current user from SecurityContext
- Files: `AuthController.java`, `AuthControllerIT.java`
- Depends on: B6-02, B5-02

**B7-02** — UserController `[1 pt] [low risk]`
- All endpoints: `@PreAuthorize("hasRole('ADMIN')")`
- `GET /users` → paginated + search
- `POST /users` → 201 + Location header
- `GET /users/{id}`, `PATCH /users/{id}`
- Files: `UserController.java`, `UserControllerIT.java`
- Depends on: B6-03

**B7-03** — GroupController `[1 pt] [low risk]`
- All endpoints: `@PreAuthorize("hasRole('ADMIN')")`
- CRUD + member management
- Files: `GroupController.java`
- Depends on: B6-04

**B7-04** — CategoryController `[1 pt] [low risk]`
- Create/update/delete: `@PreAuthorize("hasRole('ADMIN')")`
- List/get: any authenticated user (filtered by PermissionService)
- Files: `CategoryController.java`
- Depends on: B6-05

**B7-05** — DocumentController `[1 pt] [low risk]`
- Extract `userId` from `SecurityContext` → pass to service
- All permission checks happen in DocumentService
- Files: `DocumentController.java`
- Depends on: B6-06

**B7-06** — PermissionController `[2 pt] [medium risk]`
- `GET/PUT/DELETE /categories/{id}/permissions/users/{userId}`
- `GET/PUT/DELETE /categories/{id}/permissions/groups/{groupId}`
- Requires EDIT permission on the category (checked via PermissionService)
- Files: `PermissionController.java`, `PermissionControllerIT.java`
- Depends on: B6-01

---

### PHASE F1 — Frontend Setup [3 pts]

**F1-01** — Angular 21 project init `[1 pt] [low risk]`
- `ng new system-management-ui --routing --style=scss --standalone`
- Configure `angular.json`, `tsconfig.json`
- Install: `@angular/material`, `@ngrx/store` (if state management needed)
- Files: `package.json`, `angular.json`, `src/environments/`
- Depends on: nothing (parallel with backend)

**F1-02** — Core module: Auth service + Interceptor + Guards `[2 pt] [medium risk]`
- `AuthService`: login, logout, refresh, currentUser signal
- `AuthInterceptor`: catches 401 → calls refresh → retries original request once
- `AuthGuard`: redirects unauthenticated to `/login`
- `AdminGuard`: redirects non-ADMIN to home
- Files: `auth.service.ts`, `auth.interceptor.ts`, `auth.guard.ts`, `admin.guard.ts`
- Depends on: F1-01

---

### PHASE F2 — Auth Feature [2 pts]

**F2-01** — Login page `[2 pt] [low risk]`
- Email + password form with validation
- Error states: wrong credentials, account locked (show Retry-After countdown)
- Redirects to dashboard on success
- Files: `login/login.component.ts`, `login/login.component.html`
- Depends on: F1-02

---

### PHASE F3 — Feature Modules [8 pts]

**F3-01** — User management module `[2 pt] [low risk]`
- List with search + pagination
- Create/edit modal
- Deactivate confirmation dialog
- Files: `features/users/`
- Depends on: F1-02

**F3-02** — Group management module `[2 pt] [low risk]`
- List groups, create/edit/delete
- Member management panel
- Files: `features/groups/`
- Depends on: F1-02

**F3-03** — Category + Permission management `[2 pt] [medium risk]`
- Category list (filtered by permission automatically)
- Permission management panel (users + groups tabs)
- Files: `features/categories/`
- Depends on: F1-02

**F3-04** — Document management `[2 pt] [low risk]`
- Document list within category
- Create/edit/delete with permission-aware UI (hide actions user cannot perform)
- Files: `features/documents/`
- Depends on: F3-03

---

### PHASE D1 — DevOps [5 pts]

**D1-01** — Docker + Compose `[2 pt] [low risk]`
- `backend/Dockerfile`: multi-stage build (JDK builder → JRE runtime), non-root user
- `frontend/Dockerfile`: Node build → nginx serve
- `docker-compose.yml`: app + db (postgres:16) + frontend + healthchecks
- Files: `backend/Dockerfile`, `frontend/Dockerfile`, `docker-compose.yml`
- Depends on: B7-06

**D1-02** — GitHub Actions CI `[3 pt] [low risk]`
- `.github/workflows/ci.yml`:
  - Job: test (with Testcontainers PostgreSQL service)
  - Job: build (Docker image)
  - Job: security scan (OWASP dependency check)
- Files: `.github/workflows/ci.yml`
- Depends on: D1-01

---

## Parallel Execution Map

```
Sprint 1:
  B1-01 → B1-02 → B1-03
      ↓
  B2-01 → B3-01 → B4-01
      ↓
  B5-01 → B5-02          F1-01 (start in parallel)
      ↓         \
  B6-01 (PERM)  B6-02 (AUTH)

Sprint 2:
  B6-01 done → B6-03, B6-04, B6-05, B6-06 (all parallel)
      ↓
  B7-01 → B7-02, B7-03, B7-04, B7-05, B7-06 (parallel after services)

Sprint 3:
  F1-02 → F2-01 → F3-01, F3-02, F3-03 (parallel)
                       ↓
                   F3-04
  B7-06 done → D1-01 → D1-02
```

---

## Definition of Done (per task)

- [ ] Unit tests written first (TDD — red before green)
- [ ] All tests pass (`mvn test` / `ng test`)
- [ ] Code reviewed (no MUST FIX open)
- [ ] Security findings from security-review.md addressed (HIGH/MEDIUM)
- [ ] `features_tracking.md` updated
