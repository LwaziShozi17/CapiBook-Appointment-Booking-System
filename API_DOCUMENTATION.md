# API Documentation — CapiBook

For interactive exploration, visit **Swagger UI** at `http://localhost:8080/swagger-ui.html`

---

## Authentication

All endpoints except `/api/v1/auth/*` and `/actuator/*` require JWT authentication.

### Register

```http
POST /api/v1/auth/register
Content-Type: application/json

{
  "email": "customer@example.com",
  "password": "SecurePassword123!",
  "firstName": "John",
  "lastName": "Doe",
  "phoneNumber": "0821234567"
}

# Response: 201 Created
{
  "success": true,
  "message": "Registration successful",
  "data": {
    "accessToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
    "refreshToken": "uuid-string",
    "tokenType": "Bearer",
    "expiresIn": 900,
    "user": {
      "id": "uuid",
      "email": "customer@example.com",
      "firstName": "John",
      "lastName": "Doe",
      "phoneNumber": "0821234567",
      "role": "CUSTOMER"
    }
  }
}
```

**Validation:**
- Email: valid format, unique
- Password: minimum 8 characters
- First/Last Name: required, non-blank

**Role:** Always `CUSTOMER` — clients cannot self-assign elevated roles.

---

### Login

```http
POST /api/v1/auth/login
Content-Type: application/json

{
  "email": "customer@example.com",
  "password": "SecurePassword123!"
}

# Response: 200 OK
{
  "success": true,
  "message": "Login successful",
  "data": {
    "accessToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
    "refreshToken": "uuid-string",
    "tokenType": "Bearer",
    "expiresIn": 900,
    "user": { ... }
  }
}

# 401 Unauthorized (bad credentials)
# 423 Locked (account locked after 5 failed attempts)
# 429 Too Many Requests (rate limited)
```

---

### Refresh Token

```http
POST /api/v1/auth/refresh
Content-Type: application/json

{
  "refreshToken": "uuid-string"
}

# Response: 200 OK (new tokens)
{
  "data": {
    "accessToken": "new-jwt",
    "refreshToken": "new-uuid",
    ...
  }
}

# 401 Unauthorized (refresh token expired or revoked)
```

Refresh tokens rotate on every refresh and are revoked on logout or new login.

---

### Logout

```http
POST /api/v1/auth/logout
Content-Type: application/json

{
  "refreshToken": "uuid-string"
}

# Response: 204 No Content
```

---

## User Profile

### Get Current User

```http
GET /api/v1/users/me
Authorization: Bearer <accessToken>

# Response: 200 OK
{
  "data": {
    "id": "uuid",
    "email": "customer@example.com",
    "firstName": "John",
    "lastName": "Doe",
    "phoneNumber": "0821234567",
    "role": "CUSTOMER"
  }
}

# 401 Unauthorized (invalid or expired token)
```

### Update Profile

```http
PUT /api/v1/users/me
Authorization: Bearer <accessToken>
Content-Type: application/json

{
  "firstName": "Jane",
  "lastName": "Doe",
  "phoneNumber": "0829876543"
}

# Response: 200 OK
```

---

## Availability & Booking

### Get Available Slots

```http
GET /api/v1/availability?branchId=<uuid>&serviceId=<uuid>&date=2026-09-15
Authorization: Bearer <accessToken>

# Response: 200 OK
{
  "data": [
    {
      "slotTime": "09:00",
      "status": "AVAILABLE"
    },
    {
      "slotTime": "09:30",
      "status": "BOOKED"
    },
    {
      "slotTime": "10:00",
      "status": "AVAILABLE"
    }
  ]
}

# 404 Not Found (branch or service doesn't exist)
```

**Slot Statuses:**
- `AVAILABLE` — slot is free
- `BOOKED` — slot is at capacity
- `UNAVAILABLE` — branch is closed/on holiday

---

### Create Appointment

```http
POST /api/v1/appointments
Authorization: Bearer <accessToken>
Content-Type: application/json

{
  "branchId": "uuid",
  "serviceId": "uuid",
  "appointmentDate": "2026-09-15",
  "startTime": "09:00",
  "notes": "Please bring ID document"
}

# Response: 201 Created
{
  "data": {
    "id": "uuid",
    "referenceNumber": "CAP-2026-A3F5E",
    "status": "PENDING",
    "appointmentDate": "2026-09-15",
    "startTime": "09:00",
    "customerId": "uuid",
    "branchId": "uuid",
    "serviceId": "uuid",
    "notes": "Please bring ID document",
    "createdAt": "2026-09-02T10:00:00Z"
  }
}

# 409 Conflict (slot at capacity or duplicate booking at same time)
# 422 Unprocessable Entity (invalid status transition)
```

