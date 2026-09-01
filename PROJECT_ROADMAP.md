# CapiBook — Appointment Booking System
## Project Development Roadmap

> **This file is the single source of truth for the development of this application.**
>
> Every architectural decision, every phase, every definition of done, and every
> status update lives here. Do not silently change, skip, or reorder anything in
> this document without first identifying the conflict, explaining the proposed
> change, explaining why it is necessary, and receiving explicit approval.

---

## PROJECT STATUS

| Phase | Title                                 | Status      |
|-------|---------------------------------------|-------------|
| 0     | Project Foundation                    | COMPLETE    |
| 1     | Authentication & User Management      | COMPLETE    |
| 2     | Branch & Banking Service Management   | COMPLETE    |
| 3     | Availability & Time Slot Management   | COMPLETE    |
| 4     | Core Appointment Booking              | COMPLETE    |
| 5     | Appointment Lifecycle Management      | NOT STARTED |
| 6     | Kafka Event-Driven Architecture       | NOT STARTED |
| 7     | Notifications & Communication         | NOT STARTED |
| 8     | Admin Management & Dashboard          | NOT STARTED |
| 9     | Customer Frontend                     | NOT STARTED |
| 10    | Admin Frontend                        | NOT STARTED |
| 11    | Testing & Quality Assurance           | NOT STARTED |
| 12    | Docker & Environment Management       | NOT STARTED |
| 13    | CI/CD Pipeline                        | NOT STARTED |
| 14    | Observability & Monitoring            | NOT STARTED |
| 15    | Security Hardening                    | NOT STARTED |
| 16    | Production Readiness & Deployment     | NOT STARTED |

Valid status values: `NOT STARTED` | `IN PROGRESS` | `BLOCKED` | `COMPLETE`

A phase may only be marked **COMPLETE** when every item in its Definition of Done
has been individually verified — not because files exist and not because the code
compiles.

---

## EXISTING REPOSITORY — OBSERVATIONS & CONFLICTS

Before any implementation begins, the following discrepancies between the current
repository state and this roadmap must be resolved:

| # | Observation | Required Action |
|---|-------------|-----------------|
| 1 | `pom.xml` declares `java.version=17`, but Spring Boot 4.1.1 requires Java 21 as its minimum supported version. | Update `java.version` to `21` in `pom.xml` before Phase 0 begins. |
| 2 | Base package is `com.capitec.CapiBook` (capital `B`). Java package naming conventions require all-lowercase. | Rename to `com.capitec.capibook` during Phase 0. Existing class is only the bootstrap; rename is low-risk. |
| 3 | Dependencies present: `spring-boot-starter-data-jpa`, `spring-boot-starter-security`, `spring-boot-starter-webmvc`. Missing: PostgreSQL driver, Flyway, JWT library, Kafka, Spring Boot Actuator, OpenAPI/Swagger, Bean Validation starter. | Add all missing dependencies in Phase 0. |
| 4 | Test dependencies use non-standard artifact IDs (`spring-boot-starter-data-jpa-test`, etc.). The standard test dependency is `spring-boot-starter-test`. | Replace with `spring-boot-starter-test` in Phase 0; verify this is correct for Spring Boot 4.x. |
| 5 | No frontend directory exists. | Create `frontend/` during Phase 0. |
| 6 | `application.properties` contains only `spring.application.name`. | Expand during Phase 0 to include all required configuration keys. |
| 7 | No `PROJECT_ROADMAP.md` existed before this file was created. | N/A — resolved by this document. |

---

## 1. PROJECT OVERVIEW

**CapiBook** is a Capitec-inspired banking appointment booking system built as a
high-quality software engineering portfolio project. It is **not** an official
Capitec system.

The system allows:

**Customers to:**
- Register and log in
- Manage their profile
- Browse banking services
- Select a branch
- Select a date and available time slot
- Book, view, cancel, and reschedule appointments
- Receive appointment notifications

**Administrators to:**
- Manage branches, banking services, and appointments
- Manage branch availability and operating hours
- View users (where authorised)
- View analytics and audit logs

The project demonstrates professional junior-to-mid-level software engineering
practices across the full stack: secure backend API, event-driven messaging,
tested business logic, containerised infrastructure, CI/CD pipeline, and
observability.

---

## 2. TECHNOLOGY STACK

### Backend

| Technology | Purpose |
|------------|---------|
| Java 21 | Application language |
| Spring Boot 4.x | Application framework |
| Spring Web MVC | REST API layer |
| Spring Security | Authentication & authorisation |
| Spring Data JPA | Persistence layer |
| Spring Kafka | Event-driven messaging |
| Maven | Build tool |
| PostgreSQL | Relational database |
| Flyway | Database migration |
| JWT (JJWT or Nimbus JOSE+JWT) | Stateless authentication tokens |
| Bean Validation (Jakarta Validation) | Input validation |
| SpringDoc OpenAPI / Swagger | API documentation |
| Spring Boot Actuator | Health & metrics endpoints |

### Frontend

| Technology | Purpose |
|------------|---------|
| React | UI framework |
| Vite | Build tool & dev server |
| TypeScript | Language (preferred over plain JS for type safety) |
| React Router | Client-side routing |
| Axios | HTTP client |

A consistent styling solution must be identified and documented before any
frontend component is written. Do not mix CSS frameworks. Identify the approach
in Phase 0 and commit to it.

### Testing

| Technology | Purpose |
|------------|---------|
| JUnit 5 | Unit & integration test runner |
| Mockito | Mocking framework |
| Spring Boot Test | Spring context integration tests |
| MockMvc | Controller layer tests |
| Testcontainers | Isolated PostgreSQL & Kafka for integration tests |
| JaCoCo | Code coverage reporting |
| React Testing Library | Frontend component & integration tests |

### Infrastructure

| Technology | Purpose |
|------------|---------|
| Docker | Container runtime |
| Docker Compose | Local multi-service orchestration |
| GitHub Actions | CI/CD pipeline |
| Apache Kafka | Event streaming |
| PostgreSQL | Production & test database |
| Prometheus | Metrics collection |
| Grafana | Metrics dashboards |

### Quality & Security

| Tool | Purpose |
|------|---------|
| Qodana | Static code analysis |
| OWASP Dependency-Check | Dependency vulnerability scanning |
| Trivy | Container image vulnerability scanning |

---

## 3. ARCHITECTURAL PRINCIPLES

The following principles are **non-negotiable** throughout every phase:

- **SOLID** — Every class has a single responsibility; depend on abstractions.
- **DRY** — Shared logic is extracted; no copy-paste duplication.
- **KISS** — The simplest design that satisfies the requirement.
- **Separation of concerns** — Controller, Service, Repository are distinct layers.
- **Dependency inversion** — Depend on interfaces, not concrete implementations.
- **Constructor injection** — Never use field injection (`@Autowired` on fields).
- **Stateless JWT authentication** — No server-side session state.
- **RESTful API design** — Correct HTTP verbs, meaningful URIs, appropriate status codes.
- **DTO-based API contracts** — Entities are never serialised directly to API responses.
- **Centralised exception handling** — `@ControllerAdvice` handles all errors uniformly.
- **Input validation** — All external input is validated at the controller boundary.
- **Database migrations** — All schema changes go through Flyway; never hand-edit production schema.
- **Automated testing** — Every non-trivial behaviour has a test.
- **Secure configuration** — Secrets live in environment variables, never in source code.
- **Environment-based configuration** — `dev`, `test`, and `prod` profiles exist and are distinct.

### Synchronous Request Flow

