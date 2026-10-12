# Stage 1 handoff to Stage 2

## Starting point

Stage 1 created and seeded the MySQL 8 schema named `slcm_portal`. The user has reported that the Workbench setup and seed checks passed. Preserve the existing database and SQL files. Stage 2 backend source now lives in `backend/`; no frontend or future domain APIs have been added.

## Full table inventory

`users`, `students`, `semesters`, `subjects`, `uploaded_files`, `syllabi`, `grade_scale`, `enrollments`, `assessment_components`, `marks`, `timetable_slots`, `class_sessions`, `attendance_records`, `holidays`, `assignments`, `submissions`, `exams`, `attendance_requests`, `document_types`, `document_requests`, `fee_records`, `payments`, `audit_logs`, `app_settings`.

Important joins: users→students; semesters→subjects/enrollments/exams/fees; students→enrollments/submissions/attendance_requests/document_requests/fees; subjects→syllabi/enrollments/timetable/class sessions/assignments/exams/attendance requests; enrollments→marks/attendance; fee_records→payments; document_requests and syllabi/submissions can reference uploaded_files.

## Demo identities

* `student.demo` / `password` → STUDENT, Meera Iyer, `SLCM2026-001`
* `admin.demo` / `password` → ADMIN
* `student.riya` / `password` → STUDENT, Riya Menon, `SLCM2026-002`

The stored hash is `$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy`, a BCrypt hash for `password` compatible with Spring Security's `BCryptPasswordEncoder`. Stage 2 must execute `matches` in the backend test suite; this worker could not run Maven/Spring or MySQL and must not claim that runtime check passed.

## Backend configuration

Use environment variables, not committed credentials:

```text
SLCM_DB_URL=jdbc:mysql://localhost:3306/slcm_portal?useSSL=false&serverTimezone=UTC
SLCM_DB_USERNAME=<local mysql user>
SLCM_DB_PASSWORD=<local mysql password>
SLCM_JWT_SECRET=<long random local-development secret>
SLCM_FILE_STORAGE_PATH=<local ignored directory>
```

Configure Hibernate with `ddl-auto=validate`; do not let application startup create, update, or wipe this schema. Map `BIGINT UNSIGNED` to Java `Long`, `DECIMAL` to `BigDecimal`, SQL enums to explicit Java enums, `JSON` to a deliberate JSON mapping, and timestamps/dates to `Instant`/`LocalDateTime`/`LocalDate` as appropriate. Preserve the unique constraints and use repositories keyed by the entity relationships rather than trusting student IDs from request bodies.

## Stage 2 delivered

1. Java 21 Spring Boot Maven project and MySQL datasource configuration.
2. Exact JPA mappings for `users` and `students`, with `ddl-auto=validate`.
3. BCrypt login against the real `users.password_hash` column.
4. Signed stateless JWT issuance and bearer validation.
5. `/api/health`, `/api/auth/login`, `/api/auth/me`, and role smoke endpoints.
6. Validation, authentication/authorization failure handling, unit/web tests, and API/architecture/testing docs.

The exact authenticated identity for `/api/auth/me` comes from `Authentication.getName()` populated by the validated JWT; it is never accepted from a client-supplied user or student ID.

## Recommended Stage 3 order

1. Install Maven and execute `mvn test`; configure the environment variables and run the app against the existing database to prove Hibernate validation.
2. Add entities/repositories for semesters, subjects, enrollments, marks, sessions, and attendance.
3. Implement and test student dashboard, attendance, marks, CGPA, assignments, exams, and syllabus APIs.
4. Add documents, fees/payments, and admin APIs after the student read flows are stable. Keep audit logging for state-changing admin actions.

## Required environment and verification

Run the SQL in MySQL Workbench, inspect the result grids from `99_verify_queries.sql`, and save the actual row counts/errors in the next status update. Verify there are 24 tables, zero orphan failures, four expected Meera attendance ratios, no out-of-range marks, one verified and one submitted payment, and the expected document statuses. Then run BCrypt matching from a Spring test.

## Stage 3 delivered

Stage 3 adds `docs/API_INVENTORY.md` and the complete route families under `student`, `admin`, and `finance`. Student routes use the JWT username to resolve the student profile. Academic reads and derived calculations use parameterized JdbcTemplate queries. Admin mutations are DTO-validated, role protected, transactional where needed, and audited. Payment verification is restricted to admins and receipts are available only for verified payments. Uploads use safe generated storage names and integrity checks. A later verification pass corrected configuration and route-level issues, but no Maven/MySQL runtime proof is available in this worker.

The Stage 3 implementation intentionally does not modify the database schema or SQL scripts. Receipts are HTML downloads rather than PDFs. SGPA is explicitly undefined until official university rules are supplied; the documented provisional CGPA calculation remains `sum(recorded marks)/5`, rounded to two decimals. Hibernate `ddl-auto=validate` covers only the JPA-mapped authentication entities; JdbcTemplate table compatibility must be checked separately against the SQL and verification queries.

## Next handoff

Before frontend work, install Maven and run `mvn test` from `backend/`, then run the application against the existing `slcm_portal` database with `ddl-auto=validate`. Exercise login, student dashboard/attendance/marks/document/fee flows, admin CRUD/review/payment verification, and multipart upload/download flows. Fix any compile or schema-validation issues found in that runtime pass.

## Stage 4 frontend handoff

`frontend/` contains the Vite React app. Configure `VITE_API_BASE_URL`, run `npm install`, then `npm run dev`. Implemented routes are `/login`, `/`, `/subjects/:id/syllabus`, `/attendance/:id`, `/focus`, `/marks`, `/reports`, `/documents`, `/payments`, and protected `/admin`. Connected flows include JWT authentication, dashboard, focus-mode persistence, subjects/semesters/schedule/attendance/assignments, multipart assignment submission, syllabus/history views, marks analysis, CGPA trend/report, document requests/downloads, fee/payment submission, verified receipt downloads, and admin document/payment review. The supplied references establish the gold/yellow/white visual system, bordered panels, wide tables, compact gold controls, and collapsible left navigation; those patterns are shared by the implemented pages.

`npm run build` passed in this workspace. Backend runtime integration and authenticated browser checks remain local verification tasks because Maven was unavailable and the MySQL password was not available to this worker.
