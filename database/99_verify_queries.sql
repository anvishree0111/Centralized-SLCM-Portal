-- Read-only verification suite. Run after all three setup scripts.
USE slcm_portal;

-- 1. Inventory: expected table count is 24.
SELECT COUNT(*) AS table_count
FROM information_schema.tables
WHERE table_schema = DATABASE();
SELECT table_name FROM information_schema.tables
WHERE table_schema = DATABASE() ORDER BY table_name;

-- 2. Constraints and indexes.
SELECT table_name, constraint_name, constraint_type
FROM information_schema.table_constraints
WHERE table_schema = DATABASE()
ORDER BY table_name, constraint_type, constraint_name;
SELECT table_name, index_name, column_name, non_unique
FROM information_schema.statistics
WHERE table_schema = DATABASE()
ORDER BY table_name, index_name, seq_in_index;

-- 3. Seed counts and demo identities. password_hash must begin with $2a$.
SELECT role, COUNT(*) AS users FROM users GROUP BY role ORDER BY role;
SELECT username, role, enabled, CHAR_LENGTH(password_hash) AS hash_length,
       LEFT(password_hash, 4) AS hash_prefix
FROM users ORDER BY username;
SELECT reg_no, full_name, username, program, section
FROM students JOIN users ON users.id = students.user_id ORDER BY reg_no;

-- 4. Parent/child relationship orphan checks: each query should return zero.
SELECT 'orphan_students' AS check_name, COUNT(*) AS failures
FROM students s LEFT JOIN users u ON u.id=s.user_id WHERE u.id IS NULL
UNION ALL SELECT 'orphan_subjects', COUNT(*) FROM subjects s LEFT JOIN semesters x ON x.id=s.semester_id WHERE x.id IS NULL
UNION ALL SELECT 'orphan_enrollments', COUNT(*) FROM enrollments e LEFT JOIN students s ON s.id=e.student_id LEFT JOIN subjects x ON x.id=e.subject_id LEFT JOIN semesters m ON m.id=e.semester_id WHERE s.id IS NULL OR x.id IS NULL OR m.id IS NULL
UNION ALL SELECT 'orphan_marks', COUNT(*) FROM marks m LEFT JOIN enrollments e ON e.id=m.enrollment_id LEFT JOIN assessment_components c ON c.id=m.component_id WHERE e.id IS NULL OR c.id IS NULL
UNION ALL SELECT 'orphan_attendance', COUNT(*) FROM attendance_records a LEFT JOIN class_sessions c ON c.id=a.session_id LEFT JOIN enrollments e ON e.id=a.enrollment_id WHERE c.id IS NULL OR e.id IS NULL
UNION ALL SELECT 'orphan_payments', COUNT(*) FROM payments p LEFT JOIN fee_records f ON f.id=p.fee_record_id WHERE f.id IS NULL
UNION ALL SELECT 'orphan_documents', COUNT(*) FROM document_requests d LEFT JOIN students s ON s.id=d.student_id LEFT JOIN document_types t ON t.id=d.document_type_id WHERE s.id IS NULL OR t.id IS NULL;

-- 5. Attendance must be derived from records. Expected Meera ratios: 80%, 70%, 60%, 90%.
SELECT s.short_name, COUNT(a.id) AS held,
       SUM(a.status='PRESENT') AS attended,
       ROUND(100 * SUM(a.status='PRESENT') / COUNT(a.id), 2) AS attendance_percent,
       GREATEST(0, CEIL(3 * COUNT(a.id) - 4 * SUM(a.status='PRESENT'))) AS classes_needed_for_75
FROM attendance_records a
JOIN enrollments e ON e.id=a.enrollment_id
JOIN subjects s ON s.id=e.subject_id
JOIN students st ON st.id=e.student_id
WHERE st.reg_no='SLCM2026-001'
GROUP BY e.id, s.short_name ORDER BY s.short_name;
SELECT COUNT(*) AS attendance_count_mismatches
FROM (
  SELECT e.id, COUNT(a.id) held, SUM(a.status='PRESENT') attended
  FROM enrollments e LEFT JOIN attendance_records a ON a.enrollment_id=e.id
  GROUP BY e.id
) x WHERE attended > held OR held = 0;

-- 6. Marks must not exceed a component's max marks.
SELECT m.id, m.marks_obtained, c.max_marks
FROM marks m JOIN assessment_components c ON c.id=m.component_id
WHERE m.marks_obtained < 0 OR m.marks_obtained > c.max_marks;

-- 7. Fees: only VERIFIED payments count toward paid amount; submitted payment is not paid.
SELECT fr.id, se.label, fr.total_amount,
       COALESCE(SUM(CASE WHEN p.status='VERIFIED' THEN p.amount ELSE 0 END),0) AS verified_amount,
       CASE WHEN COALESCE(SUM(CASE WHEN p.status='VERIFIED' THEN p.amount ELSE 0 END),0) >= fr.total_amount THEN 'PAID' ELSE 'PENDING' END AS derived_status
FROM fee_records fr JOIN semesters se ON se.id=fr.semester_id
LEFT JOIN payments p ON p.fee_record_id=fr.id
WHERE fr.student_id=(SELECT id FROM students WHERE reg_no='SLCM2026-001')
GROUP BY fr.id, se.label, fr.total_amount ORDER BY se.semester_no;
SELECT id, status, verified_at, verified_by, receipt_no
FROM payments WHERE status='VERIFIED' AND (verified_at IS NULL OR verified_by IS NULL OR receipt_no IS NULL);

-- 8. Document workflow statuses and issued-file rule.
SELECT status, COUNT(*) AS requests FROM document_requests GROUP BY status ORDER BY status;
SELECT id, status FROM document_requests WHERE status='APPROVED' AND issued_file_id IS NULL;

-- 9. Upcoming student dashboard records using the fixed Stage 1 reference date.
SELECT a.title, s.short_name, a.due_at FROM assignments a JOIN subjects s ON s.id=a.subject_id
WHERE a.due_at > '2026-10-11' ORDER BY a.due_at;
SELECT e.exam_date, s.short_name, e.venue FROM exams e JOIN subjects s ON s.id=e.subject_id
WHERE e.exam_date > '2026-10-11' ORDER BY e.exam_date;
