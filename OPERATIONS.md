# Operations & Disaster Recovery — CapiBook

---

## Database Backup & Restore

### Automated Backups (PostgreSQL on Homebrew)

For local development, backups are **NOT** automated. For production, use AWS RDS automatic backups or similar managed service.

### Manual Backup (Local Development)

```bash
# Backup the development database to a SQL file
pg_dump capibook_dev > capibook_backup_$(date +%Y%m%d_%H%M%S).sql

# Verify backup size
ls -lh capibook_backup_*.sql
```

### Restore from Backup

**Complete Restore (destructive):**

```bash
# Drop and recreate database
dropdb capibook_dev
createdb capibook_dev

# Restore from backup
psql capibook_dev < capibook_backup_20260902_100000.sql

# Verify data
psql capibook_dev -c "SELECT COUNT(*) FROM users;"
```

**Selective Restore (single table):**

```bash
# Restore just the appointments table
pg_restore --table appointments capibook_backup.sql | psql capibook_dev
```

### Point-in-Time Recovery (PITR)

**Production RDS:**
1. Go to AWS RDS console
2. Select instance → Automated backups tab
3. Click "Restore to point in time"
4. Select target date/time
5. New instance created (5–10 min); rename and promote to primary
6. Update DNS / connection string in backend

**RTO:** 10–15 minutes  
**RPO:** 1 hour (depends on backup frequency)

---

## Health Checks & Monitoring

### Manual Health Check

```bash
# Backend health
curl http://localhost:8080/actuator/health

# Expected response: 200 OK
{
  "status": "UP",
  "components": {
    "db": { "status": "UP" },
    "diskSpace": { "status": "UP" }
  }
}

# Metrics (Prometheus format)
curl http://localhost:8080/actuator/prometheus | head -20
```

### Common Failure Scenarios

| Scenario | Symptom | Fix |
|----------|---------|-----|
| Database down | 503, "Cannot get JDBC Connection" | `brew services start postgresql@15` |
| Kafka down | Events not published (not blocking) | Start Kafka; events will catch up |
| Connection pool exhausted | Long queue delays | Increase `maximum-pool-size` in application.yml |
| Disk full | 500 errors, OOM | Free disk space; expand volume |
| JVM OOM | Application crashes | Increase `-Xmx` in JVM args |

---

## Log Analysis

### Enable Debug Logging (Local)

```yaml
# src/main/resources/application-dev.yml
logging:
  level:
    root: WARN
    com.capitec.capibook: DEBUG
    org.springframework.security: DEBUG
    org.springframework.web: DEBUG
```

Then restart backend and tail logs:

```bash
tail -f $(find target/logs -type f -name "*.log" 2>/dev/null | head -1)
```

### JSON Logs (Production)

Logs are structured JSON via Logstash encoder. Parse with `jq`:

```bash
# Count 500 errors
docker logs capibook-backend | jq 'select(.level == "ERROR")' | wc -l

# Extract error details
docker logs capibook-backend | jq -r '.message' | grep -i "error"
```

---

## Performance Tuning

### Database Connection Pool

Current settings (application.yml):

```yaml
spring:
  datasource:
    hikari:
      maximum-pool-size: 20
      minimum-idle: 5
      connection-timeout: 30000
      idle-timeout: 600000
```

**When to adjust:**
- If you see "HikariPool waiting for available connection" warnings: increase `maximum-pool-size`
- If connections are idle: decrease `minimum-idle` to save memory

### JVM Tuning

Current (Spring Boot defaults):

```bash
java -Xms512m -Xmx1024m -jar capibook.jar
```

**For higher throughput:**
```bash
java -Xms2g -Xmx4g \
  -XX:+UseG1GC \
  -XX:MaxGCPauseMillis=200 \
  -XX:+PrintGCDetails \
  -jar capibook.jar
```

**Monitoring JVM:**
```bash
# Real-time JVM stats
jstat -gc -h5 <pid> 1000  # Every 1 second

# Heap usage over time
curl http://localhost:8080/actuator/metrics/jvm.memory.usage | jq .
```

---

## Troubleshooting

### "PKIX path building failed"

**Symptom:** Maven or Java fails with SSL/TLS error during dependency download

**Root Cause:** Zscaler corporate proxy intercepting HTTPS

**Solution:** Certificate already imported to `$JAVA_HOME/lib/security/cacerts`. If still failing:

```bash
# Get Java home
java -XshowSettings:properties -version 2>&1 | grep java.home

# Verify cert is imported
keytool -list -alias zscaler -keystore $JAVA_HOME/lib/security/cacerts -storepass changeit

# If missing, reimport
keytool -import -alias zscaler -file ~/Downloads/zscaler-root.crt \
  -keystore $JAVA_HOME/lib/security/cacerts -storepass changeit -noprompt
```

### "The Authentication attempt failed" (Kafka)

**Symptom:** Kafka producer/consumer hangs or fails

**Likely Cause:** Kafka broker down or wrong bootstrap server

