# Architecture — CapiBook

---

## System Overview

CapiBook is a **microservices-inspired monolith** with a React frontend, Spring Boot backend, PostgreSQL database, and Kafka event streaming. Designed for production-grade observability and security from day one.

```
┌─────────────────────────────────────────────────────────────┐
│                      End User                                │
└─────────────────────────────────────────────────────────────┘
                              │
                              ▼
        ┌─────────────────────────────────────────┐
        │   React Frontend (Vite + Tailwind)      │
        │   - Customer booking wizard             │
        │   - Admin dashboard & analytics         │
        │   - Real-time availability              │
        └─────────────────────────────────────────┘
                              │
                   HTTP (REST + JWT)
                              │
        ┌─────────────────────────────────────────┐
        │ Spring Boot Backend (Java 21)           │
        │ - Auth (JWT, brute-force)               │
        │ - Appointment CRUD + lifecycle          │
        │ - Availability engine                   │
        │ - Admin API                             │
        │ - Event publishing (Kafka)              │
        │ - Metrics (Prometheus)                  │
        └─────────────────────────────────────────┘
                    │              │
         ┌──────────┴──────────┬───┴──────────┐
         ▼                     ▼              ▼
    ┌────────────┐    ┌────────────┐   ┌──────────┐
    │ PostgreSQL │    │   Kafka    │   │ Actuator │
    │ capibook   │    │  cluster   │   │/Prometheus
    └────────────┘    └────────────┘   └──────────┘
         │                    │              │
         │                    ▼              │
         │            ┌─────────────────┐    │
         │            │ Event Consumers │    │
         │            │ - Notifications │    ▼
         │            │ - Audit logs    │  ┌────────┐
         │            │ - Analytics     │  │Grafana │
         │            └─────────────────┘  └────────┘
         │                    │
         └────────────────────┘
              (Flyway migrations)
```

---

## Technology Stack

### Backend
- **Framework**: Spring Boot 4.1.1 (Spring Security, Data JPA, Actuator)
- **Language**: Java 21
- **Database**: PostgreSQL 15 (Flyway migrations)
- **API Documentation**: SpringDoc OpenAPI (Swagger UI)
- **Testing**: JUnit 5, Mockito (262 tests, 96% instruction coverage)
- **Observability**: Micrometer + Prometheus, Logback + Logstash encoder, Spring Cloud Sleuth (future)
- **Security**: JWT (JJWT), BCrypt, Bucket4j (rate limiting)
- **Messaging**: Spring Kafka

### Frontend
- **Framework**: React 18 (TypeScript)
- **Build Tool**: Vite 5
- **Styling**: Tailwind CSS v4
- **Routing**: React Router v6
- **HTTP Client**: Axios (with refresh interceptor)
- **Testing**: Vitest (future; currently no test framework)
- **Linting**: oxlint (ESLint-compatible)

### DevOps & Infrastructure
- **Containerisation**: Docker (multi-stage builds)
- **Orchestration**: Docker Compose (local dev)
- **CI/CD**: GitHub Actions (backend, frontend, security workflows)
- **Code Quality**: JaCoCo (80% instruction coverage gate), SpotBugs, OWASP Dependency-Check, Trivy

---

## Data Model

### Core Entities

```
User
├── id (UUID, PK)
├── email (unique)
├── passwordHash (BCrypt)
├── role (ENUM: CUSTOMER, BRANCH_ADMIN, SYSTEM_ADMIN)
├── active (boolean, default true)
├── failed_login_attempts (int, for brute-force)
├── locked_until (timestamp, for lockout)
├── branch_id (FK, nullable — BRANCH_ADMIN only)
└── timestamps (createdAt, updatedAt)

Branch
├── id (UUID, PK)
├── name
├── address
├── phoneNumber
├── max_concurrent_appointments (int, default 1)
└── operating_hours (1:N relationship)

BranchOperatingHours
├── id (UUID, PK)
├── branch_id (FK)
├── dayOfWeek (ENUM)
└── startTime, endTime

BankingService
├── id (UUID, PK)
├── name
├── description
└── durationMinutes (int)

Appointment
├── id (UUID, PK)
├── referenceNumber (CAP-YYYY-XXXXX)
├── customer_id (FK → User)
├── branch_id (FK → Branch)
├── service_id (FK → BankingService)
├── appointmentDate (LocalDate)
├── startTime (LocalTime)
├── status (ENUM: PENDING, CONFIRMED, COMPLETED, CANCELLED, RESCHEDULED, NO_SHOW)
├── notes (String, max 500)
└── timestamps

AppointmentHistory
├── id (UUID, PK)
├── appointment_id (FK)
├── status (transition destination)
├── reason (String)
└── timestamp

PublicHoliday
├── id (UUID, PK)
├── date
├── name (e.g., "Christmas")
└── country

BranchAvailabilityException
├── id (UUID, PK)
├── branch_id (FK)
├── date (branch is closed on this date)
└── reason

AuditLog
├── id (UUID, PK)
├── action (String)
├── actor_id (nullable, User who triggered)
├── timestamp
└── details (JSON)

Notification
├── id (UUID, PK)
├── recipient_id (FK → User)
├── channel (EMAIL, SMS, IN_APP)
├── status (SENT, FAILED)
└── timestamp

RefreshToken
├── id (UUID, PK)
├── user_id (FK)
├── token (unique)
├── expiresAt
├── revoked (boolean)
```

