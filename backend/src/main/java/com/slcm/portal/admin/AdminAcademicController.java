package com.slcm.portal.admin;

import com.slcm.portal.admin.AdminRequests.*;
import jakarta.validation.Valid;
import java.util.List;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

/** Admin academic API. All request DTOs are validated before service-level relationship checks. */
@RestController
@RequestMapping("/api/admin")
public class AdminAcademicController {
    private final AdminAcademicService service;
    public AdminAcademicController(AdminAcademicService service) { this.service=service; }
    @GetMapping("/students") public List<Map<String,Object>> students(@RequestParam(defaultValue="100") int limit,@RequestParam(defaultValue="0") int offset){return service.list(AdminResource.STUDENTS,limit,offset);}
    @PostMapping("/students") @ResponseStatus(HttpStatus.CREATED) public Map<String,Object> createStudent(@Valid @RequestBody Student r){return service.student(null,r);}
    @GetMapping("/students/{id}") public Map<String,Object> student(@PathVariable long id){return service.get(AdminResource.STUDENTS,id);}
    @PutMapping("/students/{id}") public Map<String,Object> updateStudent(@PathVariable long id,@Valid @RequestBody Student r){return service.student(id,r);}
    @DeleteMapping("/students/{id}") @ResponseStatus(HttpStatus.NO_CONTENT) public void deleteStudent(@PathVariable long id){service.delete(AdminResource.STUDENTS,id);}

    @GetMapping("/semesters") public List<Map<String,Object>> semesters(@RequestParam(defaultValue="100") int l,@RequestParam(defaultValue="0") int o){return service.list(AdminResource.SEMESTERS,l,o);}
    @PostMapping("/semesters") @ResponseStatus(HttpStatus.CREATED) public Map<String,Object> createSemester(@Valid @RequestBody Semester r){return service.semester(null,r);}
    @GetMapping("/semesters/{id}") public Map<String,Object> semester(@PathVariable long id){return service.get(AdminResource.SEMESTERS,id);}
    @PutMapping("/semesters/{id}") public Map<String,Object> updateSemester(@PathVariable long id,@Valid @RequestBody Semester r){return service.semester(id,r);}
    @DeleteMapping("/semesters/{id}") @ResponseStatus(HttpStatus.NO_CONTENT) public void deleteSemester(@PathVariable long id){service.delete(AdminResource.SEMESTERS,id);}

    @GetMapping("/subjects") public List<Map<String,Object>> subjects(@RequestParam(defaultValue="100") int l,@RequestParam(defaultValue="0") int o){return service.list(AdminResource.SUBJECTS,l,o);}
    @PostMapping("/subjects") @ResponseStatus(HttpStatus.CREATED) public Map<String,Object> createSubject(@Valid @RequestBody Subject r){return service.subject(null,r);}
    @GetMapping("/subjects/{id}") public Map<String,Object> subject(@PathVariable long id){return service.get(AdminResource.SUBJECTS,id);}
    @PutMapping("/subjects/{id}") public Map<String,Object> updateSubject(@PathVariable long id,@Valid @RequestBody Subject r){return service.subject(id,r);}
    @DeleteMapping("/subjects/{id}") @ResponseStatus(HttpStatus.NO_CONTENT) public void deleteSubject(@PathVariable long id){service.delete(AdminResource.SUBJECTS,id);}

    @GetMapping("/enrollments") public List<Map<String,Object>> enrollments(@RequestParam(defaultValue="100") int l,@RequestParam(defaultValue="0") int o){return service.list(AdminResource.ENROLLMENTS,l,o);}
    @PostMapping("/enrollments") @ResponseStatus(HttpStatus.CREATED) public Map<String,Object> createEnrollment(@Valid @RequestBody Enrollment r){return service.enrollment(null,r);}
    @PutMapping("/enrollments/{id}") public Map<String,Object> updateEnrollment(@PathVariable long id,@Valid @RequestBody Enrollment r){return service.enrollment(id,r);}
    @DeleteMapping("/enrollments/{id}") @ResponseStatus(HttpStatus.NO_CONTENT) public void deleteEnrollment(@PathVariable long id){service.delete(AdminResource.ENROLLMENTS,id);}

