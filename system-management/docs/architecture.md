# Architecture Design

**Agent:** `@architect`
**Project:** System Management
**Date:** 2026-04-25
**Input:** requirements.md, feature-spec.md

---

## Problem Statement

Design a system that serves a web SPA (Angular 21) backed by a single REST API (Spring Boot), with PostgreSQL for persistence. The system requires stateless JWT authentication via cookies, a three-layer permission model resolved on every request, and zero caching of permissions or roles.

---

## Architecture Decision: Monolith

**Chosen:** Single Spring Boot application serving all domains (auth, users, groups, categories, documents, permissions).

**Why not microservices:**
- Team size and scope do not justify the operational overhead
- Permission resolution requires joins across users, groups, and permissions — cross-service calls would add latency and consistency risk
- A well-structured monolith with clear package boundaries can be split later if needed

---

## System Overview

```
┌─────────────────────────────────────────────────┐
│                  Browser (Angular 21)            │
│  - HTTP-only cookies (access_token, refresh_token│
│  - No localStorage/sessionStorage for tokens     │
└──────────────────┬──────────────────────────────┘
                   │ HTTPS
┌──────────────────▼──────────────────────────────┐
│            Spring Boot API (Java 21)             │
│                                                  │
│  ┌─────────────┐  ┌──────────────────────────┐  │
│  │ JWT Filter  │  │   Controllers (REST)      │  │
│  │ (cookie →   │  │   /api/v1/...            │  │
│  │  principal) │  └──────────┬───────────────┘  │
│  └─────────────┘             │                  │
│                   ┌──────────▼───────────────┐  │
│                   │   Services (Business)     │  │
│                   │   - AuthService           │  │
│                   │   - PermissionService     │  │
│                   │   - UserService           │  │
│                   │   - GroupService          │  │
│                   │   - CategoryService       │  │
│                   │   - DocumentService       │  │
│                   └──────────┬───────────────┘  │
│                   ┌──────────▼───────────────┐  │
│                   │   Repositories (JPA)      │  │
│                   └──────────┬───────────────┘  │
└──────────────────────────────┼──────────────────┘
                               │
┌──────────────────────────────▼──────────────────┐
│              PostgreSQL + Flyway                 │
│  - 8 tables (see data-model.md)                 │
│  - Versioned migrations                         │
└─────────────────────────────────────────────────┘
```

---

## Authentication Flow

```
Client                     API                        DB
  │                          │                          │
  │── POST /auth/login ──────▶                          │
  │   {email, password}      │── findByEmail ──────────▶│
  │                          │◀── User (or null) ───────│
  │                          │                          │
  │                  BCrypt.check(password, hash)       │
  │                  (always runs — prevents timing)    │
  │                          │                          │
  │                  if locked → 423                    │
  │                  if inactive → 401                  │
  │                  if wrong → 401 + increment counter │
  │                          │                          │
  │                  generateAccessToken(userId)        │
  │                  generateRefreshToken → SHA-256     │
  │                          │── save RefreshToken ────▶│
  │                          │                          │
  │◀── 200 + Set-Cookie ─────│                          │
  │    access_token (15min)  │                          │
  │    refresh_token (7days) │                          │
```

---

## Token Refresh Flow (Atomic Rotation)

```
Client                     API                        DB
  │                          │                          │
  │── POST /auth/refresh ────▶                          │
  │   Cookie: refresh_token  │                          │
  │                          │── SELECT FOR UPDATE ────▶│
  │                          │   WHERE hash=? AND       │
  │                          │   revoked=false          │
  │                          │◀── token (or not found) ─│
  │                          │                          │
  │              if not found:                          │
  │                find by hash (revoked?)              │
  │                if found → family compromise         │
  │                  revokeAllByFamilyId               │
  │                throw 401                           │
  │                          │                          │
  │              if found:                              │
  │                mark old token revoked               │
  │                create new token (same familyId)     │
  │                          │── save both ────────────▶│
  │◀── 204 + new cookies ────│                          │
```

