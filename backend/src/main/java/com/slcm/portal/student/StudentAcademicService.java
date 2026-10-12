package com.slcm.portal.student;

import java.math.BigDecimal;
import java.time.*;
import java.util.*;
import org.springframework.jdbc.core.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.multipart.MultipartFile;
import com.slcm.portal.finance.DocumentStorage;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import java.sql.Statement;
import static org.springframework.http.HttpStatus.*;
import static com.slcm.portal.student.AcademicDtos.*;
import org.springframework.beans.factory.annotation.Autowired;

@Service
@PreAuthorize("hasRole('STUDENT')")
@Transactional(readOnly = true)
public class StudentAcademicService {
    private final JdbcTemplate jdbc;
    private final Clock clock;
    private final DocumentStorage files;
    @Autowired
    public StudentAcademicService(JdbcTemplate jdbc, DocumentStorage files) { this(jdbc, Clock.systemDefaultZone(), files); }
    StudentAcademicService(JdbcTemplate jdbc, Clock clock, DocumentStorage files) { this.jdbc = jdbc; this.clock = clock; this.files = files; }
    private LocalDateTime now() { return LocalDateTime.now(clock); }
    private <T> List<T> query(Class<T> type, String sql, Object... args) { return jdbc.query(sql, new DataClassRowMapper<>(type), args); }
    private <T> T one(List<T> rows, String message) { if (rows.isEmpty()) throw new ResponseStatusException(NOT_FOUND, message); return rows.get(0); }
    Identity identity() {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) throw new ResponseStatusException(UNAUTHORIZED);
        return one(query(Identity.class, "SELECT s.id,s.user_id,s.focus_mode_enabled FROM students s JOIN users u ON u.id=s.user_id WHERE u.username=? AND u.enabled=true AND u.role='STUDENT'", auth.getName()), "Student profile not found");
    }
    public List<Semester> semesters() {
        return query(Semester.class, "SELECT DISTINCT se.id,se.semester_no,se.label,se.start_date,se.end_date,se.is_current FROM semesters se JOIN enrollments e ON e.semester_id=se.id WHERE e.student_id=? AND e.status<>'DROPPED' ORDER BY se.semester_no", identity().id());
    }
    private Semester semester(long id) { return one(semesters().stream().filter(s -> s.id()==id).toList(), "Semester not found"); }
    public List<Subject> subjects(Long semesterId) {
        if (semesterId != null) semester(semesterId);
        return query(Subject.class, "SELECT e.id enrollment_id,s.id subject_id,e.semester_id,s.code,s.name,s.credits,e.status,g.letter final_grade FROM enrollments e JOIN subjects s ON s.id=e.subject_id JOIN semesters se ON se.id=e.semester_id LEFT JOIN grade_scale g ON g.id=e.final_grade_id WHERE e.student_id=? AND e.status<>'DROPPED' AND " + (semesterId == null ? "se.is_current=true" : "e.semester_id=?") + " ORDER BY s.code", semesterId == null ? new Object[]{identity().id()} : new Object[]{identity().id(),semesterId});
    }
    private Subject subject(long subjectId) {
        return one(query(Subject.class, "SELECT e.id enrollment_id,s.id subject_id,e.semester_id,s.code,s.name,s.credits,e.status,g.letter final_grade FROM enrollments e JOIN subjects s ON s.id=e.subject_id LEFT JOIN grade_scale g ON g.id=e.final_grade_id WHERE e.student_id=? AND e.subject_id=? AND e.status<>'DROPPED' ORDER BY e.semester_id DESC", identity().id(), subjectId), "Subject not found");
    }
    public SemesterDetail semesterDetail(long id) { return new SemesterDetail(semester(id), subjects(id), gpa(id)); }
    public Gpa gpa(long semesterId) {
        semester(semesterId);
        BigDecimal total = jdbc.queryForObject("SELECT COALESCE(SUM(m.marks_obtained),0) FROM marks m JOIN enrollments e ON e.id=m.enrollment_id WHERE e.student_id=? AND e.semester_id=? AND e.status<>'DROPPED'", BigDecimal.class, identity().id(), semesterId);
        return AcademicCalculations.gpa(total);
    }
    public List<Slot> schedule() {
        return query(Slot.class, "SELECT DISTINCT t.id,t.subject_id,s.name subject_name,t.day_of_week,t.start_time,t.end_time,t.room FROM timetable_slots t JOIN subjects s ON s.id=t.subject_id JOIN enrollments e ON e.subject_id=s.id JOIN semesters se ON se.id=e.semester_id WHERE e.student_id=? AND e.status<>'DROPPED' AND se.is_current=true ORDER BY t.day_of_week,t.start_time", identity().id());
    }
    public List<TodaySlot> today() {
        LocalDate date = now().toLocalDate();
        if (!jdbc.queryForList("SELECT id FROM holidays WHERE holiday_date=?", date).isEmpty()) return List.of();
        Set<Long> active = new HashSet<>();
        for (Semester semester : semesters()) if (semester.isCurrent() && !date.isBefore(semester.startDate()) && !date.isAfter(semester.endDate())) for (Subject subject : subjects(semester.id())) active.add(subject.subjectId());
        long student = identity().id();
        return schedule().stream().filter(t -> t.dayOfWeek()==date.getDayOfWeek().getValue() && active.contains(t.subjectId())).map(t -> {
            List<String> statuses = jdbc.queryForList("SELECT ar.status FROM attendance_records ar JOIN class_sessions cs ON cs.id=ar.session_id JOIN enrollments e ON e.id=ar.enrollment_id WHERE e.student_id=? AND e.status<>'DROPPED' AND cs.slot_id=? AND cs.session_date=?", String.class, student,t.id(),date);
            return new TodaySlot(t,date,statuses.isEmpty()?"UNMARKED":statuses.get(0));
        }).toList();
    }
    private static final String ASSIGNMENTS = "SELECT a.id,a.subject_id,a.title,a.description,a.due_at,a.max_marks,su.id submission_id,su.file_id,su.note,COALESCE(su.status,'NOT_SUBMITTED') status,su.submitted_at,su.marks FROM assignments a LEFT JOIN submissions su ON su.assignment_id=a.id AND su.student_id=? WHERE EXISTS (SELECT 1 FROM enrollments e WHERE e.student_id=? AND e.subject_id=a.subject_id AND e.status<>'DROPPED')";
    public List<Assignment> assignments(boolean due) {
        long id=identity().id();
        return query(Assignment.class, ASSIGNMENTS + (due ? " AND a.due_at>=? AND (su.id IS NULL OR su.status='NOT_SUBMITTED')" : "") + " ORDER BY a.due_at,a.id", due ? new Object[]{id,id,now()} : new Object[]{id,id});
    }
    public Assignment assignment(long id) { long student=identity().id(); return one(query(Assignment.class,ASSIGNMENTS+" AND a.id=?",student,student,id),"Assignment not found"); }
    private Assignment assignmentForSubmission(long id, long student) {
        jdbc.queryForList("SELECT id FROM assignments WHERE id=? FOR UPDATE",id);
        // A locking read sees a concurrent submitter's committed status even under MySQL REPEATABLE READ.
        return one(query(Assignment.class,ASSIGNMENTS+" AND a.id=? FOR UPDATE",student,student,id),"Assignment not found");
    }
    @Transactional
    public Assignment submit(long id, SubmissionInput input) {
        Identity student=identity();
        Assignment assignment=assignmentForSubmission(id,student.id());
        if (now().isAfter(assignment.dueAt())) throw new ResponseStatusException(CONFLICT,"Assignment deadline has passed");
        if (!"NOT_SUBMITTED".equals(assignment.status())) throw new ResponseStatusException(CONFLICT,"Assignment already submitted");
        if ((input.note()==null || input.note().isBlank()) && input.fileId()==null) throw new ResponseStatusException(BAD_REQUEST,"A note or uploaded file is required");
        if (input.fileId()!=null && jdbc.queryForList("SELECT id FROM uploaded_files WHERE id=? AND uploaded_by=?",input.fileId(),student.userId()).isEmpty()) throw new ResponseStatusException(NOT_FOUND,"Uploaded file not found");
        if (assignment.submissionId()==null) jdbc.update("INSERT INTO submissions (assignment_id,student_id,file_id,submitted_at,note,status) VALUES (?,?,?,?,?,'SUBMITTED')",id,student.id(),input.fileId(),now(),input.note());
        else jdbc.update("UPDATE submissions SET file_id=?,submitted_at=?,note=?,status='SUBMITTED' WHERE id=? AND student_id=?",input.fileId(),now(),input.note(),assignment.submissionId(),student.id());
        return assignment(id);
    }
    @Transactional
    public Assignment submit(long id, String note, MultipartFile file) {
        Identity student=identity();
        Assignment assignment=assignmentForSubmission(id,student.id());
        if (now().isAfter(assignment.dueAt())) throw new ResponseStatusException(CONFLICT,"Assignment deadline has passed");
        if (!"NOT_SUBMITTED".equals(assignment.status())) throw new ResponseStatusException(CONFLICT,"Assignment already submitted");
        if (file == null || file.isEmpty()) throw new ResponseStatusException(BAD_REQUEST,"A submission file is required");
        if (note != null && note.length()>10000) throw new ResponseStatusException(BAD_REQUEST,"Submission note is too long");
        DocumentStorage.Stored stored=files.store(file);
        long fileId;
        try {
            var key=new GeneratedKeyHolder();
            jdbc.update(connection -> {
                var statement=connection.prepareStatement("INSERT INTO uploaded_files(original_name,stored_name,content_type,size_bytes,sha256,uploaded_by) VALUES (?,?,?,?,?,?)", Statement.RETURN_GENERATED_KEYS);
                statement.setString(1,stored.originalName()); statement.setString(2,stored.storedName()); statement.setString(3,stored.contentType());
                statement.setLong(4,stored.size()); statement.setString(5,stored.sha256()); statement.setLong(6,student.userId()); return statement;
            }, key);
            fileId=Objects.requireNonNull(key.getKey()).longValue();
        } catch (RuntimeException e) {
            throw new ResponseStatusException(SERVICE_UNAVAILABLE,"Submission storage unavailable");
        }
        if (assignment.submissionId()==null) jdbc.update("INSERT INTO submissions (assignment_id,student_id,file_id,submitted_at,note,status) VALUES (?,?,?,?,?,'SUBMITTED')",id,student.id(),fileId,now(),note);
        else jdbc.update("UPDATE submissions SET file_id=?,submitted_at=?,note=?,status='SUBMITTED' WHERE id=? AND student_id=?",fileId,now(),note,assignment.submissionId(),student.id());
        return assignment(id);
    }
    public List<Exam> exams() { return query(Exam.class,"SELECT ex.id,ex.subject_id,ex.semester_id,s.name subject_name,ex.exam_type,ex.exam_date,ex.start_time,ex.venue FROM exams ex JOIN subjects s ON s.id=ex.subject_id WHERE (ex.exam_date>? OR (ex.exam_date=? AND ex.start_time>=?)) AND EXISTS (SELECT 1 FROM enrollments e WHERE e.student_id=? AND e.subject_id=ex.subject_id AND e.semester_id=ex.semester_id AND e.status<>'DROPPED') ORDER BY ex.exam_date,ex.start_time",now().toLocalDate(),now().toLocalDate(),now().toLocalTime(),identity().id()); }
    public Syllabus syllabus(long subjectId) { subject(subjectId); return one(query(Syllabus.class,"SELECT subject_id,content,file_id,updated_at FROM syllabi WHERE subject_id=?",subjectId),"Syllabus not found"); }
    @Transactional
    public Preferences preferences(Preferences preferences) { jdbc.update("UPDATE students SET focus_mode_enabled=? WHERE id=?",preferences.focusModeEnabled(),identity().id()); return preferences; }
    private List<AttendanceEvent> history(Subject subject) {
        Semester semester=semester(subject.semesterId());
        return query(AttendanceEvent.class,"SELECT cs.id session_id,cs.session_date,t.start_time,t.end_time,COALESCE(ar.status,'UNMARKED') status FROM class_sessions cs JOIN timetable_slots t ON t.id=cs.slot_id LEFT JOIN attendance_records ar ON ar.session_id=cs.id AND ar.enrollment_id=? WHERE cs.subject_id=? AND cs.session_date BETWEEN ? AND ? AND (cs.session_date<? OR (cs.session_date=? AND t.end_time<=?)) ORDER BY cs.session_date,t.start_time",subject.enrollmentId(),subject.subjectId(),semester.startDate(),semester.endDate(),now().toLocalDate(),now().toLocalDate(),now().toLocalTime());
    }
    public List<AttendanceEvent> history(long subjectId) { return history(subject(subjectId)); }
    public List<Attendance> attendance() {
        Set<LocalDate> holidays = new HashSet<>(jdbc.query("SELECT holiday_date FROM holidays",(rs,n)->rs.getDate(1).toLocalDate()));
        List<Slot> slots=schedule();
        return subjects(null).stream().map(s -> {
            List<AttendanceEvent> records=history(s);
            Semester semester=semester(s.semesterId());
            long remaining=AcademicCalculations.remaining(slots.stream().filter(t->t.subjectId()==s.subjectId()).toList(),semester.startDate(),semester.endDate(),now(),holidays);
            return AcademicCalculations.attendance(s.subjectId(),s.name(),records.size(),records.stream().filter(r->"PRESENT".equals(r.status())).count(),remaining);
        }).toList();
    }
    public List<AttendanceRequest> requests() { return query(AttendanceRequest.class,"SELECT id,subject_id,type,reason,status,reviewed_at,remarks,created_at FROM attendance_requests WHERE student_id=? ORDER BY created_at DESC,id DESC",identity().id()); }
    @Transactional
    public AttendanceRequest request(AttendanceRequestInput input) {
        subject(input.subjectId());
        org.springframework.jdbc.support.KeyHolder key=new org.springframework.jdbc.support.GeneratedKeyHolder();
        long student=identity().id();
        jdbc.update(connection->{var statement=connection.prepareStatement("INSERT INTO attendance_requests (student_id,subject_id,type,reason) VALUES (?,?,?,?)",java.sql.Statement.RETURN_GENERATED_KEYS); statement.setLong(1,student); statement.setLong(2,input.subjectId()); statement.setString(3,input.type()); statement.setString(4,input.reason()); return statement;},key);
        return one(query(AttendanceRequest.class,"SELECT id,subject_id,type,reason,status,reviewed_at,remarks,created_at FROM attendance_requests WHERE id=? AND student_id=?",Objects.requireNonNull(key.getKey()).longValue(),student),"Attendance request not found");
    }
    public List<Grade> grades() { return query(Grade.class,"SELECT id,letter,grade_points,min_percent,max_percent FROM grade_scale ORDER BY min_percent DESC"); }
    private List<Mark> marks(long enrollmentId) { return query(Mark.class,"SELECT m.enrollment_id,m.component_id,c.code,m.marks_obtained,c.max_marks,c.weight_percent FROM marks m JOIN assessment_components c ON c.id=m.component_id JOIN enrollments e ON e.id=m.enrollment_id WHERE m.enrollment_id=? AND e.student_id=? AND e.status<>'DROPPED' ORDER BY c.id",enrollmentId,identity().id()); }
    public Analysis analysis(Long semesterId) {
        List<Grade> grades=grades();
        List<SubjectMarks> result=subjects(semesterId).stream().map(s->{List<Mark> marks=marks(s.enrollmentId()); BigDecimal percent=AcademicCalculations.weighted(marks); Grade grade=AcademicCalculations.grade(percent,grades); return new SubjectMarks(s,marks,percent,marks.isEmpty() || grade==null ? null : grade.letter(),"PROVISIONAL_RECORDED_COMPONENTS_ONLY");}).toList();
        List<SubjectMarks> recorded=result.stream().filter(s->!s.components().isEmpty()).toList();
        BigDecimal max=recorded.stream().map(SubjectMarks::weightedPercent).max(BigDecimal::compareTo).orElse(null);
        BigDecimal min=recorded.stream().map(SubjectMarks::weightedPercent).min(BigDecimal::compareTo).orElse(null);
        return new Analysis(result,recorded.stream().filter(s->s.weightedPercent().compareTo(max)==0).map(s->s.subject().subjectId()).toList(),recorded.stream().filter(s->s.weightedPercent().compareTo(min)==0).map(s->s.subject().subjectId()).toList());
    }
    public Prediction predict(PredictionInput input) {
        List<Long> semesterIds=jdbc.queryForList("SELECT semester_id FROM enrollments WHERE id=? AND student_id=? AND status<>'DROPPED'",Long.class,input.enrollmentId(),identity().id());
        long semesterId=one(semesterIds,"Enrollment not found");
        List<Mark> recorded=marks(input.enrollmentId());
        Map<Long,Mark> projected=new LinkedHashMap<>(); recorded.forEach(m->projected.put(m.componentId(),m));
        Set<Long> seen=new HashSet<>();
        for (HypotheticalMark mark:input.marks()) {
            if (!seen.add(mark.componentId())) throw new ResponseStatusException(BAD_REQUEST,"Duplicate component");
            Component component=one(query(Component.class,"SELECT id,code,max_marks,weight_percent FROM assessment_components WHERE id=?",mark.componentId()),"Component not found");
            if (mark.marks().compareTo(component.maxMarks())>0 || mark.marks().signum()<0) throw new ResponseStatusException(BAD_REQUEST,"Marks outside component range");
            projected.put(component.id(),new Mark(input.enrollmentId(),component.id(),component.code(),mark.marks(),component.maxMarks(),component.weightPercent()));
        }
        BigDecimal percent=AcademicCalculations.weighted(new ArrayList<>(projected.values()));
        Grade grade=AcademicCalculations.grade(percent,grades());
        BigDecimal total=jdbc.queryForObject("SELECT COALESCE(SUM(m.marks_obtained),0) FROM marks m JOIN enrollments e ON e.id=m.enrollment_id WHERE e.student_id=? AND e.semester_id=? AND e.status<>'DROPPED'",BigDecimal.class,identity().id(),semesterId);
        total=total.subtract(recorded.stream().map(Mark::marksObtained).reduce(BigDecimal.ZERO,BigDecimal::add)).add(projected.values().stream().map(Mark::marksObtained).reduce(BigDecimal.ZERO,BigDecimal::add));
        return new Prediction(percent,grade==null?null:grade.letter(),grade==null?null:grade.gradePoints(),AcademicCalculations.gpa(total),"PROVISIONAL_RECORDED_AND_HYPOTHETICAL_COMPONENTS_ONLY");
    }
    public List<Trend> trend() { return semesters().stream().map(s->new Trend(s.id(),s.label(),gpa(s.id()))).toList(); }
    public Focus focus() { List<Exam> exams=exams(); List<Assignment> due=assignments(true); List<Attendance> alerts=attendance().stream().filter(a->a.held()>0 && a.percentage().compareTo(BigDecimal.valueOf(75))<0).toList(); long requests=requests().stream().filter(r->"REQUESTED".equals(r.status())).count(); return new Focus(identity().focusModeEnabled(),exams,due,alerts,requests,exams.size()+due.size()+alerts.size()+requests); }
    public Dashboard dashboard() { return new Dashboard(semesters().stream().filter(Semester::isCurrent).toList(),today(),assignments(true),attendance(),analysis(null),trend()); }
}
