# Security Review

**Agent:** `@security-reviewer`
**Project:** System Management
**Date:** 2026-04-25
**Input:** requirements.md, architecture.md, api-spec.md, data-model.md
**Method:** Design-time review — no code exists yet

---

## Summary

| Severity | Count |
|----------|-------|
| CRITICAL | 0 |
| HIGH | 2 |
| MEDIUM | 5 |
| LOW | 4 |
| INFO | 3 |
| **Total** | **14** |

All HIGH findings must be resolved before implementation begins. MEDIUM findings must be addressed before release.

---

## Findings

---

### FINDING-001 — HIGH

**TYPE:** Broken Access Control — IDOR  
**LOCATION:** `api-spec.md` → `GET /documents/{id}`, `architecture.md` → DocumentService  
**DESCRIPTION:**  
`GET /documents/{id}` fetches a document by ID. If permission is checked *after* the fetch, a user with no access to the category can confirm a document exists (via 403 vs 404 timing or message difference) and potentially receive data if the check is skipped or mis-ordered.

**IMPACT:**  
Any authenticated user can retrieve documents from categories they have no access to — full IDOR. Satisfies DOC-07 requirement violation.

**REMEDIATION:**  
The implementation MUST:
1. Fetch the document from DB (get its `category_id`)
2. Resolve permission for `(userId, category_id)`
3. If permission < READ → return **403 immediately**, before returning any document data

```java
// CORRECT order — never return data before permission check
Document doc = documentRepository.findById(docId)
    .orElseThrow(() -> new ResourceNotFoundException(...));
permissionService.requirePermission(userId, doc.getCategoryId(), Permission.READ);
return doc; // only reached if permission check passes
```

Never fetch-then-check in separate service calls. Keep both in one transactional method.

---

### FINDING-002 — HIGH

**TYPE:** Broken Authentication — Token Scope  
**LOCATION:** `architecture.md` → Token Refresh Flow, `api-spec.md` → `/auth/refresh`  
**DESCRIPTION:**  
The architecture mentions the refresh token cookie but does not explicitly restrict its cookie `Path` to `/api/v1/auth/refresh`. If the `Path` is `/` (the default), the refresh token is sent with every request to the API — unnecessarily exposing it to all endpoints and increasing the attack surface.

**IMPACT:**  
If any endpoint logs request cookies, or if a future XSS vulnerability emerges, the refresh token (which grants long-lived access) is exposed far more broadly than needed.

**REMEDIATION:**  
Set the cookie `Path` attribute to the refresh endpoint only:

```java
ResponseCookie.from("refresh_token", rawToken)
    .httpOnly(true)
    .secure(true)
    .sameSite("Strict")
    .path("/api/v1/auth/refresh")   // ← MUST be scoped, not "/"
    .maxAge(Duration.ofDays(7))
    .build();
```

The access token cookie may use `Path=/`.

---

### FINDING-003 — MEDIUM

**TYPE:** Security Misconfiguration — Missing HTTP Security Headers  
**LOCATION:** `architecture.md` → Non-Functional Requirements  
**DESCRIPTION:**  
The architecture defines cookie security attributes but omits HTTP response security headers. Without these, the browser cannot enforce content type restrictions, clickjacking protection, or transport security.

**IMPACT:**  
- Missing `X-Content-Type-Options: nosniff` → MIME type sniffing attacks
- Missing `X-Frame-Options: DENY` → clickjacking of the Angular app
- Missing `Strict-Transport-Security` → HTTPS downgrade attacks
- Missing `Content-Security-Policy` → XSS escalation risk

**REMEDIATION:**  
Add a global security headers filter in Spring Boot:

```java
@Bean
SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
    http.headers(headers -> headers
        .contentTypeOptions(Customizer.withDefaults())
        .frameOptions(frame -> frame.deny())
        .httpStrictTransportSecurity(hsts -> hsts
            .includeSubDomains(true)
            .maxAgeInSeconds(31536000))
    );
}
```

For Angular: add `Content-Security-Policy` via nginx or Spring response headers.

---

### FINDING-004 — MEDIUM

**TYPE:** Insufficient Brute-Force Protection  
**LOCATION:** `requirements.md` → AUTH-05, `api-spec.md` → `/auth/login`  
**DESCRIPTION:**  
The per-account lockout (5 attempts, 30 min) only protects individual accounts. A distributed attacker using many IPs can attempt `N × 5` password guesses across `N` accounts before any single account locks — bypassing the lockout entirely.

**IMPACT:**  
Credential stuffing and low-and-slow brute force attacks succeed without triggering any lockout.

**REMEDIATION:**  
Add IP-based rate limiting on `/auth/login`. Use a sliding window:

```yaml
# application.yml — Spring Boot + Bucket4j or custom filter
login:
  rate-limit:
    per-ip: 20 requests per minute
    global: 500 requests per minute
```

Return `429 Too Many Requests` with `Retry-After` header on breach. This is in addition to, not replacing, the per-account lockout.

