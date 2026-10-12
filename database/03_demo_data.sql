-- Fictional, repeatable local-development data. Run after 01_schema.sql and
-- 02_reference_data.sql. The BCrypt hash below is the standard Spring
-- Security-compatible hash for the demo password "password".
USE slcm_portal;
START TRANSACTION;

INSERT IGNORE INTO users (username, password_hash, role, enabled) VALUES
 ('student.demo', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', 'STUDENT', TRUE),
 ('admin.demo', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', 'ADMIN', TRUE),
 ('student.riya', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', 'STUDENT', TRUE);

SET @student_user_id = (SELECT id FROM users WHERE username = 'student.demo');
SET @admin_user_id = (SELECT id FROM users WHERE username = 'admin.demo');
SET @riya_user_id = (SELECT id FROM users WHERE username = 'student.riya');

INSERT IGNORE INTO students (user_id, reg_no, full_name, email, program, section, focus_mode_enabled) VALUES
 (@student_user_id, 'SLCM2026-001', 'Meera Iyer', 'meera.iyer@example.test', 'B.Tech Computer Science', 'CSE-A', FALSE),
 (@riya_user_id, 'SLCM2026-002', 'Riya Menon', 'riya.menon@example.test', 'B.Tech Computer Science', 'CSE-A', TRUE);
SET @student_id = (SELECT id FROM students WHERE reg_no = 'SLCM2026-001');
SET @riya_id = (SELECT id FROM students WHERE reg_no = 'SLCM2026-002');
SET @sem1 = (SELECT id FROM semesters WHERE semester_no = 1);
SET @sem2 = (SELECT id FROM semesters WHERE semester_no = 2);
SET @sem3 = (SELECT id FROM semesters WHERE semester_no = 3);
SET @sem4 = (SELECT id FROM semesters WHERE semester_no = 4);

INSERT IGNORE INTO subjects (semester_id, code, name, short_name, credits) VALUES
 (@sem1, 'CS101', 'Programming Fundamentals', 'PF', 4),
 (@sem2, 'CS201', 'Data Structures and Algorithms', 'DAA', 4),
 (@sem3, 'CS301', 'Database Management Systems', 'DBMS', 4),
 (@sem4, 'CS401', 'Cryptography', 'Crypto', 4),
 (@sem4, 'CS402', 'Design and Analysis of Algorithms', 'DAA', 4),
 (@sem4, 'CS403', 'Operating Systems', 'OS', 4),
 (@sem4, 'CS404', 'Automata and Compiler Design', 'ACD', 3);
SET @crypto = (SELECT id FROM subjects WHERE code = 'CS401');
SET @daa = (SELECT id FROM subjects WHERE code = 'CS402');
SET @os = (SELECT id FROM subjects WHERE code = 'CS403');
SET @acd = (SELECT id FROM subjects WHERE code = 'CS404');

INSERT IGNORE INTO syllabi (subject_id, content) VALUES
 (@crypto, 'Number theory, symmetric cryptography, public-key cryptography, hashes and digital signatures.'),
 (@daa, 'Asymptotic analysis, divide and conquer, greedy algorithms, dynamic programming and graph algorithms.'),
 (@os, 'Processes, threads, CPU scheduling, memory management, file systems and deadlocks.'),
 (@acd, 'Regular languages, context-free grammars, parsing, automata and compiler phases.');

INSERT IGNORE INTO enrollments (student_id, subject_id, semester_id, status) VALUES
 (@student_id, @crypto, @sem4, 'ENROLLED'), (@student_id, @daa, @sem4, 'ENROLLED'),
 (@student_id, @os, @sem4, 'ENROLLED'), (@student_id, @acd, @sem4, 'ENROLLED'),
 (@riya_id, @crypto, @sem4, 'ENROLLED'), (@riya_id, @daa, @sem4, 'ENROLLED'),
 (@riya_id, @os, @sem4, 'ENROLLED'), (@riya_id, @acd, @sem4, 'ENROLLED');
SET @e_crypto = (SELECT id FROM enrollments WHERE student_id=@student_id AND subject_id=@crypto AND semester_id=@sem4);
SET @e_daa = (SELECT id FROM enrollments WHERE student_id=@student_id AND subject_id=@daa AND semester_id=@sem4);
SET @e_os = (SELECT id FROM enrollments WHERE student_id=@student_id AND subject_id=@os AND semester_id=@sem4);
SET @e_acd = (SELECT id FROM enrollments WHERE student_id=@student_id AND subject_id=@acd AND semester_id=@sem4);
SET @e_riya_daa = (SELECT id FROM enrollments WHERE student_id=@riya_id AND subject_id=@daa AND semester_id=@sem4);
SET @mid = (SELECT id FROM assessment_components WHERE code='MID_TERM');
SET @end = (SELECT id FROM assessment_components WHERE code='END_TERM');

INSERT IGNORE INTO marks (enrollment_id, component_id, marks_obtained, entered_by) VALUES
 (@e_crypto,@mid,25,@admin_user_id), (@e_crypto,@end,65,@admin_user_id),
 (@e_daa,@mid,13,@admin_user_id), (@e_daa,@end,50,@admin_user_id),
 (@e_os,@mid,17,@admin_user_id), (@e_os,@end,55,@admin_user_id),
 (@e_acd,@mid,23,@admin_user_id), (@e_acd,@end,60,@admin_user_id);
UPDATE enrollments e JOIN grade_scale g ON g.letter = CASE
 WHEN e.id=@e_crypto THEN 'A+' WHEN e.id=@e_acd THEN 'A' WHEN e.id=@e_os THEN 'B' ELSE 'C' END
 SET e.final_grade_id=g.id WHERE e.student_id=@student_id AND e.semester_id=@sem4;

INSERT IGNORE INTO timetable_slots (subject_id, day_of_week, start_time, end_time, room) VALUES
 (@crypto, 1, '13:10:00', '14:00:00', 'LHC 01'), (@daa, 2, '10:10:00', '11:00:00', 'LHC 02'),
 (@os, 3, '11:10:00', '12:00:00', 'LHC 03'), (@acd, 4, '09:10:00', '10:00:00', 'LHC 04');
SET @slot_crypto=(SELECT id FROM timetable_slots WHERE subject_id=@crypto);
SET @slot_daa=(SELECT id FROM timetable_slots WHERE subject_id=@daa);
SET @slot_os=(SELECT id FROM timetable_slots WHERE subject_id=@os);
SET @slot_acd=(SELECT id FROM timetable_slots WHERE subject_id=@acd);

-- Ten completed sessions per subject make attendance auditable: Crypto 8/10,
-- DAA 7/10, OS 6/10 and ACD 9/10 for Meera.
INSERT IGNORE INTO class_sessions (slot_id, subject_id, session_date) VALUES
 (@slot_crypto,@crypto,'2026-09-07'),(@slot_crypto,@crypto,'2026-09-14'),(@slot_crypto,@crypto,'2026-09-21'),(@slot_crypto,@crypto,'2026-09-28'),(@slot_crypto,@crypto,'2026-10-05'),(@slot_crypto,@crypto,'2026-10-12'),(@slot_crypto,@crypto,'2026-10-19'),(@slot_crypto,@crypto,'2026-10-26'),(@slot_crypto,@crypto,'2026-11-02'),(@slot_crypto,@crypto,'2026-11-09'),
 (@slot_daa,@daa,'2026-09-08'),(@slot_daa,@daa,'2026-09-15'),(@slot_daa,@daa,'2026-09-22'),(@slot_daa,@daa,'2026-09-29'),(@slot_daa,@daa,'2026-10-06'),(@slot_daa,@daa,'2026-10-13'),(@slot_daa,@daa,'2026-10-20'),(@slot_daa,@daa,'2026-10-27'),(@slot_daa,@daa,'2026-11-03'),(@slot_daa,@daa,'2026-11-10'),
 (@slot_os,@os,'2026-09-09'),(@slot_os,@os,'2026-09-16'),(@slot_os,@os,'2026-09-23'),(@slot_os,@os,'2026-09-30'),(@slot_os,@os,'2026-10-07'),(@slot_os,@os,'2026-10-14'),(@slot_os,@os,'2026-10-21'),(@slot_os,@os,'2026-10-28'),(@slot_os,@os,'2026-11-04'),(@slot_os,@os,'2026-11-11'),
 (@slot_acd,@acd,'2026-09-10'),(@slot_acd,@acd,'2026-09-17'),(@slot_acd,@acd,'2026-09-24'),(@slot_acd,@acd,'2026-10-01'),(@slot_acd,@acd,'2026-10-08'),(@slot_acd,@acd,'2026-10-15'),(@slot_acd,@acd,'2026-10-22'),(@slot_acd,@acd,'2026-10-29'),(@slot_acd,@acd,'2026-11-05'),(@slot_acd,@acd,'2026-11-12');

INSERT IGNORE INTO attendance_records (session_id,enrollment_id,status,marked_by)
 SELECT cs.id, @e_crypto, IF(DAY(cs.session_date) IN (21,28), 'ABSENT','PRESENT'), @admin_user_id FROM class_sessions cs WHERE cs.subject_id=@crypto;
INSERT IGNORE INTO attendance_records (session_id,enrollment_id,status,marked_by)
 SELECT cs.id, @e_daa, IF(DAY(cs.session_date) IN (22,29,6), 'ABSENT','PRESENT'), @admin_user_id FROM class_sessions cs WHERE cs.subject_id=@daa;
INSERT IGNORE INTO attendance_records (session_id,enrollment_id,status,marked_by)
 SELECT cs.id, @e_os, IF(DAY(cs.session_date) IN (23,30,7,14), 'ABSENT','PRESENT'), @admin_user_id FROM class_sessions cs WHERE cs.subject_id=@os;
INSERT IGNORE INTO attendance_records (session_id,enrollment_id,status,marked_by)
 SELECT cs.id, @e_acd, IF(DAY(cs.session_date)=24, 'ABSENT','PRESENT'), @admin_user_id FROM class_sessions cs WHERE cs.subject_id=@acd;

INSERT IGNORE INTO holidays (holiday_date, description) VALUES
 ('2026-10-02','Gandhi Jayanti'), ('2026-10-20','University Foundation Day');

INSERT IGNORE INTO assignments (subject_id,created_by,title,description,due_at,max_marks) VALUES
 (@os,@admin_user_id,'OS Process Scheduling','Implement and compare two CPU scheduling strategies.','2026-10-18 23:59:00',20),
 (@daa,@admin_user_id,'Dynamic Programming Worksheet','Solve the supplied recurrence and optimization exercises.','2026-10-20 23:59:00',20),
 (@acd,@admin_user_id,'Parser Design Notes','Submit a concise LL(1) parsing design note.','2026-10-24 23:59:00',20);
SET @assignment_os=(SELECT id FROM assignments WHERE title='OS Process Scheduling');
INSERT IGNORE INTO submissions (assignment_id,student_id,status,note) VALUES
 (@assignment_os,@student_id,'NOT_SUBMITTED','');

INSERT IGNORE INTO exams (subject_id,semester_id,exam_type,exam_date,start_time,venue) VALUES
 (@os,@sem4,'END_TERM','2026-11-24','09:00:00','Exam Hall A'),(@crypto,@sem4,'END_TERM','2026-11-26','09:00:00','Exam Hall A'),
 (@daa,@sem4,'END_TERM','2026-11-27','09:00:00','Exam Hall B'),(@acd,@sem4,'END_TERM','2026-11-28','09:00:00','Exam Hall B');

INSERT IGNORE INTO attendance_requests (student_id,subject_id,type,reason,status) VALUES
 (@student_id,@os,'UNDERTAKING','I was ill during the first two weeks and will maintain attendance for the remaining sessions.','REQUESTED');

SET @bonafide=(SELECT id FROM document_types WHERE code='BONAFIDE');
SET @noc=(SELECT id FROM document_types WHERE code='NOC');
INSERT IGNORE INTO document_requests (student_id,document_type_id,status,purpose) VALUES
 (@student_id,@bonafide,'REQUESTED','Internship verification'),(@student_id,@noc,'APPROVED','Industry visit');

INSERT IGNORE INTO fee_records (student_id,semester_id,total_amount,due_date) VALUES
 (@student_id,@sem3,150000.00,'2025-12-15'),(@student_id,@sem4,150000.00,'2026-11-15');
SET @fee3=(SELECT id FROM fee_records WHERE student_id=@student_id AND semester_id=@sem3);
SET @fee4=(SELECT id FROM fee_records WHERE student_id=@student_id AND semester_id=@sem4);
INSERT IGNORE INTO payments (fee_record_id,amount,mode,transaction_reference,status,paid_at,verified_at,receipt_no,recorded_by,verified_by) VALUES
 (@fee3,150000.00,'NET_BANKING','DEMO-TXN-SEM3','VERIFIED','2025-12-01 12:30:00','2025-12-01 15:00:00','DEMO-REC-SEM3',@student_user_id,@admin_user_id),
 (@fee4,50000.00,'UPI','DEMO-TXN-SEM4','SUBMITTED','2026-10-10 10:00:00',NULL,NULL,@student_user_id,NULL);

INSERT IGNORE INTO audit_logs (actor_user_id,action,entity_type,entity_id,details) VALUES
 (@admin_user_id,'SEED_CREATED','DATABASE',NULL,JSON_OBJECT('stage','1','reference_date','2026-10-11'));

COMMIT;
