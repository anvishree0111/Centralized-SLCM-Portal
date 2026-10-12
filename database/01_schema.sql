-- SLCM Portal / Stage 1 schema
-- MySQL 8.0+. This script is intentionally non-destructive: it creates the
-- database if absent and fails rather than dropping an existing database.
CREATE DATABASE IF NOT EXISTS slcm_portal
  CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci;
USE slcm_portal;

CREATE TABLE IF NOT EXISTS users (
  id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  username VARCHAR(80) NOT NULL,
  password_hash VARCHAR(100) NOT NULL,
  role ENUM('STUDENT','ADMIN') NOT NULL,
  enabled BOOLEAN NOT NULL DEFAULT TRUE,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (id), UNIQUE KEY uk_users_username (username)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS students (
  id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  user_id BIGINT UNSIGNED NOT NULL,
  reg_no VARCHAR(30) NOT NULL,
  full_name VARCHAR(120) NOT NULL,
  email VARCHAR(160) NOT NULL,
  program VARCHAR(120) NOT NULL,
  section VARCHAR(30) NOT NULL,
  focus_mode_enabled BOOLEAN NOT NULL DEFAULT FALSE,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id), UNIQUE KEY uk_students_user (user_id), UNIQUE KEY uk_students_reg_no (reg_no),
  UNIQUE KEY uk_students_email (email),
  CONSTRAINT fk_students_user FOREIGN KEY (user_id) REFERENCES users(id)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS semesters (
  id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  semester_no TINYINT UNSIGNED NOT NULL,
  label VARCHAR(40) NOT NULL,
  start_date DATE NOT NULL,
  end_date DATE NOT NULL,
  is_current BOOLEAN NOT NULL DEFAULT FALSE,
  PRIMARY KEY (id), UNIQUE KEY uk_semesters_no (semester_no),
  CONSTRAINT chk_semesters_dates CHECK (end_date >= start_date)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS subjects (
  id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  semester_id BIGINT UNSIGNED NOT NULL,
  code VARCHAR(20) NOT NULL,
  name VARCHAR(150) NOT NULL,
  short_name VARCHAR(40) NOT NULL,
  credits DECIMAL(4,1) NOT NULL,
  PRIMARY KEY (id), UNIQUE KEY uk_subjects_code (code), KEY ix_subjects_semester (semester_id),
  CONSTRAINT fk_subjects_semester FOREIGN KEY (semester_id) REFERENCES semesters(id),
  CONSTRAINT chk_subjects_credits CHECK (credits > 0)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS uploaded_files (
  id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  original_name VARCHAR(255) NOT NULL,
  stored_name VARCHAR(255) NOT NULL,
  content_type VARCHAR(120) NOT NULL,
  size_bytes BIGINT UNSIGNED NOT NULL,
  sha256 CHAR(64) NOT NULL,
  uploaded_by BIGINT UNSIGNED NOT NULL,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id), UNIQUE KEY uk_uploaded_files_stored_name (stored_name),
  KEY ix_uploaded_files_uploader (uploaded_by),
  CONSTRAINT fk_uploaded_files_user FOREIGN KEY (uploaded_by) REFERENCES users(id)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS syllabi (
  id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  subject_id BIGINT UNSIGNED NOT NULL,
  content TEXT NULL,
  file_id BIGINT UNSIGNED NULL,
  updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (id), UNIQUE KEY uk_syllabi_subject (subject_id),
  CONSTRAINT fk_syllabi_subject FOREIGN KEY (subject_id) REFERENCES subjects(id),
  CONSTRAINT fk_syllabi_file FOREIGN KEY (file_id) REFERENCES uploaded_files(id)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS grade_scale (
  id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  letter VARCHAR(5) NOT NULL,
  grade_points DECIMAL(4,2) NOT NULL,
  min_percent DECIMAL(5,2) NOT NULL,
  max_percent DECIMAL(5,2) NOT NULL,
  PRIMARY KEY (id), UNIQUE KEY uk_grade_scale_letter (letter),
  CONSTRAINT chk_grade_scale_range CHECK (min_percent >= 0 AND max_percent <= 100 AND min_percent <= max_percent)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS enrollments (
  id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  student_id BIGINT UNSIGNED NOT NULL,
  subject_id BIGINT UNSIGNED NOT NULL,
  semester_id BIGINT UNSIGNED NOT NULL,
  status ENUM('ENROLLED','DROPPED','COMPLETED') NOT NULL DEFAULT 'ENROLLED',
  final_grade_id BIGINT UNSIGNED NULL,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id), UNIQUE KEY uk_enrollments_student_subject_term (student_id, subject_id, semester_id),
  KEY ix_enrollments_subject (subject_id), KEY ix_enrollments_semester (semester_id),
  CONSTRAINT fk_enrollments_student FOREIGN KEY (student_id) REFERENCES students(id),
  CONSTRAINT fk_enrollments_subject FOREIGN KEY (subject_id) REFERENCES subjects(id),
  CONSTRAINT fk_enrollments_semester FOREIGN KEY (semester_id) REFERENCES semesters(id)
  ,CONSTRAINT fk_enrollments_grade FOREIGN KEY (final_grade_id) REFERENCES grade_scale(id)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS assessment_components (
  id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  code VARCHAR(30) NOT NULL,
  max_marks DECIMAL(7,2) NOT NULL,
  weight_percent DECIMAL(5,2) NOT NULL,
  PRIMARY KEY (id), UNIQUE KEY uk_assessment_components_code (code),
  CONSTRAINT chk_assessment_component_values CHECK (max_marks > 0 AND weight_percent >= 0 AND weight_percent <= 100)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS marks (
  id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  enrollment_id BIGINT UNSIGNED NOT NULL,
  component_id BIGINT UNSIGNED NOT NULL,
  marks_obtained DECIMAL(7,2) NOT NULL,
  entered_by BIGINT UNSIGNED NOT NULL,
  entered_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id), UNIQUE KEY uk_marks_enrollment_component (enrollment_id, component_id),
  CONSTRAINT fk_marks_enrollment FOREIGN KEY (enrollment_id) REFERENCES enrollments(id),
  CONSTRAINT fk_marks_component FOREIGN KEY (component_id) REFERENCES assessment_components(id),
  CONSTRAINT fk_marks_user FOREIGN KEY (entered_by) REFERENCES users(id),
  CONSTRAINT chk_marks_nonnegative CHECK (marks_obtained >= 0)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS timetable_slots (
  id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  subject_id BIGINT UNSIGNED NOT NULL,
  day_of_week TINYINT UNSIGNED NOT NULL,
  start_time TIME NOT NULL,
  end_time TIME NOT NULL,
  room VARCHAR(40) NOT NULL,
  PRIMARY KEY (id), KEY ix_timetable_subject_day (subject_id, day_of_week),
  CONSTRAINT fk_timetable_subject FOREIGN KEY (subject_id) REFERENCES subjects(id),
  CONSTRAINT chk_timetable_day CHECK (day_of_week BETWEEN 1 AND 7),
  CONSTRAINT chk_timetable_time CHECK (end_time > start_time)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS class_sessions (
  id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  slot_id BIGINT UNSIGNED NOT NULL,
  subject_id BIGINT UNSIGNED NOT NULL,
  session_date DATE NOT NULL,
  PRIMARY KEY (id), UNIQUE KEY uk_class_sessions_slot_date (slot_id, session_date),
  KEY ix_class_sessions_subject_date (subject_id, session_date),
  CONSTRAINT fk_class_sessions_slot FOREIGN KEY (slot_id) REFERENCES timetable_slots(id),
  CONSTRAINT fk_class_sessions_subject FOREIGN KEY (subject_id) REFERENCES subjects(id)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS attendance_records (
  id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  session_id BIGINT UNSIGNED NOT NULL,
  enrollment_id BIGINT UNSIGNED NOT NULL,
  status ENUM('PRESENT','ABSENT','LEAVE') NOT NULL,
  marked_by BIGINT UNSIGNED NOT NULL,
  marked_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id), UNIQUE KEY uk_attendance_session_enrollment (session_id, enrollment_id),
  KEY ix_attendance_enrollment (enrollment_id),
  CONSTRAINT fk_attendance_session FOREIGN KEY (session_id) REFERENCES class_sessions(id),
  CONSTRAINT fk_attendance_enrollment FOREIGN KEY (enrollment_id) REFERENCES enrollments(id),
  CONSTRAINT fk_attendance_user FOREIGN KEY (marked_by) REFERENCES users(id)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS holidays (
  id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  holiday_date DATE NOT NULL,
  description VARCHAR(160) NOT NULL,
  PRIMARY KEY (id), UNIQUE KEY uk_holidays_date (holiday_date)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS assignments (
  id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  subject_id BIGINT UNSIGNED NOT NULL,
  created_by BIGINT UNSIGNED NOT NULL,
  title VARCHAR(180) NOT NULL,
  description TEXT NULL,
  due_at DATETIME NOT NULL,
  max_marks DECIMAL(7,2) NOT NULL,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id), KEY ix_assignments_due (due_at),
  CONSTRAINT fk_assignments_subject FOREIGN KEY (subject_id) REFERENCES subjects(id),
  CONSTRAINT fk_assignments_user FOREIGN KEY (created_by) REFERENCES users(id),
  CONSTRAINT chk_assignments_marks CHECK (max_marks > 0)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS submissions (
  id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  assignment_id BIGINT UNSIGNED NOT NULL,
  student_id BIGINT UNSIGNED NOT NULL,
  file_id BIGINT UNSIGNED NULL,
  submitted_at DATETIME NULL,
  note TEXT NULL,
  status ENUM('NOT_SUBMITTED','SUBMITTED','LATE','REVIEWED') NOT NULL DEFAULT 'NOT_SUBMITTED',
  marks DECIMAL(7,2) NULL,
  PRIMARY KEY (id), UNIQUE KEY uk_submissions_assignment_student (assignment_id, student_id),
  CONSTRAINT fk_submissions_assignment FOREIGN KEY (assignment_id) REFERENCES assignments(id),
  CONSTRAINT fk_submissions_student FOREIGN KEY (student_id) REFERENCES students(id),
  CONSTRAINT fk_submissions_file FOREIGN KEY (file_id) REFERENCES uploaded_files(id),
  CONSTRAINT chk_submissions_marks CHECK (marks IS NULL OR marks >= 0)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS exams (
  id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  subject_id BIGINT UNSIGNED NOT NULL,
  semester_id BIGINT UNSIGNED NOT NULL,
  exam_type ENUM('MID_TERM','END_TERM','PRACTICAL','VIVA') NOT NULL,
  exam_date DATE NOT NULL,
  start_time TIME NOT NULL,
  venue VARCHAR(100) NOT NULL,
  PRIMARY KEY (id), KEY ix_exams_semester_date (semester_id, exam_date),
  CONSTRAINT fk_exams_subject FOREIGN KEY (subject_id) REFERENCES subjects(id),
  CONSTRAINT fk_exams_semester FOREIGN KEY (semester_id) REFERENCES semesters(id)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS attendance_requests (
  id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  student_id BIGINT UNSIGNED NOT NULL,
  subject_id BIGINT UNSIGNED NOT NULL,
  type ENUM('UNDERTAKING','CONDONATION') NOT NULL,
  reason TEXT NOT NULL,
  status ENUM('REQUESTED','APPROVED','REJECTED') NOT NULL DEFAULT 'REQUESTED',
  reviewed_at DATETIME NULL,
  reviewed_by BIGINT UNSIGNED NULL,
  remarks TEXT NULL,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id), KEY ix_attendance_requests_student (student_id),
  CONSTRAINT fk_attendance_requests_student FOREIGN KEY (student_id) REFERENCES students(id),
  CONSTRAINT fk_attendance_requests_subject FOREIGN KEY (subject_id) REFERENCES subjects(id),
  CONSTRAINT fk_attendance_requests_reviewer FOREIGN KEY (reviewed_by) REFERENCES users(id)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS document_types (
  id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  code VARCHAR(40) NOT NULL,
  name VARCHAR(100) NOT NULL,
  description VARCHAR(255) NOT NULL,
  active BOOLEAN NOT NULL DEFAULT TRUE,
  PRIMARY KEY (id), UNIQUE KEY uk_document_types_code (code)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS document_requests (
  id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  student_id BIGINT UNSIGNED NOT NULL,
  document_type_id BIGINT UNSIGNED NOT NULL,
  status ENUM('REQUESTED','PROCESSING','APPROVED','REJECTED') NOT NULL DEFAULT 'REQUESTED',
  purpose VARCHAR(255) NULL,
  requested_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  processed_at DATETIME NULL,
  admin_remarks TEXT NULL,
  processed_by BIGINT UNSIGNED NULL,
  issued_file_id BIGINT UNSIGNED NULL,
  PRIMARY KEY (id), KEY ix_document_requests_student_status (student_id, status),
  CONSTRAINT fk_document_requests_student FOREIGN KEY (student_id) REFERENCES students(id),
  CONSTRAINT fk_document_requests_type FOREIGN KEY (document_type_id) REFERENCES document_types(id),
  CONSTRAINT fk_document_requests_processor FOREIGN KEY (processed_by) REFERENCES users(id),
  CONSTRAINT fk_document_requests_file FOREIGN KEY (issued_file_id) REFERENCES uploaded_files(id)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS fee_records (
  id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  student_id BIGINT UNSIGNED NOT NULL,
  semester_id BIGINT UNSIGNED NOT NULL,
  total_amount DECIMAL(12,2) NOT NULL,
  due_date DATE NOT NULL,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id), UNIQUE KEY uk_fee_records_student_semester (student_id, semester_id),
  CONSTRAINT fk_fee_records_student FOREIGN KEY (student_id) REFERENCES students(id),
  CONSTRAINT fk_fee_records_semester FOREIGN KEY (semester_id) REFERENCES semesters(id),
  CONSTRAINT chk_fee_records_amount CHECK (total_amount > 0)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS payments (
  id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  fee_record_id BIGINT UNSIGNED NOT NULL,
  amount DECIMAL(12,2) NOT NULL,
  mode ENUM('UPI','CARD','NET_BANKING','CASH','BANK_TRANSFER') NOT NULL,
  transaction_reference VARCHAR(100) NOT NULL,
  status ENUM('SUBMITTED','VERIFIED','REJECTED') NOT NULL DEFAULT 'SUBMITTED',
  paid_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  verified_at DATETIME NULL,
  receipt_no VARCHAR(60) NULL,
  recorded_by BIGINT UNSIGNED NULL,
  verified_by BIGINT UNSIGNED NULL,
  PRIMARY KEY (id), UNIQUE KEY uk_payments_transaction_reference (transaction_reference),
  UNIQUE KEY uk_payments_receipt_no (receipt_no), KEY ix_payments_fee_status (fee_record_id, status),
  CONSTRAINT fk_payments_fee FOREIGN KEY (fee_record_id) REFERENCES fee_records(id),
  CONSTRAINT fk_payments_recorded_by FOREIGN KEY (recorded_by) REFERENCES users(id),
  CONSTRAINT fk_payments_verified_by FOREIGN KEY (verified_by) REFERENCES users(id),
  CONSTRAINT chk_payments_amount CHECK (amount > 0),
  CONSTRAINT chk_payments_verified_fields CHECK (status <> 'VERIFIED' OR (verified_at IS NOT NULL AND verified_by IS NOT NULL AND receipt_no IS NOT NULL))
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS audit_logs (
  id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  actor_user_id BIGINT UNSIGNED NULL,
  action VARCHAR(80) NOT NULL,
  entity_type VARCHAR(80) NOT NULL,
  entity_id BIGINT UNSIGNED NULL,
  details JSON NULL,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id), KEY ix_audit_logs_entity (entity_type, entity_id),
  CONSTRAINT fk_audit_logs_actor FOREIGN KEY (actor_user_id) REFERENCES users(id)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS app_settings (
  setting_key VARCHAR(80) NOT NULL,
  setting_value VARCHAR(255) NOT NULL,
  description VARCHAR(255) NULL,
  updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (setting_key)
) ENGINE=InnoDB;
