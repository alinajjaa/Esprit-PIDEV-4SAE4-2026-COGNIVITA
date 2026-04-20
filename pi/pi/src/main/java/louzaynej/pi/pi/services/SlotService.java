package louzaynej.pi.pi.services;

import louzaynej.pi.pi.dto.SlotDto;
import louzaynej.pi.pi.model.RendezVous;
import louzaynej.pi.pi.repositories.RendezVousRepository;
import org.springframework.stereotype.Service;

import java.time.*;
import java.util.ArrayList;
import java.util.List;

@Service
public class SlotService {

    private final RendezVousRepository rendezVousRepository;
    private static final Duration RDV_DURATION = Duration.ofMinutes(30);

    public SlotService(RendezVousRepository rendezVousRepository) {
        this.rendezVousRepository = rendezVousRepository;
    }

    public List<SlotDto> nextAvailableSlots(Long medecinId, LocalDateTime from, int count) {
        if (count <= 0) count = 3;

        LocalDateTime startSearch = (from != null) ? from : LocalDateTime.now();

        // Round to next 30-min boundary
        startSearch = roundUpToSlot(startSearch, 30);

        // Look ahead window (e.g. 14 days) to keep DB query light
        LocalDateTime endSearch = startSearch.plusDays(14);

        // Load nearby appointments once
        List<RendezVous> existing =
                rendezVousRepository.findByMedecinIdAndDateHeureBetween(medecinId, startSearch.minusMinutes(60), endSearch);

        List<SlotDto> result = new ArrayList<>();
        LocalDateTime cursor = startSearch;

        while (result.size() < count && cursor.isBefore(endSearch)) {
            if (isWithinWorkingHours(cursor)) {
                LocalDateTime end = cursor.plus(RDV_DURATION);

                if (!overlaps(existing, cursor, end)) {
                    result.add(new SlotDto(cursor, end));
                }
            }
            cursor = cursor.plusMinutes(30);
        }

        return result;
    }

    private boolean overlaps(List<RendezVous> existing, LocalDateTime start, LocalDateTime end) {
        for (RendezVous r : existing) {
            LocalDateTime eStart = r.getDateHeure();
            LocalDateTime eEnd = eStart.plus(RDV_DURATION);

            if (start.isBefore(eEnd) && end.isAfter(eStart)) return true;
        }
        return false;
    }

    private boolean isWithinWorkingHours(LocalDateTime t) {
        LocalTime time = t.toLocalTime();
        return !time.isBefore(LocalTime.of(8, 0)) && time.isBefore(LocalTime.of(17, 0));
    }

    private LocalDateTime roundUpToSlot(LocalDateTime dt, int slotMinutes) {
        int minute = dt.getMinute();
        int mod = minute % slotMinutes;
        if (mod == 0 && dt.getSecond() == 0 && dt.getNano() == 0) return dt.withSecond(0).withNano(0);

        int add = slotMinutes - mod;
        return dt.plusMinutes(add).withSecond(0).withNano(0);
    }
}