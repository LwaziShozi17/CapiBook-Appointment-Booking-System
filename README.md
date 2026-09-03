# CapiBook — Appointment Booking System

A full-stack appointment booking platform for banking services, built with **Spring Boot 4**, **React 19**, **PostgreSQL**, and **Kafka**. Production-grade observability, security, and testing throughout.

---

## Quick Start (Local Development)

### Prerequisites
- **Java 21+**
- **PostgreSQL 15**
- **Node.js 20+**
- **Docker** (Rancher Desktop or Docker Desktop)
- **Maven 3.9+** (or use the included `./mvnw` wrapper — no install required)

### 1. Clone & Set Up

```bash
git clone https://github.com/capitec/capibook.git
cd capibook

# Create a .env file at the project root with at minimum:
# JWT_SECRET=<generate a 256-bit secret, e.g. openssl rand -hex 32>
# All other variables have safe defaults (see docker-compose.yml for the full list)
```

### 2. Database Setup

```bash
# Start PostgreSQL (Homebrew on macOS)
brew services start postgresql@15

# Create development database
createdb capibook_dev
psql capibook_dev -c "CREATE USER capibook WITH PASSWORD 'capibook'; ALTER ROLE capibook CREATEDB;"

# Flyway migrations run automatically on first backend start
```

### 3. Start Backend

```bash
./mvnw spring-boot:run
```

Backend runs at `http://localhost:8080`. Swagger UI: `http://localhost:8080/swagger-ui.html`

### 4. Start Frontend

```bash
cd frontend
npm ci
npm run dev
```

Frontend runs at `http://localhost:5173`

### 5. Run Docker Stack (Optional)

For full end-to-end including Kafka, Prometheus, Grafana:

```bash
# Start Docker first, then:
docker-compose up -d

# Backend at http://localhost:8080
# Frontend at http://localhost:3000
# Kafka UI at http://localhost:8085
# Grafana at http://localhost:3001 (user: admin, pass: capibook)
# Prometheus at http://localhost:9090
```

---

## Demo Data

Flyway migration V18 seeds the following data on first start — no manual setup needed:

**10 SA branches** (one per province; Gauteng gets two):
Sandton City, Soweto Maponya Mall, Cape Town Waterfront, Durban Workshop, Gqeberha Greenacres, Bloemfontein Mimosa Mall, Polokwane Mall of the North, Nelspruit Riverside Mall, Rustenburg Waterfall Mall, Kimberley Diamond Pavilion.

**10 demo customers** — all share the same password:

| Email | Password |
|---|---|
| sipho.ndlovu@gmail.com | `Password@1` |
| naledi.mokoena@gmail.com | `Password@1` |
| thabo.dlamini@gmail.com | `Password@1` |
| ayesha.patel@gmail.com | `Password@1` |
| pieter.vanzyl@gmail.com | `Password@1` |
| zanele.khumalo@gmail.com | `Password@1` |
| andre.botha@gmail.com | `Password@1` |
| fatima.omar@gmail.com | `Password@1` |
| lebo.sithole@gmail.com | `Password@1` |
| priya.naidoo@gmail.com | `Password@1` |

**20 appointments** across all branches with a mix of COMPLETED, CONFIRMED, CANCELLED, and PENDING statuses.

### Creating an Admin User

No admin account is seeded automatically. To access the admin dashboard, insert a `SYSTEM_ADMIN` user directly:

```sql
-- Connect: psql -U capibook -d capibook_dev
INSERT INTO users (id, email, password_hash, first_name, last_name, role, active, created_at, updated_at)
VALUES (
  gen_random_uuid(),
  'admin@capibook.co.za',
  '$2a$12$XLXME3FzgTSc7QFKP7sOZuXqjHBqhZcUvj5zxGz2DXlmknFRSlXRa', -- Password@1
  'System', 'Admin',
  'SYSTEM_ADMIN', TRUE,
  CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
);
```

Then log in at `http://localhost:5173/login` with `admin@capibook.co.za` / `Password@1` and navigate to `/admin`.

---

## Architecture

See **[ARCHITECTURE.md](ARCHITECTURE.md)** for:
- System design and data flow
- Technology choices and rationale
- Deployment architecture
- Key components and responsibilities

---

## API Documentation

Interactive Swagger UI available at:
- **Local**: `http://localhost:8080/swagger-ui.html`
- **Production**: `https://<domain>/swagger-ui.html`

See **[API_DOCUMENTATION.md](API_DOCUMENTATION.md)** for:
- Endpoint reference
- Authentication flow
- Error handling
- Example payloads

---

## Security

See **[SECURITY.md](SECURITY.md)** for:
- Authentication & JWT token management
- Brute-force protection & rate limiting
- Role-based access control
- Input validation & injection prevention
- Secrets management
- Known limitations and future work

---

## Development

### Running Tests

```bash
# All tests (262 total)
./mvnw test

# Coverage report (JaCoCo — 96% instruction coverage)
./mvnw verify
# Report at: target/site/jacoco/index.html

# Frontend lint
cd frontend && npm run lint
```

