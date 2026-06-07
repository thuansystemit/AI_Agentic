# AI Agentic Framework

A full **Software Development Life Cycle (SDLC)** agent framework for Claude Code — 38 specialized agents covering every phase from raw idea to production, wired together through a **document-first, handoff-driven pipeline** with a **full AI memory architecture** so every agent knows exactly what was decided before it, what already exists in the codebase, and what to produce next.

---

## Core Design Principles

| Principle | What it means |
|-----------|--------------|
| **Document-first** | Every agent thinks, then writes a numbered artifact to `{PIPELINE_DOCS}/` before declaring done. No artifact = not done. |
| **Dual-document** | Each step produces a **pair**: a human-readable `NN-name.md` and a compact agent-to-agent `NN-name.ctx.md`. Agents read the cheap `.ctx.md` by default and pull the full doc only for detail — ~85% fewer tokens across the chain. See [agent-handoff-protocol.md](agent-handoff-protocol.md). |
| **Handoff-driven** | Each agent ends with an explicit handoff block naming the next agent, the documents written (`.md` + `.ctx.md`), and the path to pass forward. |
| **Path-agnostic** | No hardcoded folder paths. `{PIPELINE_DOCS}` is resolved at pipeline start by `ba-agent` to fit any project structure. |
| **Graph-based codebase understanding** | The context manager builds a typed entity graph (`codebase-graph.yaml`) of the entire codebase. Understanding code = path-finding on the graph, not reading files. Works at any scale. |
| **AI memory architecture** | Seven memory types implemented: semantic, short-term, working, episodic, long-term, knowledge graph, multi-agent shared. Each solves a specific failure mode in multi-agent work. |
| **Token-optimised** | Six efficiency strategies keep context slices to ~600 tokens/agent: contract layer, graph subgraph filtering, graduated compression, push/pull injection, delta state, deduplication. |
| **Session-resilient** | Work survives session expiry via `HANDOFF.md`. Any session resumes from the exact checkpoint without re-running completed phases. |
| **Stack-agnostic** | Codebase graph is built via grep/git log across Java, Go, Python, TypeScript, Angular, React, Vue — not hardcoded to any one stack. |

---

## How It Works

```
User describes a feature
        │
        ▼
   @ba-agent                    ← resolves {PIPELINE_DOCS}, initializes pipeline + memory
        │
        ▼  Phase 1 — Discovery
   @product-manager             → writes 01-product-spec.md
        │
        ▼
   @requirements-analyst        → writes 02-requirements.md
        │
        ▼  Phase 2 — Design
   @architect                   → writes 03-architecture.md
        │
   ┌────┴──────────────────────┐
   ▼           ▼               ▼    ← parallel
@api-designer  @data-modeler  @ux-designer
→ 04-api-spec  → 05-data-model → 06-ux-flows
        │
        ▼  Phase 3 — Planning
   @estimator                   → writes 07-estimates.md
        │
        ▼
   @planner                     → writes 08-sprint-plan.md
        │
   ┌────┴──────────────────────┐    ← parallel
   ▼                           ▼
@java-developer   @angular-frontend-engineer
        └──────────────┬────────┘
                       ▼           → both append to 09-implementation-log.md
        │
        ▼  Phase 5 — QA
   @qa-engineer                 → writes 10-test-plan.md
        │
        ▼  Phase 6 — Release
   @release-manager             → writes 11-release-notes.md
        │
        ▼  Phase 7 — Post-Release
   @observability-engineer + @docs-updater + @retrospective-facilitator
```

**The `context-manager`** runs across every arrow — querying the codebase graph before each agent, injecting compressed memory slices, verifying documents were written, updating the knowledge graph, logging to the timeline, and keeping `HANDOFF.md` current so sessions resume instantly.

---

## Understanding an Existing Codebase

Before any pipeline work begins on an existing project, the `context-manager` builds a **codebase graph** — a typed entity map of every class, method, table, endpoint, topic, and test in the project, plus all relationships between them. Once built, agents navigate code by **path-finding on the graph** rather than reading files.

### Step 1 — Build the codebase graph

