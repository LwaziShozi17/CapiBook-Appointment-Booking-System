# CLAUDE_PROJECT_PLAN.md
## Capitec Appointment Booking System — Claude Code Execution Plan

> **Purpose:** Token-efficient, persistent execution plan for Claude Code sessions.
> Read this file at the start of every session to understand current state, constraints, and next task.
> The authoritative roadmap is `PROJECT_ROADMAP.md`. This file tracks execution state.

---

## CURRENT STATE (update after every session)

```
CURRENT PHASE:    Phase 3 — Availability & Time Slot Management
STATUS:           NOT STARTED
LAST COMPLETED:   Phase 2 — Branch & Banking Service Management
BLOCKERS:         None
NEXT TASK:        Implement PublicHoliday entity, AvailabilityService (slot generation engine), AvailabilityController
                  New migrations: V8__create_public_holidays.sql, V9__seed_south_african_holidays.sql
                  New packages: availability/, holiday/
                  Endpoint: GET /api/v1/availability?branchId=&serviceId=&date=
TEST STATUS:      86/86 passing (./mvnw test)
```

---

## ACTUAL TECHNOLOGY STACK (inspected 2026-09-01)

### Backend
| Component | Technology |
|-----------|-----------|
| Language | Java 21 |
| Framework | Spring Boot 4.1.1 |
| Web | Spring Web MVC |
| Security | Spring Security + JWT (JJWT 0.12.6) |
| Persistence | Spring Data JPA + Hibernate |
| Database | PostgreSQL (Flyway migrations) |
| Migrations | Flyway (V1__init, V2__create_users, V3__create_refresh_tokens exist) |
| Messaging | Spring Kafka |
| API Docs | SpringDoc OpenAPI 2.8.3 (Swagger UI at /swagger-ui.html) |
| Health | Spring Boot Actuator |
| Build | Maven (mvnw) |
| Test | JUnit 5, Mockito, MockMvc, Testcontainers (PostgreSQL + Kafka), JaCoCo |

### Frontend
| Component | Technology |
|-----------|-----------|
| Framework | React 19 |
| Build | Vite 8 |
| Language | TypeScript 6 |
| Router | React Router 7 |
| HTTP | Axios |
| Styling | Tailwind CSS 4 |
| Lint | oxlint |

### Root Package
`com.capitec.capibook`

### Key Paths
- Backend source: `src/main/java/com/capitec/capibook/`
- Resources: `src/main/resources/`
- Migrations: `src/main/resources/db/migration/`
- Frontend: `frontend/src/`
- Frontend API calls: `frontend/src/api/`
- Tests: `src/test/java/com/capitec/capibook/`

### Existing Migrations
- `V1__init.sql` — baseline
- `V2__create_users.sql` — User entity
- `V3__create_refresh_tokens.sql` — RefreshToken entity

### Existing Packages
- `auth/` — AuthController, AuthService, JwtService, JwtAuthenticationFilter, RefreshToken, RefreshTokenRepository + DTOs
- `user/` — User, UserController, UserService, UserRepository, UserDetailsServiceImpl, Role + DTOs
- `config/` — SecurityConfig, OpenApiConfig
- `common/` — ApiResponse
- `exception/` — GlobalExceptionHandler, DuplicateEmailException, InvalidTokenException, ResourceNotFoundException

---

## PHASE STATUS TABLE

| Phase | Title | Status |
|-------|-------|--------|
| 0 | Project Foundation | COMPLETE |
| 1 | Authentication & User Management | COMPLETE |
| 2 | Branch & Banking Service Management | NOT STARTED |
| 3 | Availability & Time Slot Management | NOT STARTED |
| 4 | Core Appointment Booking | NOT STARTED |
| 5 | Appointment Lifecycle Management | NOT STARTED |
| 6 | Kafka Event-Driven Architecture | NOT STARTED |
| 7 | Notifications & Communication | NOT STARTED |
| 8 | Admin Management & Dashboard | NOT STARTED |
| 9 | Customer Frontend | NOT STARTED |
| 10 | Admin Frontend | NOT STARTED |
| 11 | Testing & Quality Assurance | NOT STARTED |
| 12 | Docker & Environment Management | NOT STARTED |
| 13 | CI/CD Pipeline | NOT STARTED |
| 14 | Observability & Monitoring | NOT STARTED |
| 15 | Security Hardening | NOT STARTED |
| 16 | Production Readiness & Deployment | NOT STARTED |

