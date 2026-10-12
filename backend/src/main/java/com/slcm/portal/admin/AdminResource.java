package com.slcm.portal.admin;

/** Closed SQL identifier allowlist. No identifiers are taken from request input. */
enum AdminResource {
    STUDENTS("students", "id,user_id,reg_no,full_name,email,program,section,focus_mode_enabled,created_at"),
    SEMESTERS("semesters", "id,semester_no,label,start_date,end_date,is_current"),
    SUBJECTS("subjects", "id,semester_id,code,name,short_name,credits"),
    ENROLLMENTS("enrollments", "id,student_id,subject_id,semester_id,status,final_grade_id,created_at"),
    TIMETABLE("timetable_slots", "id,subject_id,day_of_week,start_time,end_time,room"),
    SESSIONS("class_sessions", "id,slot_id,subject_id,session_date"),
    COMPONENTS("assessment_components", "id,code,max_marks,weight_percent"),
    GRADES("grade_scale", "id,letter,grade_points,min_percent,max_percent"),
    MARKS("marks", "id,enrollment_id,component_id,marks_obtained,entered_by,entered_at"),
    ASSIGNMENTS("assignments", "id,subject_id,created_by,title,description,due_at,max_marks,created_at"),
    SUBMISSIONS("submissions", "id,assignment_id,student_id,file_id,submitted_at,note,status,marks"),
    EXAMS("exams", "id,subject_id,semester_id,exam_type,exam_date,start_time,venue"),
    SYLLABI("syllabi", "id,subject_id,content,file_id,updated_at"),
    REQUESTS("attendance_requests", "id,student_id,subject_id,type,reason,status,reviewed_at,reviewed_by,remarks,created_at"),
    AUDIT("audit_logs", "id,actor_user_id,action,entity_type,entity_id,details,created_at");
    final String table;
    final String projection;
    AdminResource(String table, String projection) { this.table=table; this.projection=projection; }
}