**Validation:**
- `appointmentDate` must be in the future
- `branchId` and `serviceId` must exist
- No duplicate bookings on same date/time for customer
- Slot must not exceed branch capacity

---

### Get My Appointments

```http
GET /api/v1/appointments/my?page=0&size=10
Authorization: Bearer <accessToken>

# Response: 200 OK
{
  "data": {
    "content": [
      {
        "id": "uuid",
        "referenceNumber": "CAP-2026-A3F5E",
        "status": "CONFIRMED",
        ...
      }
    ],
    "totalPages": 2,
    "totalElements": 15,
    "currentPage": 0,
    "pageSize": 10
  }
}
```

---

### Get Appointment Details

```http
GET /api/v1/appointments/<appointmentId>
Authorization: Bearer <accessToken>

# Response: 200 OK
{
  "data": {
    "id": "uuid",
    "referenceNumber": "CAP-2026-A3F5E",
    "status": "CONFIRMED",
    "appointmentDate": "2026-09-15",
    "startTime": "09:00",
    "customerId": "uuid",
    "branchId": "uuid",
    "serviceId": "uuid",
    "notes": "Please bring ID document",
    "createdAt": "2026-09-02T10:00:00Z"
  }
}

# 403 Forbidden (not your appointment, not an admin)
# 404 Not Found
```

---

## Appointment Lifecycle

### Cancel Appointment

```http
PATCH /api/v1/appointments/<appointmentId>/cancel
Authorization: Bearer <accessToken>
Content-Type: application/json

{}

# Response: 200 OK
{
  "data": {
    "status": "CANCELLED",
    ...
  }
}

# 403 Forbidden (not your appointment)
# 422 Unprocessable Entity (terminal state or past appointment)
```

Can only cancel:
- `PENDING` or `CONFIRMED` appointments
- With appointment date in the future

---

### Confirm Appointment (Admin)

```http
PATCH /api/v1/appointments/<appointmentId>/confirm
Authorization: Bearer <adminToken>
Content-Type: application/json

{
  "reason": "Customer confirmed over phone"
}

# Response: 200 OK
```

Requires `BRANCH_ADMIN` or `SYSTEM_ADMIN` role.

---

### Complete Appointment (Admin)

```http
PATCH /api/v1/appointments/<appointmentId>/complete
Authorization: Bearer <adminToken>
Content-Type: application/json

{}

# Response: 200 OK
```

Mark appointment as successfully completed.

---

### No-Show (Admin)

```http
PATCH /api/v1/appointments/<appointmentId>/no-show
Authorization: Bearer <adminToken>
Content-Type: application/json

{}

# Response: 200 OK
```

Mark appointment as not attended.

---

### Reschedule Appointment

```http
PATCH /api/v1/appointments/<appointmentId>/reschedule
Authorization: Bearer <accessToken>
Content-Type: application/json

{
  "branchId": "uuid",
  "serviceId": "uuid",
  "appointmentDate": "2026-09-20",
  "startTime": "14:00",
  "reason": "Work conflict",
  "notes": "New preferred time"
}

# Response: 200 OK
{
  "data": {
    "originalId": "uuid",
    "status": "RESCHEDULED",
    "newAppointmentId": "uuid",
    "newAppointmentStatus": "PENDING"
  }
}
```

- Original appointment marked `RESCHEDULED`
- New appointment created as `PENDING`
- Customer keeps new reference number

---

### Appointment History

```http
GET /api/v1/appointments/<appointmentId>/history
Authorization: Bearer <accessToken>

# Response: 200 OK
{
  "data": [
    {
      "status": "PENDING",
      "timestamp": "2026-09-02T10:00:00Z",
      "reason": "Appointment created"
    },
    {
      "status": "CONFIRMED",
      "timestamp": "2026-09-02T11:00:00Z",
      "reason": "Customer confirmed over phone"
    }
  ]
}
```

All status transitions for an appointment.

---

## Admin Endpoints

### List Users (SYSTEM_ADMIN)

```http
GET /api/v1/admin/users
Authorization: Bearer <systemAdminToken>

# Response: 200 OK
{
  "data": [
    {
      "id": "uuid",
      "email": "user@example.com",
      "firstName": "John",
      "role": "CUSTOMER",
      "active": true
    }
  ]
}
```

---

### Create Branch Admin (SYSTEM_ADMIN)

