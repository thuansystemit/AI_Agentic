# Agent Handoff Protocol — Dual-Document Pipeline

Every pipeline agent produces **two** artifacts per step, not one:

| Artifact | Audience | Optimized for | Who reads it |
|----------|----------|---------------|--------------|
| `NN-name.md` | **Humans** | Understanding — prose, rationale, tables, diagrams | People reviewing the project |
| `NN-name.ctx.md` | **The next AI agent** | Tokens — IDs, constraints, pointers, zero prose | The next agent, **by default** |

`.ctx` = *context handoff*. It is the machine-to-machine contract between agents. The human doc is the source of record; the `.ctx.md` is the compressed read path.

## Why two documents

The human doc is ~600–1500 tokens of narrative. The next agent needs only the **facts** inside it — the IDs, the chosen options, the hard constraints. Reading the whole prose doc to extract 80 tokens of facts is the expensive path, and it repeats at every one of the ~11 handoffs.

```
Read 11 full human docs  ≈ 8 000–12 000 tokens   ← old default
Read 11 .ctx.md handoffs  ≈ 1 000–1 500 tokens    ← new default  (~85% saving)
```

The human docs stay pristine for people; agents ride the cheap rail.

## The three rules

1. **The author writes both.** The agent that just produced the content writes its own `.ctx.md` as the final step of the same run — it knows the content best and avoids a second agent re-reading 1000 tokens to distill 80. The `.ctx.md` **is** the contract; the context-manager no longer re-derives contracts from full docs (see `agents/context_management.md`).
2. **A `.ctx.md` is a schema, not a summary.** Actual values only — IDs, paths, types, chosen options, one-line decisions. Strip every rationale paragraph, example, and diagram. (Extract, don't summarise.)
3. **Default read = `.ctx.md`; pull the human doc on demand.** Each agent's "Instructions for next agent" points to the upstream `.ctx.md` files. The next agent reads the full `NN-name.md` **only** when it needs the detail behind a referenced ID (the `pull_hint` field says when that's worth it).

## Universal `.ctx.md` schema

Every `.ctx.md` is a single YAML document. These header fields are common to all agents:

```yaml
---
doc: NN-name                 # this handoff's numbered slug
agent: <agent-name>          # who authored it
phase: <N>                   # pipeline phase
status: complete
human_doc: NN-name.md        # the full doc — pull only for detail
next: [<agent>, ...]         # who consumes this next
provides: { ... }            # canonical IDs/facts THIS agent introduced
constraints: [ ... ]         # hard rules that propagate downstream (do not violate)
decisions: { D-NNN: "<one line>" }   # architect onward; IDs canonical in decisions.md
open: [ ... ]                # BLOCKING items only — empty list if none
pull_hint: "<what lives only in human_doc and when to read it>"
# ... agent-specific payload below ...
---
```

**Authoring rules**

- `provides:` is the **canonical** location for the IDs this agent introduces (stories, REQs, endpoints, tables…). Downstream `.ctx.md` files *reference* these IDs — they never re-list them.
- `constraints:` carries forward only **hard rules** (auth pattern, PK type, base package, migration version, coverage gate). Once a constraint appears, every later `.ctx.md` keeps propagating it.
- `open:` lists only items that **block** the next agent. Non-blocking notes stay in the human doc.
- Keep the whole file under ~150 tokens where the content allows. If it grows past that, you're summarising prose — cut to IDs and pointers.

## Per-agent `.ctx.md` files

| # | Human doc | Handoff | Author |
|---|-----------|---------|--------|
| 01 | `01-product-spec.md` | `01-product-spec.ctx.md` | `@product-manager` |
| 02 | `02-requirements.md` | `02-requirements.ctx.md` | `@requirements-analyst` |
| 03 | `03-architecture.md` | `03-architecture.ctx.md` | `@architect` |
| 04 | `04-api-spec.md` / `.yaml` | `04-api-spec.ctx.md` | `@api-designer` |
| 05 | `05-data-model.md` | `05-data-model.ctx.md` | `@data-modeler` |
| 06 | `06-ux-flows.md` | `06-ux-flows.ctx.md` | `@ux-designer` |
| 07 | `07-estimates.md` | `07-estimates.ctx.md` | `@estimator` |
| 08 | `08-sprint-plan.md` | `08-sprint-plan.ctx.md` | `@planner` |
| 09 | `09-implementation-log.md` | `09-implementation-log.ctx.md` | `@java-developer` + `@angular-frontend-engineer` (sectioned append) |
| 10 | `10-test-plan.md` | `10-test-plan.ctx.md` | `@qa-engineer` |
| 11 | `11-release-notes.md` | `11-release-notes.ctx.md` | `@release-manager` |

The exact per-agent template lives in each agent file under **"Mandatory Context Handoff (`.ctx.md`)"**, so a directly-invoked agent carries its own schema with no external lookup.

## Layout (sibling suffix)

```
{PIPELINE_DOCS}/
├── PIPELINE_STATE.md          ← @ba-agent (tracks both .md and .ctx.md per row)
├── 01-product-spec.md         ← human
├── 01-product-spec.ctx.md     ← agent handoff
├── 02-requirements.md
├── 02-requirements.ctx.md
├── 03-architecture.md
├── 03-architecture.ctx.md
└── …
```

## Reading discipline for every agent

When invoked, an agent's **first** reads are the upstream `.ctx.md` files named in the handoff. It pulls a full `NN-name.md` **only** when a `pull_hint` tells it the detail it needs lives there (e.g. field-level API schemas, full Gherkin scenarios, ERD). Reading a full human doc "just in case" is the anti-pattern this protocol exists to remove.
