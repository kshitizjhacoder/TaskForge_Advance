# TaskForge

Project and task management API with AI-assisted planning. Work in progress; a full README
with a live demo, screenshots and architecture diagram comes with v1.0.

## Tech stack (so far)
Java 21 · Spring Boot 3.5 · Spring Security + JWT (jjwt) · Spring Data JPA · MySQL 8 · Flyway ·
S3-compatible storage (AWS SDK v2, MinIO) · MapStruct · Lombok · Bucket4j · springdoc OpenAPI ·
JUnit 5 · Mockito · Testcontainers

## Architecture
Feature-first packages, each split into hexagonal layers:

```
com.kshitiz.taskforge
├── common/   shared code: exceptions, paging, base entity, error handler, current user, file storage
├── auth/     sign-up, login, JWT tokens, security rules, rate limiting
├── master/   master data (priorities, statuses, roles) served from DB tables
├── user/     users, roles and reporting lines
├── project/  projects and project membership
└── task/     tasks, comments, assignments, attachments, permission policy
    ├── domain/       business objects and rules (plain Java) + repository ports
    ├── application/  services (use cases, transactions) + request/response DTOs
    └── adapter/      REST controllers and JPA persistence
```

## Main features
- **Projects:** managers and admins create projects; the owner adds and removes members.
  Every task belongs to a project, and only members (and admins) can see a project and its tasks.
  Non-members get 404, so a project's existence is not revealed.
- **Tasks:** create, edit, delete (soft), status workflow (TO_DO → IN_PROGRESS → DONE, BLOCKED),
  tags, filtered and paged search, comments, assignees (project members only).
- **Attachments:** upload files to tasks (10 MB max, common document and image types), stored in
  S3-compatible storage and downloaded through the API after a permission check.
- **Master data:** priorities, statuses and roles for dropdowns at `GET /api/master`.

## Authentication
- `POST /api/auth/register` – public sign-up (always creates a MEMBER)
- `POST /api/auth/login` – returns an access token (15 min) and a refresh token (7 days)
- `POST /api/auth/refresh` – exchanges a refresh token for a new pair
- Send `Authorization: Bearer <accessToken>` on every other request.
- Auth endpoints are rate-limited per IP (10 requests/minute by default).
- The first admin is created at startup from `ADMIN_EMAIL` / `ADMIN_PASSWORD` if no admin exists.

## Permissions
First, the user must be a member of the project (admins see every project). Then:

| Action | Member | Project owner | MANAGER | ADMIN |
|---|---|---|---|---|
| See project, its tasks, comments, files | ✅ | ✅ | ✅ (if member) | ✅ |
| Create tasks, comment, upload files | ✅ | ✅ | ✅ (if member) | ✅ |
| Edit, delete or assign a task | own tasks | ✅ | ✅ | ✅ |
| Change a task's status | own or assigned | ✅ | ✅ | ✅ |
| Edit a comment | own | own | own | own |
| Delete a comment | own | own | own | ✅ |
| Delete an attachment | own uploads, own tasks | ✅ | ✅ | ✅ |
| Create a project | – | – | ✅ | ✅ |
| Edit/delete project, manage members | – | ✅ | – | ✅ |
| Create users, change roles or managers, delete users | – | – | – | ✅ |

## Running locally
Requirements: JDK 21, Maven, Docker.

```bash
docker compose up -d        # MySQL on 3306, MinIO on 9000 (console: http://localhost:9001)
cd backend
mvn spring-boot:run         # "dev" profile is the default
```

The `dev` profile already points at the Docker services and supplies a development JWT secret
and an admin account `admin@taskforge.local` / `admin12345`. Never use these outside your machine.

- Swagger UI: http://localhost:8080/swagger-ui.html (log in, then click **Authorize** and paste the access token)
- Health: http://localhost:8080/actuator/health

## Tests
```bash
cd backend
mvn test   # integration tests need Docker running (Testcontainers MySQL + MinIO)
```
