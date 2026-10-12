package com.slcm.portal.student;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.*;
import java.util.List;

public final class AcademicDtos {
    private AcademicDtos() {}
    public record Identity(long id, long userId, boolean focusModeEnabled) {}
    public record Semester(long id, int semesterNo, String label, LocalDate startDate, LocalDate endDate, boolean isCurrent) {}
    public record Subject(long enrollmentId, long subjectId, long semesterId, String code, String name, BigDecimal credits, String status, String finalGrade) {}
    public record SemesterDetail(Semester semester, List<Subject> subjects, Gpa performance) {}
    public record Slot(long id, long subjectId, String subjectName, int dayOfWeek, LocalTime startTime, LocalTime endTime, String room) {}
    public record TodaySlot(Slot slot, LocalDate date, String attendanceStatus) {}
    public record Assignment(long id, long subjectId, String title, String description, LocalDateTime dueAt, BigDecimal maxMarks, Long submissionId, Long fileId, String note, String status, LocalDateTime submittedAt, BigDecimal marks) {}
    public record SubmissionInput(@Size(max=10000) String note, @Positive Long fileId) {}
    public record Exam(long id, long subjectId, long semesterId, String subjectName, String examType, LocalDate examDate, LocalTime startTime, String venue) {}
    public record Syllabus(long subjectId, String content, Long fileId, LocalDateTime updatedAt) {}
    public record Preferences(@NotNull Boolean focusModeEnabled) {}
    public record AttendanceEvent(long sessionId, LocalDate sessionDate, LocalTime startTime, LocalTime endTime, String status) {}
    public record Attendance(long subjectId, String subjectName, long held, long attended, BigDecimal percentage, long needed, long remaining, long leavesAllowed, int thresholdPercent) {}
    public record AttendanceRequest(long id, long subjectId, String type, String reason, String status, LocalDateTime reviewedAt, String remarks, LocalDateTime createdAt) {}
    public record AttendanceRequestInput(@Positive long subjectId, @NotBlank @Pattern(regexp="UNDERTAKING|CONDONATION") String type, @NotBlank @Size(max=10000) String reason) {}
    public record Grade(long id, String letter, BigDecimal gradePoints, BigDecimal minPercent, BigDecimal maxPercent) {}
    public record Component(long id, String code, BigDecimal maxMarks, BigDecimal weightPercent) {}
    public record Mark(long enrollmentId, long componentId, String code, BigDecimal marksObtained, BigDecimal maxMarks, BigDecimal weightPercent) {}
    public record SubjectMarks(Subject subject, List<Mark> components, BigDecimal weightedPercent, String grade, String status) {}
    public record Analysis(List<SubjectMarks> subjects, List<Long> strongestSubjectIds, List<Long> weakestSubjectIds) {}
    public record HypotheticalMark(@Positive long componentId, @NotNull @DecimalMin("0") BigDecimal marks) {}
    public record PredictionInput(@Positive long enrollmentId, @NotEmpty List<@Valid HypotheticalMark> marks) {}
    public record Prediction(BigDecimal weightedPercent, String grade, BigDecimal gradePoints, Gpa semesterGpa, String status) {}
    @com.fasterxml.jackson.annotation.JsonInclude(com.fasterxml.jackson.annotation.JsonInclude.Include.ALWAYS)
    public record Gpa(BigDecimal cgpa, BigDecimal sgpa, String status, String policy) {}
    public record Trend(long semesterId, String label, Gpa performance) {}
    public record Focus(boolean enabled, List<Exam> exams, List<Assignment> dueAssignments, List<Attendance> attendanceAlerts, long pendingAttendanceRequests, long bellCount) {}
    public record Dashboard(List<Semester> currentSemesters, List<TodaySlot> schedule, List<Assignment> dueAssignments, List<Attendance> attendance, Analysis performance, List<Trend> gpa) {}
}
