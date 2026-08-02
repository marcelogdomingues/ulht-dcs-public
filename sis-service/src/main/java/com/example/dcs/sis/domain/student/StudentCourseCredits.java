package com.example.dcs.sis.domain.student;

public record StudentCourseCredits(
    int courseCredits,
    int obtainedCredits,
    double averageGrade,
    int averageGradeRound,
    String errorCode
) {}