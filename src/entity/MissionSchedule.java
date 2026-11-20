package entity;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Set;

/** Recurrence settings used to create individual Mission occurrences. */
public record MissionSchedule(
        int scheduleId,
        int flightPathId,
        LocalDate startDate,
        LocalTime startTime,
        String repeatFrequency,
        Set<DayOfWeek> repeatDays,
        String notificationPreference,
        String status) {

    public MissionSchedule {
        repeatDays = Set.copyOf(repeatDays);
    }
}
