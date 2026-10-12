package com.slcm.portal.admin;

import static com.slcm.portal.admin.AdminResource.*;
import static org.springframework.http.HttpStatus.*;

import com.slcm.portal.admin.AdminRequests.*;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.sql.Statement;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@PreAuthorize("hasRole('ADMIN')")
@Transactional
public class AdminAcademicService {
    private final JdbcTemplate jdbc;
    private final PasswordEncoder passwords;
    public AdminAcademicService(JdbcTemplate jdbc, PasswordEncoder passwords) {
        this.jdbc=jdbc; this.passwords=passwords;
    }

    /** Explicit, safe database projections, never persistence entities. */
    public List<Map<String,Object>> list(AdminResource resource, int limit, int offset) {
        require(limit >= 1 && limit <= 500 && offset >= 0, "Invalid pagination");
        if (resource == STUDENTS) {
            return jdbc.queryForList(studentProjection()+" order by s.id limit ? offset ?", limit, offset);
        }
        return jdbc.queryForList("select "+resource.projection+" from "+resource.table+
                " order by id"+(resource == AUDIT ? " desc" : "")+" limit ? offset ?", limit, offset);
    }

    public Map<String,Object> get(AdminResource resource, long id) {
        require(id > 0, "Invalid id");
        String sql=resource == STUDENTS ? studentProjection()+" where s.id=?" :
                "select "+resource.projection+" from "+resource.table+" where id=?";
        return one(sql, id);
    }

    private String studentProjection() {
        return "select s.id,s.user_id,s.reg_no,s.full_name,s.email,s.program,s.section,"+
                "s.focus_mode_enabled,s.created_at,u.username,u.enabled from students s join users u on u.id=s.user_id";
    }

    public Map<String,Object> student(Long id, Student r) {
        long actor=actor();
        long userId;
        if (r.password()!=null) {
            require(!r.password().isBlank() && r.password().length()>=8 &&
                    r.password().getBytes(StandardCharsets.UTF_8).length<=72, "Password must contain 8 characters and at most 72 UTF-8 bytes");
        }
        if (id == null) {
            require(r.password()!=null, "Password is required for a new student");
            userId=insert("users", values("username",r.username(),"password_hash",passwords.encode(r.password()),
                    "role","STUDENT","enabled",r.enabled()));
        } else {
            userId=number(lock(STUDENTS,id),"user_id");
            var user=one("select id,role from users where id=? for update",userId);
            require("STUDENT".equals(user.get("role")), "Student account role is invalid");
            var userValues=values("username",r.username(),"enabled",r.enabled());
            if(r.password()!=null) userValues.put("password_hash",passwords.encode(r.password()));
            update("users", userId, userValues);
        }
        var fields=values("user_id",userId,"reg_no",r.regNo(),"full_name",r.fullName(),"email",r.email(),
                "program",r.program(),"section",r.section(),"focus_mode_enabled",r.focusModeEnabled());
        long saved=save(STUDENTS,id,fields,actor);
        return get(STUDENTS,saved);
    }

    public Map<String,Object> semester(Long id, Semester r) {
        require(!r.endDate().isBefore(r.startDate()), "Semester end must not precede start");
        if(id!=null) {
            lock(SEMESTERS,id);
            conflictIf(count("select count(*) from class_sessions c join subjects s on s.id=c.subject_id where s.semester_id=? and (c.session_date<? or c.session_date>?)",
                    id,r.startDate(),r.endDate())>0 || count("select count(*) from exams where semester_id=? and (exam_date<? or exam_date>?)",id,r.startDate(),r.endDate())>0,
                    "Semester dates exclude existing academic events");
        }
        return saved(SEMESTERS,id,values("semester_no",r.semesterNo(),"label",r.label(),"start_date",r.startDate(),"end_date",r.endDate(),"is_current",r.isCurrent()));
    }

    public Map<String,Object> subject(Long id, Subject r) {
        lock(SEMESTERS,r.semesterId());
        if(id!=null) {
            var old=lock(SUBJECTS,id);
            if(number(old,"semester_id")!=r.semesterId()) {
                conflictIf(count("select count(*) from enrollments where subject_id=?",id)>0 ||
                        count("select count(*) from exams where subject_id=?",id)>0 ||
                        count("select count(*) from class_sessions where subject_id=?",id)>0,
                        "Subject semester cannot change while linked academic records exist");
            }
        }
        return saved(SUBJECTS,id,values("semester_id",r.semesterId(),"code",r.code(),"name",r.name(),"short_name",r.shortName(),"credits",r.credits()));
    }

