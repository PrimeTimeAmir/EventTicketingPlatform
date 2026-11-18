# Event Ticketing Platform



A RESTful event ticketing backend built with Java 21 and Spring Boot. The platform supports user authentication, event management, seat reservations, booking, payment simulation, electronic ticket issuance, asynchronous messaging, and audit logging.



## Features



### Authentication and Authorization

- User registration and login

- JWT-based authentication

- Refresh token rotation and revocation

- Role-based access control

- Roles: ADMIN, ORGANIZER, CUSTOMER



### Event Catalog

- Venue management

- Sections and seat management

- Event creation and management

- Event publication and status transitions

- Event-specific ticket pricing



### Booking and Ticketing

- Temporary seat reservations (Seat Holds)

- Reservation expiration

- Booking creation and management

- Ticket issuance after successful payment

- Ticket retrieval and check-in



### Payments

- Payment creation

- Payment simulation (success and failure scenarios)

- Booking confirmation after successful payment

- Automatic ticket issuance



### Messaging

- Transactional Outbox pattern

- RabbitMQ publisher and consumer

- Publisher confirms

- Duplicate message prevention

- Processed message tracking



### Audit Logging

- User registration and login history

- Booking and payment history

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



The application follows a layered architecture organized into business modules.



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

|   +-- consumer/

|   +-- producer/

|   +-- config/

|   +-- domain/

|   +-- repo/

|   +-- service/

|

+-- support/

&#x20;   +-- audit/

&#x20;       +-- api/

&#x20;       +-- domain/

&#x20;       +-- repo/

&#x20;       +-- service/

```



### Main Business Flow



1\. A customer registers and authenticates.

2\. An organizer creates and publishes an event.

3\. The customer selects available seats.

4\. The system creates a temporary seat hold.

5\. A booking is created using the active hold.

6\. The customer completes a simulated payment.

7\. The booking is confirmed and tickets are issued.

8\. A ticket issuance event is stored in the Outbox.

9\. The Outbox publisher sends the event to RabbitMQ.

10\. The consumer processes the message and prevents duplicate processing.

11\. Audit records capture relevant business actions.



## Requirements



- Java 21

- Docker Desktop

- Docker Compose

- Maven 3.9+ (for running tests locally)



## Getting Started



### 1. Clone the Repository



```bash

git clone <YOUR\_GITHUB\_REPOSITORY\_URL>

cd EventTicketingPlatform

```



### 2. Configure Environment Variables



Create a local environment file:



```bash

cp .env.example .env

```



On Windows PowerShell:



```powershell

Copy-Item .env.example .env

```



Generate a secure JWT secret locally.



PowerShell example:



```powershell

$bytes = New-Object byte\[] 32

\[System.Security.Cryptography.RandomNumberGenerator]::Fill($bytes)

$JWT\_SECRET = -join ($bytes | ForEach-Object {

&#x20;   $\_.ToString("x2")

})

$JWT\_SECRET

```



Put the generated value in your local `.env` file:



```dotenv

JWT\_SECRET=your-generated-secret

```



Never commit `.env` or publish real secrets.



### 3. Start the Application with Docker Compose



```bash

docker compose up -d --build

```



This starts the application and its supporting services.



### 4. Verify the Services



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

&#x20; "status": "UP"

}

```



### 5. Access Swagger UI



Open:



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

| Spring Boot Application | 8080 | 8080 |

| PostgreSQL | 5433 | 5432 |

| Redis | 6379 | 6379 |

| RabbitMQ | 5672 | 5672 |

| RabbitMQ Management UI | 15672 | 15672 |



The PostgreSQL host port is mapped to 5433 to avoid conflicts with locally installed PostgreSQL instances. Containers communicate with PostgreSQL using its internal port 5432.



RabbitMQ Management UI:



```text

http://localhost:15672

```



Use the credentials configured in `docker-compose.yml`.



## API Overview



The API uses the `/api/v1` base path.



| Module | Example Endpoint | Description |

|---|---|---|

| Authentication | POST `/api/v1/auth/register` | Register a user |

| Authentication | POST `/api/v1/auth/login` | Authenticate |

| Authentication | POST `/api/v1/auth/refresh` | Refresh access token |

| Events | POST `/api/v1/events` | Create an event |

| Events | GET `/api/v1/events/{id}` | Get event details |

| Events | PATCH `/api/v1/events/{id}/status` | Change event status |

| Pricing | POST `/api/v1/events/{eventId}/prices` | Set section pricing |

| Sections | GET `/api/v1/sections/{sectionId}/seats` | List section seats |

| Booking | POST `/api/v1/bookings` | Create a booking |

| Payments | POST `/api/v1/payments` | Create a payment |

| Tickets | GET `/api/v1/tickets/my` | Get customer tickets |

| Audit | GET `/api/v1/audit/my` | Get personal audit history |

| Audit | GET `/api/v1/audit` | Get global audit history (ADMIN) |



Additional endpoints are documented in Swagger UI.



## Security



- Password hashing with BCrypt

- JWT access tokens

- Refresh token rotation and revocation

- Role-based endpoint authorization

- Protected API endpoints

- Environment-based secret configuration



Never commit credentials, access tokens, refresh tokens, or `.env`.



## Database Management



Flyway manages database schema migrations.



Migration scripts are located in:



```text

src/main/resources/db/migration/

```



Hibernate schema validation is enabled. Applied migration files should not be modified; create a new migration for future schema changes.



## Testing



Run the complete test suite:



```bash

mvn clean test

```



The project uses JUnit 5, Mockito, and Testcontainers.



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



The full automated test suite passed with 39 tests, 0 failures, and 0 errors.



---



\*\*Academic Project\*\*



Developed as part of the CIT4242 Advanced Programming course.




