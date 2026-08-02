package com.example.dcs.sis.domain.student;

import java.util.List;

public record StudentEnrolments(
        int count,
        List<Enrolment> enrolmentList,
        String errorCode
) {
    public record Enrolment(
            String academicYear,
            String courseName,
            String curricularUnitName,
            int curricularUnitCode,
            String className,
            int curricularYear,
            int ects,
            String programme
    ) {
    }
}