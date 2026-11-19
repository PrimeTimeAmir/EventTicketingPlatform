# Event Ticketing Platform

A RESTful event ticketing backend built with Java 21 and Spring Boot.

The platform provides event management, seat reservations, booking, payment simulation, electronic ticket issuance, asynchronous messaging, and audit logging.

**Repository:** [PrimeTimeAmir/EventTicketingPlatform](https://github.com/PrimeTimeAmir/EventTicketingPlatform)

## Features

### Authentication and Authorization
- User registration and login
- JWT-based authentication
- Refresh token rotation and revocation
- BCrypt password hashing
- Role-based access control: `ADMIN`, `ORGANIZER`, `CUSTOMER`

### Event Catalog
- Venue management
- Sections and automatic seat management
- Event creation, updates, and publication
- Event status transitions
- Event-specific ticket pricing

### Booking and Ticketing
- Temporary seat reservations (Seat Holds)
- Automatic hold expiration
- Booking creation and confirmation
- Electronic ticket issuance
- Ticket retrieval and check-in

### Payments
- Payment creation
- Payment simulation for successful and failed transactions
- Booking confirmation after successful payment
- Automatic ticket issuance

### Messaging
- Transactional Outbox pattern
- RabbitMQ publisher and consumer
- Publisher confirms and message acknowledgements
- Duplicate message prevention
- Processed message tracking

### Audit Logging
- User registration and login history
- Booking and payment activity
- Ticket issuance history
- Personal audit history
- Administrator-only global audit history

## Technology Stack

| Technology | Purpose |
|---|---|
| Java 21 | Programming language |
| Spring Boot 3.5.16 | Backend framework |
| Spring Security | Authentication and authorization |
| Spring Data JPA | Database access |
| PostgreSQL 16 | Relational database |
| Flyway | Database migrations |
| Redis 7 | Caching |
| RabbitMQ 3 | Asynchronous messaging |
| JWT | Token-based authentication |
| Maven | Build and dependency management |
| Docker Compose | Container orchestration |
| Testcontainers | Integration testing |
| JUnit 5 and Mockito | Automated testing |
| Swagger / OpenAPI | API documentation |

## Architecture

The project follows a modular layered architecture.

```text
src/main/java/kz/kimep/ticketing/
|
+-- identity/
|   +-- api/
|   +-- domain/
|   +-- repo/
|   +-- security/
|   +-- service/
|
+-- catalog/
|   +-- api/
|   +-- domain/
|   +-- repo/
|   +-- service/
|
+-- booking/
|   +-- api/
|   +-- config/
|   +-- domain/
|   +-- repo/
|   +-- service/
|
+-- payment/
|   +-- api/
|   +-- domain/
|   +-- repo/
|   +-- service/
|
+-- messaging/
|   +-- config/
|   +-- consumer/
|   +-- domain/
|   +-- producer/
|   +-- repo/
|   +-- service/
|
+-- support/
    +-- audit/
        +-- api/
        +-- domain/
        +-- repo/
        +-- service/
```

### Main Business Flow

1. A customer registers and authenticates.
2. An organizer creates and publishes an event.
3. The customer selects available seats.
4. The system creates a temporary seat hold.
5. A booking is created using the active hold.
6. The customer completes a simulated payment.
7. The booking is confirmed and tickets are issued.
8. A ticket issuance event is stored in the Outbox.
9. The Outbox publisher sends the event to RabbitMQ.
10. The consumer processes the message and prevents duplicate processing.
11. Audit records capture relevant business actions.

## Requirements

- Java 21
- Docker Desktop
- Docker Compose
- Maven 3.9+ (for local test execution)

Docker Desktop is required for integration tests using Testcontainers.

## Getting Started

### 1. Clone the Repository

```bash
git clone https://github.com/PrimeTimeAmir/EventTicketingPlatform.git
cd EventTicketingPlatform
```

### 2. Configure Environment Variables

Create a local environment file from the example:

**Windows PowerShell:**

```powershell
Copy-Item .env.example .env
```

**Linux / macOS:**

```bash
cp .env.example .env
```

Open `.env` and replace all placeholder values.

Required variables:

```dotenv
JWT_SECRET=your-generated-secret
DB_PASSWORD=your-database-password
RABBITMQ_PASSWORD=your-rabbitmq-password
```

Generate a secure JWT secret using PowerShell:

```powershell
$bytes = New-Object byte[] 32
[System.Security.Cryptography.RandomNumberGenerator]::Fill($bytes)
$jwt = -join ($bytes | ForEach-Object { $_.ToString("x2") })
$jwt
```

Never commit `.env` or publish real credentials.

### 3. Start the Application

Build and start all services:

```bash
docker compose up -d --build
```

Docker Compose starts:
- Spring Boot application
- PostgreSQL
- Redis
- RabbitMQ

### 4. Verify the Services

Check container status:

```bash
docker compose ps
```

Check application health:

```text
http://localhost:8080/actuator/health
```

Expected response:

```json
{
  "status": "UP"
}
```

### 5. Access API Documentation

Swagger UI:

```text
http://localhost:8080/swagger-ui/index.html
```

OpenAPI specification:

```text
http://localhost:8080/v3/api-docs
```

## Service Ports

| Service | Host Port | Container Port |
|---|---:|---:|
| Spring Boot | 8080 | 8080 |
| PostgreSQL | 5433 | 5432 |
| Redis | 6379 | 6379 |
| RabbitMQ | 5672 | 5672 |
| RabbitMQ Management UI | 15672 | 15672 |

The PostgreSQL host port is mapped to `5433` to avoid conflicts with locally installed PostgreSQL instances. Containers communicate with PostgreSQL through its internal port `5432`.

RabbitMQ Management UI:

```text
http://localhost:15672
```

Use the credentials configured in your local `.env` file.

## API Overview

The REST API uses the `/api/v1` base path.

| Module | Method | Endpoint | Description |
|---|---|---|---|
| Authentication | POST | `/api/v1/auth/register` | Register a user |
| Authentication | POST | `/api/v1/auth/login` | Authenticate |
| Authentication | POST | `/api/v1/auth/refresh` | Refresh access token |
| Events | POST | `/api/v1/events` | Create an event |
| Events | GET | `/api/v1/events/{id}` | Get event details |
| Events | PATCH | `/api/v1/events/{id}/status` | Change event status |
| Pricing | POST | `/api/v1/events/{eventId}/prices` | Set section pricing |
| Seats | GET | `/api/v1/sections/{sectionId}/seats` | List seats |
| Booking | POST | `/api/v1/bookings` | Create a booking |
| Payments | POST | `/api/v1/payments` | Create a payment |
| Tickets | GET | `/api/v1/tickets/my` | Get customer tickets |
| Audit | GET | `/api/v1/audit/my` | Personal audit history |
| Audit | GET | `/api/v1/audit` | Global audit history (ADMIN only) |

See Swagger UI for the complete list of available endpoints and request schemas.

## Security

- BCrypt password hashing
- JWT access tokens
- Refresh token rotation and revocation
- Role-based endpoint authorization
- Environment-based secret configuration
- Protected API endpoints

Never commit credentials, access tokens, refresh tokens, or `.env`.

## Database Management

Flyway manages database schema migrations.

Migration scripts are located in:

```text
src/main/resources/db/migration/
```

Hibernate schema validation is enabled.

Applied migration files should not be modified. Create a new migration for future schema changes.

## Testing

Run the complete test suite:

```bash
mvn clean test
```

The project uses:
- JUnit 5
- Mockito
- Testcontainers

The verified test suite contains 39 tests covering:

- Booking service
- Ticket service
- Payment service
- Audit service
- Database migrations
- RabbitMQ message consumer
- Outbox publisher
- Outbox transaction behavior

Integration tests use PostgreSQL and RabbitMQ containers through Testcontainers.

## Docker Commands

Start all services:

```bash
docker compose up -d --build
```

View service status:

```bash
docker compose ps
```

View application logs:

```bash
docker compose logs -f app
```

Stop services:

```bash
docker compose down
```

Do not use `docker compose down -v` unless you intentionally want to delete persistent database volumes.

## Project Status

The following workflows have been implemented and tested:

- Authentication and authorization
- Event and venue management
- Section and seat management
- Seat reservation
- Booking creation
- Payment simulation
- Electronic ticket issuance
- RabbitMQ and transactional Outbox
- Audit logging

**Automated testing:** 39 tests passed, 0 failures, 0 errors.

---

## Academic Project

Developed as part of the CIT4242 Advanced Programming course.