Point the context manager at any existing project:

```
Use @context-manager to build the codebase graph for:
Project: /path/to/existing-project
```

It will run a 3-pass grep scan (no external tooling required):

```
Pass 1 — NODES   grep for class declarations, route annotations, @Table, migration files
Pass 2 — EDGES   grep for imports, method calls, ORM mappings, Kafka producers/consumers
Pass 3 — ENRICH  add line ranges, stereotypes, hot-zone flags from git log (last 30 days)
```

Output: `{CONTEXT_STORE}/codebase/codebase-graph.yaml`  
Build time: ~3–15 minutes for a 500k-line codebase. Run once; update incrementally after each session.

Works for: **Java/Spring, Go, Python/FastAPI/Django, TypeScript/NestJS/Angular/React/Vue**, and polyglot monorepos.

### Step 2 — Ask questions about the codebase

Once the graph is built, agents can answer architectural questions instantly via path-finding — no file reading:

| Question | Query type | What the graph returns |
|---------|-----------|----------------------|
| "How does user authentication work?" | Forward path | `endpoint:POST-/auth/login → controller → service → repository → table:users` |
| "What would break if I rename the orders table?" | Reverse path | Blast radius: N entities, N methods, N controllers, N tests |
| "What does OrderService touch?" | Neighbourhood | All connected nodes to depth 2: repositories, mappers, events, callers, tests |
| "How does the mobile client reach the payments database?" | Shortest path | 5-hop path across service boundaries |
| "What services does auth-service depend on?" | Neighbourhood | Direct: common-security, common-events. Transitive: Kafka cluster, PostgreSQL |
| "Which tests cover REQ-005?" | Graph lookup | test:TC-007, test:TC-012 — via `covers` edges in knowledge-graph.yaml |

### Step 3 — Use agents with full codebase awareness

Any agent invoked after the graph is built automatically receives a **graph query result** (60–150 tokens) instead of a bulk file scan:

```
Use @architect to design a new export feature.
Project: /path/to/existing-project
PIPELINE_DOCS: /path/to/existing-project/docs/sdlc
```

The context manager runs the `architect` graph query (neighbourhood of all modules, cross-service edges) and injects the result. The architect sees a compact, accurate picture of the existing system — not a wall of file content.

### Incremental graph updates

The graph stays current after each implementation session:

```
After @java-developer writes new classes   → context-manager adds class + method nodes
After @data-modeler writes migrations      → context-manager adds table nodes + migration edges
After @api-designer defines endpoints      → context-manager updates graph-cache/endpoints.tsv
```

Trigger a manual refresh at any time:

```
Use @context-manager to refresh the codebase graph.
Project: /path/to/project
Scope: order-service/   ← rescan only this area (faster than full rebuild)
```

### Large codebase strategy

| Codebase size | Approach |
|--------------|---------|
| < 50k lines | Full graph build — all nodes and edges |
| 50k–500k lines | Full graph build — takes 5–15 min; run once, update incrementally |
| 500k–2M lines | Scoped build — build graph per service/module; link at module boundary edges |
| > 2M lines | Scoped build per team boundary + manual `code-map.md` for cross-team surface |

For very large codebases, the graph is still one file per scope. The context manager stitches module graphs together for cross-service queries.

---

## Quick Start

### 1. Start a new feature (full pipeline)

```
Use @ba-agent to implement:
"Users should be able to export their order history as a CSV file."
Project is at /path/to/project
```

`ba-agent` will:
1. Detect the project root and resolve `{PIPELINE_DOCS}` (e.g. `/path/to/project/docs/sdlc`)
2. Announce: `📁 Pipeline docs: /path/to/project/docs/sdlc [NEW]`
3. Signal `context-manager` to build the codebase graph if not already built
4. Drive the full pipeline from Phase 1 through release, gating at each phase

---

### 2. Start with explicit context management

```
Use @context-manager to initialize context for:
Feature: "CSV export for order history"
Project: /path/to/project
```

Then invoke agents manually, passing `PIPELINE_DOCS` each time:

```
Use @product-manager to define the feature.
PIPELINE_DOCS: /path/to/project/docs/sdlc
```