---

## Key Design Decisions

### 1. JWT Authentication
**Choice:** Stateless JWT with refresh token rotation.

**Why:**
- Scales horizontally (no session store)
- Frontend can be a static SPA (no CORS issues)
- Refresh token rotation mitigates JWT compromise
- Refresh tokens revoked on logout or new login

**Trade-off:** Cannot instantaneously revoke access tokens (15-minute grace period acceptable for appointment booking).

### 2. Transactional Outbox Pattern (Deferred)
**Current State:** Events published via `@TransactionalEventListener(AFTER_COMMIT)`.

**Why This Approach:**
- Application events decouple from Kafka
- Events guaranteed to fire only after DB commit
- No duplicate handling (idempotent consumers accept re-delivered events)

**Limitation:** No transactional guarantee that events are sent. If Kafka is down, events are lost (acceptable for notifications/audit).

**Future:** Implement transactional outbox table with a separate publisher process for exactly-once semantics.

### 3. Pessimistic Locking (Appointment Slots)
**Choice:** `@Lock(PESSIMISTIC_WRITE)` on Branch row during `createAppointment`.

**Why:**
- Prevents race conditions on slot capacity
- Database enforces constraint (no application-level race windows)
- `@Transactional` ensures lock is held until commit

**Semantics:**
```java
@Lock(LockModeType.PESSIMISTIC_WRITE)
Optional<Branch> findById(UUID id);
```

### 4. Admin Branch Scoping (Phase 8)
**Choice:** Branch restriction enforced at service layer via `caller.getBranchId()`.

**Why:**
- Prevents SQL injection via branch parameter
- Consistent across all admin endpoints
- Easy to test and audit

### 5. Single React App (No Sub-apps)
**Choice:** Customer and admin routes in one Vite app under `/admin/*` prefix.

**Why:**
- Simpler build pipeline
- Shared component library
- Shared auth context
- No iframe embedding or cross-origin complexity

### 6. H2 for Testing (Not Testcontainers)
**Choice:** In-memory H2 in `MODE=PostgreSQL` for integration tests.

**Why:**
- Fast test startup (< 1 second)
- No Docker required
- Catches ~95% of SQL bugs (full compatibility with `MODE=PostgreSQL`)

**Limitation:** Sequence-generated IDs differ; UUIDs mitigate this. Testcontainers deferred to post-MVP ops improvement.

### 7. No Distributed Tracing (Yet)
**Choice:** Prometheus metrics only; no Jaeger/Datadog.

**Why:**
- Simpler instrumentation
- Metrics sufficient for portfolio system
- Single-process monolith (not yet microservices)

**Future:** Spring Cloud Sleuth + Jaeger for distributed tracing across services.

---

## API Design

### Request/Response Envelope

All responses wrapped in consistent envelope:

```json
{
  "success": true,
  "message": "Optional human-readable message",
  "data": { "actual": "payload" },
  "errors": { "field": "validation error" }
}
```

### Pagination
- Standard query params: `page`, `size`, `sort`
- Returns metadata: `totalElements`, `totalPages`, `currentPage`

### Status Codes
- 2xx: Success
- 400: Validation error
- 401: Authentication failure
- 403: Authorization failure
- 409: Conflict (duplicate, slot full)
- 422: Unprocessable (invalid state transition)
- 429: Rate limited
- 423: Account locked

