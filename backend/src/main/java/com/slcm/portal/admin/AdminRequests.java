package com.slcm.portal.admin;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

/** Write contracts deliberately exclude server-owned identity and audit fields. */
public final class AdminRequests {
    private AdminRequests() {}
    public enum EnrollmentStatus { ENROLLED, DROPPED, COMPLETED }
    public enum AttendanceStatus { PRESENT, ABSENT, LEAVE }
    public enum ExamType { MID_TERM, END_TERM, PRACTICAL, VIVA }
    public enum ReviewStatus { APPROVED, REJECTED }
    public record Student(@NotBlank @Size(max=80) String username,
            @Size(min=8,max=72) String password, @NotNull Boolean enabled,
            @NotBlank @Size(max=30) String regNo, @NotBlank @Size(max=120) String fullName,
            @NotBlank @Email @Size(max=160) String email, @NotBlank @Size(max=120) String program,
            @NotBlank @Size(max=30) String section, @NotNull Boolean focusModeEnabled) {}
    public record Semester(@NotNull @Min(1) @Max(255) Integer semesterNo,
            @NotBlank @Size(max=40) String label, @NotNull LocalDate startDate,
            @NotNull LocalDate endDate, @NotNull Boolean isCurrent) {}
    public record Subject(@NotNull @Positive Long semesterId, @NotBlank @Size(max=20) String code,
            @NotBlank @Size(max=150) String name, @NotBlank @Size(max=40) String shortName,
            @NotNull @DecimalMin(value="0",inclusive=false) @Digits(integer=3,fraction=1) BigDecimal credits) {}
    public record Enrollment(@NotNull @Positive Long studentId, @NotNull @Positive Long subjectId,
            @NotNull @Positive Long semesterId, @NotNull EnrollmentStatus status, @Positive Long finalGradeId) {}
    public record Timetable(@NotNull @Positive Long subjectId, @NotNull @Min(1) @Max(7) Integer dayOfWeek,
            @NotNull LocalTime startTime, @NotNull LocalTime endTime, @NotBlank @Size(max=40) String room) {}
    public record Session(@NotNull @Positive Long slotId, @NotNull @Positive Long subjectId,
            @NotNull LocalDate sessionDate) {}
    public record Attendance(@NotNull @Positive Long enrollmentId, @NotNull AttendanceStatus status) {}
    public record AttendanceBatch(@NotEmpty @Size(max=1000) List<@NotNull @Valid Attendance> records) {}
    public record Component(@NotBlank @Size(max=30) String code,
            @NotNull @DecimalMin(value="0",inclusive=false) @Digits(integer=5,fraction=2) BigDecimal maxMarks,
            @NotNull @DecimalMin("0") @DecimalMax("100") @Digits(integer=3,fraction=2) BigDecimal weightPercent) {}
    public record Grade(@NotBlank @Size(max=5) String letter,
            @NotNull @DecimalMin("0") @DecimalMax("10") @Digits(integer=2,fraction=2) BigDecimal gradePoints,
            @NotNull @DecimalMin("0") @DecimalMax("100") @Digits(integer=3,fraction=2) BigDecimal minPercent,
            @NotNull @DecimalMin("0") @DecimalMax("100") @Digits(integer=3,fraction=2) BigDecimal maxPercent) {}
    public record Mark(@NotNull @Positive Long enrollmentId, @NotNull @Positive Long componentId,
            @NotNull @DecimalMin("0") @Digits(integer=5,fraction=2) BigDecimal marksObtained) {}
    public record Assignment(@NotNull @Positive Long subjectId, @NotBlank @Size(max=180) String title,
            @Size(max=16000) String description, @NotNull LocalDateTime dueAt,
            @NotNull @DecimalMin(value="0",inclusive=false) @Digits(integer=5,fraction=2) BigDecimal maxMarks) {}
    public record SubmissionReview(@NotNull @DecimalMin("0") @Digits(integer=5,fraction=2) BigDecimal marks) {}
    public record Exam(@NotNull @Positive Long subjectId, @NotNull @Positive Long semesterId,
            @NotNull ExamType examType, @NotNull LocalDate examDate, @NotNull LocalTime startTime,
            @NotBlank @Size(max=100) String venue) {}
    public record Syllabus(@Size(max=16000) String content, @Positive Long fileId) {}
    public record AttendanceReview(@NotNull ReviewStatus status, @Size(max=16000) String remarks) {}
    public record Setting(@NotBlank @Size(max=255) String value, @Size(max=255) String description) {}
}