---

## PHASE SUMMARIES (compact — full spec in PROJECT_ROADMAP.md)

### Phase 2 — Branch & Banking Service Management
**Goal:** Branch CRUD + BranchOperatingHours + BankingService CRUD + seed data
**New migrations:** V4__create_branches.sql, V5__create_branch_operating_hours.sql, V6__create_banking_services.sql, V7__seed_banking_services.sql
**New packages:** `branch/`, `servicecatalog/`
**Auth rules:** Write endpoints require SYSTEM_ADMIN; GET endpoints require any authenticated user
**Key constraint:** Soft delete only (active = false); never hard delete

### Phase 3 — Availability & Time Slot Management
**Goal:** Slot generation engine — given branch + service + date → list of AVAILABLE/BOOKED/UNAVAILABLE slots
**New migrations:** V8__create_public_holidays.sql, V9__seed_south_african_holidays.sql
**New packages:** `availability/`, `holiday/`
**Critical rule:** Backend is the sole source of slot availability; frontend only renders what it receives
**Endpoint:** `GET /api/v1/availability?branchId=&serviceId=&date=`

### Phase 4 — Core Appointment Booking
**Goal:** Atomic, concurrent-safe appointment creation
**New migrations:** V10__create_appointments.sql
**New packages:** `appointment/`
**Concurrency:** Must prevent double-booking; use DB unique constraint on (branch_id, appointment_date, start_time) + pessimistic or optimistic locking; document choice
**Coverage target:** ≥ 90% for appointment package; write a concurrency test

### Phase 5 — Appointment Lifecycle Management
**Goal:** Cancel, confirm, complete, no-show, reschedule + appointment history
**New migrations:** V11__create_appointment_history.sql
**State machine:** PENDING → CONFIRMED → COMPLETED/CANCELLED/NO_SHOW/RESCHEDULED
**Rescheduling:** Creates new PENDING appointment; marks original as RESCHEDULED

### Phase 6 — Kafka Event-Driven Architecture
**Goal:** Publish domain events to Kafka after DB commits; implement AuditConsumer
**Topics:** appointment.created, appointment.cancelled, appointment.confirmed, appointment.rescheduled, appointment.completed, appointment.no_show
**Critical rule:** Kafka is NEVER in the critical path of a booking; DB commits first, then event published
**Consider:** Transactional Outbox Pattern (document decision either way)

### Phase 7 — Notifications & Communication
**Goal:** NotificationConsumer → NotificationService → NotificationProvider interface
**Initial delivery:** Console/mock email provider; interface allows real provider without logic change
**Triggers:** Appointment created, confirmed, cancelled, rescheduled, reminder

### Phase 8 — Admin Management & Dashboard
**Goal:** Admin-only endpoints for appointment management, branch management, analytics, audit logs
**Security rule:** Every admin endpoint enforces role via Spring Security; frontend gating is UX only

### Phase 9 — Customer Frontend
**Goal:** React booking flow: service → branch → date → slot → confirm → success
**Slot visualisation:** AVAILABLE=green/selectable, BOOKED=red/not selectable, UNAVAILABLE=grey/not selectable
**Auth:** AuthContext + JWT via Axios interceptor + silent token refresh

### Phase 10 — Admin Frontend
**Goal:** Admin dashboard, appointment management, branch/service management, analytics, audit log pages

### Phase 11 — Testing & QA
**Goal:** Coverage audit and gap fill
**Targets:** Overall ≥ 80%, core booking 100%, auth ≥ 95%
**Must test:** Concurrent booking, holiday booking, terminal state transitions, Kafka consumer idempotency

### Phase 12 — Docker & Environment Management
**Goal:** `docker compose up --build` starts postgres + kafka + backend + frontend + kafka-ui
**No hard-coded secrets in any compose file**

### Phase 13 — CI/CD Pipeline
**Goal:** GitHub Actions for backend (test + coverage + OWASP + Docker + Trivy) and frontend (lint + test + build)

### Phase 14 — Observability & Monitoring
**Goal:** Actuator + Micrometer + Prometheus + Grafana dashboards; structured JSON logging in prod profile

