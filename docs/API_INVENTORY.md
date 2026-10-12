# Stage 3 API inventory

This inventory is derived from the Stage 1 implementation plan supplied in the project brief, the existing 24-table schema, and the Stage 2 contract. No separate implementation-plan file was present in the workspace. Stage 2 endpoints remain unchanged. Every protected endpoint requires a valid JWT; `/api/student/**` requires `STUDENT`, `/api/admin/**` requires `ADMIN`.

Status values: **implemented / runtime-unverified** means code and route exist but Maven/MySQL execution was unavailable; **implemented / unit-tested-not-run** means a focused test exists but was not executed; **implemented / runtime-tested** is intentionally not used in this workspace.

## Existing foundation

| Method | URL | Role | Purpose / response | Main tables |
|---|---|---|---|---|
| GET | `/api/health` | public | Health `{status}` | none | implemented / runtime-unverified |
| POST | `/api/auth/login` | public | Validated username/password → JWT, username, role | users | implemented / runtime-unverified |
| GET | `/api/auth/me` | authenticated | Safe current user profile | users, students | implemented / runtime-unverified |

## Student APIs

| Method | URL | Purpose / response | Tables and rules |
|---|---|---|---|
| GET | `/api/student/dashboard` | Aggregated current semester, schedule, assignments, attendance, performance, GPA | students, semesters, subjects, enrollments, timetable, attendance, assignments, marks | implemented / runtime-unverified |
| GET | `/api/student/semesters` | Student-visible semester list | semesters, enrollments |
| GET | `/api/student/semesters/{semesterId}` | Semester summary and enrolled subjects | semesters, enrollments, subjects |
| GET | `/api/student/subjects` | Current-semester subjects/enrollments | enrollments, subjects |
| GET | `/api/student/schedule/today` | Today’s schedule with attendance | timetable_slots, subjects, enrollments, attendance |
| GET | `/api/student/schedule` | Full timetable | timetable_slots, subjects |
| GET | `/api/student/assignments?due=true` | Assignments and own submission status | assignments, submissions, subjects |
| GET | `/api/student/assignments/{assignmentId}` | Assignment detail and own status | assignments, submissions |
| POST | `/api/student/assignments/{assignmentId}/submissions` | Submit note/file metadata; validates ownership and deadline | submissions, assignments, students, uploaded_files |
| GET | `/api/student/exams` | Upcoming exams | exams, subjects, semesters |
| GET | `/api/student/subjects/{subjectId}/syllabus` | Subject syllabus | syllabi, subjects, enrollments |
| GET | `/api/student/focus-mode` | Exams, alerts, bell count | exams, assignments, attendance, attendance_requests |
| PUT | `/api/me/preferences` | Update focus mode preference | students; current user only |
| GET | `/api/student/attendance/summary` | Derived percentage, held, attended, needed, leaves | attendance_records, enrollments, subjects, app_settings |
| GET | `/api/student/attendance/{subjectId}` | Subject attendance history | attendance_records, class_sessions, enrollments |
| GET | `/api/student/attendance/requests` | Own undertaking history | attendance_requests |
| POST | `/api/student/attendance-requests` | Create undertaking/condonation | attendance_requests; current student only |
| GET | `/api/student/marks/analysis` | Marks by subject/component, strong/weak analysis | marks, components, enrollments, subjects, grade_scale |
| POST | `/api/student/marks/predict` | Validated hypothetical marks and predicted grade/GPA | grade_scale, marks; no persistence |
| GET | `/api/student/grade-scale` | Configured grade scale | grade_scale |
| GET | `/api/student/cgpa/trend` | Semester GPA/CGPA trend | enrollments, marks, grade_scale, semesters |
| GET | `/api/student/semesters/{semesterId}/report` | Course/credits/grade report | enrollments, subjects, grade_scale |
| GET | `/api/document-types` | Active document catalogue | document_types |
| GET | `/api/student/document-requests` | Own document request history | document_requests, document_types |
| POST | `/api/student/document-requests` | Request active document type | document_requests |
| GET | `/api/student/document-requests/{id}` | Own request detail | document_requests |
| GET | `/api/student/document-requests/{id}/download` | Download/receipt metadata only when issued | document_requests, uploaded_files |
| GET | `/api/student/fees?semesterId=` | Own fee records with verified paid/pending totals | fee_records, payments |
| POST | `/api/student/fees/{feeId}/payments` | Submit payment reference; remains SUBMITTED | fee_records, payments |
| GET | `/api/student/payments` | Own payment history | payments, fee_records |
| GET | `/api/student/payments/{paymentId}/receipt` | Receipt only for VERIFIED payment | payments |

