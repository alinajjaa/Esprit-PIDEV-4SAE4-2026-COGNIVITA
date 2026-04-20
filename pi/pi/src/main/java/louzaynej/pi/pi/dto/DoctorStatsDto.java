package louzaynej.pi.pi.dto;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

public record DoctorStatsDto(
        Long medecinId,
        LocalDate from,
        LocalDate to,

        // KPI
        long total,
        long upcoming,
        long today,
        long uniquePatients,
        Map<String, Long> byStatus,
        double cancellationRate,

        // Timeseries + charts
        List<DayCount> rdvPerDay,
        List<HourCount> rdvPerHour,
        List<WeekdayCount> rdvPerWeekday,

        List<NameCount> topRooms,
        List<NameCount> topNurses,
        List<NameCount> topMedications,
        List<NameCount> topPatients
) {
    public record DayCount(LocalDate day, long count) {}
    public record HourCount(int hour, long count) {}
    public record WeekdayCount(int weekday, long count) {}
    public record NameCount(String name, long count) {}
}