---

### FINDING-005 — MEDIUM

**TYPE:** Sensitive Data Exposure — Error Responses  
**LOCATION:** `architecture.md` → Non-Functional Requirements  
**DESCRIPTION:**  
Architecture declares `server.error.include-stacktrace: never` and `include-message: never` but does not specify how `GlobalExceptionHandler` formats exception messages. If handlers pass `exception.getMessage()` directly to the API response, internal error details (SQL state, table names, class names) leak to clients.

**IMPACT:**  
Database schema, internal class names, and error causes are exposed — reduces attacker effort to craft targeted attacks.

**REMEDIATION:**  
`GlobalExceptionHandler` must map exception types to **safe, pre-defined messages** — never forward `exception.getMessage()`:

```java
@ExceptionHandler(DataIntegrityViolationException.class)
public ResponseEntity<ErrorResponse> handleDataIntegrity(DataIntegrityViolationException ex) {
    // NEVER: return ErrorResponse.of(ex.getMessage())
    // ALWAYS: return a safe, pre-defined message
    return ResponseEntity.status(409)
        .body(new ErrorResponse("CONFLICT", "Resource already exists"));
}
```

---

### FINDING-006 — MEDIUM

**TYPE:** Security Misconfiguration — CORS  
**LOCATION:** `architecture.md` → Non-Functional Requirements  
**DESCRIPTION:**  
Architecture states "explicit allowed origins list — no wildcards" but does not specify what happens if `CORS_ALLOWED_ORIGINS` environment variable is missing or misconfigured (e.g., accidentally set to `*`).

**IMPACT:**  
If CORS is misconfigured to `*` in production, cross-origin requests with credentials become possible — enabling CSRF-like attacks from any origin despite `SameSite=Strict` cookies (which only protect same-site requests).

**REMEDIATION:**  
Validate the CORS origin list at startup. Fail fast if empty or contains `*`:

```java
@PostConstruct
void validateCors() {
    if (allowedOrigins.isEmpty() || allowedOrigins.contains("*")) {
        throw new IllegalStateException(
            "CORS_ALLOWED_ORIGINS must be set to explicit origins. Wildcards are not permitted.");
    }
}
```

---

### FINDING-007 — MEDIUM

**TYPE:** Broken Authentication — BCrypt Check Order  
**LOCATION:** `architecture.md` → Authentication Flow  
**DESCRIPTION:**  
The architecture correctly describes running BCrypt even for missing users (prevents timing-based enumeration). However, it does not specify the order of checks for **locked** and **inactive** accounts relative to the BCrypt call. If locking or inactivity is checked *before* BCrypt, the timing difference reveals whether the account exists and is locked vs. simply having a wrong password.

**IMPACT:**  
Timing oracle: attackers can enumerate locked valid accounts vs. non-existent emails.

**REMEDIATION:**  
Enforce this exact order in `AuthService.login()`:

```
1. Look up user by email (may be null)
2. Run BCrypt.check(password, user?.hash ?? DUMMY_HASH)  ← always runs
3. If user == null OR !passwordMatches → throw 401
4. If !user.isActive()    → throw 401 (same message as wrong password)
5. If user.isLocked()     → throw 423 with Retry-After
6. Reset failed counter, issue tokens
```

BCrypt must ALWAYS be step 2 — before any account state checks.

---

### FINDING-008 — LOW

**TYPE:** Insufficient Logging — Missing Security Audit Trail  
**LOCATION:** `data-model.md` — no audit table  
**DESCRIPTION:**  
No audit log is captured for security-sensitive events: logins, failed login attempts, permission grants/revocations, user deactivation, role changes. Without this, post-incident forensics are impossible and compliance requirements (SOC 2, ISO 27001) cannot be met.

**IMPACT:**  
Unable to detect or investigate: privilege escalation, insider threats, credential compromise.

**REMEDIATION:**  
Log the following events to structured application logs (minimum) with: `userId`, `action`, `targetId`, `ipAddress`, `timestamp`:

- `AUTH_LOGIN_SUCCESS` / `AUTH_LOGIN_FAILURE` / `AUTH_ACCOUNT_LOCKED`
- `PERMISSION_GRANTED` / `PERMISSION_REVOKED`
- `USER_DEACTIVATED` / `USER_ROLE_CHANGED`
- `TOKEN_REUSE_DETECTED`

For MVP: structured log output is sufficient. Add a DB audit table post-MVP.

---

### FINDING-009 — LOW

**TYPE:** Security Misconfiguration — Unbounded Pagination  
**LOCATION:** `api-spec.md` → all list endpoints  
**DESCRIPTION:**  
List endpoints accept a `size` query parameter with no documented maximum. A request with `size=100000` on the documents endpoint could return millions of bytes in a single response, enabling denial-of-service or data harvesting.

**IMPACT:**  
Resource exhaustion (DB memory, response buffer) and potential bulk data exfiltration.

