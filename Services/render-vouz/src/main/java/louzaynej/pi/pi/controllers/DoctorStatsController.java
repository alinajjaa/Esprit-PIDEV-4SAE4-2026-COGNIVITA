package louzaynej.pi.pi.controllers;

import louzaynej.pi.pi.dto.DoctorStatsDto;
import louzaynej.pi.pi.services.DoctorStatsService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/doctors")
public class DoctorStatsController {

    private final DoctorStatsService statsService;

    public DoctorStatsController(DoctorStatsService statsService) {
        this.statsService = statsService;
    }

    @GetMapping("/{medecinId}/stats")
    public DoctorStatsDto stats(
            @PathVariable Long medecinId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to
    ) {
        return statsService.getStats(medecinId, from, to);
    }
}