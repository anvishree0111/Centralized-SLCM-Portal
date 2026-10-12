# Stage 2 API contract

* `GET /api/health` → `{"status":"ok"}` (public).
* `POST /api/auth/login` with `{ "username": "...", "password": "..." }` → `{ "token", "username", "role" }`. Missing fields return 400; unknown, disabled, or incorrect credentials return the same 401 response.
* `GET /api/auth/me` with `Authorization: Bearer <token>` → `{ "id", "username", "role", "enabled" }`.
* `GET /api/test/student` requires STUDENT; `GET /api/test/admin` requires ADMIN. These are security smoke endpoints and not future domain APIs.

Errors are JSON with timestamp/status/error/message/path where handled by the application. Authentication and authorization failures never disclose whether a username exists.

## Stage 3 resources

The complete Stage 3 endpoint inventory, including request DTOs, response shapes, tables, rules, and test expectations, is in `docs/API_INVENTORY.md`. Implemented route groups are:

* Student: `/api/student/dashboard`, `/semesters`, `/subjects`, `/schedule`, `/assignments`, `/exams`, `/subjects/{id}/syllabus`, `/focus-mode`, attendance, marks/prediction, CGPA/report, documents, fees, payments, and `/api/me/preferences`.
* Admin: CRUD and review routes under `/api/admin` for students, semesters, subjects, enrollments, timetable, class sessions/attendance, components, grade scale, marks, assignments/submissions, exams, syllabi, attendance requests, settings, audit logs, documents, fees, and payments.

Student routes always resolve the student through the authenticated username. Admin mutations use validated DTOs and write `audit_logs`. Payment submissions are `SUBMITTED` until an administrator verifies them; only `VERIFIED` rows count toward paid totals and produce receipts.