### Phase 15 — Security Hardening
**Goal:** Rate limiting on auth endpoints, brute-force protection, HTTPS, security headers, OWASP + Trivy clean

### Phase 16 — Production Readiness & Deployment
**Goal:** All phases complete; README, API_DOCUMENTATION.md, ARCHITECTURE.md, SECURITY.md written; CI green

---

## TOKEN-EFFICIENCY RULES

### Rule 1 — One phase at a time
Never attempt multiple phases in one session.

### Rule 2 — Inspect before modifying
1. Identify relevant files
2. Read only necessary files
3. Explain change briefly
4. Implement
5. Run `./mvnw test` or focused test command
6. Report relevant results only

### Rule 3 — Avoid unnecessary reads
Never read: `node_modules/`, `target/`, `.git/`, `dist/`, lock files unless debugging build issues, large logs.
Use targeted `find` and `grep` instead of recursive directory dumps.

### Rule 4 — Avoid massive command output
Run `./mvnw test -pl . -Dtest=SpecificTest` for targeted tests rather than full suite when debugging.
When a test fails, report the failure message and stack trace — not thousands of lines of output.

### Rule 5 — Use this file as the persistent state
Do not rediscover architecture each session. Update the CURRENT STATE section above after every session.
Reference `PROJECT_ROADMAP.md` only when you need the full Definition of Done checklist for the current phase.

### Rule 6 — Update CURRENT STATE at end of every session
Update the block at the top of this file:
- CURRENT PHASE
- STATUS
- LAST COMPLETED
- BLOCKERS
- NEXT TASK
- TEST STATUS

### Rule 7 — Use /clear strategically
After a phase is complete: `/clear`, then start the next phase reading only this file.
If a session grows too large mid-phase: `/compact`, preserve objective + files changed + decisions + test results + next steps.

### Rule 8 — Commit after stable milestones
```bash
git status
git diff
git add <specific files>
git commit -m "feat: ..."
```
Never commit secrets. Never add AI attribution.

---

## SESSION HANDOFF FORMAT

Update this section at the end of every session:

```markdown
## SESSION HANDOFF

### Completed This Session
- ...

### Files Changed
- ...

### Tests
- ...

### Known Issues
- ...

### Architecture Decisions
- ...

### Next Task
- ...
```

---

## ARCHITECTURAL DECISIONS (confirmed, do not re-debate)

| Decision | Choice | Reason |
|----------|--------|--------|
| Build tool | Maven | Existing pom.xml |
| DB migrations | Flyway | Already configured |
| JWT | JJWT 0.12.6 | Already in pom.xml |
| Token storage | Access token in body; refresh token in body (document if changed) | Phase 1 choice |
| Admin creation | Seeded via Flyway migration; NOT via registration endpoint | Security requirement |
| Frontend styling | Tailwind CSS 4 | Already in package.json |
| All API calls | `frontend/src/api/` only; never inline in components | Established convention |
| Error responses | `ApiResponse<T>` wrapper via `GlobalExceptionHandler` | Already implemented |
| Soft deletes | `active = false` flag; never hard delete branches/services | Referential integrity |
| Kafka criticality | Never in booking critical path; DB commits first | Reliability requirement |

---

## DEFINITION OF DONE (applies to every phase)

A phase is complete ONLY when ALL of the following are true:
- Implementation complete
- Validation exists
- Error handling exists
- Relevant unit tests written and passing
- Relevant integration tests written and passing (where appropriate)
- Security implications reviewed
- `PROJECT_ROADMAP.md` phase status updated to COMPLETE
- This file's CURRENT STATE updated
- No unrelated files changed

---

## IMPORTANT CONSTRAINTS

- **No AI attribution** in any git commit, source file, or documentation
- **No secrets** committed to source control (use environment variables)
- **No hard-coded role assignment** — registration always assigns CUSTOMER
- **No business logic in controllers or repositories**
- **No field injection** — constructor injection only
- **No direct entity serialisation** — DTOs at all API boundaries
- **No frontend availability logic** — backend is sole source of truth
- **Never skip tests** to meet a deadline
- **Never modify applied Flyway migrations** — always create new ones
- **API prefix** — all endpoints under `/api/v1/`
