package com.example.dcs.sis.domain.academic;

public record ClassSchedule(
        String curricularUnitName,
        String className,
        String startTime,
        String inttendedDuration,
        String roomName,
        String teacherName,
        String classType,
        Integer classTypeCode // Use Integer to allow for null if not always present
) {}