**REMEDIATION:**  
Enforce a maximum page size in Spring Data:

```java
// In controller or global config
int effectiveSize = Math.min(size, 100); // cap at 100
Pageable pageable = PageRequest.of(page, effectiveSize);
```

Or via Spring configuration:
```yaml
spring.data.web.pageable.max-page-size: 100
```

---

### FINDING-010 — LOW

**TYPE:** Broken Authentication — No Global Session Invalidation  
**LOCATION:** `api-spec.md` → `/auth/logout`, `requirements.md` → AUTH-09  
**DESCRIPTION:**  
Logout only revokes the *current* refresh token. If a user's account is compromised (token stolen from another device), logging out from one device does not invalidate the attacker's session.

**IMPACT:**  
Compromised sessions remain active until the 7-day refresh token TTL expires.

**REMEDIATION:**  
Add a "logout all devices" endpoint (can be deferred to post-MVP):

```
POST /api/v1/auth/logout/all
→ revokeAllByUserId(currentUserId)
```

User deactivation already implements this (AUTH-04) — the same `revokeAllByUserId` call should be accessible as a user-initiated action.

---

### FINDING-011 — LOW

**TYPE:** Security Misconfiguration — Missing Retry-After Header  
**LOCATION:** `api-spec.md` → `/auth/login` → 423 response  
**DESCRIPTION:**  
The 423 (Locked) response is defined in the API spec but the `Retry-After` header is listed as optional. Without it, clients cannot implement smart retry logic and users cannot know when to try again.

**IMPACT:**  
Poor UX leads to continued login attempts during lockout, amplifying load and confusing legitimate users.

**REMEDIATION:**  
Make `Retry-After` mandatory on 423:

```java
@ExceptionHandler(AccountLockedException.class)
public ResponseEntity<ErrorResponse> handleLocked(AccountLockedException ex) {
    return ResponseEntity.status(423)
        .header("Retry-After", String.valueOf(ex.getRetryAfterSeconds()))
        .body(new ErrorResponse("ACCOUNT_LOCKED", "Account locked. Try again later."));
}
```

---

### FINDING-012 — INFO

**TYPE:** Informational — Actuator Endpoint Review  
**LOCATION:** `architecture.md` → Non-Functional Requirements  
**DESCRIPTION:**  
Only `health` and `info` actuator endpoints are exposed. Verify that `/actuator/info` does not include sensitive data (build metadata with internal paths, dependency versions that assist fingerprinting).

**REMEDIATION:**  
Configure `/actuator/info` to expose only safe fields:

```yaml
management:
  info:
    env:
      enabled: false  # do not expose env vars via info
    git:
      mode: simple    # only branch + commit hash, not full git info
```

---

### FINDING-013 — INFO

**TYPE:** Informational — Refresh Token Entropy  
**LOCATION:** `architecture.md` → Token Refresh Flow  
**DESCRIPTION:**  
The raw refresh token is generated as `UUID.randomUUID()`. UUID v4 provides 122 bits of randomness, which is sufficient for security. SHA-256 hashing before storage is correct. No action required — confirming this is explicitly intentional.

**REMEDIATION:** None. Document the decision in code comments.

---

### FINDING-014 — INFO

**TYPE:** Informational — Concurrent Refresh Race Condition  
**LOCATION:** `architecture.md` → Token Refresh Flow  
**DESCRIPTION:**  
Architecture mentions atomic rotation but does not specify the DB locking mechanism. Without `SELECT FOR UPDATE`, two concurrent refresh requests with the same token could both succeed before either revokes the old token — creating two valid tokens from one.

**REMEDIATION:**  
Use `SELECT FOR UPDATE` (pessimistic write lock) in the repository:

```java
@Lock(LockModeType.PESSIMISTIC_WRITE)
@Query("SELECT t FROM RefreshToken t WHERE t.tokenHash = :hash AND t.revoked = false")
Optional<RefreshToken> findByTokenHashAndIsRevokedFalse(String hash);
```

Both the revoke and the new-token insert must occur in the same `@Transactional` method.

---

## Required Actions Before Implementation

| Finding | Action | Owner |
|---------|--------|-------|
| FINDING-001 (HIGH) | Implement permission check BEFORE data return in DocumentService | Developer |
| FINDING-002 (HIGH) | Scope refresh cookie Path to `/api/v1/auth/refresh` | Developer |
| FINDING-003 (MEDIUM) | Add security headers in SecurityConfig | Developer |
| FINDING-004 (MEDIUM) | Add IP-based rate limiting on `/auth/login` | Developer |
| FINDING-005 (MEDIUM) | GlobalExceptionHandler uses pre-defined messages only | Developer |
| FINDING-006 (MEDIUM) | Validate CORS origins at startup, reject wildcards | Developer |
| FINDING-007 (MEDIUM) | Enforce BCrypt-first order in AuthService | Developer |
| FINDING-014 (INFO) | Use SELECT FOR UPDATE on refresh token lookup | Developer |
