
USE slcm_portal;
START TRANSACTION;

INSERT IGNORE INTO app_settings (setting_key, setting_value, description) VALUES
 ('attendance.threshold.percent', '75', 'Minimum attendance percentage before a warning is shown'),
 ('academic.cgpa.rule', 'SUM_INCLUDED_MARKS_DIVIDED_BY_5', 'Provisional project rule; backend calculation remains configurable'),
 ('academic.cgpa.rounding.scale', '2', 'Round calculated CGPA to two decimal places'),
 ('seed.reference_date', '2026-10-11', 'Fixed date used by the Stage 1 demo data');

INSERT IGNORE INTO grade_scale (letter, grade_points, min_percent, max_percent) VALUES
 ('A+', 10.00, 90.00, 100.00),
 ('A', 9.00, 80.00, 89.99),
 ('B', 8.00, 70.00, 79.99),
 ('C', 7.00, 60.00, 69.99),
 ('D', 6.00, 50.00, 59.99),
 ('F', 0.00, 0.00, 49.99);

INSERT IGNORE INTO assessment_components (code, max_marks, weight_percent) VALUES
 ('MID_TERM', 30.00, 30.00),
 ('END_TERM', 70.00, 70.00);

INSERT IGNORE INTO document_types (code, name, description, active) VALUES
 ('BONAFIDE', 'Bonafide Certificate', 'Proof of current student enrollment', TRUE),
 ('NOC', 'No Objection Certificate', 'Certificate issued for an approved purpose', TRUE),
 ('TRANSCRIPT', 'Transcript', 'Official academic record', TRUE);

INSERT IGNORE INTO semesters (semester_no, label, start_date, end_date, is_current) VALUES
 (1, 'Semester I', '2024-07-01', '2024-12-20', FALSE),
 (2, 'Semester II', '2025-01-06', '2025-06-20', FALSE),
 (3, 'Semester III', '2025-07-01', '2025-12-20', FALSE),
 (4, 'Semester IV', '2026-07-01', '2026-12-20', TRUE);

COMMIT;
