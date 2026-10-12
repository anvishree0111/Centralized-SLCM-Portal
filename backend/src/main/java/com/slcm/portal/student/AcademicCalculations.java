package com.slcm.portal.student;

import java.math.*;
import java.time.*;
import java.util.*;
import static com.slcm.portal.student.AcademicDtos.*;

public final class AcademicCalculations {
    private AcademicCalculations() {}
    public static Attendance attendance(long subjectId, String name, long held, long attended, long remaining) {
        BigDecimal percentage = held == 0 ? BigDecimal.ZERO.setScale(2) : BigDecimal.valueOf(attended * 100).divide(BigDecimal.valueOf(held), 2, RoundingMode.HALF_UP);
        // Exact policy result; negative values indicate that 75% is unreachable.
        // Presentation clients may clamp the displayed number of usable leaves.
        long leaves = Math.floorDiv(4 * attended + remaining - 3 * held, 4);
        return new Attendance(subjectId, name, held, attended, percentage, Math.max(0, 3 * held - 4 * attended), remaining, leaves, 75);
    }
    public static long remaining(List<Slot> slots, LocalDate start, LocalDate end, LocalDateTime now, Set<LocalDate> holidays) {
        long count = 0;
        for (LocalDate date = start.isAfter(now.toLocalDate()) ? start : now.toLocalDate(); !date.isAfter(end); date = date.plusDays(1)) {
            if (holidays.contains(date)) continue;
            for (Slot slot : slots) if (slot.dayOfWeek() == date.getDayOfWeek().getValue() && date.atTime(slot.startTime()).isAfter(now)) count++;
        }
        return count;
    }
    public static BigDecimal weighted(List<Mark> marks) {
        return marks.stream().map(m -> m.marksObtained().multiply(m.weightPercent()).divide(m.maxMarks(), 8, RoundingMode.HALF_UP)).reduce(BigDecimal.ZERO, BigDecimal::add).setScale(2, RoundingMode.HALF_UP);
    }
    public static Grade grade(BigDecimal percent, List<Grade> grades) {
        return grades.stream().filter(g -> percent.compareTo(g.minPercent()) >= 0 && percent.compareTo(g.maxPercent()) <= 0).findFirst().orElse(null);
    }
    public static Gpa gpa(BigDecimal recordedMarks) {
        return new Gpa(recordedMarks.divide(BigDecimal.valueOf(5), 2, RoundingMode.HALF_UP), null, "PROVISIONAL", "Sum of recorded marks for non-dropped enrollments in this semester / 5; SGPA policy undefined");
    }
}