Each agent writes its numbered artifact, ends with a handoff block, and tells you exactly what to invoke next.

---

### 3. Resume after a session expires

```
Use @context-manager to restore the session.
Project: /path/to/project
```

It will:
1. Find `PIPELINE_STATE.md` → extract `{PIPELINE_DOCS}` and `{CONTEXT_STORE}`
2. Read the episodic timeline — last 3 events tell you exactly where things stopped
3. Verify every claimed document exists on disk
4. Report the knowledge graph state (node/edge count)
5. Tell you exactly which agent to invoke next

---

### 4. Use a single agent directly (no pipeline)

```
Use @code-reviewer to review this pull request: [paste diff]
```

```
Use @security-reviewer to audit the auth flow in this service.
```

Single-agent use works without a pipeline — these agents don't require `{PIPELINE_DOCS}`.

---

## The Document Chain

Every pipeline run produces the same numbered artifacts regardless of project. The **folder path** varies (`{PIPELINE_DOCS}`); the **filenames** are the stable inter-agent contract. Each step writes a **pair** — `NN-name.md` for humans and `NN-name.ctx.md` (compact YAML) as the agent-to-agent read path. Full schema: [agent-handoff-protocol.md](agent-handoff-protocol.md).

```
{PIPELINE_DOCS}/
├── PIPELINE_STATE.md              ← live tracker; first line: PIPELINE_DOCS: [abs path]
│
├── 01-product-spec.md   + .ctx.md ← @product-manager
│     Stories, RICE scores, MVP scope, success metrics
│
├── 02-requirements.md   + .ctx.md ← @requirements-analyst
│     Formal requirements (REQ-IDs), Gherkin scenarios, traceability matrix
│
├── 03-architecture.md   + .ctx.md ← @architect
│     Style decision, component registry, data ownership, NFR coverage
│     .ctx.md carries the hard constraints (auth, PK type, package) downstream
│
├── 04-api-spec.md       + .ctx.md ← @api-designer (summary + handoff)
├── 04-api-spec.yaml               ← @api-designer (full OpenAPI 3.x — pull for field detail)
│
├── 05-data-model.md     + .ctx.md ← @data-modeler
│     ERD, DDL, migration plan, next migration version
│
├── 06-ux-flows.md       + .ctx.md ← @ux-designer
│     User journeys, screen specs, all component states, UX copy
│
├── 07-estimates.md      + .ctx.md ← @estimator
│     Task breakdown, story points, critical path, risk register
│
├── 08-sprint-plan.md    + .ctx.md ← @planner
│     Sprint backlog, task assignments, Definition of Done
│
├── 09-implementation-log.md + .ctx.md ← @java-developer + @angular-frontend-engineer
│     Both sectioned: .md uses <!-- SECTION:agent -->; .ctx.md uses backend:/frontend: keys
│
├── 10-test-plan.md      + .ctx.md ← @qa-engineer
│     Test cases (P0–P3), quality gates, defect log
│
└── 11-release-notes.md  + .ctx.md ← @release-manager
      Version, Go/No-Go, changelog, rollback plan, stakeholder notes
```

---

## The `{PIPELINE_DOCS}` Variable

This framework never hardcodes a folder path:

| Who | What |
|-----|------|
| **`ba-agent`** | Resolves `{PIPELINE_DOCS}` at pipeline start: `git rev-parse --show-toplevel` → checks for `docs/`, falls back to `ai-docs/sdlc`, or asks the user |
| **Every pipeline agent** | Receives `{PIPELINE_DOCS}` in its context or from the prior handoff block — never re-discovers it |
| **`context-manager`** | Derives `{CONTEXT_STORE} = {PIPELINE_DOCS}/context` automatically |
| **Direct invocation** | Agent reads first line of `PIPELINE_STATE.md` (`PIPELINE_DOCS: [path]`) to discover the path |

---

## Context Manager — AI Memory Architecture

The `context-manager` implements **seven AI memory types**, each solving a specific failure mode in multi-agent, multi-session software delivery.

### Memory type overview

