# Model Assignment Guide

Every agent in this framework is assigned a specific Claude model, temperature, and token limit — tuned to match the complexity and nature of its task.

---

## Why Different Models?

Not all tasks need the same reasoning depth. Using the wrong model in either direction has real costs:

| Mistake | Consequence |
|---------|-------------|
| Opus for simple tasks | Slow responses, high cost, no quality gain |
| Haiku for complex tasks | Shallow analysis, missed edge cases, wrong decisions |
| Wrong temperature | Too creative (hallucinations) or too rigid (generic output) |

The right model for the right task = **60–70% cost reduction** with **no quality loss**.

---

## Model Tiers

### claude-opus-4-6 — Deep Reasoning
**When to assign**: Tasks where missing something has real consequences, requires multi-step adversarial thinking, or involves decisions that are expensive to reverse.

```
temperature: 0.3   (analytical, consistent)
max_tokens:  8192  (thorough, detailed output)
```

| Agent | Why Opus |
|-------|---------|
| `architect` | Design trade-offs span many dimensions — wrong choice costs weeks |
| `security-reviewer` | Missing a CVE can mean a breach |
| `white-hacker` | Attack chain reasoning requires creative adversarial thinking |
| `grey-hacker` | Vulnerability chaining — connecting subtle dots others miss |
| `requirements-analyst` | Exhaustive edge case discovery before a line of code is written |
| `data-modeler` | Schema is hard to change — access patterns need deep analysis |
| `performance-tester` | Root cause analysis spans DB, network, app, and infra layers |

---

### claude-sonnet-4-6 — Balanced Reasoning
**When to assign**: Daily engineering work — code generation, reviews, planning, communication. Needs solid reasoning and language understanding but not Opus-level depth.

```
temperature: 0.4–0.6  (varies by task — see per-agent notes)
max_tokens:  4096
```

| Agent | Temp | Why this temperature |
|-------|------|----------------------|
| `code-reviewer` | 0.5 | Balanced — structured but catches creative issues |
| `java-developer` | 0.5 | Code generation needs some variability for clean solutions |
| `typescript-reviewer` | 0.5 | Same as code-reviewer |
| `go-reviewer` | 0.5 | Same as code-reviewer |
| `python-reviewer` | 0.5 | Same as code-reviewer |
| `api-designer` | 0.5 | Spec generation — consistent but not mechanical |
| `tdd-guide` | 0.5 | Test + implementation needs balanced creativity |
| `incident-responder` | 0.3 | **Lower** — calm, consistent output under pressure |
| `chief-of-staff` | 0.6 | **Higher** — warm, human communication tone |
| `product-manager` | 0.5 | Feature specs need structure + creative user empathy |
| `qa-engineer` | 0.5 | Test case design — thorough but not rigid |
| `devops-engineer` | 0.4 | **Lower** — CI/CD configs must be accurate |
| `release-manager` | 0.4 | **Lower** — checklists must be deterministic |
| `observability-engineer` | 0.4 | **Lower** — alert YAML must be precise |
| `planner` | 0.5 | Task ordering needs reasoning not memorization |
| `estimator` | 0.5 | Estimates need context-sensitive judgment |
| `ux-designer` | 0.6 | **Higher** — UX copy and flows need human warmth |
| `e2e-runner` | 0.4 | **Lower** — Playwright code must be consistent |

---

### claude-haiku-4-5-20251001 — Fast & Deterministic
**When to assign**: Pattern matching, template filling, checklist execution, high-frequency tasks. Speed and cost matter; deep synthesis does not.

```
temperature: 0.1–0.2  (highly deterministic)
max_tokens:  2048     (concise, focused output)
```

| Agent | Temp | Why Haiku |
|-------|------|-----------|
| `build-error-resolver` | 0.1 | Error → known fix pattern, no creativity needed |
| `docs-updater` | 0.1 | Mechanical content sync, high volume |
| `refactor-cleaner` | 0.1 | Apply cleanup rules, deterministic output |
| `tech-debt-tracker` | 0.1 | Score and categorize against a rubric |
| `retrospective-facilitator` | 0.2 | **Slightly higher** — template fill with human warmth |
| `loop-operator` | 0.1 | Coordination logic — delegates thinking to specialists |
| `database-reviewer` | 0.1 | Checklist-based review against known rules |

---

## Temperature Guide

Temperature controls how deterministic vs. creative the output is:

```
0.0 ─────────────────────────────────────── 1.0
│                                             │
Deterministic                           Creative
(same output                         (varied, inventive,
 every time)                          sometimes surprising)

Configs / YAML / Code  →  0.1–0.3
Analysis / Review      →  0.3–0.5
Writing / Planning     →  0.5–0.6
Creative / UX / Comms  →  0.6–0.7
```

