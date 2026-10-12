package com.slcm.portal.student;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.http.MediaType;
import static com.slcm.portal.student.AcademicDtos.*;

@RestController
@Validated
@PreAuthorize("hasRole('STUDENT')")
public class StudentAcademicController {
    private final StudentAcademicService service;
    public StudentAcademicController(StudentAcademicService service) { this.service=service; }
    @GetMapping("/api/student/dashboard") public Dashboard dashboard() { return service.dashboard(); }
    @GetMapping("/api/student/semesters") public List<Semester> semesters() { return service.semesters(); }
    @GetMapping({"/api/student/semesters/{semesterId}","/api/student/semesters/{semesterId}/report"}) public SemesterDetail semester(@PathVariable @Positive long semesterId) { return service.semesterDetail(semesterId); }
    @GetMapping("/api/student/subjects") public List<Subject> subjects(@RequestParam(required=false) @Positive Long semesterId) { return service.subjects(semesterId); }
    @GetMapping("/api/student/schedule") public List<Slot> schedule() { return service.schedule(); }
    @GetMapping("/api/student/schedule/today") public List<TodaySlot> today() { return service.today(); }
    @GetMapping("/api/student/assignments") public List<Assignment> assignments(@RequestParam(defaultValue="false") boolean due) { return service.assignments(due); }
    @GetMapping("/api/student/assignments/{id}") public Assignment assignment(@PathVariable @Positive long id) { return service.assignment(id); }
    @PostMapping(value="/api/student/assignments/{id}/submissions", consumes=MediaType.APPLICATION_JSON_VALUE) public Assignment submit(@PathVariable @Positive long id,@Valid @RequestBody SubmissionInput input) { return service.submit(id,input); }
    @PostMapping(value="/api/student/assignments/{id}/submissions", consumes=MediaType.MULTIPART_FORM_DATA_VALUE) public Assignment submitMultipart(@PathVariable @Positive long id,@RequestPart(required=false) String note,@RequestPart MultipartFile file) { return service.submit(id,note,file); }
    @GetMapping("/api/student/exams") public List<Exam> exams() { return service.exams(); }
    @GetMapping("/api/student/subjects/{id}/syllabus") public Syllabus syllabus(@PathVariable @Positive long id) { return service.syllabus(id); }
    @GetMapping("/api/student/focus-mode") public Focus focus() { return service.focus(); }
    @PutMapping("/api/me/preferences") public Preferences preferences(@Valid @RequestBody Preferences input) { return service.preferences(input); }
    @GetMapping("/api/student/attendance/summary") public List<Attendance> attendance() { return service.attendance(); }
    @GetMapping("/api/student/attendance/{id}") public List<AttendanceEvent> history(@PathVariable @Positive long id) { return service.history(id); }
    @GetMapping("/api/student/attendance/requests") public List<AttendanceRequest> requests() { return service.requests(); }
    @PostMapping("/api/student/attendance-requests") public AttendanceRequest request(@Valid @RequestBody AttendanceRequestInput input) { return service.request(input); }
    @GetMapping("/api/student/grade-scale") public List<Grade> grades() { return service.grades(); }
    @GetMapping("/api/student/marks/analysis") public Analysis analysis(@RequestParam(required=false) @Positive Long semesterId) { return service.analysis(semesterId); }
    @PostMapping("/api/student/marks/predict") public Prediction predict(@Valid @RequestBody PredictionInput input) { return service.predict(input); }
    @GetMapping("/api/student/cgpa/trend") public List<Trend> trend() { return service.trend(); }
}
