# Agents Reference

All 30 agents at a glance — what each does, when to use it, and an example prompt.

---

## Discovery Agents

### `@product-manager`
Turns raw ideas into user stories, feature specs, and prioritized backlogs.

**Use when**: Starting a new feature, grooming the backlog, defining MVP scope.

```
Use @product-manager to write a feature spec for:
"Users should be able to invite teammates to their workspace."
Include acceptance criteria and success metrics.
```

---

### `@requirements-analyst`
Hardens specs with edge cases, Gherkin scenarios, and formal requirements.

**Use when**: The spec feels vague or incomplete, before handing to engineering.

```
Use @requirements-analyst to find all edge cases in this user story
and write Gherkin acceptance criteria: [paste story]
```

---

### `@estimator`
Decomposes features into tasks with story points, dependencies, and risks.

**Use when**: Sprint planning, project scoping, answering "how long will this take?"

```
Use @estimator to break this feature into engineering tasks with estimates:
[paste spec]
```

---

## Design Agents

### `@architect`
Evaluates design options with trade-offs and recommends a system architecture.

**Use when**: Building something new, making a significant infrastructure decision.

```
Use @architect to design the notification system for our platform.
We need email, push, and in-app. Current stack: Spring Boot + PostgreSQL.
```

---

### `@ux-designer`
Defines user flows, component states, interaction patterns, and UX copy.

**Use when**: Building any user-facing feature; before frontend implementation begins.

```
Use @ux-designer to define all states (loading, error, empty, success)
and UX copy for the checkout flow.
```

---

### `@api-designer`
Designs RESTful APIs using a contract-first approach, producing OpenAPI 3.x specs.

**Use when**: Creating new endpoints, reviewing API consistency, planning breaking changes.

```
Use @api-designer to write an OpenAPI spec for a user management API:
CRUD for users, plus endpoints for invite and deactivate.
```

---

### `@data-modeler`
Designs database schemas with ERDs, DDL, indexes, and zero-downtime migration plans.

**Use when**: Creating new tables, planning schema changes, reviewing migration safety.

```
Use @data-modeler to design the schema for a multi-tenant subscription system.
We use PostgreSQL.
```

---

## Build Agents

### `@planner`
Breaks a feature into ordered, independently committable implementation tasks.

**Use when**: Starting implementation, assigning work across the team.

```
Use @planner to create an implementation plan for this API spec: [paste spec]
```

---

### `@tdd-guide`
Coaches test-driven development — write failing tests first, then implementation.

**Use when**: Starting any new function or service; improving test coverage.

```
Use @tdd-guide to write failing unit tests for a UserService.create()
method that validates email uniqueness before saving.
```

---

### `@java-developer`
Senior Java/Spring Boot engineer — writes and reviews production-ready Java code.

**Use when**: Implementing features in Java, reviewing Spring Boot code, JVM issues.

```
Use @java-developer to implement a PasswordResetService
with token generation, expiry (15 min), and validation.
```

---

### `@typescript-reviewer`
Reviews TypeScript/JavaScript for type safety, async patterns, and React best practices.

**Use when**: Reviewing TS/JS PRs, auditing React components, fixing type errors.

```
Use @typescript-reviewer to review this React component: [paste code]
```

---

### `@go-reviewer`
Reviews Go code for idiomatic style, error handling, and concurrency safety.

**Use when**: Reviewing Go PRs, diagnosing goroutine leaks, idiomatic Go questions.

```
Use @go-reviewer to review this Go HTTP handler and repository: [paste code]
```

---

### `@python-reviewer`
Reviews Python for PEP 8, type hints, idiomatic patterns, and pytest best practices.

**Use when**: Reviewing Python PRs, auditing Django/FastAPI code, improving type coverage.

```
Use @python-reviewer to review this FastAPI endpoint: [paste code]
```

---

### `@code-reviewer`
Multi-dimension code review: correctness, security, performance, and maintainability.

**Use when**: Pre-merge review of any language, final quality gate before shipping.

```
Use @code-reviewer to review this pull request diff: [paste diff]
```

---

### `@security-reviewer`
OWASP-based security analysis — finds injection, auth, and data exposure vulnerabilities.

**Use when**: Security review of any code change, especially auth, payments, or user data.

```
Use @security-reviewer to check this authentication implementation
for security vulnerabilities: [paste code]
```

---

## Test Agents

### `@qa-engineer`
Writes test plans, prioritized test cases, and defect reports.

**Use when**: Planning testing for a feature, defining quality gates, reporting bugs.

```
Use @qa-engineer to write a full test plan for the user registration feature,
including edge cases and regression checklist.
```

---

### `@e2e-runner`
Writes Playwright end-to-end tests for critical user journeys.

**Use when**: Automating E2E coverage for key user flows.

```
Use @e2e-runner to write Playwright tests for the checkout flow:
add to cart → enter payment → confirm order.
```

---

### `@performance-tester`
Designs load tests, analyzes bottlenecks, and recommends performance fixes.