```
HTTP Request
     ↓
Controller  (validates input, delegates to service, maps response DTO)
     ↓
Service     (business logic, orchestration, transaction boundaries)
     ↓
Repository  (database access via Spring Data JPA)
     ↓
PostgreSQL
```

### Asynchronous / Event-Driven Flow

```
Business Service
      ↓
PostgreSQL Transaction (committed first)
      ↓
Domain Event published
      ↓
Kafka Producer (fire-and-forget after DB commit)
      ↓
Kafka Topic
      ↓
Kafka Consumer
      ↓
Independent Processing (notification, audit, analytics)
```

**Critical rule:** Kafka is NEVER in the critical path of a booking transaction.
The database is the source of truth. Kafka delivers side-effects asynchronously.

---

## 4. DATABASE MIGRATION STRATEGY

All schema changes use Flyway versioned migrations under:

```
src/main/resources/db/migration/
```

Migration naming convention:

```
V1__create_users.sql
V2__create_roles.sql
V3__create_branches.sql
V4__create_banking_services.sql
V5__create_appointments.sql
V6__create_availability.sql
V7__create_public_holidays.sql
V8__create_audit_logs.sql
V9__create_refresh_tokens.sql
```

Rules:
- Never modify an already-applied migration.
- Schema changes after initial application always use a new migration file.
- Seed data (e.g. initial admin user, initial banking services) may live in a
  `R__` (repeatable) migration or a dedicated `V*__seed_*.sql` migration.

---

## 5. EXPECTED PROJECT STRUCTURE

### Repository Root

```
CapiBook-Appointment-Booking-System/
├── PROJECT_ROADMAP.md          ← This file
├── README.md                   ← Created later (Phase 16)
├── API_DOCUMENTATION.md        ← Created later
├── ARCHITECTURE.md             ← Created later
├── SECURITY.md                 ← Created later
├── docker-compose.yml          ← Phase 12
├── docker-compose.dev.yml      ← Phase 12
├── pom.xml
├── mvnw / mvnw.cmd
└── src/
    ├── main/
    │   ├── java/com/capitec/capibook/
    │   └── resources/
    │       ├── application.yml
    │       ├── application-dev.yml
    │       ├── application-test.yml
    │       ├── application-prod.yml
    │       └── db/migration/
    └── test/
        └── java/com/capitec/capibook/
```

### Backend Package Structure

```
com.capitec.capibook/
├── CapiBookApplication.java
│
├── config/           ← Spring Security config, JWT config, OpenAPI config, CORS
├── common/           ← Shared DTOs (ApiResponse, PageResponse), base entities
├── exception/        ← Global exception handler, custom exceptions
│
├── auth/             ← Registration, login, token refresh, logout
├── user/             ← User entity, UserService, UserRepository, UserController
│
├── branch/           ← Branch entity, CRUD, BranchController
├── servicecatalog/   ← BankingService entity, CRUD, ServiceController
│
├── availability/     ← Slot generation engine, AvailabilityController
├── holiday/          ← PublicHoliday entity, HolidayService
│
├── appointment/      ← Appointment entity, booking logic, AppointmentController
│
├── kafka/            ← KafkaProducerConfig, KafkaConsumerConfig, event DTOs
├── notification/     ← NotificationConsumer, NotificationService
├── audit/            ← AuditConsumer, AuditLog entity
│
└── admin/            ← Admin-specific endpoints, analytics, dashboard data
```

Do not create packages that have no classes yet.
Add packages as their phase begins.

### Frontend Structure

```
frontend/
├── index.html
├── vite.config.ts
├── package.json
├── tsconfig.json
└── src/
    ├── main.tsx
    ├── App.tsx
    │
    ├── api/            ← Axios client, all API call functions (NOT in components)
    ├── assets/         ← Images, fonts, static resources
    ├── components/     ← Reusable UI components
    ├── context/        ← React context (AuthContext, etc.)
    ├── hooks/          ← Custom React hooks
    ├── pages/          ← Page-level components (one per route)
    ├── routes/         ← Route definitions, ProtectedRoute wrapper
    ├── utils/          ← Helper functions (date formatting, validation, etc.)
    └── styles/         ← Global styles, theme variables
```

API calls must never be written inline inside components. All backend
communication lives in `src/api/`.

---

## 6. API VERSIONING

All backend endpoints are prefixed with `/api/v1/`. This prefix is applied at
the controller level and documented in OpenAPI.

---

## 7. PHASE GATE PROCESS

Every phase follows this gate:

```
Phase N begins
      ↓
Implementation (code, migrations, config)
      ↓
Unit Tests written and passing
      ↓
Integration Tests written and passing
      ↓
Security verification (for phases with security surface)
      ↓
Definition of Done verified — every checkbox ticked
      ↓
Phase N marked COMPLETE
      ↓
Phase N+1 may begin
```

If a gate fails, the phase status becomes `BLOCKED`:

```
Status: BLOCKED
Reason: <specific, concrete reason>
Required action: <specific action needed to unblock>
```

Do not proceed past a blocked gate.

---

## PHASE 0 — PROJECT FOUNDATION

**Status:** COMPLETE
**Depends on:** Nothing
**Blocks:** All subsequent phases

### Objective

Establish the technical foundation so that every later phase has a working,
testable, and deployable starting point.

### Backend Tasks

- [ ] Resolve all items in the **Existing Repository — Observations & Conflicts** table above.
- [ ] Update `java.version` to `21` in `pom.xml`.
- [ ] Rename base package from `com.capitec.CapiBook` to `com.capitec.capibook`.
- [ ] Add missing `pom.xml` dependencies:
  - PostgreSQL JDBC driver
  - Flyway Core
  - JJWT (access token + refresh token support) or Nimbus JOSE+JWT
  - Spring Boot Starter Validation (Bean Validation)
  - SpringDoc OpenAPI (Swagger UI)
  - Spring Boot Starter Actuator
  - Spring Kafka
  - Testcontainers (PostgreSQL + Kafka modules, test scope)
  - JaCoCo Maven plugin
- [ ] Replace non-standard test dependencies with `spring-boot-starter-test`.
- [ ] Convert `application.properties` to `application.yml`.
- [ ] Create `application-dev.yml` — local development profile.
- [ ] Create `application-test.yml` — test profile (in-memory or Testcontainers DB).
- [ ] Create `application-prod.yml` — production profile (all secrets from env vars).
- [ ] Configure PostgreSQL datasource (dev profile points to local DB; secrets via env vars).
- [ ] Configure Flyway (enabled, baseline-on-migrate for dev only).
- [ ] Configure Spring Boot Actuator — expose `/actuator/health` publicly.
- [ ] Create initial Flyway migration (`V0__baseline.sql` or empty `V1__init.sql`) so Flyway runs cleanly.
- [ ] Create `GlobalExceptionHandler` skeleton (`@ControllerAdvice`) in `exception/`.
- [ ] Create `ApiResponse<T>` wrapper DTO in `common/`.
- [ ] Configure OpenAPI / Swagger with title, version, and security scheme placeholder.
- [ ] Verify the application starts (`mvnw spring-boot:run -Dspring-boot.run.profiles=dev`).

### Frontend Tasks

- [ ] Create `frontend/` directory with Vite + React + TypeScript scaffold.
- [ ] Install React Router and Axios.
- [ ] Create `src/api/apiClient.ts` — Axios instance with base URL from env var.
- [ ] Create `.env.development` pointing to `http://localhost:8080/api/v1`.
- [ ] Create basic `App.tsx` with placeholder route structure.
- [ ] Confirm the frontend dev server starts (`npm run dev`).
- [ ] Document the chosen styling solution (e.g. Tailwind CSS, CSS Modules, MUI).

