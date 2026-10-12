# SLCM database guide

## Scope and setup

The database is MySQL 8 with InnoDB, `utf8mb4`, and snake_case identifiers. Run the four scripts from repository root in numeric order. `01_schema.sql` is non-destructive and creates `slcm_portal` if absent. The other seed scripts use natural-key idempotency (`INSERT IGNORE`) and do not delete rows. For a deterministic first install, use a new empty local schema.

Example client command (replace the placeholder password locally; do not commit it):

```bash
mysql -u <user> -p < database/01_schema.sql
mysql -u <user> -p slcm_portal < database/02_reference_data.sql
mysql -u <user> -p slcm_portal < database/03_demo_data.sql
mysql -u <user> -p slcm_portal < database/99_verify_queries.sql
```

The supplied seed reference date is **2026-10-11**. Demo assignment and exam dates are after that date so an initial dashboard has upcoming work. To refresh dates later, update `app_settings.seed.reference_date` and the date literals in the demo script as one documented change; do not make the database depend on server startup time.

## Table inventory and relationships

There are 24 tables:

* `users` stores login identity, BCrypt hash, role, and enabled state. `students.user_id` is a one-to-one extension for student profile data.
* `semesters` owns academic periods. `subjects.semester_id` assigns the catalogue subject to a period. `enrollments` joins students, subjects, and semesters and optionally points at `grade_scale` for a finalized grade.
* `grade_scale` and `assessment_components` are configurable reference tables. `marks` joins an enrollment to a component and records the entering admin.
* `timetable_slots` describes the weekly slot; `class_sessions` materializes a dated occurrence; `attendance_records` joins a session to an enrollment. Attendance percentage, classes needed, and leaves allowed are derived, never stored.
* `holidays` supports working-day calculations.
* `assignments` belong to subjects and creators. `submissions` is unique per assignment/student and optionally references `uploaded_files`.
* `exams` joins a subject and semester. `syllabi` is one per subject and contains text or an optional uploaded file.
* `attendance_requests` supports student undertaking/condonation review.
* `document_types` is the catalogue; `document_requests` joins a student and type and optionally points to an issued file.
* `fee_records` is unique per student/semester. `payments` records submitted, verified, or rejected attempts. Paid status is derived by summing only verified payments; a verified payment must have verifier, time, and receipt number.
* `audit_logs` records actor and JSON details. `app_settings` stores configurable rules such as the attendance threshold and provisional CGPA rule.

All foreign keys are explicit. The schema uses `CHECK` constraints for positive amounts, date/time ranges, percentage ranges, and verified-payment fields. The later JPA project should use `spring.jpa.hibernate.ddl-auto=validate`.

## Rules represented by the schema

* Attendance threshold defaults to 75% in `app_settings`. `LEAVE` is recorded distinctly but is not counted as attended; the backend should define/report its policy consistently.
* Required derived attendance formula for attended `a`, held `t`: `max(0, ceil(3*t - 4*a))` classes needed to reach 75%, assuming every future class is attended. Leaves allowed with `r` future sessions: `floor(a + r - 0.75*(t+r))`.
* The project-specific provisional CGPA rule is `SUM(included marks) / 5`, rounded to two decimals. This is not claimed to be an official university rule. The table structure supports replacing it with a configured policy later; do not add an independently editable CGPA column.
* The seeded grade scale is explicitly provisional: A+=10 (90–100), A=9 (80–89.99), B=8 (70–79.99), C=7 (60–69.99), D=6 (50–59.99), F=0. It must be confirmed before production use.

## Seed data

The primary fictional student is Meera Iyer (`SLCM2026-001`) and the second is Riya Menon (`SLCM2026-002`). Meera has four current-semester enrollments, marks, future exams, assignments, a low-OS-attendance undertaking, requested/approved documents, and pending plus verified fee examples. Attendance records intentionally produce 80% Crypto, 70% DAA, 60% OS, and 90% ACD from their underlying sessions.

BCrypt hash value used for all local demo accounts is the Spring-compatible `$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy`, documented for password `password`. A Spring Security `BCryptPasswordEncoder.matches("password", storedHash)` check should be run by Stage 2. No plaintext password is inserted into `users`.
