# Frontend Component Design Patterns

## Component Principles

### Single Responsibility
Each component does one thing. If a component fetches data AND renders a list AND handles pagination — split it.

```
- UserListPage        (routing, layout)
  - UserList          (renders the list)
    - UserCard        (renders one user)
  - Pagination        (handles page navigation)
```

### Props Contract
- Props interface should be minimal and explicit
- Required props first, optional props after
- Use discriminated unions for components with multiple modes

```typescript
// BAD — too many optionals, unclear contract
type ButtonProps = {
  label?: string
  icon?: string
  loading?: boolean
  disabled?: boolean
  variant?: 'primary' | 'secondary' | 'danger'
}

// GOOD — explicit required fields
type ButtonProps = {
  label: string
  variant: 'primary' | 'secondary' | 'danger'
  icon?: string
  loading?: boolean
  disabled?: boolean
}
```

## State Management

### Local vs. Global State
- **Local state** (`useState`): UI state that doesn't leave the component (open/closed, hover, active tab)
- **Server state** (React Query / SWR): data fetched from API — don't duplicate in Redux
- **Global state** (Zustand / Context): auth, theme, user preferences

### Derived State
Never store derived state — compute it from source of truth:

```typescript
// BAD
const [total, setTotal] = useState(0)
useEffect(() => setTotal(items.reduce((sum, i) => sum + i.price, 0)), [items])

// GOOD
const total = items.reduce((sum, i) => sum + i.price, 0)
```

## Data Fetching (Next.js)

- Server Components for initial data — no loading spinners, no client JS
- `use client` only when you need interactivity or browser APIs
- `loading.tsx` for route-level loading states
- `error.tsx` for route-level error boundaries

```typescript
// Server Component — no useEffect, no useState
export default async function UserPage({ params }) {
  const user = await getUserById(params.id) // direct DB/API call
  return <UserProfile user={user} />
}
```

## Styling

- Tailwind utility classes — co-located with markup, no context switching
- Extract repeated class groups into a `cn()` helper or component variant
- No inline styles except for dynamic values (e.g., `style={{ width: progress + '%' }}`)
- Dark mode via `dark:` prefix, not JS toggling