**Never go above 0.7** for engineering agents — hallucinations increase significantly.

---

## Token Limit Guide

```
max_tokens: 8192  →  Opus agents — thorough, multi-section reports
max_tokens: 4096  →  Sonnet agents — complete but focused output
max_tokens: 2048  →  Haiku agents — concise, targeted responses
```

---

## Full Assignment Table

| Agent | Model | Temp | Tokens | Tier reason |
|-------|-------|------|--------|-------------|
| `architect` | Opus 4.6 | 0.3 | 8192 | High-stakes design decisions |
| `security-reviewer` | Opus 4.6 | 0.3 | 8192 | Miss nothing — breaches are costly |
| `white-hacker` | Opus 4.6 | 0.3 | 8192 | Adversarial reasoning depth |
| `grey-hacker` | Opus 4.6 | 0.4 | 8192 | Creative vulnerability chaining |
| `requirements-analyst` | Opus 4.6 | 0.3 | 8192 | Exhaustive edge case discovery |
| `data-modeler` | Opus 4.6 | 0.3 | 8192 | Schema decisions are hard to undo |
| `performance-tester` | Opus 4.6 | 0.3 | 8192 | Multi-layer root cause analysis |
| `code-reviewer` | Sonnet 4.6 | 0.5 | 4096 | Daily PR review, solid reasoning |
| `java-developer` | Sonnet 4.6 | 0.5 | 4096 | Code generation, strong Java knowledge |
| `typescript-reviewer` | Sonnet 4.6 | 0.5 | 4096 | TS/React review |
| `go-reviewer` | Sonnet 4.6 | 0.5 | 4096 | Go idioms and concurrency |
| `python-reviewer` | Sonnet 4.6 | 0.5 | 4096 | PEP + mypy + pytest |
| `api-designer` | Sonnet 4.6 | 0.5 | 4096 | OpenAPI spec generation |
| `tdd-guide` | Sonnet 4.6 | 0.5 | 4096 | Test + implementation coaching |
| `incident-responder` | Sonnet 4.6 | 0.3 | 4096 | Calm, consistent under pressure |
| `chief-of-staff` | Sonnet 4.6 | 0.6 | 4096 | Warm human communication |
| `product-manager` | Sonnet 4.6 | 0.5 | 4096 | Feature specs and prioritization |
| `qa-engineer` | Sonnet 4.6 | 0.5 | 4096 | Test planning and case design |
| `devops-engineer` | Sonnet 4.6 | 0.4 | 4096 | Accurate CI/CD and Docker configs |
| `release-manager` | Sonnet 4.6 | 0.4 | 4096 | Deterministic checklists |
| `observability-engineer` | Sonnet 4.6 | 0.4 | 4096 | Precise alert YAML and metrics |
| `planner` | Sonnet 4.6 | 0.5 | 4096 | Implementation task ordering |
| `estimator` | Sonnet 4.6 | 0.5 | 4096 | Story points and risk judgment |
| `ux-designer` | Sonnet 4.6 | 0.6 | 4096 | Human-centred flows and copy |
| `e2e-runner` | Sonnet 4.6 | 0.4 | 4096 | Consistent Playwright code |
| `build-error-resolver` | Haiku 4.5 | 0.1 | 2048 | Error → fix pattern matching |
| `docs-updater` | Haiku 4.5 | 0.1 | 2048 | Mechanical sync, high volume |
| `refactor-cleaner` | Haiku 4.5 | 0.1 | 2048 | Rule-based cleanup |
| `tech-debt-tracker` | Haiku 4.5 | 0.1 | 2048 | Score against rubric |
| `retrospective-facilitator` | Haiku 4.5 | 0.2 | 2048 | Template fill with warmth |
| `loop-operator` | Haiku 4.5 | 0.1 | 2048 | Coordination, delegates thinking |
| `database-reviewer` | Haiku 4.5 | 0.1 | 2048 | Checklist review |

---

## Cost Impact Estimate

Assuming 100 agent calls/day across a team of 5:

```
Scenario A — All Opus (no tiering)
  100 calls × Opus pricing = $$$$

Scenario B — Tiered (this framework)
   7 Opus calls   (7%)   → complex tasks
  18 Sonnet calls (18%)  → varies by usage
  75 Haiku calls  (75%)  → high-volume tasks
  
Estimated saving: ~65% vs all-Opus
Quality impact:   none — each agent uses the minimum model it actually needs
```

---

## Changing a Model Assignment

If you find an agent is producing shallow output → promote it one tier.
If an agent is overkill for your use case → demote it one tier.

Change in two places:
1. The frontmatter `model:` field in `agents/<name>.md`
2. The `"model"` field in `.claude-plugin/plugin.json`
