# TaskForge

Project and task management API with AI-assisted planning. Work in progress; a full README
with a live demo, screenshots and architecture diagram comes with v1.0.

## Tech stack (so far)
Java 21 · Spring Boot 3.5 · Spring Security + JWT (jjwt) · Spring Data JPA · MySQL 8 · Flyway ·
MapStruct · Lombok · Bucket4j · springdoc OpenAPI · JUnit 5 · Mockito · Testcontainers

## Architecture
Feature-first packages, each split into hexagonal layers:

```
com.kshitiz.taskforge
├── common/   shared code: exceptions, paging, base entity, error handler, current user
├── auth/     sign-up, login, JWT tokens, security rules, rate limiting
├── master/   master data (priorities, statuses, roles) served from DB tables
├── user/     users, roles and reporting lines
└── task/     tasks, comments, assignments, permission policy
    ├── domain/       business objects and rules (plain Java) + repository ports
    ├── application/  services (use cases, transactions) + request/response DTOs
    └── adapter/      REST controllers and JPA persistence
```

## Authentication
- `POST /api/auth/register` – public sign-up (always creates a MEMBER)
- `POST /api/auth/login` – returns an access token (15 min) and a refresh token (7 days)
- `POST /api/auth/refresh` – exchanges a refresh token for a new pair
- Send `Authorization: Bearer <accessToken>` on every other request.
- Auth endpoints are rate-limited per IP (10 requests/minute by default).
- The first admin is created at startup from `ADMIN_EMAIL` / `ADMIN_PASSWORD` if no admin exists.

| Action | MEMBER | MANAGER | ADMIN |
|---|---|---|---|
| Read tasks and users, create tasks, comment | ✅ | ✅ | ✅ |
| Edit, delete or assign a task | own tasks | ✅ | ✅ |
| Change a task's status | own or assigned | ✅ | ✅ |
| Edit a comment | own | own | own |
| Delete a comment | own | own | ✅ |
| Create users, change roles or managers, delete users | – | – | ✅ |

## Running locally
Requirements: JDK 21, Maven, MySQL 8 (or Docker).

```bash
# set DB_URL, DB_USERNAME, DB_PASSWORD (see .env.example)
cd backend
mvn spring-boot:run
```

The `dev` profile (default) supplies a development JWT secret and an admin account
`admin@taskforge.local` / `admin12345`. Never use these outside your machine.

- Swagger UI: http://localhost:8080/swagger-ui.html (log in, then click **Authorize** and paste the access token)
- Health: http://localhost:8080/actuator/health

## Tests
```bash
cd backend
mvn test   # integration tests need Docker running (Testcontainers MySQL)
```
