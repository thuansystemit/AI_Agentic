# Features Tracking

## Project: System Management
**Stack:** Java 21 · Spring Boot · Angular 21 · PostgreSQL · JWT (HTTP-only cookies)
**Package:** com.darkness.system.management
**Started:** 2026-04-25

---

## SDLC Pipeline Status

| Phase | Agent | Document | Status |
|-------|-------|----------|--------|
| 1 — Requirements | `@requirements-analyst` | `requirements.md` | ✅ Done |
| 2 — Feature Spec | `@product-manager` | `feature-spec.md` | ✅ Done |
| 3 — Architecture | `@architect` | `architecture.md` | ✅ Done |
| 3 — API Design | `@api-designer` | `api-spec.md` | ✅ Done |
| 3 — Data Model | `@data-modeler` | `data-model.md` | ✅ Done |
| 3 — UX Design | `@ux-designer` | `ux-design.md` | ⬜ Pending |
| 4 — Security Review | `@security-reviewer` | `security-review.md` | ✅ Done |
| 5 — Sprint Plan | `@estimator` + `@planner` | `implementation-plan.md` | ✅ Done |
| 6 — TDD | `@tdd-guide` | (test files) | ✅ Done |
| 6 — Backend Build | `@java-developer` | (source files) | ✅ Done |
| 6 — Frontend Build | `@ux-designer` | (source files) | ✅ Done |
| 7 — QA | `@qa-engineer` | (unit tests) | ✅ Done |
| 7 — API Testing | `@api-tester` | (curl smoke tests) | ⬜ Run after each feature |
| 8 — DevOps | `@devops-engineer` | `Dockerfile`, `docker-compose.yml`, `ci.yml` | ✅ Done |
| 8 — Release | `@release-manager` | (CI/CD pipeline) | ✅ Done |

---

## Phase Gate Log

| Date | Phase Completed | Key Decisions | Confirmed By |
|------|----------------|---------------|--------------|
| 2026-04-25 | Phase 1 — Requirements | ADMIN-only categories, cascade delete on group, hidden categories with no permission | User |
| 2026-05-03 | Phase 1 — Implementation | Permission service rewritten (3-layer, batch resolve), VIEWER hidden-category enforcement, effectivePermission in CategoryResponse, permissions dialog, profile page, 123 tests passing | User |
| 2026-04-25 | Phase 2 — Design | Monolith, offset pagination, no Angular SSR | User |
| 2026-04-25 | Phase 3 — Security Review | 0 CRITICAL, 2 HIGH, 5 MEDIUM — all addressed in implementation plan | User |
| 2026-04-25 | Phase 5 — Backend Build | 114 tests, 0 failures, JaCoCo 99.4% instruction / 96.3% branch | Auto |
| 2026-04-25 | Phase 6 — Frontend Build | Angular 21 standalone, all feature modules, clean build | Auto |
| 2026-04-25 | Phase 8 — DevOps | Multi-stage Dockerfiles, docker-compose with healthchecks, GitHub Actions CI | Auto |

---

## Key Decisions Log

| Decision | Chosen | Rationale | Phase |
|----------|--------|-----------|-------|
| Category creator | ADMIN only | Prevents unauthorized category sprawl | Requirements |
| Group deletion | Cascade revoke immediately | No orphan permissions | Requirements |
| No-permission categories | Hidden from listing | Least-privilege, no info leakage | Requirements |
| JWT storage | HTTP-only, Secure, SameSite=Strict cookies | Prevents XSS token theft | Requirements |
| Role in JWT | Never — resolved from DB | Prevents stale-role privilege | Requirements |
| Permission resolution | Most permissive wins across 3 layers | Explicit design goal | Requirements |

---

## Open Questions

| # | Question | Owner | Status |
|---|----------|-------|--------|
| 1 | Who can create categories — ADMIN only or any user? | User | ✅ Resolved — ADMIN only |
| 2 | What happens to group permissions when a group is deleted? | User | ✅ Resolved — cascade delete via FK ON DELETE CASCADE |
| 3 | If a user has no permission on a category, is it hidden or visible? | User | ✅ Resolved — hidden (VIEWER gets no access without explicit grant) |
