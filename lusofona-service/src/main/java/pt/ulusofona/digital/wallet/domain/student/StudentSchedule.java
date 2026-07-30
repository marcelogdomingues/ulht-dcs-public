package pt.ulusofona.digital.wallet.domain.student;

import pt.ulusofona.digital.wallet.domain.academic.MonthInfo;
import pt.ulusofona.digital.wallet.domain.academic.ScheduleEntry;

import java.util.List;


public record StudentSchedule(
    List<MonthInfo> listOfMonths,
    int count,
    List<ScheduleEntry> schedule,
    String errorCode
) {}