| # | Type | File | Scope | What it solves |
|---|------|------|-------|----------------|
| 1 | **Semantic** | `codebase-graph.yaml` + `graph-cache/` | Project | Agents know what exists — no duplication, no version conflict |
| 2 | **Short-term** | Context slice (injected) | Per invocation | Right knowledge, right agent, within token budget |
| 3 | **Working** | `WORKING_MEMORY.md` | Per invocation | Agent knows exactly what it is focused on right now |
| 4 | **Episodic** | `timeline.md` | Full run | Reconstruct the causal chain: who did what, when, why |
| 5 | **Long-term** | `decisions.md` + `HANDOFF.md` | Full run | Cross-session persistence — decisions and resume state |
| 6 | **Knowledge graph** | `knowledge-graph.yaml` | Full run | Typed relationships across pipeline entities — impact analysis, traceability |
| 7 | **Multi-agent shared** | `09-implementation-log.md` (sectioned) | Phase 4 | Parallel writes without conflict |

---

### Memory 1 — Semantic: Graph-based Codebase Understanding

**Core idea:** Understanding code = **path-finding on a graph**, not reading files.  
When an agent asks "how does authentication work?" — the answer is a path through the graph. No file scanning needed.

#### The codebase graph (`codebase-graph.yaml`)

A single typed entity graph covering the entire codebase, built once via grep and cached:

```
Node types:
  module, package, class, method, field
  table, column, migration
  endpoint, topic, test_class, test_method, config

Edge types (directed):
  Structural:   contains, extends, implements
  Dependencies: imports, calls, reads, writes
  Persistence:  maps_to, queries, creates, alters
  API surface:  exposes, request_type, response_type, auth_by
  Messaging:    publishes, consumes, schema
  Testing:      tests, covers, exercises
  Cross-service: calls_api_of, publishes_to, consumes_from
```

Built in 3 passes (no external tooling — grep + git log):

```
Pass 1 — NODES: grep for class declarations, route annotations, @Table, migration files
Pass 2 — EDGES: grep for imports, method calls, ORM annotations, kafka producers/consumers
Pass 3 — ENRICH: add line ranges, stereotypes, transactional flags, hot-zone flags from git log
```

#### 4 path-finding query types

| Query | Algorithm | Example question answered |
|-------|-----------|--------------------------|
| **Forward path** | BFS following edges forward | "How does creating an order work end-to-end?" |
| **Reverse path** | BFS following inbound edges | "What would break if I change table:orders?" |
| **Neighbourhood** | BFS both directions, depth 2 | "What is OrderService connected to?" |
| **Shortest path** | Dijkstra (unweighted) | "How does ExportListComponent reach the database?" |

Example — forward path result injected to agent (~120 tokens):
```
endpoint:POST-api-v1-orders
  [handled_by] → method:OrderController.create      (OrderController.java:34)
  [calls]      → method:OrderService.createOrder    (OrderService.java:45)
  [calls]      → method:OrderRepository.save        → table:orders
  [calls]      → method:OrderEventPublisher.publish → topic:orders.order.created
                                                       [consumes] InventoryConsumer
                                                       [consumes] NotificationConsumer
```

---

### Memory 2–3 — Short-term + Working Memory: Token-Optimised Injection

Six strategies reduce context slices from ~2 000 tokens/agent to ~600 tokens/agent (**~70% reduction**):

| # | Strategy | Mechanism | Saving |
|---|----------|-----------|--------|
| S1 | **Contract layer** | 5 tiny typed schemas extracted from large docs (story, req, api, data, constraint) | 60–70% of slice |
| S2 | **Subgraph filtering** | Pipeline knowledge graph pruned to nodes the current agent touches | 70–80% of graph size |
| S3 | **Graduated compression** | hot=full, 1-phase-back=3:1, 2-phases-back=8:1, older=drop | 40–50% for older content |
| S4 | **Push / pull injection** | Always push ≤ 200 tokens; everything else listed as available-to-pull | 50–70% overall |
| S5 | **Delta state** | Pass events since last phase-gate baseline, not full PIPELINE_STATE | 80–90% of state overhead |
| S6 | **Deduplication** | One canonical location per fact; all others reference, never repeat | 20–30% scattered |

