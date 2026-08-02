package com.example.dcs.sis.domain.student;

import com.example.dcs.sis.domain.academic.MonthInfo;
import com.example.dcs.sis.domain.academic.ScheduleEntry;

import java.util.List;


public record StudentSchedule(
    List<MonthInfo> listOfMonths,
    int count,
    List<ScheduleEntry> schedule,
    String errorCode
) {}