**Fix:**
```bash
# Check broker status
docker ps | grep kafka

# Restart if needed
docker-compose restart kafka

# Check connectivity
nc -zv localhost 9092
```

### High Memory Usage

**Symptom:** Process uses > 2GB RAM

**Diagnosis:**
```bash
# Check heap usage
curl http://localhost:8080/actuator/metrics/jvm.memory.usage | jq '.measurements'

# Check garbage collection stats
curl http://localhost:8080/actuator/metrics/jvm.gc.memory.allocated | jq '.measurements'
```

**Fix:**
- Reduce batch sizes in data loading
- Enable query result caching (if available)
- Restart application to clear heap fragmentation

### "No such table" or "Unknown column"

**Symptom:** Application fails with migration error

**Fix:**
```bash
# Clear migrations and re-run
flyway clean  # DESTRUCTIVE - only in dev!
mvn flyway:migrate -Dflyway.profiles=dev
```

---

## Monitoring & Alerting

### Prometheus Queries (PromQL)

```promql
# Request rate (req/sec)
rate(http_requests_total[5m])

# Error rate (%)
100 * rate(http_requests_total{status=~"5.."}[5m]) / rate(http_requests_total[5m])

# p95 latency
histogram_quantile(0.95, rate(http_request_duration_seconds_bucket[5m]))

# Appointment booking throughput
rate(appointments_booked_total[1m])

# Database connection pool saturation
db_pool_active_connections / db_pool_max_connections
```

### Alert Rules (Example)

Create `alerts.yml` for Prometheus:

```yaml
groups:
  - name: capibook
    rules:
      - alert: HighErrorRate
        expr: rate(http_requests_total{status=~"5.."}[5m]) > 0.05
        for: 5m
        annotations:
          summary: "High error rate on CapiBook backend"

      - alert: DatabaseDown
        expr: db_health == 0
        for: 1m
        annotations:
          summary: "Database health check failed"

      - alert: KafkaDown
        expr: kafka_health == 0
        for: 2m
        annotations:
          summary: "Kafka health check failed (events delayed)"

      - alert: DiskSpaceLow
        expr: node_filesystem_avail_bytes / node_filesystem_size_bytes < 0.1
        for: 5m
        annotations:
          summary: "Disk space < 10%"
```

---

## Incident Response

### Appointment Data Loss

**If**: A customer reports missing appointment after successful booking

**Steps:**
1. Check database directly:
```sql
SELECT * FROM appointments WHERE customer_id = '<uuid>' 
  ORDER BY created_at DESC LIMIT 10;
```

2. If not in DB:
   - Check logs for INSERT error
   - Verify transaction committed
   - Check if customer was rate-limited

3. If in DB but not visible in UI:
   - Check JWT token expiry
   - Verify customer ID in token matches record
   - Check UI cache (`localStorage`)

4. Escalation:
   - If legitimate data loss: restore from backup
   - Create manual appointment for customer
   - Log incident in audit trail

### Brute-force Attack Detection

**Symptom:** Rate limit hits (429) from same IP repeatedly

**Response:**
```bash
# Check failed login attempts
SELECT COUNT(*) FROM users WHERE failed_login_attempts >= 5;

# Unlock accounts manually (if necessary)
UPDATE users SET failed_login_attempts = 0, locked_until = NULL 
  WHERE email = 'attacker@example.com';
```

### Kafka Consumer Lag

**Symptom:** Notifications delayed or missing

**Response:**
```bash
# Check consumer lag
kafka-consumer-groups --bootstrap-server localhost:9092 \
  --group capibook-notification-group --describe

# If lagging, restart consumer
docker-compose restart capibook-notification-consumer
```

---

## Runbook Summary

| Failure | Detection | TTM | Fix | Verification |
|---------|-----------|-----|-----|--------------|
| DB down | Health check 503 | 2m | `brew services start postgresql@15` | curl /actuator/health |
| Kafka down | Events stop flowing | 5m | `docker-compose restart kafka` | Kafka logs clear |
| Memory leak | JVM heap > 80% | 10m | Restart backend | Heap resets to baseline |
| Data loss | Customer complaint | 30m | Restore from backup | Spot-check records |
| DDoS / Rate limit | Many 429s | 5m | WAF rule / Block IP | Traffic normalizes |

---

## Contacts & Escalation

For this portfolio project, contact the developer directly. In a real org:

| Issue | Owner | Escalate |
|-------|-------|----------|
| Application bug | Backend team lead | CTO |
| Database issues | DBA | VP Infrastructure |
| Security incident | Security engineer | CISO |
| Performance | Platform engineer | VP Eng |

---

## References

- PostgreSQL backup docs: https://www.postgresql.org/docs/15/backup.html
- Prometheus alerting: https://prometheus.io/docs/prometheus/latest/configuration/alerting_rules/
- Spring Boot Actuator: https://spring.io/guides/gs/actuator-service/
- Kafka monitoring: https://kafka.apache.org/documentation/#monitoring