### Definition of Done

```
[ ] pom.xml declares Java 21
[ ] Base package is com.capitec.capibook (all lowercase)
[ ] All required dependencies are present in pom.xml
[ ] Application starts without errors (dev profile)
[ ] PostgreSQL connection is established on startup
[ ] Flyway executes without errors on startup
[ ] /actuator/health returns HTTP 200
[ ] Swagger UI is accessible at /swagger-ui.html or /swagger-ui/index.html
[ ] application-dev.yml, application-test.yml, application-prod.yml exist
[ ] No secrets are committed to source control
[ ] Frontend dev server starts without errors
[ ] Frontend can reach backend (CORS configured for dev)
[ ] At least one passing test exists (the default Spring Boot context test)
[ ] mvnw test completes without failures
```

---

## PHASE 1 — AUTHENTICATION & USER MANAGEMENT

**Status:** COMPLETE
**Depends on:** Phase 0 COMPLETE
**Blocks:** Phase 2 and all subsequent phases

### Objective

Implement secure, stateless JWT-based authentication and user management.
Every later phase's security model depends on this phase being correct.

### Entities

**User**
```
id              UUID / Long (PK)
email           VARCHAR UNIQUE NOT NULL
passwordHash    VARCHAR NOT NULL
firstName       VARCHAR NOT NULL
lastName        VARCHAR NOT NULL
phoneNumber     VARCHAR
role            ENUM (CUSTOMER, BRANCH_ADMIN, SYSTEM_ADMIN)
active          BOOLEAN DEFAULT TRUE
createdAt       TIMESTAMP
updatedAt       TIMESTAMP
```

**RefreshToken**
```
id          UUID / Long (PK)
token       VARCHAR UNIQUE NOT NULL
user        FK → User
expiresAt   TIMESTAMP NOT NULL
revoked     BOOLEAN DEFAULT FALSE
createdAt   TIMESTAMP
```

### Roles

| Role | Description |
|------|-------------|
| `CUSTOMER` | Default role assigned to all self-registered users |
| `BRANCH_ADMIN` | Manages a specific branch; assigned by `SYSTEM_ADMIN` |
| `SYSTEM_ADMIN` | Full system access; seeded via migration, never self-registered |

**Critical security rule:** The public registration endpoint MUST hard-code
`role = CUSTOMER`. The request body MUST NOT contain a `role` field. Any attempt
to self-assign an elevated role must be silently ignored or explicitly rejected.

### JWT Configuration

| Token | Lifetime | Storage |
|-------|----------|---------|
| Access Token | ~15 minutes | HTTP response body; sent in `Authorization: Bearer` header |
| Refresh Token | ~7 days | HTTP-only cookie OR response body (document chosen approach) |

Passwords must NEVER:
- Appear in any log statement
- Be returned in any API response
- Be stored in plain text

### Endpoints

```
POST /api/v1/auth/register       → Public; creates CUSTOMER account
POST /api/v1/auth/login          → Public; returns access + refresh tokens
POST /api/v1/auth/refresh        → Public (with valid refresh token); returns new access token
POST /api/v1/auth/logout         → Authenticated; revokes refresh token

GET  /api/v1/users/me            → CUSTOMER, BRANCH_ADMIN, SYSTEM_ADMIN; own profile
PUT  /api/v1/users/me            → CUSTOMER, BRANCH_ADMIN, SYSTEM_ADMIN; update own profile
```

### Implementation Checklist

- [ ] `User` entity and Flyway migration
- [ ] `RefreshToken` entity and Flyway migration
- [ ] `UserRepository` (Spring Data JPA)
- [ ] `RefreshTokenRepository`
- [ ] `UserService` — registration, profile retrieval, profile update
- [ ] `AuthService` — login, token generation, refresh, logout
- [ ] `JwtService` — token creation, validation, claims extraction
- [ ] `JwtAuthenticationFilter` — intercepts requests, validates JWT
- [ ] `SecurityConfig` — configures SecurityFilterChain, CORS, CSRF, public/protected routes
- [ ] `AuthController` — `/api/v1/auth/*`
- [ ] `UserController` — `/api/v1/users/me`
- [ ] Request DTOs: `RegisterRequest`, `LoginRequest`, `UpdateProfileRequest`
- [ ] Response DTOs: `AuthResponse`, `UserProfileResponse`
- [ ] BCrypt password encoder (via Spring Security `PasswordEncoder`)
- [ ] Registration endpoint assigns `CUSTOMER` role unconditionally
- [ ] `GlobalExceptionHandler` handles authentication errors (401, 403)
- [ ] Flyway migrations: `V1__create_users.sql`, `V2__create_refresh_tokens.sql`

### Tests Required

- [ ] `UserServiceTest` — unit tests for registration, duplicate email, profile update
- [ ] `AuthServiceTest` — unit tests for login, token refresh, logout
- [ ] `JwtServiceTest` — token creation, validation, expiry, tampered token
- [ ] `AuthControllerTest` — MockMvc tests for all auth endpoints
- [ ] `UserControllerTest` — MockMvc tests for profile endpoints
- [ ] Integration test: full register → login → access protected endpoint → refresh → logout flow
- [ ] Security test: unauthenticated access returns 401
- [ ] Security test: wrong role returns 403
- [ ] Security test: customer cannot register as SYSTEM_ADMIN

### Definition of Done

```
[ ] Registration creates a CUSTOMER account with BCrypt-hashed password
[ ] Duplicate email registration returns 409 Conflict
[ ] Login returns a valid JWT access token
[ ] Login returns a valid refresh token
[ ] Expired access token is rejected (401)
[ ] Tampered JWT is rejected (401)
[ ] Refresh token endpoint issues a new access token
[ ] Logout revokes the refresh token
[ ] Protected endpoints return 401 without a token
[ ] Protected endpoints return 403 with insufficient role
[ ] Customer cannot create a BRANCH_ADMIN or SYSTEM_ADMIN via registration
[ ] Profile retrieval returns correct data (no password field)
[ ] Profile update persists changes
[ ] No password appears in any log or API response
[ ] All unit tests pass
[ ] All integration tests pass
[ ] All security tests pass
[ ] Code coverage ≥ 80% for auth package
```

---

## PHASE 2 — BRANCH & BANKING SERVICE MANAGEMENT

**Status:** NOT STARTED
**Depends on:** Phase 1 COMPLETE
**Blocks:** Phase 3

### Objective

Build the business catalogue: branches and banking services. These are the
reference data that the availability engine and appointment booking depend on.

### Branch Entity

```
id              UUID / Long (PK)
branchCode      VARCHAR UNIQUE NOT NULL
name            VARCHAR NOT NULL
address         VARCHAR NOT NULL
city            VARCHAR NOT NULL
province        VARCHAR NOT NULL
postalCode      VARCHAR NOT NULL
latitude        DECIMAL
longitude       DECIMAL
phoneNumber     VARCHAR
email           VARCHAR
active          BOOLEAN DEFAULT TRUE
createdAt       TIMESTAMP
updatedAt       TIMESTAMP
```

Branch Operating Hours are stored separately (one record per day of week per branch):

```
id              PK
branch          FK → Branch
dayOfWeek       ENUM (MONDAY..SUNDAY)
openTime        TIME
closeTime       TIME
isClosed        BOOLEAN DEFAULT FALSE
```

### Banking Service Entity

```
id                  UUID / Long (PK)
name                VARCHAR UNIQUE NOT NULL
description         TEXT
durationMinutes     INTEGER NOT NULL
active              BOOLEAN DEFAULT TRUE
createdAt           TIMESTAMP
updatedAt           TIMESTAMP
```

