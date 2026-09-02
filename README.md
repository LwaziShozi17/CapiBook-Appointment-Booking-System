# CapiBook — Appointment Booking System

A full-stack appointment booking platform for banking services, built with **Spring Boot 4**, **React 18**, **PostgreSQL**, and **Kafka**. Production-grade observability, security, and testing throughout.

---

## Quick Start (Local Development)

### Prerequisites
- **Java 21** (OpenJDK 26 or later)
- **PostgreSQL 15**
- **Node.js 20+**
- **Rancher Desktop** (or Docker if available; Docker Desktop crashes on macOS in this environment)
- **Maven 3.9+**

### 1. Clone & Set Up

```bash
git clone https://github.com/capitec/capibook.git
cd capibook

# Copy environment template
cp .env.example .env

# Edit .env with your local settings (default values work for dev)
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
mvn clean install
mvn spring-boot:run -Dspring-boot.run.profiles=dev
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
# Start Rancher Desktop first, then:
docker-compose up -d

# Backend at http://localhost:8080
# Frontend at http://localhost:3000
# Kafka UI at http://localhost:8080/kafka-ui (if included)
# Grafana at http://localhost:3001 (user: admin, pass: admin)
# Prometheus at http://localhost:9090
```

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
mvn test

# Backend only
mvn test -pl .

# Frontend
cd frontend && npm test

# Coverage report (JaCoCo)
mvn verify
# Report at: target/site/jacoco/index.html
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
# Grafana login: admin / admin
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
mvn flyway:migrate -Dflyway.placeholders.profile=dev
```

---

## Troubleshooting

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

### Frontend build errors

Clear node_modules and reinstall:

```bash
cd frontend
rm -rf node_modules package-lock.json
npm ci
npm run dev
```

### Docker-Compose fails

Ensure Rancher Desktop is running:

```bash
rancher-desktop
```

Then:

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
| 0–15 | ✅ COMPLETE | Foundation, API, UI, observability, security |
| 16 | 🚀 IN PROGRESS | Documentation, production readiness |

All 262 tests passing. All phases documented in [PROJECT_ROADMAP.md](PROJECT_ROADMAP.md).

---

## License

This is a portfolio project. Use freely for learning and demonstration purposes.

---

## Contact

For questions or feedback about the system design, see [ARCHITECTURE.md](ARCHITECTURE.md) and [PROJECT_ROADMAP.md](PROJECT_ROADMAP.md).