    @GetMapping("/timetable") public List<Map<String,Object>> timetable(@RequestParam(defaultValue="100") int l,@RequestParam(defaultValue="0") int o){return service.list(AdminResource.TIMETABLE,l,o);}
    @PostMapping("/timetable") @ResponseStatus(HttpStatus.CREATED) public Map<String,Object> createTimetable(@Valid @RequestBody Timetable r){return service.timetable(null,r);}
    @PutMapping("/timetable/{id}") public Map<String,Object> updateTimetable(@PathVariable long id,@Valid @RequestBody Timetable r){return service.timetable(id,r);}
    @DeleteMapping("/timetable/{id}") @ResponseStatus(HttpStatus.NO_CONTENT) public void deleteTimetable(@PathVariable long id){service.delete(AdminResource.TIMETABLE,id);}

    @GetMapping("/class-sessions") public List<Map<String,Object>> sessions(@RequestParam(defaultValue="100") int l,@RequestParam(defaultValue="0") int o){return service.list(AdminResource.SESSIONS,l,o);}
    @PostMapping("/class-sessions") @ResponseStatus(HttpStatus.CREATED) public Map<String,Object> createSession(@Valid @RequestBody Session r){return service.session(null,r);}
    @PutMapping("/class-sessions/{id}") public Map<String,Object> updateSession(@PathVariable long id,@Valid @RequestBody Session r){return service.session(id,r);}
    @DeleteMapping("/class-sessions/{id}") @ResponseStatus(HttpStatus.NO_CONTENT) public void deleteSession(@PathVariable long id){service.delete(AdminResource.SESSIONS,id);}
    @GetMapping("/class-sessions/{id}/attendance") public List<Map<String,Object>> getAttendance(@PathVariable long id){return service.attendance(id);}
    @PutMapping("/class-sessions/{id}/attendance") public List<Map<String,Object>> attendance(@PathVariable long id,@Valid @RequestBody AttendanceBatch r){return service.attendance(id,r);}

    @GetMapping("/assessment-components") public List<Map<String,Object>> components(@RequestParam(defaultValue="100") int l,@RequestParam(defaultValue="0") int o){return service.list(AdminResource.COMPONENTS,l,o);}
    @PostMapping("/assessment-components") @ResponseStatus(HttpStatus.CREATED) public Map<String,Object> createComponent(@Valid @RequestBody Component r){return service.component(null,r);}
    @PutMapping("/assessment-components/{id}") public Map<String,Object> updateComponent(@PathVariable long id,@Valid @RequestBody Component r){return service.component(id,r);}
    @DeleteMapping("/assessment-components/{id}") @ResponseStatus(HttpStatus.NO_CONTENT) public void deleteComponent(@PathVariable long id){service.delete(AdminResource.COMPONENTS,id);}
    @GetMapping("/grade-scale") public List<Map<String,Object>> grades(@RequestParam(defaultValue="100") int l,@RequestParam(defaultValue="0") int o){return service.list(AdminResource.GRADES,l,o);}
    @PostMapping("/grade-scale") @ResponseStatus(HttpStatus.CREATED) public Map<String,Object> createGrade(@Valid @RequestBody Grade r){return service.grade(null,r);}
    @PutMapping("/grade-scale/{id}") public Map<String,Object> updateGrade(@PathVariable long id,@Valid @RequestBody Grade r){return service.grade(id,r);}
    @DeleteMapping("/grade-scale/{id}") @ResponseStatus(HttpStatus.NO_CONTENT) public void deleteGrade(@PathVariable long id){service.delete(AdminResource.GRADES,id);}