**Banking services to seed (these come from the database, NOT hard-coded in React):**

| Service | Duration |
|---------|----------|
| Card Collection | 15 min |
| Card Replacement | 20 min |
| Account Assistance | 30 min |
| Banking App Assistance | 20 min |
| Credit Application Consultation | 45 min |
| Credit Account Enquiries | 30 min |
| Credit Check Assistance | 30 min |
| Savings Consultation | 45 min |
| Insurance Consultation | 45 min |
| Funeral Plan Consultation | 30 min |
| International Banking Assistance | 30 min |
| Digital Banking Assistance | 20 min |

### Endpoints

```
# Branch — SYSTEM_ADMIN only for write operations
POST   /api/v1/branches
GET    /api/v1/branches
GET    /api/v1/branches/{id}
PUT    /api/v1/branches/{id}
DELETE /api/v1/branches/{id}   (soft delete: set active = false)

# Branch Operating Hours
PUT    /api/v1/branches/{id}/operating-hours

# Banking Services — SYSTEM_ADMIN only for write operations
POST   /api/v1/services
GET    /api/v1/services
GET    /api/v1/services/{id}
PUT    /api/v1/services/{id}
DELETE /api/v1/services/{id}   (soft delete: set active = false)
```

Authenticated customers can call the `GET` (read) endpoints to browse branches
and services.

### Flyway Migrations

- `V3__create_branches.sql`
- `V4__create_branch_operating_hours.sql`
- `V5__create_banking_services.sql`
- `V6__seed_banking_services.sql`

### Definition of Done

```
[ ] Branch CRUD endpoints implemented and tested
[ ] Branch soft delete implemented (active = false, not hard delete)
[ ] Branch operating hours stored and retrievable per branch
[ ] Branch validation: required fields, valid province, valid postal code
[ ] Banking service CRUD implemented and tested
[ ] Banking service soft delete implemented
[ ] Service duration stored and validated (> 0 minutes)
[ ] Services seeded via Flyway migration
[ ] Authorisation enforced: write endpoints require SYSTEM_ADMIN
[ ] Read endpoints accessible to authenticated users
[ ] Flyway migrations execute cleanly
[ ] Unit tests pass
[ ] Integration tests pass
```

---

## PHASE 3 — AVAILABILITY & TIME SLOT MANAGEMENT

**Status:** NOT STARTED
**Depends on:** Phase 2 COMPLETE
**Blocks:** Phase 4

### Objective

Build the availability engine. Given a branch, a service, and a date, generate
the set of appointment time slots and their availability status.

### Availability Logic

The engine must consider **all** of the following before marking a slot available:

