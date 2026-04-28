# Agent Design Patterns

## Core Agent Architecture

```
User Input
    │
    ▼
┌─────────────┐
│  Planner    │ ← decides what tools/agents to use
└─────────────┘
    │
    ▼
┌─────────────┐     ┌──────────────┐
│  Executor   │────▶│   Tools      │ (search, code, APIs)
└─────────────┘     └──────────────┘
    │
    ▼
┌─────────────┐
│  Verifier   │ ← checks result, decides to retry or finish
└─────────────┘
    │
    ▼
Final Response
```

## Patterns

### Single Agent with Tools
Best for: well-scoped tasks, one primary capability
```
Agent
  ├── tool: search_web
  ├── tool: run_code
  └── tool: write_file
```

### Multi-Agent (Orchestrator + Specialists)
Best for: complex tasks requiring different expertise
```
Orchestrator Agent
  ├── delegates to: Planner Agent
  ├── delegates to: Code Reviewer Agent
  └── delegates to: Security Agent
```

### Supervisor Pattern
A supervisor agent monitors worker agents and can intervene or re-assign.

### Human-in-the-Loop
Build explicit pause points:
- Before irreversible actions (deleting data, sending emails)
- When confidence is low
- When the task scope changes unexpectedly

## Memory Types

| Type | Scope | Use Case |
|------|-------|----------|
| In-context | Single conversation | Working state, recent history |
| External (vector DB) | Cross-session | Knowledge base, past interactions |
| Structured (DB) | Persistent | User preferences, task history |
| Cache | Short-lived | Expensive computation results |

## Failure Handling

```python
MAX_RETRIES = 3

for attempt in range(MAX_RETRIES):
    try:
        result = agent.run(task)
        if verifier.is_valid(result):
            return result
        # Bad result — retry with feedback
        task = task.with_feedback(verifier.explain(result))
    except ToolError as e:
        if attempt == MAX_RETRIES - 1:
            escalate_to_human(task, e)
        # else retry
```

## Evaluation

Before shipping an agent:
- [ ] Tested on 20+ diverse inputs
- [ ] Failure modes documented
- [ ] Latency and cost per call measured
- [ ] Human-in-the-loop for edge cases defined
- [ ] Logging captures inputs, outputs, and tool calls
- [ ] Evals run in CI to catch regressions
