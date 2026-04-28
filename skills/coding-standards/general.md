# General Coding Standards

These standards apply across all languages and projects.

## Naming

- Names should reveal intent: `getUserById` not `getUser2` or `fetch`
- Boolean variables/functions: `is`, `has`, `can`, `should` prefix (`isActive`, `hasPermission`)
- Collections are plural: `users`, `orderItems`
- Constants are SCREAMING_SNAKE_CASE: `MAX_RETRIES`, `DEFAULT_TIMEOUT`
- Avoid abbreviations unless universally understood (`url`, `id`, `http`)

## Functions

- One function = one thing. If you need "and" to describe it, split it.
- Max 3-4 parameters; beyond that, use an options object/struct
- Functions should have no hidden side effects
- Pure functions are preferred — same input always gives same output

## Comments

- Write comments to explain **why**, not **what**. The code explains what.
- Mark non-obvious workarounds with `// HACK:`, `// WORKAROUND:`, and a link to the issue
- `TODO:` comments must include owner and ticket: `// TODO(thuanvan): remove after #123 is done`
- Delete commented-out code — version control has the history

## Error Handling

- Always handle errors explicitly — never swallow silently
- Add context when re-throwing: "failed to create user" not just re-throwing the original
- Distinguish between expected errors (validation) and unexpected errors (bugs)
- Log errors at the boundary where they're handled, not where they're thrown

## Git Hygiene

- Each commit is a single logical change
- Commit messages: imperative mood, under 72 chars (`Add user email validation`)
- PRs are small and focused — one concern per PR
- No commented-out code, debug statements, or `console.log` in commits