1. Branch operating hours for the requested day of week
2. Banking service duration (slots are spaced by duration)
3. Existing confirmed/pending appointments that occupy a slot
4. Public holidays (branch closed)
5. Weekends (unless the branch is open on that day)
6. Branch capacity (maximum concurrent appointments)
7. Time zones (store all times as UTC; display in branch's local time zone)
8. Manually configured unavailable periods (e.g. branch maintenance)

### Slot Status Values

```
AVAILABLE    → Customer may book this slot
BOOKED       → Slot is already occupied
UNAVAILABLE  → Slot cannot be booked (holiday, closed, outside hours)
```

### Example

```
Branch operating hours: Monday 08:00–17:00
Service duration: 30 minutes

Generated slots for a Monday:
  08:00 → AVAILABLE
  08:30 → AVAILABLE
  09:00 → BOOKED (existing appointment)
  09:30 → AVAILABLE
  ...
  16:30 → AVAILABLE
  17:00 → Not generated (service would end at 17:30, after closing)
```

### Public Holidays Entity

```
id          PK
date        DATE UNIQUE NOT NULL
name        VARCHAR NOT NULL
description VARCHAR
```

Flyway migration: `V7__create_public_holidays.sql` + `V8__seed_south_african_holidays.sql`

### Endpoint

```
GET /api/v1/availability?branchId={id}&serviceId={id}&date={YYYY-MM-DD}

Response:
{
  "branchId": "...",
  "serviceId": "...",
  "date": "2025-03-15",
  "slots": [
    { "startTime": "08:00", "endTime": "08:30", "status": "AVAILABLE" },
    { "startTime": "08:30", "endTime": "09:00", "status": "BOOKED" },
    ...
  ]
}
```

**The frontend must NOT determine slot availability.** The backend response is
the definitive source of truth. The frontend renders what the backend returns.

### Definition of Done

```
[ ] Slot generation produces correct slots for operating hours + service duration
[ ] Slots end before branch closing time (no overflow past closing)
[ ] Existing confirmed/pending appointments mark slots as BOOKED
[ ] Public holidays mark all slots as UNAVAILABLE
[ ] Days the branch is closed mark all slots as UNAVAILABLE
[ ] Branch capacity limit is respected
[ ] Time zone handling is correct
[ ] Edge cases tested: last slot of day, holiday on booking date, branch closed that day
[ ] Unit tests cover slot generation logic exhaustively
[ ] Integration tests verify the full availability query against a test database
[ ] No slot availability decision is made in the frontend
```

---

## PHASE 4 — CORE APPOINTMENT BOOKING

**Status:** NOT STARTED
**Depends on:** Phase 3 COMPLETE
**Blocks:** Phase 5

### Objective

Implement the central booking transaction. This is the most critical piece of
business logic in the system.

### Appointment Entity

```
id              UUID / Long (PK)
customer        FK → User
branch          FK → Branch
service         FK → BankingService
appointmentDate DATE NOT NULL
startTime       TIME NOT NULL
endTime         TIME NOT NULL
status          ENUM (PENDING, CONFIRMED, CANCELLED, COMPLETED, NO_SHOW, RESCHEDULED)
referenceNumber VARCHAR UNIQUE NOT NULL (generated, e.g. CAP-2025-XXXXX)
notes           TEXT
createdAt       TIMESTAMP
updatedAt       TIMESTAMP
```

Flyway migration: `V9__create_appointments.sql`

### Booking Flow

```
POST /api/v1/appointments
      ↓
Authenticate request (JWT required)
      ↓
Validate request body (Bean Validation)
      ↓
Verify branch exists and is active
      ↓
Verify service exists and is active
      ↓
Verify appointment date is in the future
      ↓
Verify branch is open on that date (not holiday, not closed that day)
      ↓
Verify slot is still available (re-check, not trust frontend)
      ↓
Acquire row-level lock or use optimistic locking to prevent double booking
      ↓
Create appointment record (status = PENDING or CONFIRMED)
      ↓
Commit transaction
      ↓
Return appointment confirmation DTO
```

### Endpoints

```
POST /api/v1/appointments               → CUSTOMER only; creates a new appointment
GET  /api/v1/appointments/{id}          → Owner, BRANCH_ADMIN (own branch), SYSTEM_ADMIN
GET  /api/v1/appointments/my            → CUSTOMER; returns own appointments (paginated)
```

### Concurrency & Double-Booking Prevention

**This is non-negotiable.** Two customers must not be able to book the same slot
concurrently. Implement one of:

- Database-level unique constraint on `(branch_id, appointment_date, start_time)` combined with service-layer check-then-insert in a serialised transaction.
- Pessimistic locking (`SELECT … FOR UPDATE`) on a slot-level record.
- Optimistic locking with a `@Version` field and retry handling.

Document the chosen approach and why.

Do not rely on frontend validation. Do not assume requests arrive serially.
Write a concurrency test that demonstrates two simultaneous requests and verifies
only one succeeds.

### Validation Checklist

```
[ ] User is authenticated
[ ] Branch ID provided and exists
[ ] Branch is active
[ ] Service ID provided and exists
[ ] Service is active
[ ] Appointment date is in the future (not today or earlier)
[ ] Branch is open on the requested date
[ ] Date is not a public holiday
[ ] Requested time slot is within branch operating hours
[ ] Slot is AVAILABLE (re-checked on server, not trusted from frontend)
[ ] Customer does not already have a booking at the same date+time
[ ] Slot is not double-booked by concurrent request
```

### Definition of Done

```
[ ] Appointment creation endpoint works end-to-end
[ ] Appointment reference number is generated and unique
[ ] All validation checks enforced
[ ] Confirmed appointment is retrievable by ID
[ ] Customer can list their own appointments
[ ] BRANCH_ADMIN cannot see appointments from another branch
[ ] Double-booking is prevented under concurrent requests (verified by test)
[ ] Transaction boundaries are correct (no partial writes)
[ ] Unit tests cover all validation paths
[ ] Integration tests cover the full booking flow
[ ] Concurrency test demonstrates single-booking guarantee
[ ] Code coverage ≥ 90% for appointment package
```

---

## PHASE 5 — APPOINTMENT LIFECYCLE MANAGEMENT

**Status:** NOT STARTED
**Depends on:** Phase 4 COMPLETE
**Blocks:** Phase 6

### Objective

Implement the full appointment state machine and allow customers and admins to
manage the lifecycle of appointments.

### Status State Machine

```
PENDING ──────────────────────→ CONFIRMED
   │                                │
   ↓                                ↓
CANCELLED               CANCELLED / COMPLETED / NO_SHOW
                                    │
                              RESCHEDULED (creates new PENDING appointment)
```

Valid transitions:

| From | To | Who |
|------|----|-----|
| PENDING | CONFIRMED | BRANCH_ADMIN, SYSTEM_ADMIN |
| PENDING | CANCELLED | CUSTOMER (own), BRANCH_ADMIN, SYSTEM_ADMIN |
| CONFIRMED | CANCELLED | CUSTOMER (own, with notice period), BRANCH_ADMIN, SYSTEM_ADMIN |
| CONFIRMED | COMPLETED | BRANCH_ADMIN, SYSTEM_ADMIN |
| CONFIRMED | NO_SHOW | BRANCH_ADMIN, SYSTEM_ADMIN |
| CONFIRMED | RESCHEDULED | CUSTOMER (own), BRANCH_ADMIN, SYSTEM_ADMIN |
| Any terminal | Any | Not allowed |

Terminal states: `CANCELLED`, `COMPLETED`, `NO_SHOW`

### Appointment History Entity

```
id                  PK
appointment         FK → Appointment
previousStatus      ENUM
newStatus           ENUM
changedBy           FK → User
changeReason        VARCHAR
changedAt           TIMESTAMP
```

Flyway migration: `V10__create_appointment_history.sql`

### Endpoints

```
PATCH /api/v1/appointments/{id}/cancel
PATCH /api/v1/appointments/{id}/confirm
PATCH /api/v1/appointments/{id}/complete
PATCH /api/v1/appointments/{id}/no-show
PATCH /api/v1/appointments/{id}/reschedule
GET   /api/v1/appointments/{id}/history
```

### Access Rules

```
CUSTOMER       → May cancel or reschedule own appointments only
BRANCH_ADMIN   → May manage appointments at their authorised branch only
SYSTEM_ADMIN   → System-wide access to all appointments
```

### Rescheduling

Rescheduling creates a new `PENDING` appointment and marks the original as
`RESCHEDULED`. This preserves history and triggers a new availability check for
the new slot.

### Definition of Done

```
[ ] Cancel endpoint works for CUSTOMER (own appointment)
[ ] Cancel endpoint works for BRANCH_ADMIN (own branch)
[ ] Cancel endpoint works for SYSTEM_ADMIN
[ ] Customer cannot cancel another customer's appointment (403)
[ ] Confirm endpoint works for BRANCH_ADMIN and SYSTEM_ADMIN
[ ] Complete endpoint works for BRANCH_ADMIN and SYSTEM_ADMIN
[ ] No-show endpoint works for BRANCH_ADMIN and SYSTEM_ADMIN
[ ] Reschedule endpoint creates new appointment and marks original RESCHEDULED
[ ] Invalid status transitions are rejected with a clear error
[ ] Terminal state cannot be transitioned
[ ] Appointment history is recorded for every status change
[ ] History is retrievable via GET endpoint
[ ] Unit tests cover all transition paths
[ ] Integration tests cover lifecycle end-to-end
```

---

## PHASE 6 — KAFKA EVENT-DRIVEN ARCHITECTURE

**Status:** NOT STARTED
**Depends on:** Phase 5 COMPLETE
**Blocks:** Phase 7

### Objective

Introduce Kafka to decouple side-effects (notifications, audit, analytics) from
the core booking transaction.

### Why Kafka Is Introduced Here

Kafka is introduced only after the core synchronous booking flow is proven to work.
Reasons:
1. Kafka is not required for correctness of the booking transaction.
2. Adding Kafka before the core flow exists would couple system stability to
   Kafka availability.
3. The transactional outbox pattern (see below) ensures events are never lost
   even if Kafka is temporarily unavailable.

### Core Rule

**Kafka is NEVER in the critical path of a booking.**

```
Correct:
  HTTP Request → DB Transaction commits → Event published to Kafka

Wrong:
  HTTP Request → Kafka publish → if Kafka is down → booking fails
```

### Topics

| Topic | Published by | Consumed by |
|-------|-------------|-------------|
| `appointment.created` | AppointmentService | NotificationConsumer, AuditConsumer, AnalyticsConsumer |
| `appointment.cancelled` | AppointmentService | NotificationConsumer, AuditConsumer |
| `appointment.confirmed` | AppointmentService | NotificationConsumer, AuditConsumer |
| `appointment.rescheduled` | AppointmentService | NotificationConsumer, AuditConsumer |
| `appointment.completed` | AppointmentService | AuditConsumer, AnalyticsConsumer |
| `appointment.no_show` | AppointmentService | AuditConsumer, AnalyticsConsumer |

### Event DTO Example

```json
{
  "eventId": "uuid",
  "eventType": "APPOINTMENT_CREATED",
  "occurredAt": "2025-03-15T09:00:00Z",
  "appointmentId": "...",
  "customerId": "...",
  "branchId": "...",
  "serviceId": "...",
  "appointmentDate": "2025-03-20",
  "startTime": "10:00",
  "referenceNumber": "CAP-2025-00042"
}
```

### Transactional Outbox Pattern

To guarantee events are never lost, consider implementing the **Transactional
Outbox Pattern**:

1. Within the same database transaction that creates/updates the appointment,
   write an `OutboxEvent` record to an `outbox_events` table.
2. A background poller reads unprocessed `OutboxEvent` records and publishes them
   to Kafka.
3. Once published, mark the `OutboxEvent` as processed.

This ensures: if the DB transaction commits, the event will eventually be
published. If the DB transaction rolls back, no event is published.

Document whether this pattern is implemented or deferred.

### Implementation Checklist

- [ ] Add Kafka producer configuration
- [ ] Add Kafka consumer configuration
- [ ] Define event DTO classes in `kafka/events/`
- [ ] Publish `AppointmentCreatedEvent` from `AppointmentService` (after DB commit)
- [ ] Publish events for all other lifecycle transitions
- [ ] `NotificationConsumer` skeleton (used in Phase 7)
- [ ] `AuditConsumer` skeleton (logs to `audit_logs` table)
- [ ] `AnalyticsConsumer` skeleton (placeholder for Phase 8)
- [ ] Kafka topics configured in `application.yml`
- [ ] `docker-compose.dev.yml` includes Kafka + Zookeeper (or KRaft)

### Definition of Done

```
[ ] Kafka producer sends events after DB transaction commits
[ ] Kafka consumers receive events
[ ] AuditConsumer writes to audit_logs table
[ ] Kafka failure does NOT prevent appointment creation
[ ] Events are not lost (outbox pattern documented; implementation decision recorded)
[ ] Kafka integration tests pass (using Testcontainers Kafka)
[ ] Consumer group IDs are stable and documented
```

---

## PHASE 7 — NOTIFICATIONS & COMMUNICATION

**Status:** NOT STARTED
**Depends on:** Phase 6 COMPLETE
**Blocks:** Phase 8

### Objective

Implement event-driven notification delivery for appointment lifecycle events.

### Notification Triggers

| Event | Notification Sent |
|-------|-------------------|
| Appointment Created | Booking confirmation |
| Appointment Confirmed | Confirmation notice |
| Appointment Cancelled | Cancellation notice |
| Appointment Rescheduled | Rescheduling confirmation |
| Appointment Reminder | Reminder (e.g. 24h before appointment) |

### Channels (Phased)

| Channel | Phase |
|---------|-------|
| Email (mock / console log in dev) | Phase 7 |
| Real email provider (e.g. SendGrid, JavaMail) | Phase 7 or defer |
| SMS | Defer to later |
| Push notification | Defer to later |

Initial implementation may use a console-logging mock. Document the abstraction
so a real provider can be wired in without changing business logic.

### Architecture

```
Kafka Topic
      ↓
NotificationConsumer (Kafka listener)
      ↓
NotificationService (selects channel, builds message)
      ↓
NotificationProvider (interface)
      ↓
EmailNotificationProvider / MockNotificationProvider
```

### Notification Record Entity (optional but recommended)

```
id              PK
appointment     FK → Appointment
user            FK → User
channel         ENUM (EMAIL, SMS, PUSH)
status          ENUM (PENDING, SENT, FAILED)
sentAt          TIMESTAMP
errorMessage    VARCHAR
```

### Definition of Done

```
[ ] Kafka consumer processes appointment events
[ ] Notification service dispatches notifications for all trigger events
[ ] Email (or mock) notification sent on appointment creation
[ ] Notification interface allows real provider to be wired in without logic changes
[ ] Notification failures do not affect core booking transaction
[ ] Tests verify notification dispatch (using mock provider)
```

---

## PHASE 8 — ADMIN MANAGEMENT & DASHBOARD

**Status:** NOT STARTED
**Depends on:** Phase 7 COMPLETE
**Blocks:** Phase 9

### Objective

Build the backend administration API used by the admin frontend in Phase 10.

### Features

**Branch Admin:**
- View and manage appointments at their authorised branch
- Confirm, complete, or mark appointments as no-show
- Update branch operating hours
- Set branch availability exceptions (closed dates, maintenance windows)

**System Admin:**
- All Branch Admin capabilities across all branches
- Manage branches (CRUD)
- Manage banking services (CRUD)
- Manage BRANCH_ADMIN accounts (create, deactivate)
- View all customers (read-only, no sensitive data)
- View audit logs
- View analytics/summary dashboard data

### Analytics Endpoints (SYSTEM_ADMIN only)

```
GET /api/v1/admin/analytics/appointments/summary
    → total booked, cancelled, completed, no-show per period

GET /api/v1/admin/analytics/branches/{id}/utilisation
    → slot utilisation rate per branch

GET /api/v1/admin/analytics/services/popularity
    → most-booked services
```

### Audit Log Entity

```
id          PK
actor       FK → User (who performed the action)
action      VARCHAR NOT NULL (e.g. "APPOINTMENT_CANCELLED")
entityType  VARCHAR (e.g. "Appointment")
entityId    VARCHAR
details     JSONB / TEXT
createdAt   TIMESTAMP
```

Flyway migration: `V11__create_audit_logs.sql`

### Critical Security Rule

**Backend authorization is security. Frontend authorization is UX.**

Never expose an admin endpoint that relies solely on the frontend hiding it.
Every admin endpoint must verify the caller's role via Spring Security.

### Definition of Done

```
[ ] BRANCH_ADMIN can manage appointments for their branch only
[ ] BRANCH_ADMIN cannot access another branch's appointments (403)
[ ] SYSTEM_ADMIN has access to all admin endpoints
[ ] Customer cannot reach any admin endpoint (403)
[ ] Audit log is written for all admin actions
[ ] Analytics endpoints return correct aggregated data
[ ] Branch availability exceptions can be created and respected by availability engine
[ ] All endpoints tested with correct and incorrect roles
```

---

## PHASE 9 — CUSTOMER FRONTEND

**Status:** NOT STARTED
**Depends on:** Phase 8 COMPLETE (all required backend APIs exist)
**Blocks:** Phase 10

### Objective

Build the customer-facing React application.

### Pages

| Page | Route | Auth Required |
|------|-------|---------------|
| Home / Landing | `/` | No |
| Login | `/login` | No |
| Register | `/register` | No |
| Profile | `/profile` | Yes (CUSTOMER) |
| Book Appointment | `/book` | Yes (CUSTOMER) |
| My Appointments | `/appointments` | Yes (CUSTOMER) |
| Appointment Detail | `/appointments/:id` | Yes (CUSTOMER) |
| Booking Confirmation | `/booking-confirmation` | Yes (CUSTOMER) |

### Booking Flow (multi-step)

```
Step 1: Choose Service (list from GET /api/v1/services)
      ↓
Step 2: Choose Branch (list from GET /api/v1/branches)
      ↓
Step 3: Choose Date (date picker)
      ↓
Step 4: Choose Time Slot (slots from GET /api/v1/availability)
      ↓
Step 5: Confirm Details
      ↓
Step 6: Submit (POST /api/v1/appointments)
      ↓
Step 7: Booking Confirmation Page
```

### Slot Visualisation

The UI must display time slots with these indicators:

| Status | Visual |
|--------|--------|
| `AVAILABLE` | Green / selectable |
| `BOOKED` | Red / not selectable |
| `UNAVAILABLE` | Grey / not selectable |

The frontend reads these statuses from the backend response. It does NOT compute
availability independently.

### Auth State

- `AuthContext` stores the current user and JWT.
- JWT is attached to every request via Axios interceptor.
- On 401 response, attempt token refresh; if refresh fails, redirect to login.
- Refresh token storage method (HTTP-only cookie or localStorage) must be
  consistent with the approach chosen in Phase 1 and documented.

### Protected Routes

A `ProtectedRoute` component wraps all routes that require authentication.
Unauthenticated access redirects to `/login`.

### Definition of Done

```
[ ] All pages render without errors
[ ] Booking flow completes end-to-end (service → branch → date → slot → confirm → success)
[ ] Available slots shown in green, booked in red, unavailable in grey
[ ] Login and registration work against the real backend
[ ] JWT is attached to all authenticated requests
[ ] Token refresh works transparently
[ ] Protected routes redirect unauthenticated users to login
[ ] Profile page displays and updates user data
[ ] My Appointments page lists the customer's appointments
[ ] Appointment detail page shows appointment information
[ ] Cancel and reschedule work from the appointment detail page
[ ] Frontend unit tests for components pass
[ ] Booking flow integration test passes
[ ] No API calls are made from inside UI component bodies (all calls in src/api/)
```

---

## PHASE 10 — ADMIN FRONTEND

**Status:** NOT STARTED
**Depends on:** Phase 9 COMPLETE
**Blocks:** Phase 11

### Objective

Build the admin React application (may be a separate route prefix or a separate
Vite app — document the chosen approach in Phase 0).

### Pages

| Page | Route | Role Required |
|------|-------|---------------|
| Admin Dashboard | `/admin` | BRANCH_ADMIN, SYSTEM_ADMIN |
| Appointment Management | `/admin/appointments` | BRANCH_ADMIN, SYSTEM_ADMIN |
| Branch Management | `/admin/branches` | SYSTEM_ADMIN |
| Service Management | `/admin/services` | SYSTEM_ADMIN |
| User Management | `/admin/users` | SYSTEM_ADMIN |
| Analytics | `/admin/analytics` | SYSTEM_ADMIN |
| Audit Logs | `/admin/audit` | SYSTEM_ADMIN |

### Authorization

Frontend shows/hides menu items based on role (UX).
Backend enforces role on every request (Security).

Never rely solely on frontend gating.

### Definition of Done

```
[ ] Admin dashboard loads for BRANCH_ADMIN and SYSTEM_ADMIN
[ ] Customer cannot access any /admin route
[ ] BRANCH_ADMIN sees only their branch's appointments
[ ] SYSTEM_ADMIN sees all data
[ ] Appointment management: confirm, complete, no-show, cancel work
[ ] Branch management: CRUD works for SYSTEM_ADMIN
[ ] Service management: CRUD works for SYSTEM_ADMIN
[ ] Analytics page displays summary data
[ ] Audit log page displays log entries
[ ] All admin routes are protected
```

---

## PHASE 11 — TESTING & QUALITY ASSURANCE

**Status:** NOT STARTED
**Depends on:** Phase 10 COMPLETE
**Blocks:** Phase 12

### Objective

Comprehensive quality gate before infrastructure, pipeline, and production work.
Testing is a continuous process — tests are written in every phase — but this
phase audits, fills gaps, and verifies coverage targets.

### Backend Coverage Targets

| Scope | Target |
|-------|--------|
| Overall | ≥ 80% |
| Portfolio quality | ≥ 90% |
| Core appointment booking logic | 100% |
| Auth & security logic | ≥ 95% |

### Test Categories Required

**Unit Tests:**
- All service classes
- JWT service
- Slot generation engine
- Appointment validation logic
- Status transition validation

**Controller Tests (MockMvc):**
- All endpoints (happy path + error paths)
- Auth header validation
- Role-based access

**Repository Tests:**
- Custom queries
- Concurrent booking scenario

**Integration Tests (Testcontainers):**
- Full register → login → book → cancel flow
- Availability query with real data
- Concurrent booking attempt

**Security Tests:**
- Unauthenticated requests return 401
- Insufficient role returns 403
- Tampered JWT returns 401
- Expired JWT returns 401

**Kafka Tests:**
- Event published after appointment creation
- Consumer processes event
- Kafka failure does not break booking

**Concurrency Tests:**
- Two simultaneous booking requests → only one succeeds

### Frontend Tests

- Component rendering (React Testing Library)
- Booking flow step navigation
- Protected route redirects unauthenticated user
- API error states render correctly
- Form validation messages render correctly

### Critical Scenarios

Every one of the following must have a test:

```
[ ] Duplicate appointment booking attempt
[ ] Concurrent booking attempt for same slot
[ ] Booking on a public holiday
[ ] Booking on a day the branch is closed
[ ] Booking a slot outside operating hours
[ ] Invalid JWT token
[ ] Expired JWT token
[ ] Unauthorized role access
[ ] Customer attempting admin endpoint
[ ] BRANCH_ADMIN accessing another branch
[ ] Cancelling an already-cancelled appointment
[ ] Transitioning from a terminal status
[ ] Kafka consumer processing duplicate event (idempotency)
[ ] Database connection failure handling
```

### Definition of Done

```
[ ] JaCoCo report generated: overall coverage ≥ 80%
[ ] Core booking logic: 100% coverage
[ ] All critical scenarios have tests
[ ] No test manipulates coverage (no empty assertions, no commented-out tests)
[ ] Frontend component tests pass
[ ] Frontend booking flow test passes
[ ] CI pipeline runs all tests and enforces coverage threshold
```

---

## PHASE 12 — DOCKER & ENVIRONMENT MANAGEMENT

**Status:** NOT STARTED
**Depends on:** Phase 11 COMPLETE
**Blocks:** Phase 13

### Objective

Containerise the full application stack so any developer can start all services
with a single command.

### Docker Compose Services

```yaml
services:
  postgres:        # PostgreSQL database
  kafka:           # Apache Kafka (KRaft mode preferred for simplicity)
  kafka-ui:        # Kafka UI (e.g. Provectus kafka-ui) for development
  backend:         # Spring Boot application
  frontend:        # React + Nginx
```

### Requirements

- Each service has a health check.
- All secrets (DB password, JWT secret) come from environment variables, never
  hard-coded in `docker-compose.yml`.
- Use a `.env` file for local development defaults; document that this file must
  NOT be committed if it contains real secrets.
- Persistent volumes for PostgreSQL data.
- Internal Docker network for inter-service communication.
- Backend does not start until PostgreSQL is healthy.

### Target Command

```bash
docker compose up --build
```

After this completes:
- Frontend accessible at `http://localhost:3000`
- Backend accessible at `http://localhost:8080`
- Swagger UI at `http://localhost:8080/swagger-ui/index.html`
- Kafka UI at `http://localhost:8085`

### Definition of Done

```
[ ] docker compose up --build completes without errors
[ ] All services start and pass their health checks
[ ] Frontend is accessible
[ ] Backend health endpoint returns 200
[ ] PostgreSQL is populated with Flyway migrations
[ ] Kafka is running and topics are created
[ ] Kafka UI is accessible
[ ] No secrets are hard-coded in docker-compose.yml
[ ] Persistent volume keeps data across container restarts
[ ] Full booking flow works in the containerised environment
```

---

## PHASE 13 — CI/CD PIPELINE

**Status:** NOT STARTED
**Depends on:** Phase 12 COMPLETE
**Blocks:** Phase 14

### Objective

Automate build, test, quality, and security checks on every push via GitHub Actions.

### Workflows

**`backend-ci.yml`** — triggered on push/PR to `main` and feature branches:
```
Checkout
      ↓
Set up Java 21
      ↓
Cache Maven dependencies
      ↓
Compile (mvn compile)
      ↓
Unit Tests (mvn test)
      ↓
Integration Tests (mvn verify)
      ↓
JaCoCo Coverage Report
      ↓
Enforce coverage threshold (build fails if below threshold)
      ↓
Static Analysis (Qodana or SpotBugs)
      ↓
OWASP Dependency-Check
      ↓
Docker build (backend image)
      ↓
Trivy container scan
```

**`frontend-ci.yml`** — triggered on push/PR:
```
Checkout
      ↓
Setup Node.js
      ↓
npm ci
      ↓
Lint
      ↓
Unit Tests
      ↓
Build (vite build)
      ↓
Docker build (frontend image)
```

**`security.yml`** — periodic and on PR:
```
OWASP Dependency-Check
Trivy filesystem scan
Secrets detection (e.g. TruffleHog / GitLeaks)
```

### Definition of Done

```
[ ] backend-ci.yml runs on every push
[ ] frontend-ci.yml runs on every push
[ ] Build fails on test failure
[ ] Build fails on coverage below threshold
[ ] Docker images build successfully in CI
[ ] Security scan runs on PRs
[ ] Pipeline results visible on GitHub
[ ] No secrets in workflow files
```

---

## PHASE 14 — OBSERVABILITY & MONITORING

**Status:** NOT STARTED
**Depends on:** Phase 13 COMPLETE
**Blocks:** Phase 15

### Objective

Implement production-grade observability: metrics, tracing, and structured logging.

### Components

**Spring Boot Actuator + Micrometer + Prometheus:**
- Expose `/actuator/prometheus` endpoint
- Scrape with Prometheus
- Visualise in Grafana

**Metrics to monitor:**

| Metric | Source |
|--------|--------|
| HTTP request rate & latency | Micrometer / Actuator |
| HTTP error rate (4xx, 5xx) | Micrometer |
| JVM memory & CPU | Micrometer |
| Database connection pool | Micrometer |
| Kafka consumer lag | Micrometer Kafka binder |
| Appointments booked per minute | Custom counter |
| Appointment cancellations | Custom counter |
| Application health | Actuator /health |

### Grafana Dashboards

Create dashboards for:
- Application overview (health, request rate, errors)
- Booking throughput
- Database performance
- Kafka consumer lag

### Logging Requirements

Structured JSON logging in production profile (e.g. via Logback).

Logging must NEVER include:
- Passwords
- JWT tokens
- Refresh tokens
- Any credential or secret
- Full customer PII unless required for audit (and then must be handled per privacy policy)

### Definition of Done

```
[ ] /actuator/health returns UP
[ ] /actuator/prometheus returns metrics
[ ] Prometheus scrapes the backend
[ ] Grafana displays application metrics dashboard
[ ] Custom booking counter increments on each booking
[ ] Kafka consumer lag is visible in Grafana
[ ] No secrets or tokens appear in any log output
[ ] Structured JSON logging active in prod profile
[ ] Alerting rules documented (even if not yet wired to a notification channel)
```

---

## PHASE 15 — SECURITY HARDENING

**Status:** NOT STARTED
**Depends on:** Phase 14 COMPLETE
**Blocks:** Phase 16

### Objective

Dedicated security review and hardening of the entire system before production.

### Areas to Review and Harden

**Authentication & Authorization:**
- [ ] Rate limiting on `/api/v1/auth/login` and `/api/v1/auth/register`
- [ ] Brute-force protection (lockout after N failed login attempts)
- [ ] Account lockout mechanism (`User.active = false` after threshold)
- [ ] JWT signing key is a strong random secret (minimum 256-bit)
- [ ] JWT signing key is stored as an environment variable, never in source

**Transport:**
- [ ] HTTPS enforced in production
- [ ] HTTP Strict Transport Security (HSTS) header
- [ ] Secure + SameSite cookie flags if using HTTP-only cookies

**API Security:**
- [ ] Security headers: `X-Content-Type-Options`, `X-Frame-Options`, `Content-Security-Policy`
- [ ] CORS restricted to known origins in production
- [ ] Input validation on all endpoints (no unvalidated strings reach database)
- [ ] SQL injection protection (JPA parameterised queries; no raw SQL with string concat)
- [ ] XSS prevention (API returns JSON; frontend escapes all rendered data)

**Secrets:**
- [ ] No secrets in `application.yml` or committed files
- [ ] Secrets loaded from environment variables
- [ ] Document recommended secret management (e.g. AWS Secrets Manager, Vault)

**Dependencies:**
- [ ] OWASP Dependency-Check shows no critical vulnerabilities
- [ ] Trivy container scan shows no critical vulnerabilities
- [ ] All dependencies are on supported, patched versions

### Security Test Scenarios

- [ ] Rate limit triggers after N rapid requests
- [ ] Account lockout triggers after N failed passwords
- [ ] Locked account cannot log in even with correct password
- [ ] JWT with invalid signature rejected
- [ ] JWT with expired claim rejected
- [ ] JWT with tampered payload rejected
- [ ] Injection attempt in booking notes field
- [ ] CSRF protection verified (if applicable)

### Definition of Done

```
[ ] Rate limiting implemented on auth endpoints
[ ] Brute-force protection implemented
[ ] HTTPS documented for production
[ ] Security headers configured
[ ] CORS restricted for production
[ ] No secrets in source control
[ ] OWASP report: no unresolved CRITICAL or HIGH vulnerabilities
[ ] Trivy report: no unresolved CRITICAL vulnerabilities
[ ] All security test scenarios pass
[ ] Security review documented in SECURITY.md
```

---

## PHASE 16 — PRODUCTION READINESS & DEPLOYMENT

**Status:** NOT STARTED
**Depends on:** Phase 15 COMPLETE
**Blocks:** Nothing (final phase)

### Objective

Final verification that the system is ready for production use and the portfolio
project is complete and professional.

### Checklist

**Infrastructure:**
- [ ] HTTPS configured
- [ ] Domain / environment configured
- [ ] Secrets management configured (not .env files)
- [ ] Database backups configured
- [ ] Database connection pooling configured (HikariCP settings reviewed)

**Observability:**
- [ ] All health checks pass
- [ ] Monitoring dashboards live
- [ ] Logging to a persistent store
- [ ] Alerting configured for critical errors

**Quality:**
- [ ] All CI/CD pipelines green
- [ ] Coverage targets met
- [ ] Security scans clean
- [ ] No known critical bugs

**Documentation:**
- [ ] `README.md` — project overview, setup instructions, how to run locally
- [ ] `API_DOCUMENTATION.md` or Swagger UI link
- [ ] `ARCHITECTURE.md` — architectural decisions, diagrams
- [ ] `SECURITY.md` — security posture, known mitigations
- [ ] `PROJECT_ROADMAP.md` — all phases COMPLETE

**Load & Resilience:**
- [ ] Load test performed (basic; document tool and results)
- [ ] Behaviour under Kafka outage documented and tested
- [ ] Behaviour under database connection loss documented

**Disaster Recovery:**
- [ ] Database backup/restore procedure documented
- [ ] Recovery time objective (RTO) documented (even as an estimate)

### Definition of Done

```
[ ] Application runs end-to-end in production environment
[ ] All 16 phases marked COMPLETE
[ ] All documentation files created and accurate
[ ] CI/CD pipeline is green
[ ] Security scan clean
[ ] Monitoring is active
[ ] README allows a new developer to clone and run the project in under 15 minutes
```

---

## CHANGE CONTROL

This roadmap is the source of truth.

If any requirement appears to conflict with a better technical approach:

1. **Identify** the specific section and requirement that conflicts.
2. **Explain** the proposed change and the reason it is necessary.
3. **Wait** for explicit approval before modifying the roadmap or the
   implementation plan.

Do not:
- Silently skip requirements.
- Silently remove tests.
- Silently change the architecture.
- Reorder phases.
- Mark a phase COMPLETE without verifying every Definition of Done item.

---

## EXISTING CODE INSPECTION RULE

Before implementing any feature, inspect:

1. The repository for existing files related to that feature.
2. `pom.xml` for existing dependencies.
3. Existing entity classes.
4. Existing security configuration.
5. Existing tests.
6. Existing migration files.

Do not create duplicate classes. Identify conflicts, explain the recommended
migration path, and proceed only with approval.

---

## GIT AUTHORSHIP

Do NOT add AI tools, Claude, or Anthropic as a Git author, co-author, or
attribution in any commit message.

Do not modify:
- `git config user.name`
- `git config user.email`

Do not create commits unless explicitly instructed.
When commits are created, they must use the developer's existing Git identity.

---

*Last updated: Phase 4 — COMPLETE (2026-09-01)*