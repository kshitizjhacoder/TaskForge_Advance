# TaskForge

Project and task management API with AI-assisted planning. Work in progress; a full README
with a live demo, screenshots and architecture diagram comes with v1.0.

## Tech stack (so far)
Java 21 · Spring Boot 3.5 · Spring Data JPA · MySQL 8 · Flyway · MapStruct · Lombok ·
springdoc OpenAPI · JUnit 5 · Mockito · Testcontainers

## Architecture
Feature-first packages, each split into hexagonal layers:

```
com.kshitiz.taskforge
├── common/   shared code: exceptions, paging, base entity, error handler, config
├── master/   master data (priorities, statuses, roles) served from DB tables
├── user/     users and reporting lines
└── task/     tasks, comments, assignments
    ├── domain/       business objects and rules (plain Java) + repository ports
    ├── application/  services (use cases, transactions) + request/response DTOs
    └── adapter/      REST controllers and JPA persistence
```

## Running locally
Requirements: JDK 21, Maven, MySQL 8 (or Docker).

```bash
# set DB_URL, DB_USERNAME, DB_PASSWORD (see .env.example)
cd backend
mvn spring-boot:run
```

- Swagger UI: http://localhost:8080/swagger-ui.html
- Health: http://localhost:8080/actuator/health

## Tests
```bash
cd backend
mvn test   # integration tests need Docker running (Testcontainers MySQL)
```
