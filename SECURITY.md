# Security Policy — CapiBook

## Overview

This document describes the security controls in place for the CapiBook appointment booking system.

---

## Authentication & Authorisation

### JWT
- Access tokens expire after 15 minutes (configurable via `JWT_ACCESS_TOKEN_EXPIRATION_MS`).
- Refresh tokens expire after 7 days, rotate on every use, and are revoked on logout or a new login.
- JWT signing key is a minimum 256-bit secret loaded exclusively from the `JWT_SECRET` environment variable — it is never committed to source control.
- All endpoints except `/api/v1/auth/**`, `/actuator/health`, and Swagger UI require a valid `Authorization: Bearer <token>` header.

### Brute-force Protection
- After **5 consecutive failed login attempts** the account is locked for **15 minutes**.
- The lock is stored in the `users.locked_until` column; a successful login resets both the counter and the lock.
- A locked account returns **HTTP 423 Locked** even if the correct password is supplied.

### Rate Limiting
- `/api/v1/auth/login` and `/api/v1/auth/register` are rate-limited per source IP.
- Default: **10 requests per minute** per IP (configurable via `app.security.rate-limit.auth-requests-per-minute`).
- Excess requests return **HTTP 429 Too Many Requests**.

### Role-based Access Control
- Three roles: `CUSTOMER`, `BRANCH_ADMIN`, `SYSTEM_ADMIN`.
- `CUSTOMER` registration is self-service; role is unconditionally set to `CUSTOMER` by the backend — clients cannot self-assign elevated roles.
- `BRANCH_ADMIN` accounts are created by `SYSTEM_ADMIN` only.
- `BRANCH_ADMIN` access is scoped to their own branch at the service layer.
- Method-level security enforced via `@PreAuthorize` / `@PostAuthorize`.

---

## Transport Security

### HTTP Headers
The following security headers are applied to every response via Spring Security:

| Header | Value |
|--------|-------|
| `X-Content-Type-Options` | `nosniff` |
| `X-Frame-Options` | `DENY` |
| `Strict-Transport-Security` | `max-age=31536000; includeSubDomains` (HTTPS only) |
| `Content-Security-Policy` | `default-src 'self'; script-src 'self'; style-src 'self' 'unsafe-inline'; img-src 'self' data:; font-src 'self'; connect-src 'self'` |

### HTTPS
- HSTS is emitted only over HTTPS connections (Spring Security skips it for plain HTTP).
- Production deployments must terminate TLS at the load balancer or nginx reverse proxy.

### CORS
- Allowed origins are configured via the `CORS_ALLOWED_ORIGINS` environment variable.
- In production this should be set to the exact frontend origin (e.g. `https://capibook.example.com`).
- Wildcard origins (`*`) must never be used in production.

---

## API Security

### Input Validation
- All request bodies are validated with `@Valid` + Bean Validation constraints before reaching the service layer.
- `notes` and `reason` fields on appointment requests are capped at **500 characters** (`@Size(max=500)`).
- Email fields use `@Email`; passwords require a minimum of 8 characters (`@Size(min=8)`).
- Validation errors return **HTTP 400 Bad Request** with a per-field error map.

### Injection Prevention
- All database queries use JPA with parameterised JPQL — no raw SQL string concatenation.
- The API returns JSON only; it does not render HTML, eliminating server-side XSS.
- Frontend is responsible for escaping dynamic content before inserting it into the DOM.

### SQL Injection
- Protected by Hibernate/JPA parameterised queries throughout.
- Flyway migrations use static SQL with no runtime variable interpolation.

---

## Secrets Management

| Secret | Storage |
|--------|---------|
| JWT signing key | `JWT_SECRET` env var |
| Database password | `SPRING_DATASOURCE_PASSWORD` env var |
| All credentials | Environment variables or a secrets manager (e.g. AWS Secrets Manager, HashiCorp Vault) |

No secrets are committed to source control. `.env` files are in `.gitignore`.

For production, use a secrets manager rather than plain environment variables. Document the chosen provider in your deployment runbook.

---

## Dependencies

- OWASP Dependency-Check runs in CI on every push to `main` (`backend-ci.yml`); builds fail on **HIGH or CRITICAL** CVEs.
- Trivy container scans run in CI; builds fail on **CRITICAL** vulnerabilities in the Docker image.
- A weekly security scan (`security.yml`) runs OWASP, Trivy filesystem scan, and GitLeaks secrets detection.

---

## Known Limitations & Future Work

| Item | Status |
|------|--------|
| HTTPS in production | Requires deployment configuration — not in scope for local dev |
| Secrets manager integration | Documented; implementation is deployment-specific |
| Database backup | Documented in Phase 16 |
| Kafka message signing | Not implemented; considered low-risk for internal cluster |
| Account email verification | Not implemented; deferred post-MVP |
| CSRF protection | Not applicable — stateless JWT, no session cookies |