## Admin APIs

Admin resources use DTOs, validation, and audit records for mutations. IDs are path/query selectors, but all relationships are checked before writes.

| Resource | Endpoints |
|---|---|
| Students | `GET/POST /api/admin/students`, `GET/PUT /api/admin/students/{id}`, `DELETE /api/admin/students/{id}` |
| Semesters | `GET/POST /api/admin/semesters`, `GET/PUT /api/admin/semesters/{id}`, `DELETE /api/admin/semesters/{id}` |
| Subjects | `GET/POST /api/admin/subjects`, `GET/PUT /api/admin/subjects/{id}`, `DELETE /api/admin/subjects/{id}` |
| Enrollments | `GET/POST /api/admin/enrollments`, `PUT/DELETE /api/admin/enrollments/{id}` |
| Timetable | `GET/POST /api/admin/timetable`, `PUT/DELETE /api/admin/timetable/{id}` |
| Sessions/attendance | `GET/POST /api/admin/class-sessions`, `GET /api/admin/class-sessions/{id}/attendance`, `PUT /api/admin/class-sessions/{id}/attendance` |
| Components/grade scale | `GET/POST/PUT/DELETE /api/admin/assessment-components/{id}`, `GET/POST/PUT/DELETE /api/admin/grade-scale/{id}` |
| Marks | `GET /api/admin/marks`, `POST/PUT /api/admin/marks` |
| Assignments | `GET/POST/PUT/DELETE /api/admin/assignments/{id}` |
| Submissions | `GET /api/admin/submissions`, `PUT /api/admin/submissions/{id}/review` |
| Exams | `GET/POST/PUT/DELETE /api/admin/exams/{id}` |
| Syllabi | `GET/POST/PUT /api/admin/syllabi/{subjectId}` |
| Attendance requests | `GET /api/admin/attendance-requests`, `PUT /api/admin/attendance-requests/{id}/review` |
| Documents | `GET /api/admin/document-requests`, `PUT /api/admin/document-requests/{id}/status` |
| Fees | `GET/POST/PUT/DELETE /api/admin/fees/{id}` |
| Payments | `GET /api/admin/payments`, `PUT /api/admin/payments/{id}/verify`, `PUT /api/admin/payments/{id}/reject` |
| Settings | `GET/PUT /api/admin/settings/{key}` |
| Audit | `GET /api/admin/audit-logs` |

## Cross-cutting requirements

All request bodies use validation DTOs; responses never expose entities or password hashes. Student queries resolve the student profile from the authenticated username. Attendance uses 75% and `max(0, ceil(3t - 4a))`; leaves allowed use `floor(a+r-0.75(t+r))`. Fee paid totals sum only `VERIFIED` payments. The project CGPA rule remains `sum(included marks) / 5`, rounded to two decimals; no conventional credit-weighted replacement is introduced.

## Verification status by route group

| Route group | Code status | Test status | Runtime status |
|---|---|---|---|
| Stage 2 health/auth/me | Implemented | Existing tests written | Not executed: Maven unavailable |
| Student academic/dashboard/schedule/assignments | Implemented | Calculation tests written; controller/database tests absent | Not executed: Maven/MySQL unavailable |
| Student attendance/marks/GPA/CGPA | Implemented with provisional CGPA and undefined SGPA policy | Calculation tests written; database isolation tests absent | Not executed |
| Student documents/fees/payments/receipts | Implemented | No executed workflow tests | Not executed |
| Admin academic management | Implemented | No focused admin integration tests | Not executed |
| Admin documents/fees/payments | Implemented | No focused finance integration tests | Not executed |
| Multipart storage/downloads | Implemented with HTML receipts and configured local storage | No executed file tests | Not executed |

No endpoint is claimed as runtime-tested. The original plan's future frontend, PDF generation, and official SGPA policy remain outside this verification scope.
