package pt.ulusofona.digital.wallet.domain.academic;

import java.util.List;

public record ScheduleEntry(
        long occupationDate, // Use long for Unix timestamps
        int occupationMonth,
        String dayOfWeek,
        Integer dayOfWeekCode, // Use Integer to allow for null if not always present
        String scheduleType,
        List<ClassSchedule> schedule // This 'schedule' refers to the list of classes
) {}