### Rate Limiting (Phase 15)
- 10 req/min per IP on auth endpoints
- Bucket4j with sliding window
- Returns HTTP 429 with friendly message

---

## Security Model

### Authentication
- JWT (JJWT library, HS256, RS256 ready)
- Access token: 15 minutes
- Refresh token: 7 days, rotates on use, revoked on logout
- Brute-force protection: 5 attempts → 15 min lockout
- Rate limiting: 10 auth requests per minute per IP

### Authorization
- Role-based (ROLE_CUSTOMER, ROLE_BRANCH_ADMIN, ROLE_SYSTEM_ADMIN)
- `@PreAuthorize` on methods
- Branch scoping at service layer for BRANCH_ADMIN

### Transport
- TLS termination at load balancer (production)
- HSTS header (1 year, includeSubDomains)
- CSP header (restrictive)
- X-Content-Type-Options: nosniff

### Data Protection
- Passwords: BCrypt (strength 10)
- Sensitive fields: Not logged or exposed in payloads
- Audit trail: All mutations logged with actor and timestamp

---

## Observability

### Metrics (Prometheus)

| Metric | Source | Use |
|--------|--------|-----|
| `http_requests_total` | Actuator | Request rate, errors |
| `http_request_duration_seconds` | Actuator | Latency percentiles |
| `jvm_memory_used_bytes` | Actuator | Memory pressure |
| `jvm_threads_live` | Actuator | Thread pool health |
| `db_pool_active_connections` | HikariCP | Connection pool saturation |
| `appointments_booked_total` | Custom counter | Booking throughput |
| `appointments_cancelled_total` | Custom counter | Cancellation rate |

**Scrape interval:** 15 seconds  
**Retention:** Depends on Prometheus config (default: 15 days)

### Logging

**Development:** Colored console, human-readable  
**Production:** JSON (Logstash format)

Logs include:
- Request/response metadata (not sensitive data)
- Error stack traces (for debugging)
- Audit trail (user action, timestamp, details)

**Do NOT log:**
- JWT tokens or refresh tokens
- Passwords
- Full customer PII
- Sensitive booking details (log reference number instead)

### Health Checks
- `/actuator/health`: Overall system health
- Components: DB connection pool, Kafka connectivity, disk space
- Used by load balancers and monitoring systems

### Dashboards (Grafana)
Pre-configured dashboard `capibook-overview.json` includes:
- Application health status
- HTTP rate, latency, error rate (4xx, 5xx separately)
- JVM memory usage, GC activity
- Booking counter timeline
- Database connection pool
- Appointment throughput (bookings per minute)

---

## Testing Strategy

### Unit Tests (Mockito)
- Service layer logic (auth, appointment lifecycle)
- 60+ unit tests, 99% pass rate

### Integration Tests
- Controller layer (HTTP contract)
- Service + Repository (database interactions)
- Kafka consumers (event delivery)
- 200+ integration tests

### End-to-End (Manual)
- Customer booking flow
- Admin operations
- Authentication (success, failure, lockout, rate limit)

### Coverage
- **INSTRUCTION**: 96.0% (5,459 / 5,684)
- **BRANCH**: 86.6% (149 / 172)
- **LINE**: 96.2% (1,231 / 1,279)

### CI/CD Checks
- **Backend**: compile, test (JaCoCo 80% gate), SpotBugs, OWASP Dep-Check, Docker build, Trivy scan
- **Frontend**: oxlint, vite build
- **Weekly Security**: Full OWASP scan, Trivy filesystem, GitLeaks

---

## Performance & Scalability

### Load Testing (ApacheBench)

**Setup:** Backend running locally, PostgreSQL on localhost, Kafka embedded (test profile)

```bash
ab -n 1000 -c 100 http://localhost:8080/actuator/health
```

**Results (expected):**
- Throughput: ~500–800 req/sec (depending on machine)
- Latency p50: 50ms, p99: 150ms
- Success rate: 100% (health endpoint has no dependencies)

**Real-world (appointment booking):** Lower due to DB + business logic. Expect ~100–200 req/sec with concurrent user load.

### Database Tuning
- **Connection pool**: HikariCP, 20 max connections, 5 min idle
- **Index on**: `users.email`, `appointments.customer_id`, `appointments.branch_id`, `appointments.appointment_date`
- **No N+1 queries**: JPA eager loading where needed, explicit JOIN FETCHes