```http
POST /api/v1/admin/users/branch-admins
Authorization: Bearer <systemAdminToken>
Content-Type: application/json

{
  "email": "admin@branch.com",
  "password": "SecurePassword123!",
  "firstName": "Branch",
  "lastName": "Admin",
  "branchId": "uuid"
}

# Response: 201 Created
```

---

### Deactivate User (SYSTEM_ADMIN)

```http
PUT /api/v1/admin/users/<userId>/deactivate
Authorization: Bearer <systemAdminToken>

# Response: 200 OK
```

User cannot log in after deactivation.

---

### List Appointments (Admin)

```http
GET /api/v1/admin/appointments?page=0&size=20
Authorization: Bearer <adminToken>

# BRANCH_ADMIN sees own branch appointments only
# SYSTEM_ADMIN sees all appointments
```

---

### Branch Availability Exceptions

```http
POST /api/v1/admin/branches/<branchId>/exceptions
Authorization: Bearer <adminToken>
Content-Type: application/json

{
  "date": "2026-12-25",
  "reason": "Christmas holiday"
}

# Response: 201 Created (no slots available on that date)

GET /api/v1/admin/branches/<branchId>/exceptions
# List all exceptions for a branch

DELETE /api/v1/admin/branches/<branchId>/exceptions/<exceptionId>
# Remove an exception
```

---

### Analytics (SYSTEM_ADMIN)

```http
GET /api/v1/admin/analytics/appointments/summary
Authorization: Bearer <systemAdminToken>

# Response: 200 OK
{
  "data": {
    "totalBooked": 150,
    "totalConfirmed": 120,
    "totalCompleted": 110,
    "totalCancelled": 25,
    "totalNoShow": 5
  }
}

GET /api/v1/admin/analytics/branches/<branchId>/utilisation
# Branch slot utilization percentage

GET /api/v1/admin/analytics/services/popularity
# Most booked services by count
```

---

### Audit Logs (SYSTEM_ADMIN)

```http
GET /api/v1/admin/audit-logs?page=0&size=50
Authorization: Bearer <systemAdminToken>

# Response: 200 OK
{
  "data": [
    {
      "timestamp": "2026-09-02T10:00:00Z",
      "action": "APPOINTMENT_CREATED",
      "actorEmail": "customer@example.com",
      "details": { ... }
    }
  ]
}
```

---

## Health & Observability

### Health Check

```http
GET /actuator/health

# Response: 200 OK
{
  "status": "UP",
  "components": {
    "db": { "status": "UP" },
    "kafka": { "status": "UP" },
    "diskSpace": { "status": "UP" }
  }
}
```

### Metrics (Prometheus)

```http
GET /actuator/prometheus

# Returns all metrics in Prometheus text format
```

Scrape this endpoint every 15 seconds (configured in `prometheus.yml`).

---

## Error Handling

All error responses follow this format:

```json
{
  "success": false,
  "message": "User-friendly error message",
  "errors": {
    "fieldName": "Field validation error message"
  }
}
```

### Common HTTP Status Codes

| Status | Meaning |
|--------|---------|
| 200 | Success |
| 201 | Created |
| 204 | No Content |
| 400 | Validation failed |
| 401 | Unauthorized (invalid/expired token) |
| 403 | Forbidden (insufficient role/permissions) |
| 404 | Resource not found |
| 409 | Conflict (duplicate, slot full) |
| 422 | Unprocessable Entity (invalid state transition) |
| 423 | Locked (account locked after brute-force) |
| 429 | Too Many Requests (rate limited) |
| 500 | Internal server error |

---

## Rate Limiting

- `/api/v1/auth/login`: 10 requests per minute per IP
- `/api/v1/auth/register`: 10 requests per minute per IP
- Other endpoints: No limit

**Response on limit:**
```http
HTTP/1.1 429 Too Many Requests

{
  "success": false,
  "message": "Too many requests. Please try again later."
}
```

---

## Pagination

List endpoints support pagination:

```http
GET /api/v1/appointments/my?page=0&size=10&sort=createdAt,desc

# Query Parameters:
# - page: 0-indexed page number (default: 0)
# - size: items per page (default: 10)
# - sort: field,direction (default: createdAt,desc)
```

Response includes metadata:

```json
{
  "data": {
    "content": [ ... ],
    "totalElements": 150,
    "totalPages": 15,
    "currentPage": 0,
    "pageSize": 10
  }
}
```

---

## Support

For production API questions, enable request logging in application.yml:

```yaml
logging:
  level:
    org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping: TRACE
```

See [ARCHITECTURE.md](ARCHITECTURE.md) for data model diagrams and [SECURITY.md](SECURITY.md) for authentication details.