**Working memory** (`WORKING_MEMORY.md`) is overwritten before each agent and cleared after — isolating active task, loaded docs, attention focus, and graph nodes in scope.

---

### Memory 4 — Episodic: Timeline

`timeline.md` is an **append-only timestamped event log**. Every significant event is recorded: `PIPELINE_INIT`, `AGENT_START`, `AGENT_COMPLETE`, `DECISION`, `CONTRACT_GENERATED`, `PHASE_GATE`, `SESSION_END`, `SESSION_RESUME`.

On session restore: read the last 3 timeline entries to know instantly where things stopped — without re-reading all numbered docs.

---

### Memory 5 — Long-term: Decisions + Handoff

- **`decisions.md`** — append-only log of every architectural decision (`D-001`, `D-002` …) with rationale, rejected alternatives, reversibility, and downstream impact. Every hard constraint is also pushed to `constraint-contract.md`.
- **`HANDOFF.md`** — written at every phase gate and at 70% context capacity. Contains the delta baseline timestamp, pipeline doc status, working memory at expiry, and exact next action. Max 1 200 tokens.

---

### Memory 6 — Knowledge Graph: Pipeline Traceability

`knowledge-graph.yaml` tracks typed relationships between **pipeline entities** (separate from the codebase graph which tracks code entities):

```
feature → [implements] → story → [requires] → requirement → [verified_by] → test
endpoint → [implements] → requirement
table → [used_by] → endpoint
class → [handles] → endpoint
component → [calls] → endpoint
decision → [affects] → class | table | component
```

Enables impact analysis queries: "What tests cover requirement REQ-005?" or "Which endpoints are affected by decision D-003?"

---

### Memory 7 — Multi-agent Shared: Sectioned Implementation Log

`09-implementation-log.md` is written concurrently by `@java-developer` and `@angular-frontend-engineer`. Each owns a named section using `<!-- SECTION:agent-name -->` markers. The context manager merges, detects conflicts (same file path in both sections), and updates the codebase graph after Phase 4 completes.

---

## Context Store Layout

```
{CONTEXT_STORE}/                        ← always {PIPELINE_DOCS}/context/
│
│  ── Active (per invocation) ──────────────────────────────────────────────
├── WORKING_MEMORY.md                   [Working]    overwritten + cleared each agent
│
│  ── Contracts (S1) ─────────────────────────────────────────────────────────
│   The per-doc contracts are now the agent-authored handoffs that live next to
│   the human docs: {PIPELINE_DOCS}/NN-name.ctx.md (story/req/api/data contracts).
│   The context-manager COLLECTS them — it no longer re-derives from full docs.
├── contracts/
│   └── constraint-contract.md          ~150 tokens — live hard-rule rollup of every
│                                       .ctx.md `constraints:` field, always pushed
│
│  ── Pipeline-run memory ───────────────────────────────────────────────────
├── timeline.md                         [Episodic]   append-only event log
├── decisions.md                        [Long-term]  append-only D-NNN log
├── knowledge-graph.yaml                [Knowledge]  pipeline entity relationships
│
│  ── Session persistence ───────────────────────────────────────────────────
├── HANDOFF.md                          [Long-term]  phase-gate baseline for delta state
│
│  ── Codebase graph (single source of truth for code understanding) ────────
├── codebase/
│   ├── codebase-graph.yaml             THE GRAPH — all code nodes + typed edges
│   │                                   built via 3-pass grep; queried via path-finding
│   ├── graph-cache/                    Derived projections (auto-invalidated)
│   │   ├── endpoints.tsv              cached: all endpoint nodes
│   │   ├── tables.tsv                 cached: all table + migration nodes
│   │   ├── symbols.tsv                cached: all class + method nodes
│   │   └── {query-hash}.yaml          cached: path-finding results
│   ├── tech_stack.md                   frameworks, versions, build tools
│   ├── app_overview.md                 project purpose, existing features
│   ├── backend_conventions.md          sampled from 3 hot-zone class nodes
│   └── frontend_conventions.md         sampled from 3 hot-zone component nodes
│
│  ── Immutable audit trail ─────────────────────────────────────────────────
└── snapshots/
    └── YYYY-MM-DD_HH-MM_phase-N.md
```