    public Map<String,Object> enrollment(Long id, Enrollment r) {
        lock(STUDENTS,r.studentId());
        compatible(r.subjectId(),r.semesterId());
        if(r.finalGradeId()!=null) lock(GRADES,r.finalGradeId());
        if(id!=null) {
            var old=lock(ENROLLMENTS,id);
            if(number(old,"student_id")!=r.studentId() || number(old,"subject_id")!=r.subjectId() || number(old,"semester_id")!=r.semesterId()) {
                conflictIf(count("select count(*) from marks where enrollment_id=?",id)>0 ||
                        count("select count(*) from attendance_records where enrollment_id=?",id)>0,
                        "Enrollment identity cannot change while marks or attendance exist");
            }
        }
        return saved(ENROLLMENTS,id,values("student_id",r.studentId(),"subject_id",r.subjectId(),"semester_id",r.semesterId(),
                "status",r.status().name(),"final_grade_id",r.finalGradeId()));
    }

    public Map<String,Object> timetable(Long id, Timetable r) {
        lock(SUBJECTS,r.subjectId());
        require(r.endTime().isAfter(r.startTime()), "End time must follow start time");
        if(id!=null) {
            var old=lock(TIMETABLE,id);
            if(number(old,"subject_id")!=r.subjectId() || number(old,"day_of_week")!=r.dayOfWeek())
                conflictIf(count("select count(*) from class_sessions where slot_id=?",id)>0,"Slot subject/day cannot change after sessions exist");
        }
        return saved(TIMETABLE,id,values("subject_id",r.subjectId(),"day_of_week",r.dayOfWeek(),"start_time",r.startTime(),"end_time",r.endTime(),"room",r.room()));
    }

    public Map<String,Object> session(Long id, Session r) {
        var subject=lock(SUBJECTS,r.subjectId());
        var term=lock(SEMESTERS,number(subject,"semester_id"));
        var slot=lock(TIMETABLE,r.slotId());
        require(number(slot,"subject_id")==r.subjectId(),"Session subject must match timetable subject");
        require(number(slot,"day_of_week")==r.sessionDate().getDayOfWeek().getValue(),"Session date must match timetable day");
        dateWithin(r.sessionDate(),term);
        if(id!=null) {
            var old=lock(SESSIONS,id);
            if(number(old,"subject_id")!=r.subjectId()) conflictIf(count("select count(*) from attendance_records where session_id=?",id)>0,
                    "Session subject cannot change after attendance exists");
        }
        return saved(SESSIONS,id,values("slot_id",r.slotId(),"subject_id",r.subjectId(),"session_date",r.sessionDate()));
    }

    public List<Map<String,Object>> attendance(long sessionId) {
        get(SESSIONS,sessionId);
        return jdbc.queryForList("select id,session_id,enrollment_id,status,marked_by,marked_at from attendance_records where session_id=? order by enrollment_id",sessionId);
    }

    public List<Map<String,Object>> attendance(long sessionId, AttendanceBatch r) {
        long actor=actor();
        var session=lock(SESSIONS,sessionId);
        var subject=lock(SUBJECTS,number(session,"subject_id"));
        Set<Long> seen=new HashSet<>();
        // Validate the entire batch before writing; one transaction owns all changes and its audit.
        for(var item:r.records()) {
            require(seen.add(item.enrollmentId()),"Duplicate enrollment in attendance batch");
            var enrollment=lock(ENROLLMENTS,item.enrollmentId());
            require(number(enrollment,"subject_id")==number(session,"subject_id") &&
                    number(enrollment,"semester_id")==number(subject,"semester_id"),"Attendance enrollment does not match session");
            require("ENROLLED".equals(enrollment.get("status")),"Attendance requires an active enrollment");
        }
        for(var item:r.records()) {
            write("insert into attendance_records(session_id,enrollment_id,status,marked_by) values (?,?,?,?) "+
                    "on duplicate key update status=values(status),marked_by=values(marked_by),marked_at=current_timestamp",
                    sessionId,item.enrollmentId(),item.status().name(),actor);
        }
        audit(actor,"MARK_ATTENDANCE",SESSIONS,sessionId);
        return attendance(sessionId);
    }

