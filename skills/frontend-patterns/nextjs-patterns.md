# Next.js Patterns

## App Router Structure

```
app/
├── layout.tsx           # Root layout (HTML shell, providers)
├── page.tsx             # Home route
├── loading.tsx          # Root loading UI
├── error.tsx            # Root error boundary
├── (auth)/              # Route group — no URL segment
│   ├── login/page.tsx
│   └── register/page.tsx
├── dashboard/
│   ├── layout.tsx       # Dashboard-specific layout
│   ├── page.tsx
│   └── [id]/page.tsx    # Dynamic route
└── api/
    └── users/route.ts   # API route handler
```

## Server vs. Client Components

| Concern | Server Component | Client Component |
|---------|-----------------|-----------------|
| Data fetching | Yes (default) | No |
| Browser APIs | No | Yes |
| Event handlers | No | Yes |
| useState / useEffect | No | Yes |
| Access secrets | Yes | No |

**Rule**: push `"use client"` as far down the tree as possible.

## Data Fetching Patterns

```typescript
// Parallel data fetching in Server Component
export default async function Page({ params }) {
  const [user, posts] = await Promise.all([
    getUser(params.id),
    getUserPosts(params.id),
  ])
  return <UserProfile user={user} posts={posts} />
}
```

## Caching Strategy

```typescript
// Static — cached indefinitely, revalidated on deploy
const data = await fetch(url)

// Time-based revalidation
const data = await fetch(url, { next: { revalidate: 3600 } })

// No cache — always fresh
const data = await fetch(url, { cache: 'no-store' })

// On-demand revalidation
import { revalidatePath } from 'next/cache'
revalidatePath('/dashboard')
```

## API Routes

```typescript
// app/api/users/route.ts
import { NextRequest, NextResponse } from 'next/server'

export async function GET(request: NextRequest) {
  const { searchParams } = new URL(request.url)
  const page = searchParams.get('page') ?? '1'
  
  const users = await getUsers({ page: parseInt(page) })
  return NextResponse.json(users)
}

export async function POST(request: NextRequest) {
  const body = await request.json()
  // validate body...
  const user = await createUser(body)
  return NextResponse.json(user, { status: 201 })
}
```

## Middleware

Use `middleware.ts` for:
- Auth guard (redirect to login if not authenticated)
- Locale detection
- A/B testing (rewrite to variant)

Keep middleware fast — no DB calls, minimal logic.
