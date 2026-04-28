# AI Agentic Framework

A full **Software Development Life Cycle (SDLC)** agent framework for Claude Code — 30 specialized agents covering every phase from idea to production, plus 4 skill libraries of domain knowledge.

---

## What Is This?

Each agent is a markdown file containing a specialized system prompt. When you delegate a task to an agent in Claude Code, it activates that agent's expertise, output format, and quality checklist — giving you a consistent, senior-level collaborator for every part of the product lifecycle.

```
You write a rough idea →
  product-manager turns it into user stories →
    requirements-analyst hardens the spec →
      architect + api-designer + data-modeler design the system →
        planner breaks it into tasks →
          developers build it →
            qa-engineer + security-reviewer validate it →
              release-manager ships it →
                observability-engineer monitors it →
                  retrospective-facilitator improves the process
```

---

## Quick Start

### 1. Use an agent in Claude Code

```
Use the @product-manager agent to write user stories for a user login feature with email and password.
```

```
Use the @architect agent to design the system for a multi-tenant SaaS application.
```

```
Use the @code-reviewer agent to review the following pull request: [paste diff]
```

### 2. Chain agents across a workflow

```
First use @requirements-analyst to harden this spec, then @estimator to break it into tasks.
```

### 3. Use a skill library for context

```
Using the backend-patterns/api-design skill, review this REST API design for consistency.
```

---

## Project Structure

```
ai_agentic/
├── .claude-plugin/
│   ├── plugin.json          # Registers all agents and skills
│   └── marketplace.json     # Marketplace metadata
│
├── agents/                  # 30 specialized agent prompts
│   ├── [discovery]          product-manager, requirements-analyst, estimator
│   ├── [design]             architect, ux-designer, api-designer, data-modeler
│   ├── [build]              planner, tdd-guide, java-developer, typescript-reviewer,
│   │                        go-reviewer, python-reviewer, code-reviewer, security-reviewer
│   ├── [test]               qa-engineer, e2e-runner, performance-tester
│   ├── [ship]               release-manager, devops-engineer
│   ├── [operate]            observability-engineer, incident-responder
│   ├── [iterate]            retrospective-facilitator, tech-debt-tracker, refactor-cleaner
│   └── [support]            build-error-resolver, docs-updater, chief-of-staff,
│                            loop-operator, database-reviewer
│
├── skills/                  # 4 domain knowledge libraries
│   ├── coding-standards/    Language-agnostic best practices
│   ├── backend-patterns/    API design, database patterns
│   ├── frontend-patterns/   React components, Next.js
│   └── ai-patterns/         Prompt engineering, agent design
│
└── docs/                    # This documentation
    ├── README.md             You are here
    ├── sdlc-workflow.md      How to use agents across the SDLC
    └── agents-reference.md  All 30 agents at a glance
```

---

## Documentation Index

| Document | Purpose |
|----------|---------|
| [sdlc-workflow.md](./sdlc-workflow.md) | Full SDLC walkthrough — which agent to use, when, and in what order |
| [agents-reference.md](./agents-reference.md) | Quick reference for all 32 agents with example prompts |
| [model-assignment.md](./model-assignment.md) | Why each agent uses its specific model, temperature, and token limit |

---

## Design Principles

- **One agent, one responsibility** — each agent owns a specific phase, not everything
- **Handoff-driven** — each agent's output is the next agent's input
- **Opinionated** — agents have strong defaults so you don't have to specify everything
- **Template-rich** — every agent produces consistent, structured output
- **Actionable** — no vague advice; every output includes specific next steps