### Kafka Resilience
- **Partition replicas**: 1 (single broker acceptable for portfolio)
- **Consumer group**: Stable ID (`capibook-notification-group`)
- **Offset reset**: `earliest` (replay on restart)
- **Timeout**: 30 seconds per message

---

## Deployment Architecture

### Local Development
```
Docker Compose (5 services):
├── PostgreSQL (port 5432)
├── Kafka (port 9092)
├── Backend (port 8080)
├── Frontend (port 3000)
├── Prometheus (port 9090)
└── Grafana (port 3001)
```

### Production (High-level)
```
                  ┌──────────────┐
                  │   Browser    │
                  └──────┬───────┘
                         │ HTTPS
        ┌────────────────┴────────────────┐
        ▼                                  ▼
    ┌──────────────┐          ┌──────────────────┐
    │nginx (CDN)   │          │nginx (reverse    │
    │for frontend  │          │  proxy)          │
    └──────┬───────┘          └────────┬─────────┘
           │                           │ HTTP
           │    ┌──────────────────────┴────────────────┐
           │    ▼                                        ▼
        (S3) ┌─────────────────────┐      ┌──────────────────────┐
             │ Spring Boot App     │      │ Spring Boot App      │
             │ (load balanced)     │      │ (standby)            │
             └─────────┬───────────┘      └──────────┬───────────┘
                       │                             │
        ┌──────────────┴──────────────┬──────────────┘
        ▼                             ▼
    ┌────────────────────────┐   ┌────────────────────┐
    │ RDS PostgreSQL         │   │ Managed Kafka      │
    │ (read replicas)        │   │ (Confluent/AWS)    │
    └────────────────────────┘   └────────────────────┘
        │
        ▼
    ┌────────────────────────┐
    │ Automated Backups      │
    │ (daily snapshots)      │
    └────────────────────────┘

    ┌────────────────────────────────────────────┐
    │ Observability Stack (Outside k8s)          │
    ├────────────────────────────────────────────┤
    │ ├─ Prometheus (scrape `/metrics`)          │
    │ ├─ Grafana (dashboards)                    │
    │ ├─ CloudWatch/ELK (log aggregation)        │
    │ └─ PagerDuty (alerting)                    │
    └────────────────────────────────────────────┘
```

**Key Points:**
- Load balancer distributes requests across multiple backend instances
- Database and Kafka are managed services (reduces ops burden)
- Monitoring external to application (can alert if app is down)

---

## Disaster Recovery

### Recovery Time Objective (RTO)
**15 minutes** — time to restore full service after data loss

### Recovery Point Objective (RPO)
**1 hour** — maximum acceptable data loss

### Backup Strategy

**Database:**
- Daily automated snapshots (RDS automatic backup)
- Manual snapshot before major deployments
- Point-in-time recovery available for 7 days
- Test restore quarterly

**Configuration:**
- All config in version control (not in DB)
- Secrets stored in secrets manager (not in code)

**Restore Procedure:**
```bash
# 1. RDS console: Restore from snapshot (5–10 min)
# 2. Verify connectivity: psql -h new-endpoint.rds.amazonaws.com
# 3. Run Flyway: `mvn flyway:migrate` (idempotent, re-applies existing migrations)
# 4. Restart backend instances (load balancer routes traffic to healthy instances)
# 5. Verify health: curl https://api.capibook.com/actuator/health
```

---

## Known Limitations & Future Work

| Item | Current | Future |
|------|---------|--------|
| Transactional Outbox | In-memory events (accept loss) | Outbox table + separate publisher |
| Distributed Tracing | None | Spring Cloud Sleuth + Jaeger |
| End-to-End Encryption | HTTPS only | E2E data encryption at rest |
| Rate Limiting | IP-based | User quota-based (per-API-key) |
| Multi-region | Single region | Multi-region active-active |
| CQRS | Synchronous queries | Event-sourced read models |
| Testcontainers | Not used (Docker unstable) | Full container-based integration tests |

---

## References

- Spring Boot: https://spring.io/projects/spring-boot
- PostgreSQL: https://www.postgresql.org/docs/15/
- Kafka: https://kafka.apache.org/documentation/
- React: https://react.dev/
- Prometheus: https://prometheus.io/docs/
- Grafana: https://grafana.com/docs/
- OWASP Security: https://owasp.org/www-project-top-ten/