### Code Style & Quality

- Backend: SpotBugs static analysis, OWASP Dependency-Check
- Frontend: oxlint (ESLint-compatible)
- CI/CD: All checks run on push to main

See `.github/workflows/` for CI pipeline details.

---

## Monitoring & Observability

### Local Monitoring

```bash
# Start Docker stack:
docker-compose up -d

# Prometheus scrapes backend metrics every 15 seconds
# Grafana dashboard pre-configured at localhost:3001
# Grafana login: admin / capibook
```

### Metrics Available

- HTTP request rate, latency, error rate
- JVM memory, CPU, garbage collection
- Database connection pool (HikariCP)
- Appointment booking throughput
- Application health checks

### Logs

- **Development**: Colored console output
- **Production**: JSON structured logging (Logstash format) — forward to ELK, Datadog, or CloudWatch

---

## Deployment

### Local Docker

```bash
docker-compose up -d
```

### Production (Kubernetes, Cloud Platform)

1. **Secrets**: Store `JWT_SECRET`, `SPRING_DATASOURCE_PASSWORD`, etc. in your secrets manager (AWS Secrets Manager, HashiCorp Vault, Kubernetes Secrets).
2. **HTTPS**: Terminate TLS at load balancer or nginx reverse proxy.
3. **Database**: Use managed PostgreSQL (RDS, Cloud SQL, Azure Database).
4. **Kafka**: Use managed Kafka (Confluent Cloud, AWS MSK) or self-managed cluster with SASL/SSL.
5. **Environment Variables**: Set all `app.*` and `spring.*` variables per environment.

See **[ARCHITECTURE.md](ARCHITECTURE.md)** for production deployment diagram.

---

## Database Migrations

Flyway manages all database schema changes. Migrations are in `src/main/resources/db/migration/`.

New migrations are run automatically on first backend start. To apply a specific migration:

```bash
./mvnw flyway:migrate -Dflyway.placeholders.profile=dev
```

---

## Troubleshooting

### Logback startup failure: `%clr` / `%wEx` not recognized

```
There is no conversion class registered for composite conversion word [clr]
There is no conversion supplier registered for conversion word [wEx]
```

Spring Boot's color converters need `defaults.xml` included in `logback-spring.xml`. This is already fixed in the repo — if you see this after pulling a fresh clone, ensure the file contains:

```xml
<include resource="org/springframework/boot/logging/logback/defaults.xml"/>
```

### "PKIX path building failed" (SSL/TLS Error)

Corporate Zscaler proxy intercepting HTTPS. Solution is already applied — Zscaler Root CA imported into Java cacerts at `$JAVA_HOME/lib/security/cacerts`.

If error reoccurs after JDK update, re-import the certificate:

```bash
# Download Zscaler Root CA from your corporate proxy
# Then:
keytool -import -alias zscaler -file zscaler-root.crt \
  -keystore $JAVA_HOME/lib/security/cacerts -storepass changeit
```

### Backend won't start: "Migration failed"

Check PostgreSQL is running and `capibook_dev` database exists:

```bash
psql -U postgres -c "\l" | grep capibook_dev
```

If Flyway shows a previously failed migration, repair the history and retry:

```bash
./mvnw flyway:repair
./mvnw spring-boot:run
```

### Frontend build errors

Clear node_modules and reinstall:

```bash
cd frontend
rm -rf node_modules package-lock.json
npm ci
npm run dev
```

### Docker Compose fails

Ensure Docker (Rancher Desktop or Docker Desktop) is running, then:

```bash
docker-compose up -d
```

---

## Performance & Resilience

### Load Testing

Basic load test with ApacheBench (100 concurrent users, 1000 requests):

```bash
# Backend must be running
ab -n 1000 -c 100 http://localhost:8080/actuator/health
```

See **[ARCHITECTURE.md](ARCHITECTURE.md)** for detailed load test results.

### Kafka Outage

If Kafka broker goes down:
- Appointment events are **not** published to Kafka
- Notifications, audit logs, and analytics are delayed but will catch up when Kafka recovers
- User-facing operations (booking, confirm, etc.) are **not** blocked — they complete synchronously in the database
- This is acceptable for a portfolio system; production would use transactional outbox or event sourcing

### Database Connection Loss

- Connection pool timeout after 30 seconds
- HTTP 500 returned to client
- Graceful restart of connection pool on next request
- No data loss (failed transaction rolled back)

---

## Project Status

| Phase | Status | Purpose |
|-------|--------|---------|
| 0–16 | ✅ COMPLETE | Foundation, API, UI, observability, security, production readiness |

All 262 tests passing. All phases documented in [PROJECT_ROADMAP.md](PROJECT_ROADMAP.md).

---

## License

This is a portfolio project. Use freely for learning and demonstration purposes.

---

## Contact

For questions or feedback about the system design, see [ARCHITECTURE.md](ARCHITECTURE.md) and [PROJECT_ROADMAP.md](PROJECT_ROADMAP.md).
