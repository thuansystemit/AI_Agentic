# API Design Patterns

## REST Conventions

### Resource Naming
- Nouns, not verbs: `/users`, not `/getUsers`
- Plural for collections: `/users`, `/orders`
- Nested for ownership: `/users/{id}/orders`
- Kebab-case for multi-word: `/order-items`

### HTTP Methods
| Method | Use case | Idempotent |
|--------|----------|------------|
| GET    | Read resource | Yes |
| POST   | Create resource | No |
| PUT    | Replace resource | Yes |
| PATCH  | Partial update | No |
| DELETE | Remove resource | Yes |

### Status Codes
- `200 OK` — successful GET/PUT/PATCH
- `201 Created` — successful POST (include `Location` header)
- `204 No Content` — successful DELETE
- `400 Bad Request` — validation error (include error details)
- `401 Unauthorized` — not authenticated
- `403 Forbidden` — authenticated but not authorized
- `404 Not Found` — resource doesn't exist
- `409 Conflict` — state conflict (duplicate, optimistic lock)
- `422 Unprocessable Entity` — semantic validation failure
- `500 Internal Server Error` — unexpected server error

## Request / Response Structure

### Error Response (always consistent)
```json
{
  "error": {
    "code": "VALIDATION_ERROR",
    "message": "Email is invalid",
    "fields": [
      { "field": "email", "message": "Must be a valid email address" }
    ]
  }
}
```

### Pagination
```json
{
  "data": [...],
  "pagination": {
    "page": 1,
    "per_page": 20,
    "total": 150,
    "next_cursor": "eyJpZCI6MTAwfQ=="
  }
}
```

## Versioning

- Version in URL path: `/v1/users` (simplest for client compatibility)
- Never make breaking changes to an existing version
- Deprecate with `Sunset` header before removing

## Authentication

- Stateless: JWT bearer tokens in `Authorization: Bearer <token>`
- Short-lived access tokens (15 min) + longer-lived refresh tokens
- Validate token on every request — don't cache auth state in memory
