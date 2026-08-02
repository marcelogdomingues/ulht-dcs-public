package com.example.dcs.sis.domain.student;

import java.util.List;

public record StudentGrades(
        List<GradeList> gradeList,
        String errorCode
) {
    public record GradeList(
            String academicYear,
            List<Grade> grades
    ) {
        public record Grade(
                String academicYear,
                int curricularYear,
                String curricularUnitName,
                String evaluationName,
                String evaluationStatus,
                String grade,
                int ects
        ) {
        }
    }
}