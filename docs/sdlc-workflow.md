# SDLC Workflow Guide

How to use the agent framework across the full Software Development Life Cycle — from raw idea to production and back again.

---

## The Full Flow

```
┌─────────────────────────────────────────────────────────────────┐
│                        SDLC PIPELINE                            │
│                                                                 │
│  1. DISCOVER  →  2. DESIGN  →  3. BUILD  →  4. TEST            │
│                                                                 │
│  5. SHIP  →  6. OPERATE  →  7. ITERATE  ──────────────────┐    │
│                                          └─→ back to 1    │    │
└─────────────────────────────────────────────────────────────────┘
```

---

## Phase 1 — Discovery

**Goal**: Turn a vague idea into a clear, prioritized, testable specification.

### Step 1.1 — Define the product requirement

**Agent**: `@product-manager`

```
Use @product-manager to write a feature spec for:
"Users should be able to reset their password via email"
```

**Output**: User story with acceptance criteria, out-of-scope list, success metric.

---

### Step 1.2 — Harden the specification

**Agent**: `@requirements-analyst`

```
Use @requirements-analyst to find all edge cases and write
Gherkin acceptance criteria for this user story: [paste output from product-manager]
```

**Output**: Formal requirements, Gherkin scenarios, open questions to resolve.

---

### Step 1.3 — Estimate the work

**Agent**: `@estimator`

```
Use @estimator to break this feature into engineering tasks with estimates:
[paste the hardened spec]
```

**Output**: Task list with story points, dependencies, critical path, and risks.

---

## Phase 2 — Design

**Goal**: Decide how to build it before writing any code.

### Step 2.1 — System architecture

**Agent**: `@architect`

```
Use @architect to design the system for [feature/service].
We use Java/Spring Boot + PostgreSQL + React/Next.js.
```

**Output**: Architecture options with trade-offs, a recommendation, and Mermaid diagram.

---

### Step 2.2 — User experience

**Agent**: `@ux-designer`

```
Use @ux-designer to define the user flow and all component states
for the password reset feature.
```

**Output**: Step-by-step user journey, all UI states (loading, error, success, empty), UX copy.

---

### Step 2.3 — API contract

**Agent**: `@api-designer`

```
Use @api-designer to write an OpenAPI 3.x spec for the password reset endpoints.
```

**Output**: Complete OpenAPI YAML — endpoints, request/response schemas, error codes.

---

### Step 2.4 — Database schema

**Agent**: `@data-modeler`

```
Use @data-modeler to design the schema for password reset tokens.
We use PostgreSQL.
```

**Output**: ERD, `CREATE TABLE` DDL, index plan, migration files.

---

## Phase 3 — Build

**Goal**: Implement the feature with high quality and test coverage.

### Step 3.1 — Plan implementation tasks

**Agent**: `@planner`

```
Use @planner to create an ordered implementation task list for:
[paste API spec + schema from Phase 2]
```

**Output**: Numbered task list with affected files and complexity per task.

---

### Step 3.2 — Write tests first (TDD)

**Agent**: `@tdd-guide`

```
Use @tdd-guide to write the failing unit tests for the
password reset service before I implement it.
```

**Output**: Test cases, failing test code, then minimal implementation.

---

### Step 3.3 — Implement (language-specific)

Use the agent matching your language:

| Language | Agent |
|----------|-------|
| Java / Spring Boot | `@java-developer` |
| TypeScript / Node.js | `@typescript-reviewer` |
| Go | `@go-reviewer` |
| Python | `@python-reviewer` |

```
Use @java-developer to implement the PasswordResetService
based on this spec: [paste spec]
```

---

### Step 3.4 — Review before merging

Run both reviewers in sequence:

```
Use @code-reviewer to review this implementation: [paste diff]
```

```
Use @security-reviewer to check this for security vulnerabilities: [paste diff]
```

**Output**: Categorized feedback (`[MUST FIX]`, `[SHOULD FIX]`, `[SUGGESTION]`) + verdict.

---

## Phase 4 — Test

**Goal**: Validate the feature works correctly before it ships.

### Step 4.1 — Write the test plan

**Agent**: `@qa-engineer`

```
Use @qa-engineer to write a test plan and test cases for the
password reset feature, including edge cases.
```

**Output**: Prioritized test cases (P0–P3), test environment requirements, entry/exit criteria.

---

### Step 4.2 — Automate E2E tests