    public Map<String,Object> component(Long id, Component r) {
        if(id!=null) {
            lock(COMPONENTS,id);
            conflictIf(count("select count(*) from marks where component_id=? and marks_obtained>?",id,r.maxMarks())>0,
                    "Maximum is below existing marks");
        }
        return saved(COMPONENTS,id,values("code",r.code(),"max_marks",r.maxMarks(),"weight_percent",r.weightPercent()));
    }

    public Map<String,Object> grade(Long id, Grade r) {
        require(r.minPercent().compareTo(r.maxPercent())<=0,"Grade range is reversed");
        // Serialize range edits to avoid two simultaneous administrators introducing overlaps.
        jdbc.queryForList("select id from grade_scale order by id for update");
        conflictIf(count("select count(*) from grade_scale where id<>? and min_percent<=? and max_percent>=?",
                id==null ? 0 : id,r.maxPercent(),r.minPercent())>0,"Grade range overlaps an existing range");
        return saved(GRADES,id,values("letter",r.letter(),"grade_points",r.gradePoints(),"min_percent",r.minPercent(),"max_percent",r.maxPercent()));
    }

    public Map<String,Object> mark(Mark r) {
        long actor=actor();
        var enrollment=lock(ENROLLMENTS,r.enrollmentId());
        compatible(number(enrollment,"subject_id"),number(enrollment,"semester_id"));
        require(!"DROPPED".equals(enrollment.get("status")),"Cannot enter marks for a dropped enrollment");
        var component=lock(COMPONENTS,r.componentId());
        bounded(r.marksObtained(),decimal(component,"max_marks"));
        write("insert into marks(enrollment_id,component_id,marks_obtained,entered_by) values (?,?,?,?) "+
                "on duplicate key update marks_obtained=values(marks_obtained),entered_by=values(entered_by),entered_at=current_timestamp",
                r.enrollmentId(),r.componentId(),r.marksObtained(),actor);
        var row=one("select "+MARKS.projection+" from marks where enrollment_id=? and component_id=?",r.enrollmentId(),r.componentId());
        audit(actor,"UPSERT",MARKS,number(row,"id"));
        return row;
    }

    public Map<String,Object> assignment(Long id, Assignment r) {
        lock(SUBJECTS,r.subjectId());
        var fields=values("subject_id",r.subjectId(),"title",r.title(),"description",r.description(),"due_at",r.dueAt(),"max_marks",r.maxMarks());
        if(id==null) fields.put("created_by",actor());
        else {
            var old=lock(ASSIGNMENTS,id);
            conflictIf(count("select count(*) from submissions where assignment_id=? and marks>?",id,r.maxMarks())>0,"Maximum is below existing submission marks");
            if(number(old,"subject_id")!=r.subjectId()) conflictIf(count("select count(*) from submissions where assignment_id=?",id)>0,"Assignment subject cannot change after submissions exist");
        }
        return saved(ASSIGNMENTS,id,fields);
    }

    public Map<String,Object> reviewSubmission(long id, SubmissionReview r) {
        long actor=actor();
        var submission=lock(SUBMISSIONS,id);
        var assignment=lock(ASSIGNMENTS,number(submission,"assignment_id"));
        require(!"NOT_SUBMITTED".equals(submission.get("status")) && submission.get("submitted_at")!=null,"Submission has not been submitted");
        bounded(r.marks(),decimal(assignment,"max_marks"));
        require(count("select count(*) from enrollments e join subjects s on s.id=e.subject_id where e.student_id=? and e.subject_id=? and e.semester_id=s.semester_id and e.status<>'DROPPED'",
                number(submission,"student_id"),number(assignment,"subject_id"))>0,"Submission student is not enrolled in assignment subject");
        update(SUBMISSIONS.table,id,values("marks",r.marks(),"status","REVIEWED"));
        audit(actor,"REVIEW",SUBMISSIONS,id);
        return get(SUBMISSIONS,id);
    }

    public Map<String,Object> exam(Long id, Exam r) {
        compatible(r.subjectId(),r.semesterId());
        dateWithin(r.examDate(),lock(SEMESTERS,r.semesterId()));
        return saved(EXAMS,id,values("subject_id",r.subjectId(),"semester_id",r.semesterId(),"exam_type",r.examType().name(),
                "exam_date",r.examDate(),"start_time",r.startTime(),"venue",r.venue()));
    }

