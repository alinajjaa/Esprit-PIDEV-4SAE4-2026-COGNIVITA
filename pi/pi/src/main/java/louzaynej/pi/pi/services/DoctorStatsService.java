package louzaynej.pi.pi.services;

import louzaynej.pi.pi.dto.DoctorStatsDto;
import louzaynej.pi.pi.repositories.RendezVousRepository;
import louzaynej.pi.pi.repositories.StatusCountView;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.*;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class DoctorStatsService {

    private final RendezVousRepository rdvRepo;

    public DoctorStatsService(RendezVousRepository rdvRepo) {
        this.rdvRepo = rdvRepo;
    }

    @Transactional(readOnly = true)
    public DoctorStatsDto getStats(Long medecinId, LocalDate from, LocalDate to) {
        // Normalisation: période [from 00:00, to 23:59:59]
        LocalDateTime start = from.atStartOfDay();
        LocalDateTime end = to.plusDays(1).atStartOfDay().minusNanos(1);

        long total = rdvRepo.countByMedecinIdAndDateHeureBetween(medecinId, start, end);

        LocalDateTime now = LocalDateTime.now();
        long upcoming = rdvRepo.countUpcoming(medecinId, now, end);

        LocalDate today = LocalDate.now();
        LocalDateTime todayStart = today.atStartOfDay();
        LocalDateTime todayEnd = today.plusDays(1).atStartOfDay().minusNanos(1);
        long todayCount = rdvRepo.countByMedecinIdAndDateHeureBetween(medecinId, todayStart, todayEnd);

        long uniquePatients = rdvRepo.countUniquePatients(medecinId, start, end);

        Map<String, Long> byStatus = rdvRepo.countByStatus(medecinId, start, end).stream()
                .collect(Collectors.toMap(StatusCountView::getStatus, StatusCountView::getCount));

        long cancelled = byStatus.getOrDefault("ANNULE", 0L);
        double cancellationRate = total == 0 ? 0.0 : (cancelled * 1.0) / total;

        List<DoctorStatsDto.DayCount> perDay = rdvRepo.countPerDay(medecinId, start, end).stream()
                .map(v -> new DoctorStatsDto.DayCount(v.getDay(), v.getCount()))
                .toList();

        List<DoctorStatsDto.HourCount> perHour = rdvRepo.countPerHour(medecinId, start, end).stream()
                .map(v -> new DoctorStatsDto.HourCount(v.getHour(), v.getCount()))
                .toList();

        List<DoctorStatsDto.WeekdayCount> perWeekday = rdvRepo.countPerWeekday(medecinId, start, end).stream()
                .map(v -> new DoctorStatsDto.WeekdayCount(v.getWeekday(), v.getCount()))
                .toList();

        List<DoctorStatsDto.NameCount> rooms = rdvRepo.topRooms(medecinId, start, end).stream()
                .map(v -> new DoctorStatsDto.NameCount(v.getName(), v.getCount()))
                .toList();

        List<DoctorStatsDto.NameCount> nurses = rdvRepo.topNurses(medecinId, start, end).stream()
                .map(v -> new DoctorStatsDto.NameCount(v.getName(), v.getCount()))
                .toList();

        List<DoctorStatsDto.NameCount> meds = rdvRepo.topMedications(medecinId, start, end).stream()
                .map(v -> new DoctorStatsDto.NameCount(v.getName(), v.getCount()))
                .toList();

        List<DoctorStatsDto.NameCount> topPatients = rdvRepo.topPatients(medecinId, start, end).stream()
                .limit(10)
                .map(v -> new DoctorStatsDto.NameCount(v.getName(), v.getCount()))
                .toList();

        return new DoctorStatsDto(
                medecinId, from, to,
                total, upcoming, todayCount, uniquePatients, byStatus, cancellationRate,
                perDay, perHour, perWeekday,
                rooms, nurses, meds, topPatients
        );
    }
}
