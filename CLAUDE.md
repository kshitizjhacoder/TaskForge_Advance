# CLAUDE.md

Context for Claude Code (and any developer) working on TaskForge. Read this first.

## What this is
TaskForge: a project and task management REST API, built phase by phase as a resume project.
The owner must be able to understand and explain every line and decision in interviews.

- Backend: Java 21 (local builds currently use JDK 17 with `-Djava.version=17` until JDK 21 is
  installed), Spring Boot 3.5, Spring Security + JWT (jjwt), Spring Data JPA, MySQL 8, Flyway,
  MapStruct, Lombok, S3-compatible storage (AWS SDK v2, MinIO locally), Spring AI 1.1 + Google
  Gemini, springdoc OpenAPI.
- Tests: JUnit 5, Mockito, AssertJ, `@WebMvcTest` security slice, Testcontainers (MySQL + MinIO).

## Working rules (always follow)
1. **Every class and every public method gets a Javadoc comment** explaining what it does
   (and why, where it isn't obvious). Same for TypeScript later (JSDoc).
2. **No duplication**: shared code goes in `common/` and is reused (exceptions, paging,
   `Specs`, `PageRequests`, `RestResponses`, `ProblemDetailWriter`, `FileStorage`, ...).
3. **When there are two reasonable design choices, ASK the owner. Never choose silently.**
   Present the options with trade-offs. Obvious conventions don't need a question.
4. **If something fails** (build, Docker, credentials, environment), stop and involve the
   owner instead of hacking around it.
5. Clear, consistent, readable code; small focused classes; meaningful names.
6. Work **phase by phase** (see Roadmap). After each phase, update `README.md`; the owner's
   private study notes `HOW_IT_WORKS.txt` (git-ignored) get a new section too.
7. Never commit secrets. Every secret comes from an environment variable (`.env.example`).

## Git workflow
- One branch per phase from `develop`: `feature/phase-N-<name>`.
- Commit (Conventional Commits, e.g. `feat: ...`), push the branch, then
  `git merge --no-ff` into `develop` and push. `main` is updated only at release points
  (currently at Phase 2).
- Commit/push only when the owner asks or as part of this agreed flow.

## Architecture
Feature-first packages, each split into hexagonal layers. Base package `com.kshitiz.taskforge`.

```
common/   shared: exceptions, paging, BaseJpaEntity, error handler, CurrentUser, FileStorage, config
auth/     sign-up/login/refresh, JWT filter, SecurityConfig, rate limiting, bootstrap admin
master/   master data (priorities, statuses, roles) from DB tables
user/     users, roles, managers
project/  projects + membership
task/     tasks, subtasks, comments, assignments, attachments, TaskAccessPolicy
ai/       AI task breakdown (Gemini), usage limits, guardrails
  each feature: domain/ (pure Java model, rules, ports, policies)
                application/ (one service per feature, DTOs as records)
                adapter/ (rest controllers, persistence: JPA entity + MapStruct mapper + adapter)
```

## Design decisions already made (don't re-ask)
- Errors: plain response bodies; errors are RFC 7807 `ProblemDetail` via `GlobalExceptionHandler`.
  Services throw `common.exception.*` (404/409/422/400/401/403/429/502/503), never HTTP codes.
- Master data lives in DB tables (Flyway-seeded); Java enums mirror them;
  `MasterDataConsistencyCheck` fails startup on mismatch.
- Lombok is used. IDs are UUIDs stored as `BINARY(16)`. Soft delete via Hibernate `@SoftDelete`.
- One service per feature (not one class per use case).
- Domain model is separate from JPA entities (domain + JpaEntity + MapStruct mapper + adapter).
  Domain objects: `create(...)` factory with rules, `restore()` builder for loading from the DB.
- Flyway owns the schema (`ddl-auto=validate`). Never edit an existing migration; add a new `V<n>`.
- Optimistic locking with `@Version` → 409 on conflicting edits.
- Auth: jjwt + custom `JwtAuthenticationFilter`; access token 15 min, **stateless** refresh
  token 7 days (reloads the user on refresh); public sign-up creates MEMBER; first admin from
  `ADMIN_EMAIL`/`ADMIN_PASSWORD`; Bucket4j rate limit on `/api/auth/**`.
- The logged-in user is passed explicitly to services as `CurrentUser actor`.
- Projects: owner + members (no per-project roles). Only MANAGER/ADMIN create projects.
  Every task belongs to a project. Non-members get 404. ADMIN sees everything.
  The project owner has manager-like powers over tasks in their project.
- Permissions: see `TaskAccessPolicy` and `ProjectAccessPolicy` (single source of truth).
- Attachments: S3-compatible storage behind the `FileStorage` port; downloads streamed through
  the API (not pre-signed URLs); storage first then DB on upload, DB then storage (after commit)
  on delete.
- Removing a project member publishes `ProjectMemberRemovedEvent`; the task feature removes
  their assignments (keeps dependencies one-way: task → project).
- AI: only the "task breakdown" feature. Suggestions become **subtasks** (one level,
  `parent_task_id`) only after the user accepts them. Provider chosen by one switch,
  `AI_PROVIDER` (= `spring.ai.model.chat`): `google-genai` | `fake` | `none`. Daily limits per
  user and app-wide stored in the `ai_usage` table. Guardrails: prompt-injection defence,
  Gemini safety settings, JSON output, `SuggestionSanitizer`, one retry, timeout. No
  Resilience4j for now.

## Running and testing
```bash
docker compose up -d              # MySQL 3306, MinIO 9000 (console 9001)
cd backend
mvn spring-boot:run               # dev profile: dev JWT secret, admin@taskforge.local / admin12345, fake AI
mvn -Djava.version=17 clean test  # use "clean": VS Code can leave class files Java 17 can't read
```
- Integration tests (`TaskForgeApiIntegrationTest`) need Docker; they are skipped without it.
  **As of Phase 4 they have never run** because Docker Desktop wasn't working on the dev
  machine; running them is a pending task.
- Swagger UI: http://localhost:8080/swagger-ui.html

## Roadmap / status
- [x] Phase 0–1: cleanup, port of the old project, core API (master data, users, tasks)
- [x] Phase 2: JWT auth, roles, permissions
- [x] Phase 3: projects, membership, S3 attachments
- [x] Phase 4: subtasks, AI task breakdown (Gemini via Spring AI)
- [ ] Phase 5: more tests (ArchUnit layer rules, JaCoCo coverage gate ≥ 80% domain/application)
- [ ] Phase 6: React + TypeScript + Vite frontend (Tailwind, TanStack Query, Kanban board)
- [ ] Phase 7: Dockerfiles, full docker-compose, GitHub Actions CI, deploy (Railway + Vercel),
      `.gitattributes` for line endings
- [ ] Phase 8: README polish, architecture diagram, demo account, v1.0.0 release
- [ ] Phase 9 (stretch): MCP server exposing TaskForge tools

## Known issues
- VS Code's Java extension shows false Lombok errors; trust the Maven build.
- Windows machine: no Python available; heredocs containing apostrophes can break in the Bash
  tool, so prefer the file-writing tools for multi-line content.
