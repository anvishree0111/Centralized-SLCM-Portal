package com.slcm.portal.student;

import static org.junit.jupiter.api.Assertions.*;
import java.math.*;
import java.time.*;
import java.util.*;
import org.junit.jupiter.api.Test;
import static com.slcm.portal.student.AcademicDtos.*;

class AcademicCalculationsTest {
    @Test void attendanceIsZeroSafeAndComputesNeededAndLeaves() {
        var empty = AcademicCalculations.attendance(1,"Math",0,0,4);
        assertEquals(BigDecimal.ZERO.setScale(2), empty.percentage());
        assertEquals(0, empty.needed());
        assertEquals(1, empty.leavesAllowed());
        var active = AcademicCalculations.attendance(1,"Math",10,6,4);
        assertEquals(new BigDecimal("60.00"), active.percentage());
        assertEquals(6, active.needed());
        assertEquals(-1, active.leavesAllowed());
    }
    @Test void cgpaPolicyIsExplicitAndRounded() {
        var gpa = AcademicCalculations.gpa(new BigDecimal("12.345"));
        assertEquals(new BigDecimal("2.47"), gpa.cgpa());
        assertNull(gpa.sgpa());
        assertEquals("PROVISIONAL", gpa.status());
    }
    @Test void futureOccurrencesExcludeNowAndHolidays() {
        var slot = new Slot(1,1,"Math",1,LocalTime.of(10,0),LocalTime.of(11,0),"A");
        long result=AcademicCalculations.remaining(List.of(slot),LocalDate.of(2026,1,5),LocalDate.of(2026,1,12),LocalDateTime.of(2026,1,5,9,0),Set.of(LocalDate.of(2026,1,12)));
        assertEquals(1,result);
    }
}