**Gitignore:** `{CONTEXT_STORE}/codebase/` (derived, per-machine). Commit everything else — `HANDOFF.md`, `decisions.md`, `timeline.md`, `knowledge-graph.yaml`, `contracts/`, `snapshots/`, and both `{PIPELINE_DOCS}/01–11-*.md` and `{PIPELINE_DOCS}/01–11-*.ctx.md` are the permanent record of the feature.

---

## Phase Map & Agent Reference

### Phase 0 — Requirements Intake
**Agent:** `ba-agent` (orchestrator — not delegated)  
**Does:** Resolves `{PIPELINE_DOCS}`, signals context-manager to build codebase graph, classifies scope (XS/S/M/L/XL), presents the pipeline plan, gates every phase transition.

### Phase 1 — Discovery
| Agent | Input | Output |
|-------|-------|--------|
| `product-manager` | Raw idea / stakeholder brief | `01-product-spec.md` — stories, RICE, MVP scope |
| `requirements-analyst` | `01-product-spec.md` | `02-requirements.md` — REQ-IDs, Gherkin, traceability |

### Phase 2 — Design
| Agent | Input | Output |
|-------|-------|--------|
| `architect` | `01`, `02` | `03-architecture.md` — style, components, NFRs |
| `api-designer` | `02`, `03` | `04-api-spec.md` + `04-api-spec.yaml` |
| `data-modeler` | `02`, `03`, `04` | `05-data-model.md` — ERD, DDL, migrations |
| `ux-designer` | `01`, `02`, `03` | `06-ux-flows.md` — flows, screens, UX copy |
| `security-reviewer` | `03`, `04`, `05` | Security findings (no numbered doc — feeds back to architect) |

*`api-designer`, `data-modeler`, `ux-designer` run in parallel.*

### Phase 3 — Sprint Planning
| Agent | Input | Output |
|-------|-------|--------|
| `estimator` | `01`, `02`, `03` | `07-estimates.md` — tasks, points, risks |
| `planner` | `02`, `07` | `08-sprint-plan.md` — sprint backlog, DoD |

### Phase 4 — Development
| Agent | Input | Output |
|-------|-------|--------|
| `tdd-guide` | `02`, `04` | Failing test skeletons (Red phase) |
| `java-developer` | `03`, `04`, `05`, `08` | Appends to `09-implementation-log.md` |
| `angular-frontend-engineer` | `04`, `06`, `08` | Appends to `09-implementation-log.md` |
| `code-reviewer` | PR diff + `04` | Review findings (APPROVE / REQUEST CHANGES) |

*`java-developer` and `angular-frontend-engineer` run in parallel — sectioned writes, merged by context-manager.*

### Phase 5 — QA
| Agent | Input | Output |
|-------|-------|--------|
| `qa-engineer` | `02`, `04`, `09` | `10-test-plan.md` — test cases, quality gates |
| `e2e-runner` | `10`, `04` | Playwright test files |
| `api-tester` | `04` | API smoke test results |
| `performance-tester` | `04`, `09` | k6 load test + bottleneck analysis |

### Phase 6 — Release
| Agent | Input | Output |
|-------|-------|--------|
| `devops-engineer` | `03`, `09` | Dockerfile, CI/CD pipeline, docker-compose |
| `release-manager` | `09`, `10` | `11-release-notes.md` — Go/No-Go, changelog, rollback |

### Phase 7 — Post-Release
| Agent | Input | Output |
|-------|-------|--------|
| `observability-engineer` | `03`, `11` | Alerts, SLOs, dashboards |
| `docs-updater` | `04`, `11` | Updated README, API docs, CHANGELOG |
| `chief-of-staff` | `11` | Stakeholder release notes |
| `tech-debt-tracker` | `09` | Tech debt register for shortcuts taken |
| `retrospective-facilitator` | All pipeline docs | Sprint retrospective + action items |

