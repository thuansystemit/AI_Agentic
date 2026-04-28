# Database Patterns

## Connection Management

- Always use a connection pool — never open a connection per request
- Set pool size based on DB max connections and concurrency needs
- Connection timeout: fail fast rather than queue forever

## Query Patterns

### Repository Pattern
Wrap all DB access in a repository layer — controllers/services never write SQL directly.

```python
class UserRepository:
    def find_by_id(self, user_id: int) -> User | None: ...
    def find_by_email(self, email: str) -> User | None: ...
    def create(self, data: CreateUserDTO) -> User: ...
    def update(self, user_id: int, data: UpdateUserDTO) -> User: ...
```

### Avoiding N+1 Queries
```python
# BAD — N+1
users = db.query("SELECT * FROM users")
for user in users:
    orders = db.query("SELECT * FROM orders WHERE user_id = ?", user.id)

# GOOD — single query with JOIN or batch fetch
users = db.query("""
    SELECT u.*, o.* FROM users u
    LEFT JOIN orders o ON o.user_id = u.id
""")
```

## Transactions

- Wrap multi-step mutations in a transaction
- Keep transactions short — don't do external calls inside a transaction
- Use optimistic locking for concurrent update conflicts

```python
with db.transaction():
    account.balance -= amount
    ledger.record(debit=amount)
    # commit happens automatically on exit
```

## Migrations

- One change per migration file
- Always write a rollback (`down`) migration
- Zero-downtime deployment order:
  1. Add column (nullable or with default)
  2. Deploy code that writes to both old and new
  3. Backfill data
  4. Deploy code that reads from new column
  5. Drop old column

## Indexing Strategy

- Index foreign keys always
- Index columns in `WHERE` clauses that filter large tables
- Composite indexes: column order matters — most selective first
- Use `EXPLAIN ANALYZE` before adding indexes in production