    public Map<String,Object> syllabus(long subjectId) {
        get(SUBJECTS,subjectId);
        return one("select "+SYLLABI.projection+" from syllabi where subject_id=?",subjectId);
    }

    public Map<String,Object> syllabus(long subjectId, Syllabus r) {
        long actor=actor();
        lock(SUBJECTS,subjectId);
        require((r.content()!=null && !r.content().isBlank()) || r.fileId()!=null,"Syllabus requires content or a file");
        if(r.fileId()!=null) one("select id from uploaded_files where id=? for update",r.fileId());
        write("insert into syllabi(subject_id,content,file_id) values (?,?,?) on duplicate key update content=values(content),file_id=values(file_id)",
                subjectId,r.content(),r.fileId());
        var row=syllabus(subjectId);
        audit(actor,"UPSERT",SYLLABI,number(row,"id"));
        return row;
    }

    public Map<String,Object> reviewAttendance(long id, AttendanceReview r) {
        long actor=actor();
        var request=lock(REQUESTS,id);
        conflictIf(!"REQUESTED".equals(request.get("status")),"Attendance request has already been reviewed");
        require(count("select count(*) from enrollments e join subjects s on s.id=e.subject_id where e.student_id=? and e.subject_id=? and e.semester_id=s.semester_id and e.status<>'DROPPED'",
                number(request,"student_id"),number(request,"subject_id"))>0,"Request student is not enrolled in subject");
        update(REQUESTS.table,id,values("status",r.status().name(),"remarks",r.remarks(),"reviewed_by",actor,"reviewed_at",LocalDateTime.now()));
        audit(actor,"REVIEW",REQUESTS,id);
        return get(REQUESTS,id);
    }

    public List<Map<String,Object>> settings() {
        return jdbc.queryForList("select setting_key,setting_value,description,updated_at from app_settings order by setting_key");
    }
    public Map<String,Object> setting(String key) {
        settingKey(key);
        return one("select setting_key,setting_value,description,updated_at from app_settings where setting_key=?",key);
    }
    public Map<String,Object> setting(String key, Setting r) {
        settingKey(key);
        long actor=actor();
        // Known numeric settings must remain usable by their consumers.
        if(key.equals("attendance.threshold.percent") || key.equals("attendance_threshold_percent")) {
            try { var n=new BigDecimal(r.value()); require(n.signum()>0 && n.compareTo(new BigDecimal("100"))<=0,"Attendance threshold must be in (0,100]"); }
            catch(NumberFormatException e) { throw new ResponseStatusException(BAD_REQUEST,"Attendance threshold must be numeric"); }
        }
        write("insert into app_settings(setting_key,setting_value,description) values (?,?,?) on duplicate key update setting_value=values(setting_value),description=values(description)",key,r.value(),r.description());
        // JSON_OBJECT parameterizes both key and value, and records no credentials.
        write("insert into audit_logs(actor_user_id,action,entity_type,entity_id,details) values (?,'UPSERT','app_settings',null,JSON_OBJECT('key',?))",actor,key);
        return setting(key);
    }

    public void delete(AdminResource resource,long id) {
        require(EnumSet.of(STUDENTS,SEMESTERS,SUBJECTS,ENROLLMENTS,TIMETABLE,SESSIONS,COMPONENTS,GRADES,ASSIGNMENTS,EXAMS,SYLLABI).contains(resource),"Resource cannot be deleted");
        long actor=actor();
        var row=lock(resource,id);
        write("delete from "+resource.table+" where id=?",id);
        if(resource==STUDENTS) write("delete from users where id=?",number(row,"user_id"));
        audit(actor,"DELETE",resource,id);
    }