**Use when**: Testing scalability of new endpoints, diagnosing production slowness.

```
Use @performance-tester to write a k6 load test for our search API.
Target: p95 < 500ms at 200 concurrent users.
```

---

## Ship Agents

### `@devops-engineer`
Builds CI/CD pipelines, Dockerfiles, and deployment configurations.

**Use when**: Setting up a new service, improving CI reliability, containerizing an app.

```
Use @devops-engineer to write a GitHub Actions CI pipeline and
production Dockerfile for our Spring Boot service.
```

---

### `@release-manager`
Manages versioning, changelogs, release checklists, and rollback plans.

**Use when**: Preparing any release, coordinating deployment, writing release notes.

```
Use @release-manager to prepare release v3.1.0 with these changes: [paste list]
Include a go/no-go checklist and rollback plan.
```

---

## Operate Agents

### `@observability-engineer`
Sets up structured logging, metrics, distributed tracing, and Prometheus alerts.

**Use when**: Instrumenting a new service, setting up alerts, building dashboards.

```
Use @observability-engineer to define the logging strategy, golden signal metrics,
and alert rules for our payment service.
```

---

### `@incident-responder`
Triages production incidents, coordinates response, and drafts stakeholder updates.

**Use when**: A production alert fires, something is broken in production.

```
Use @incident-responder to help triage this alert:
"Payment service error rate at 8%, started 14:22 UTC, after deploy v3.1.0"
```

---

## Iterate Agents

### `@retrospective-facilitator`
Runs sprint retros with structured formats, themes, and owned action items.

**Use when**: End of sprint or project, team process issues, post-incident reflection.

```
Use @retrospective-facilitator to prepare a sprint retrospective for Sprint 15.
The team shipped late and had 2 incidents. Recommend the best format.
```

---

### `@tech-debt-tracker`
Audits codebases for debt, scores it by impact, and creates a pay-down plan.

**Use when**: Quarterly planning, before a major refactor, when velocity is slowing.

```
Use @tech-debt-tracker to audit the user module and produce a
prioritized debt register with effort estimates.
```

---

### `@refactor-cleaner`
Removes dead code, simplifies complexity, and improves structure without changing behavior.

**Use when**: Cleaning up before adding features, reducing cognitive load in a module.

```
Use @refactor-cleaner to simplify the OrderService — it's 800 lines
and has several duplicated validation methods.
```

---

## Support Agents (use anytime)

### `@build-error-resolver`
Diagnoses and fixes build failures — compilation, dependency, CI, and test errors.

```
Use @build-error-resolver to fix this CI failure: [paste error log]
```

---

### `@docs-updater`
Keeps README, API docs, and changelogs in sync with code changes.

```
Use @docs-updater to update the README and API docs after these changes: [paste diff]
```

---

### `@database-reviewer`
Reviews SQL, migrations, ORM usage, and schema changes for safety and performance.

```
Use @database-reviewer to review this migration before we run it in production:
[paste migration file]
```

---

### `@chief-of-staff`
Drafts PR descriptions, postmortems, status updates, and stakeholder communications.

```
Use @chief-of-staff to write a postmortem for today's incident:
[paste incident timeline and root cause]
```

---

### `@loop-operator`
Runs multi-step tasks autonomously, self-correcting until done or blocked.

```
Use @loop-operator to: run the test suite, fix any failing tests, then
verify all tests pass. Stop and report if a test fails 3 times.
```

---

## Quick Lookup by Situation

| Situation | Agent |
|-----------|-------|
| "What should we build?" | `@product-manager` |
| "Did we cover all the cases?" | `@requirements-analyst` |
| "How long will this take?" | `@estimator` |
| "How should we build this?" | `@architect` |
| "What does the user experience look like?" | `@ux-designer` |
| "What are the API endpoints?" | `@api-designer` |
| "What's the database schema?" | `@data-modeler` |
| "What order do I build this in?" | `@planner` |
| "Help me write tests first" | `@tdd-guide` |
| "Review my code" | `@code-reviewer` |
| "Is this code secure?" | `@security-reviewer` |
| "Write QA test cases" | `@qa-engineer` |
| "Automate browser testing" | `@e2e-runner` |
| "Will it handle the load?" | `@performance-tester` |
| "Set up CI/CD" | `@devops-engineer` |
| "Prepare the release" | `@release-manager` |
| "Set up monitoring and alerts" | `@observability-engineer` |
| "Production is broken!" | `@incident-responder` |
| "Run the sprint retro" | `@retrospective-facilitator` |
| "What's our technical debt?" | `@tech-debt-tracker` |
| "Clean up this messy code" | `@refactor-cleaner` |
| "CI is broken" | `@build-error-resolver` |
| "Update the docs" | `@docs-updater` |
| "Is this migration safe?" | `@database-reviewer` |
| "Write this PR description" | `@chief-of-staff` |
| "Do this automatically" | `@loop-operator` |