*All Phase 7 agents run in parallel.*

---

## Scope → Pipeline Matrix

| Scope | Description | Phases |
|-------|-------------|--------|
| **XS** | Bug fix, config change | 0 → 4 → 6 |
| **S** | Single feature, 1–3 days | 0 → 1 → 3 → 4 → 5 → 6 |
| **M** | Multi-feature, 1–4 weeks | 0 → 1 → 2 → 3 → 4 → 5 → 6 → 7 |
| **L** | New product area, 1–3 months | All phases, multiple sprints |
| **XL** | New system, 3+ months | All phases, repeated sprints, `llm-cost-estimator` mandatory |

---

## Example: Full Feature Walkthrough

```
# 1. Start — ba-agent initializes the pipeline and memory
Use @ba-agent: "Allow admins to bulk-deactivate user accounts."
Project: /path/to/project

→ 📁 Pipeline docs: /path/to/project/docs/sdlc [NEW]
→ Codebase graph: building... done (847 nodes, 2341 edges)
→ PIPELINE_DOCS established. Proceeding to Phase 1.

# 2. Discovery — context-manager queries graph before each agent
Context-manager: graph query → neighbourhood(all modules, depth 1) → app_overview injected
@product-manager writes → docs/sdlc/01-product-spec.md
Context-manager: story-contract.md generated (62 tokens from 780-token doc)

@requirements-analyst writes → docs/sdlc/02-requirements.md
Context-manager: req-contract.md generated. knowledge-graph.yaml: +8 nodes (stories + reqs)

# 3. Design (parallel)
@architect writes → docs/sdlc/03-architecture.md
Context-manager: D-001 logged to decisions.md, constraint-contract.md updated

@api-designer writes → docs/sdlc/04-api-spec.md
Context-manager: api-contract.md generated (121 tokens). graph-cache/endpoints.tsv updated

@data-modeler writes → docs/sdlc/05-data-model.md
Context-manager: data-contract.md generated. codebase-graph.yaml: +3 table nodes

@ux-designer writes → docs/sdlc/06-ux-flows.md

# 4. Planning
@estimator writes → docs/sdlc/07-estimates.md
@planner writes → docs/sdlc/08-sprint-plan.md

# 5. Development (parallel — sectioned writes)
@java-developer appends → docs/sdlc/09-implementation-log.md  <!-- SECTION:java-developer -->
@angular-frontend-engineer appends → docs/sdlc/09-implementation-log.md  <!-- SECTION:angular -->
Context-manager: Phase 4 merge complete. Conflicts: 0. codebase-graph.yaml: +47 class nodes

# 6. Session expires — context-manager already wrote HANDOFF.md
Timeline last entry: SESSION_END at 14:02 (context 71% full)

# 7. New session — resume instantly
Use @context-manager to restore session.
Project: /path/to/project

→ Context Restored — "Bulk deactivate users"
→ Timeline last 3 events: AGENT_COMPLETE @java-developer | SESSION_END | (new) SESSION_RESUME
→ 01–09 verified on disk ✅
→ Graph: 894 nodes, 2418 edges (current)
→ Next: @qa-engineer

# 8. Finish
@qa-engineer writes → docs/sdlc/10-test-plan.md
@release-manager writes → docs/sdlc/11-release-notes.md → GO ✅
```

---

## Project Structure