    @GetMapping("/marks") public List<Map<String,Object>> marks(@RequestParam(defaultValue="100") int l,@RequestParam(defaultValue="0") int o){return service.list(AdminResource.MARKS,l,o);}
    @PostMapping("/marks") @ResponseStatus(HttpStatus.CREATED) public Map<String,Object> createMark(@Valid @RequestBody Mark r){return service.mark(r);}
    @PutMapping("/marks") public Map<String,Object> updateMark(@Valid @RequestBody Mark r){return service.mark(r);}
    @GetMapping("/assignments") public List<Map<String,Object>> assignments(@RequestParam(defaultValue="100") int l,@RequestParam(defaultValue="0") int o){return service.list(AdminResource.ASSIGNMENTS,l,o);}
    @PostMapping("/assignments") @ResponseStatus(HttpStatus.CREATED) public Map<String,Object> createAssignment(@Valid @RequestBody Assignment r){return service.assignment(null,r);}
    @PutMapping("/assignments/{id}") public Map<String,Object> updateAssignment(@PathVariable long id,@Valid @RequestBody Assignment r){return service.assignment(id,r);}
    @DeleteMapping("/assignments/{id}") @ResponseStatus(HttpStatus.NO_CONTENT) public void deleteAssignment(@PathVariable long id){service.delete(AdminResource.ASSIGNMENTS,id);}

    @GetMapping("/submissions") public List<Map<String,Object>> submissions(@RequestParam(defaultValue="100") int l,@RequestParam(defaultValue="0") int o){return service.list(AdminResource.SUBMISSIONS,l,o);}
    @PutMapping("/submissions/{id}/review") public Map<String,Object> reviewSubmission(@PathVariable long id,@Valid @RequestBody SubmissionReview r){return service.reviewSubmission(id,r);}
    @GetMapping("/exams") public List<Map<String,Object>> exams(@RequestParam(defaultValue="100") int l,@RequestParam(defaultValue="0") int o){return service.list(AdminResource.EXAMS,l,o);}
    @PostMapping("/exams") @ResponseStatus(HttpStatus.CREATED) public Map<String,Object> createExam(@Valid @RequestBody Exam r){return service.exam(null,r);}
    @PutMapping("/exams/{id}") public Map<String,Object> updateExam(@PathVariable long id,@Valid @RequestBody Exam r){return service.exam(id,r);}
    @DeleteMapping("/exams/{id}") @ResponseStatus(HttpStatus.NO_CONTENT) public void deleteExam(@PathVariable long id){service.delete(AdminResource.EXAMS,id);}
    @GetMapping("/syllabi/{subjectId}") public Map<String,Object> syllabus(@PathVariable long subjectId){return service.syllabus(subjectId);}
    @PostMapping("/syllabi/{subjectId}") @ResponseStatus(HttpStatus.CREATED) public Map<String,Object> createSyllabus(@PathVariable long subjectId,@Valid @RequestBody Syllabus r){return service.syllabus(subjectId,r);}
    @PutMapping("/syllabi/{subjectId}") public Map<String,Object> updateSyllabus(@PathVariable long subjectId,@Valid @RequestBody Syllabus r){return service.syllabus(subjectId,r);}
    @GetMapping("/attendance-requests") public List<Map<String,Object>> requests(@RequestParam(defaultValue="100") int l,@RequestParam(defaultValue="0") int o){return service.list(AdminResource.REQUESTS,l,o);}
    @PutMapping("/attendance-requests/{id}/review") public Map<String,Object> reviewRequest(@PathVariable long id,@Valid @RequestBody AttendanceReview r){return service.reviewAttendance(id,r);}
    @GetMapping("/settings") public List<Map<String,Object>> settings(){return service.settings();}
    @GetMapping("/settings/{key}") public Map<String,Object> setting(@PathVariable String key){return service.setting(key);}
    @PutMapping("/settings/{key}") public Map<String,Object> updateSetting(@PathVariable String key,@Valid @RequestBody Setting r){return service.setting(key,r);}
    @GetMapping("/audit-logs") public List<Map<String,Object>> audit(@RequestParam(defaultValue="100") int l,@RequestParam(defaultValue="0") int o){return service.list(AdminResource.AUDIT,l,o);}
}
