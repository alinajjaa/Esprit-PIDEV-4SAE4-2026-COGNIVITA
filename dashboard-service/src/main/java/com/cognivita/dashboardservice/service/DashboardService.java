package com.cognivita.dashboardservice.service;

import com.cognivita.dashboardservice.dto.AlertTypePressureDto;
import com.cognivita.dashboardservice.dto.AlertTrendDto;
import com.cognivita.dashboardservice.dto.PatientSafetyBurdenDto;
import com.cognivita.dashboardservice.dto.AppointmentComplianceDto;
import com.cognivita.dashboardservice.dto.MmseMonthlySeverityDto;
import com.cognivita.dashboardservice.repository.AppointmentAnalyticsRepository;
import com.cognivita.dashboardservice.repository.MmseAnalyticsRepository;
import com.cognivita.dashboardservice.repository.TrackingAlertAnalyticsRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class DashboardService {

    private final TrackingAlertAnalyticsRepository trackingAlertAnalyticsRepository;
    private final AppointmentAnalyticsRepository appointmentAnalyticsRepository;
    private final MmseAnalyticsRepository mmseAnalyticsRepository;

    public DashboardService(TrackingAlertAnalyticsRepository trackingAlertAnalyticsRepository,
                            AppointmentAnalyticsRepository appointmentAnalyticsRepository,
                            MmseAnalyticsRepository mmseAnalyticsRepository) {
        this.trackingAlertAnalyticsRepository = trackingAlertAnalyticsRepository;
        this.appointmentAnalyticsRepository = appointmentAnalyticsRepository;
        this.mmseAnalyticsRepository = mmseAnalyticsRepository;
    }

    @Transactional(readOnly = true)
    public List<AlertTypePressureDto> getAlertPressure(LocalDateTime from, LocalDateTime to) {
        return trackingAlertAnalyticsRepository.getAlertTypePressure(from, to);
    }

    @Transactional(readOnly = true)
    public List<PatientSafetyBurdenDto> getPatientSafetyRanking(LocalDateTime from, LocalDateTime to) {
        return trackingAlertAnalyticsRepository.getPatientSafetyRanking(from, to);
    }

    @Transactional(readOnly = true)
    public List<AlertTrendDto> getAlertTrend(LocalDateTime from, LocalDateTime to) {
        return trackingAlertAnalyticsRepository.getAlertTrend(from, to);
    }

    @Transactional(readOnly = true)
    public List<AppointmentComplianceDto> getAppointmentCompliance(LocalDateTime from, LocalDateTime to) {
        return appointmentAnalyticsRepository.getAppointmentCompliance(from, to);
    }

    @Transactional(readOnly = true)
    public List<MmseMonthlySeverityDto> getMmseMonthlySeverity(LocalDate fromDate, LocalDate toDate) {
        return mmseAnalyticsRepository.getMmseMonthlySeverity(fromDate, toDate);
    }
}