```
ai_agentic/
├── agents/                        # 38 specialized agent prompts
│   │
│   ├── [orchestration]
│   │   ├── ba-agent.md            # entry point — {PIPELINE_DOCS} resolution, phase gating
│   │   └── context_management.md  # AI memory backbone — 7 memory types, 6 token strategies,
│   │                              # codebase graph (nodes+edges, 4 path-finding queries)
│   │
│   ├── [Phase 1 — Discovery]
│   │   ├── product-manager.md     # → 01-product-spec.md
│   │   └── requirements-analyst.md # → 02-requirements.md
│   │
│   ├── [Phase 2 — Design]
│   │   ├── architect.md           # → 03-architecture.md
│   │   ├── api-designer.md        # → 04-api-spec.md + .yaml
│   │   ├── data-modeler.md        # → 05-data-model.md
│   │   └── ux-designer.md         # → 06-ux-flows.md
│   │
│   ├── [Phase 3 — Planning]
│   │   ├── estimator.md           # → 07-estimates.md
│   │   ├── planner.md             # → 08-sprint-plan.md
│   │   └── llm-cost-estimator.md
│   │
│   ├── [Phase 4 — Development]
│   │   ├── tdd-guide.md
│   │   ├── java-developer.md      # → appends 09-implementation-log.md
│   │   ├── angular-frontend-engineer.md  # → appends 09-implementation-log.md
│   │   ├── typescript-reviewer.md
│   │   ├── go-reviewer.md
│   │   ├── python-reviewer.md
│   │   ├── code-reviewer.md
│   │   ├── database-reviewer.md
│   │   ├── build-error-resolver.md
│   │   └── refactor-cleaner.md
│   │
│   ├── [Phase 5 — QA]
│   │   ├── qa-engineer.md         # → 10-test-plan.md
│   │   ├── e2e-runner.md
│   │   ├── api-tester.md
│   │   └── performance-tester.md
│   │
│   ├── [Phase 5 — Security]
│   │   ├── security-reviewer.md
│   │   ├── white-hacker.md
│   │   └── grey-hacker.md
│   │
│   ├── [Phase 6 — Release]
│   │   ├── devops-engineer.md
│   │   └── release-manager.md     # → 11-release-notes.md
│   │
│   ├── [Phase 7 — Post-Release]
│   │   ├── observability-engineer.md
│   │   ├── docs-updater.md
│   │   ├── chief-of-staff.md
│   │   ├── tech-debt-tracker.md
│   │   └── retrospective-facilitator.md
│   │
│   └── [Support — any phase]
│       ├── document-extractor.md
│       ├── incident-responder.md
│       └── loop-operator.md
│
└── docs/
    ├── README.md                  # you are here
    ├── sdlc-workflow.md           # step-by-step SDLC walkthrough with example prompts
    ├── agents-reference.md        # all agents at a glance with example prompts
    └── model-assignment.md        # why each agent uses its specific model + temperature
```

---

## Documentation Index

| Document | Purpose |
|----------|---------|
| [sdlc-workflow.md](./sdlc-workflow.md) | Full SDLC walkthrough — which agent, when, in what order |
| [agents-reference.md](./agents-reference.md) | Quick reference for all 38 agents with example prompts |
| [model-assignment.md](./model-assignment.md) | Why each agent uses its specific model, temperature, and token limit |

---

## Design Principles

- **One agent, one document** — each pipeline agent owns exactly one numbered output file; who wrote what is unambiguous
- **Graph over scan** — understanding code = path-finding on `codebase-graph.yaml`; no bulk file reading regardless of codebase size
- **Memory by type** — each memory failure mode gets its own memory type: working (focus), episodic (history), semantic (code facts), long-term (decisions), knowledge graph (traceability), multi-agent shared (concurrent writes)
- **Token budget enforced** — six strategies with hard component caps; the context manager trims deterministically, never arbitrarily; P0 content (constraints, graph paths, delta) is never trimmed
- **Path-agnostic** — `{PIPELINE_DOCS}` is resolved at runtime; same agents, any project structure
- **Handoff-encoded** — next agent, instructions, and `{PIPELINE_DOCS}` path are written explicitly in every handoff block; nothing is implicit
- **Session-resilient** — `HANDOFF.md` + `timeline.md` together make any session resumable from the exact checkpoint in under 10 seconds
- **Stack-agnostic** — codebase graph built via grep across Java, Go, Python, TypeScript/Angular/React/Vue; conventions sampled from hot-zone nodes only (never legacy or dead code)
- **Verification before progression** — context-manager confirms each numbered doc was written before signalling the next agent; conflicts in shared logs are detected before QA starts
- **Opinionated defaults** — strong defaults (Spring Boot 3, Java 21, Angular 21 signals, PostgreSQL + Flyway) so stack is not re-specified on every invocation; override when needed