    private Map<String,Object> saved(AdminResource resource,Long id,LinkedHashMap<String,Object> fields) {
        return get(resource,save(resource,id,fields,actor()));
    }
    private long save(AdminResource resource,Long id,LinkedHashMap<String,Object> fields,long actor) {
        if(id==null) {
            long created=insert(resource.table,fields);
            audit(actor,"CREATE",resource,created);
            return created;
        }
        lock(resource,id);
        update(resource.table,id,fields);
        audit(actor,"UPDATE",resource,id);
        return id;
    }
    private long actor() {
        var auth=SecurityContextHolder.getContext().getAuthentication();
        if(auth==null || !auth.isAuthenticated()) throw new ResponseStatusException(UNAUTHORIZED,"Authentication required");
        var rows=jdbc.queryForList("select id from users where username=? and role='ADMIN' and enabled=true",auth.getName());
        if(rows.size()!=1) throw new ResponseStatusException(FORBIDDEN,"Active administrator required");
        return number(rows.getFirst(),"id");
    }
    private void audit(long actor,String action,AdminResource resource,long id) {
        write("insert into audit_logs(actor_user_id,action,entity_type,entity_id) values (?,?,?,?)",actor,action,resource.table,id);
    }
    private Map<String,Object> lock(AdminResource resource,long id) {
        require(id>0,"Invalid id");
        return one("select "+resource.projection+" from "+resource.table+" where id=? for update",id);
    }
    private void compatible(long subjectId,long semesterId) {
        var subject=lock(SUBJECTS,subjectId);
        lock(SEMESTERS,semesterId);
        require(number(subject,"semester_id")==semesterId,"Subject does not belong to semester");
    }
    private void dateWithin(LocalDate date,Map<String,Object> semester) {
        var start=LocalDate.parse(semester.get("start_date").toString());
        var end=LocalDate.parse(semester.get("end_date").toString());
        require(!date.isBefore(start) && !date.isAfter(end),"Date is outside semester");
    }
    private void settingKey(String key) { require(key!=null && key.matches("[A-Za-z0-9_.-]{1,80}"),"Invalid setting key"); }
    private Map<String,Object> one(String sql,Object...args) {
        var rows=jdbc.queryForList(sql,args);
        if(rows.isEmpty()) throw new ResponseStatusException(NOT_FOUND,"Resource not found");
        return rows.getFirst();
    }
    private long count(String sql,Object...args) { return Objects.requireNonNull(jdbc.queryForObject(sql,Long.class,args)); }
    private int write(String sql,Object...args) {
        try { return jdbc.update(sql,args); }
        catch(DataIntegrityViolationException e) { throw new ResponseStatusException(CONFLICT,"Write conflicts with existing or linked records"); }
    }
    private long insert(String table,LinkedHashMap<String,Object> fields) {
        var keys=new GeneratedKeyHolder();
        String sql="insert into "+table+" ("+String.join(",",fields.keySet())+") values ("+String.join(",",Collections.nCopies(fields.size(),"?"))+")";
        try {
            jdbc.update(connection -> {
                var statement=connection.prepareStatement(sql,Statement.RETURN_GENERATED_KEYS);
                int i=1;
                for(var value:fields.values()) statement.setObject(i++,sqlValue(value));
                return statement;
            },keys);
        } catch(DataIntegrityViolationException e) { throw new ResponseStatusException(CONFLICT,"Write conflicts with existing or linked records"); }
        return Objects.requireNonNull(keys.getKey()).longValue();
    }
    private void update(String table,long id,LinkedHashMap<String,Object> fields) {
        var args=new ArrayList<>(fields.values()); args.add(id);
        write("update "+table+" set "+String.join(",",fields.keySet().stream().map(k->k+"=?").toList())+" where id=?",args.toArray());
    }
    private static Object sqlValue(Object value) {
        if(value instanceof LocalDate d) return java.sql.Date.valueOf(d);
        if(value instanceof LocalTime t) return java.sql.Time.valueOf(t);
        if(value instanceof LocalDateTime t) return java.sql.Timestamp.valueOf(t);
        return value;
    }
    private static LinkedHashMap<String,Object> values(Object...pairs) {
        var result=new LinkedHashMap<String,Object>();
        for(int i=0;i<pairs.length;i+=2) result.put((String)pairs[i],pairs[i+1]);
        return result;
    }
    private static long number(Map<String,Object> row,String key) { return ((Number)row.get(key)).longValue(); }
    private static BigDecimal decimal(Map<String,Object> row,String key) { return new BigDecimal(row.get(key).toString()); }
    static void bounded(BigDecimal actual,BigDecimal maximum) { require(actual!=null && actual.signum()>=0 && actual.compareTo(maximum)<=0,"Marks must be between zero and the configured maximum"); }
    private static void require(boolean condition,String message) { if(!condition) throw new ResponseStatusException(BAD_REQUEST,message); }
    private static void conflictIf(boolean condition,String message) { if(condition) throw new ResponseStatusException(CONFLICT,message); }
}
