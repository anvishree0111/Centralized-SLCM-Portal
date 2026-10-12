# Stage 2 backend architecture

`backend/` is a Java 21 Spring Boot application. Controllers expose the small Stage 2 foundation (`/api/health`, authentication, and role smoke endpoints); services contain login behavior; Spring Data repositories map only the existing `users` and `students` tables. Hibernate is configured with `ddl-auto=validate`, so startup never creates or changes the Stage 1 schema.

Authentication is stateless: login verifies the existing BCrypt `users.password_hash`, issues a signed HS256 JWT, and the bearer filter loads the enabled user and role on each request. Authorization uses `ROLE_STUDENT`/`ROLE_ADMIN`. Secrets and database settings come only from environment-backed Spring properties.

Stage 3 adds parameterized `JdbcTemplate` services and DTO controllers for student academic/attendance APIs, admin academic management, and document/finance workflows. This deliberately avoids mapping all 24 tables as mutable JPA entities in this milestone; the existing schema remains authoritative and Hibernate validation still covers the Stage 2 entities. Student identity is resolved from `users.username` to `students.id`; admin writes use transactions and append to `audit_logs`. Derived attendance and verified-payment totals are calculated at query/service time.

File-backed document and assignment uploads use generated filenames under `SLCM_DOCUMENTS_PATH` (default `./storage/documents`), with MIME/signature, size, hash, and path checks. Receipts are downloadable HTML in this stage; PDF generation remains a later enhancement.