---

## Permission Resolution Algorithm

Called on every protected request. No caching.

```
resolvePermission(userId, categoryId):

  1. role = DB.findRoleByUserId(userId)          ← always from DB, never JWT
  2. if role == ADMIN → return EDIT              ← short-circuit

  3. baseline = role == EDITOR ? WRITE : READ    ← layer 1: global role

  4. groupIds = DB.findGroupsByUserId(userId)
  5. groupMax = DB.maxPermissionForGroups(categoryId, groupIds)
                                                 ← layer 2: group permissions

  6. direct = DB.findDirectPermission(categoryId, userId)
                                                 ← layer 3: direct override

  7. return max(baseline, groupMax, direct)      ← most permissive wins
```

**Why no caching:**
Permission changes must be effective on the very next request (PERM-10, PERM-11). Caching would introduce a window where revoked permissions still work.

---

## Package Structure

```
com.darkness.system.management
├── config/
│   ├── SecurityConfig.java
│   ├── CorsConfig.java
│   └── JwtConfig.java
├── controller/
│   ├── AuthController.java
│   ├── UserController.java
│   ├── GroupController.java
│   ├── CategoryController.java
│   ├── DocumentController.java
│   └── PermissionController.java
├── service/
│   ├── AuthService.java
│   ├── PermissionService.java
│   ├── UserService.java
│   ├── GroupService.java
│   ├── CategoryService.java
│   └── DocumentService.java
├── repository/
│   ├── UserRepository.java
│   ├── GroupRepository.java
│   ├── GroupMemberRepository.java
│   ├── CategoryRepository.java
│   ├── DocumentRepository.java
│   ├── CategoryUserPermissionRepository.java
│   ├── CategoryGroupPermissionRepository.java
│   └── RefreshTokenRepository.java
├── domain/
│   ├── User.java
│   ├── Group.java
│   ├── GroupMember.java
│   ├── Category.java
│   ├── Document.java
│   ├── CategoryUserPermission.java
│   ├── CategoryGroupPermission.java
│   ├── RefreshToken.java
│   └── enums/
│       ├── GlobalRole.java
│       └── Permission.java
├── dto/
│   ├── request/
│   └── response/
├── exception/
│   └── GlobalExceptionHandler.java
└── security/
    ├── JwtTokenProvider.java
    ├── JwtAuthenticationFilter.java
    └── UserPrincipal.java
```

---

## Frontend Structure (Angular 21)

```
src/
├── app/
│   ├── core/
│   │   ├── auth/
│   │   │   ├── auth.service.ts
│   │   │   ├── auth.guard.ts
│   │   │   └── admin.guard.ts
│   │   └── interceptors/
│   │       └── auth.interceptor.ts   ← handles 401 → auto-refresh
│   ├── features/
│   │   ├── login/
│   │   ├── users/
│   │   ├── groups/
│   │   ├── categories/
│   │   ├── documents/
│   │   └── permissions/
│   └── shared/
│       ├── components/
│       └── models/
```

---

## Non-Functional Requirements

| Concern | Decision |
|---------|----------|
| Auth token storage | HTTP-only cookies — no JS access |
| CSRF protection | SameSite=Strict on cookies — no separate CSRF token needed |
| CORS | Explicit allowed origins list — no wildcards |
| Stack traces in responses | Never — `server.error.include-stacktrace: never` |
| Actuator exposure | health + info only |
| DB schema management | Flyway versioned migrations |
| Password hashing | BCrypt cost 12 (~300ms intentional delay) |
| Dependency CVE scanning | OWASP Dependency Check (fail build on CVSS ≥ 7) |

---

## Open Questions

| # | Question | Default assumption |
|---|----------|--------------------|
| OQ-01 | Rate limiting on login endpoint? | Not in MVP — add if brute-force risk identified in security review |
| OQ-02 | Pagination strategy — cursor or offset? | Offset for MVP (admin UIs, predictable datasets) |
| OQ-03 | Angular SSR needed? | No — SPA only for MVP |
