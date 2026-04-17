package com.cognivita.dashboardservice.controller;

import com.cognivita.dashboardservice.dto.AlertTypePressureDto;
import com.cognivita.dashboardservice.dto.AlertTrendDto;
import com.cognivita.dashboardservice.dto.AppointmentComplianceDto;
import com.cognivita.dashboardservice.dto.MmseMonthlySeverityDto;
import com.cognivita.dashboardservice.dto.PatientSafetyBurdenDto;
import com.cognivita.dashboardservice.service.DashboardService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/dashboard")
public class DashboardController {

    private final DashboardService dashboardService;

    public DashboardController(DashboardService dashboardService) {
        this.dashboardService = dashboardService;
    }

    @GetMapping("/alerts/pressure")
    public List<AlertTypePressureDto> getAlertPressure(
            @RequestParam("from")
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
            @RequestParam("to")
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to
    ) {
        return dashboardService.getAlertPressure(from, to);
    }

    @GetMapping("/alerts/ranking")
    public List<PatientSafetyBurdenDto> getPatientSafetyRanking(
            @RequestParam("from")
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
            @RequestParam("to")
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to
    ) {
        return dashboardService.getPatientSafetyRanking(from, to);
    }

    @GetMapping("/alerts/trend")
    public List<AlertTrendDto> getAlertTrend(
            @RequestParam("from")
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
            @RequestParam("to")
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to
    ) {
        return dashboardService.getAlertTrend(from, to);
    }

    @GetMapping("/appointments/compliance")
    public List<AppointmentComplianceDto> getAppointmentCompliance(
            @RequestParam("from")
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
            @RequestParam("to")
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to
    ) {
        return dashboardService.getAppointmentCompliance(from, to);
    }

    @GetMapping("/mmse/severity")
    public List<MmseMonthlySeverityDto> getMmseMonthlySeverity(
            @RequestParam("fromDate")
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
            @RequestParam("toDate")
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate
    ) {
        return dashboardService.getMmseMonthlySeverity(fromDate, toDate);
    }
}