**Agent**: `@e2e-runner`

```
Use @e2e-runner to write Playwright E2E tests for the
password reset user flow.
```

**Output**: Complete Playwright test suite, required `data-testid` attributes.

---

### Step 4.3 — Performance testing (for critical paths)

**Agent**: `@performance-tester`

```
Use @performance-tester to write a k6 load test for the
/auth/reset-password endpoint. Target: p95 < 300ms at 100 req/s.
```

**Output**: k6 script, SLO definition, analysis approach.

---

## Phase 5 — Ship

**Goal**: Release safely with a clear rollback plan.

### Step 5.1 — Set up CI/CD

**Agent**: `@devops-engineer`

```
Use @devops-engineer to write a GitHub Actions CI pipeline
and production Dockerfile for our Java/Spring Boot service.
```

**Output**: `.github/workflows/ci.yml`, `Dockerfile`, `docker-compose.yml`.

---

### Step 5.2 — Manage the release

**Agent**: `@release-manager`

```
Use @release-manager to prepare the release for v2.5.0.
Changes include: [paste changelog items]
```

**Output**: Changelog entry, release checklist, go/no-go criteria, rollback plan, stakeholder notes.

---

## Phase 6 — Operate

**Goal**: Know what's happening in production and respond fast when something breaks.

### Step 6.1 — Instrument the feature

**Agent**: `@observability-engineer`

```
Use @observability-engineer to define the logging, metrics,
and alerts for the password reset feature.
```

**Output**: Structured log schema, Prometheus metrics, alert rules, dashboard spec, runbook.

---

### Step 6.2 — Respond to incidents

**Agent**: `@incident-responder`

```
Use @incident-responder to help triage this production alert:
"Error rate on /auth/reset-password spiked to 15% at 14:32 UTC"
```

**Output**: Severity classification, triage steps, stakeholder update drafts, runbook.

---

## Phase 7 — Iterate

**Goal**: Improve the product and the process continuously.

### Step 7.1 — Track and pay down tech debt

**Agent**: `@tech-debt-tracker`

```
Use @tech-debt-tracker to audit the auth module for technical debt
and produce a prioritized pay-down plan.
```

**Output**: Debt register with scores, top 5 priorities, effort estimates.

---

### Step 7.2 — Clean up code safely

**Agent**: `@refactor-cleaner`

```
Use @refactor-cleaner to clean up the PasswordResetService —
remove dead code and simplify the token validation logic.
```

**Output**: Before/after diffs, risk level, test coverage note.

---

### Step 7.3 — Reflect as a team

**Agent**: `@retrospective-facilitator`

```
Use @retrospective-facilitator to run a sprint retrospective for
Sprint 22. The team had 3 production incidents and missed velocity by 20%.
```

**Output**: Format recommendation, facilitation agenda, synthesized themes, action items.

---

## Support Agents (use anytime)

These agents are useful across all phases:

| Agent | When to use |
|-------|------------|
| `@build-error-resolver` | CI is broken, compilation fails, tests won't run |
| `@docs-updater` | After any significant code change |
| `@database-reviewer` | Before running any migration in production |
| `@chief-of-staff` | Drafting PR descriptions, postmortems, status updates |
| `@loop-operator` | Running autonomous multi-step tasks end-to-end |

---

## Chaining Agents — Example: Full Feature from Scratch

```
1. @product-manager     → "Add password reset via email"
                           ↓ feature spec
2. @requirements-analyst → hardened spec + Gherkin scenarios
                           ↓ formal requirements
3. @estimator           → task breakdown + estimates
                           ↓ sprint plan
4. @architect           → system design
4. @api-designer        → OpenAPI spec              [parallel]
4. @data-modeler        → schema + migrations
                           ↓ design artifacts
5. @planner             → ordered implementation tasks
                           ↓ task list
6. @tdd-guide           → failing tests
6. @java-developer      → implementation            [parallel]
                           ↓ code + tests
7. @code-reviewer       → code quality review
7. @security-reviewer   → security review           [parallel]
                           ↓ review sign-off
8. @qa-engineer         → test plan + manual cases
8. @e2e-runner          → automated Playwright tests [parallel]
                           ↓ QA sign-off
9. @devops-engineer     → CI/CD pipeline + Dockerfile
9. @release-manager     → release checklist + changelog [parallel]
                           ↓ release package
10. @observability-engineer → alerts + dashboards + runbooks
                           ↓ production-ready
```
