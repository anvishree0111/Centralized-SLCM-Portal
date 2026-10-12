# Project status

## Stage 1 — Database and seed data

**Implementation status:** Files created; SQL execution remains unverified in this worker because the `mysql` client/server is not available on PATH.

### Delivered

* `database/01_schema.sql`: non-destructive MySQL 8 schema with 24 InnoDB tables, keys, foreign keys, indexes, and checks.
* `database/02_reference_data.sql`: settings, provisional grade scale, assessment components, document types, and four semesters.
* `database/03_demo_data.sql`: fictional users/students, current subjects/enrollments, marks, sessions/attendance, assignments, exams, undertakings, documents, fees, payments, and audit seed.
* `database/99_verify_queries.sql`: inventory, constraints, orphan checks, attendance derivation, mark bounds, payment rules, document rules, and upcoming-work queries.
* `README.md`, `docs/DATABASE.md`, `docs/DECISIONS.md`, and `docs/HANDOFF.md`.

### Checks actually performed

* Inspected the worker workspace before writing; it contained only OmniRush configuration and attachment files, not an existing application repository.
* Confirmed Java 24 and Python 3.14 are present.
* Confirmed Maven and the MySQL client are not available on PATH.
* SQL was reviewed statically for creation order and explicit relationships, but no SQL script was executed. Therefore Stage 1 is **not runtime-verified** until MySQL Workbench or the `mysql` client runs the scripts and verification suite.

### Acceptance gaps

* Run all four scripts against a clean MySQL 8 instance.
* Run the BCrypt `matches` check in a Spring Security/BCrypt-capable environment for the documented demo hash.
* Review the provisional academic rules with the product owner before production use.

The next AI must not describe the database as runtime-tested until those checks are complete.

## Stage 2 — Backend foundation and authentication

**Implementation status:** Source added under `backend/`; runtime verification is pending because Maven and the MySQL client/server are unavailable in this worker.

Delivered Java 21/Spring Boot Web, JPA/MySQL, Security, validation, jjwt, BCrypt, exact `users`/`students` mappings, stateless bearer authentication, `/api/health`, `/api/auth/login`, `/api/auth/me`, role smoke endpoints, safe errors, environment configuration, tests, and architecture/API/testing docs. Stage 1 SQL files were not modified. A controller identity lookup was explicitly based on `Authentication.getName()` so `/api/auth/me` derives identity from the validated security context.

Checks not run: `mvn test`, application startup, Hibernate `ddl-auto=validate`, and MySQL-backed API flow. The BCrypt test is written against the exact 60-character Stage 1 hash but was not executed here. Run these after installing Maven and MySQL 8; do not rerun or modify the already-verified database unnecessarily.

## Stage 3 — Complete backend API layer

**Implementation status:** Student academic, attendance, assignments, exams, syllabus, focus-mode, marks/prediction, CGPA/report, documents, fees/payments, and admin management/review routes are implemented under `backend/src/main/java/com/slcm/portal/{student,admin,finance}`. `docs/API_INVENTORY.md` is the endpoint checklist. Stage 1 SQL files remain unchanged and the frontend remains intentionally deferred.

Implemented calculations use the 75% attendance threshold, record-derived attendance, future timetable occurrences excluding holidays, verified-only financial totals, and the documented provisional `sum(recorded marks)/5` CGPA policy. SGPA is returned as undefined rather than inventing an official formula.

Runtime status: `mvn test` was attempted but Maven and a Maven wrapper are unavailable. Java 24 is present, not Java 21. The MySQL80 service and MySQL 8 client are present, but the attempted non-interactive `root` connection was rejected because no password was supplied. Therefore Stage 3 compilation/tests, Hibernate schema validation, SQL-backed endpoint flows, file persistence, and authorization integration tests are not claimed as executed. Focused calculation tests and route code are written but unexecuted.

### Verification corrections

* Removed the insecure default database credentials from `backend/src/main/resources/application.yml`; DB URL, username, and password are now required environment properties.
* Preserved `spring.jpa.hibernate.ddl-auto=validate`.

## Stage 4 — React frontend foundation

Initial React/Vite foundation added under `frontend/`, following the supplied dashboard, marks analysis, CGPA report, documents, payment history, focus-mode, and navigation references. Login uses `POST /api/auth/login` and validates identity with `GET /api/auth/me`; student screens call documented Stage 3 endpoints without fabricated records. No SQL or backend files were changed by Stage 4.

Node 24.13.1 and npm 11.8.0 were available. `npm install` and `npm run build` completed successfully in `frontend/`. No backend runtime or visual browser comparison was performed.
* Confirmed the public `GET /api/document-types` path is not blocked by an admin-only service annotation; admin-only document-type mutations remain guarded.
* Added multipart assignment submission support using the existing `uploaded_files` table and secure generated storage names.
* Added safe `ResponseStatusException`/unexpected-error JSON handling and status-transition checks for document processing.
* Added session update/delete route coverage and payment fee-total/verified-payment safeguards.

These are source corrections, not runtime proof.

## Final combined integration delivery

The existing React/Vite frontend is integrated with the Stage 2/3 REST contracts through `frontend/src/services/api.js`, JWT session storage, protected student/admin routes, dashboard/academic/attendance/assignment/report/document/payment screens, and the admin document/payment review surface. Syllabus and attendance-history routes were added, and Focus Mode now persists its preference through `/api/me/preferences`.

### Checks

* `npm run build` from `frontend/`: **passed** after the final route and Focus Mode changes.
* `mvn test` from `backend/`: **blocked**, Maven is unavailable.
* Backend startup, MySQL-backed API calls, browser authentication, file uploads, payment verification, and visual comparison: **not runtime-verified**.
* `slcm-portal-final.zip` was created and checked to contain `backend/`, `frontend/`, `database/`, and `docs/`, while excluding `node_modules`, `frontend/dist`, `backend/target`, `.env`, and workspace caches.
